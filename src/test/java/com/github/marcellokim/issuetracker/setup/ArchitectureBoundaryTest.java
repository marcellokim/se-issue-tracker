package com.github.marcellokim.issuetracker.setup;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("architecture boundary rules")
class ArchitectureBoundaryTest {

    private static final Path MAIN_SOURCE_ROOT = Path.of("src/main/java");
    private static final String ROOT_PACKAGE = "com.github.marcellokim.issuetracker";
    private static final Path ROOT_PACKAGE_PATH = MAIN_SOURCE_ROOT.resolve(ROOT_PACKAGE.replace('.', '/'));
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^import\\s+(?:static\\s+)?([\\w.]+)(?:\\.\\*)?;");

    /*
     * Temporary architecture debt marker: later clean-code slices must remove each exception
     * when that flow moves behind the intended service/presenter boundary.
     */
    private static final Set<AllowedImport> TEMPORARY_ALLOWED_IMPORTS = Set.of(
            new AllowedImport("ui/DemoDashboardPresenter.java", ROOT_PACKAGE + ".repository.IssueRepository"),
            new AllowedImport("ui/DemoDashboardPresenter.java", ROOT_PACKAGE + ".repository.ProjectRepository"),
            new AllowedImport("ui/DemoDashboardPresenter.java", ROOT_PACKAGE + ".repository.StatisticsRepository"),
            new AllowedImport("ui/DemoDashboardPresenter.java", ROOT_PACKAGE + ".repository.UserRepository")
    );

    @Test
    @DisplayName("domain package does not depend on outer layers")
    void domainDoesNotDependOnOuterLayers() throws IOException {
        assertNoForbiddenImports(
                "domain",
                Set.of(
                        ROOT_PACKAGE + ".controller",
                        ROOT_PACKAGE + ".service",
                        ROOT_PACKAGE + ".repository",
                        ROOT_PACKAGE + ".persistence",
                        ROOT_PACKAGE + ".ui",
                        ROOT_PACKAGE + ".config",
                        ROOT_PACKAGE + ".technical"
                )
        );
    }

    @Test
    @DisplayName("service package depends on repository contracts, not JDBC implementations")
    void servicesDoNotDependOnJdbcImplementations() throws IOException {
        assertNoForbiddenImports(
                "service",
                Set.of(
                        ROOT_PACKAGE + ".controller",
                        ROOT_PACKAGE + ".persistence",
                        ROOT_PACKAGE + ".ui",
                        ROOT_PACKAGE + ".config"
                )
        );
    }

    @Test
    @DisplayName("controllers delegate to services instead of repositories or persistence")
    void controllersDoNotDependOnRepositoriesOrPersistence() throws IOException {
        assertNoForbiddenImports(
                "controller",
                Set.of(
                        ROOT_PACKAGE + ".repository",
                        ROOT_PACKAGE + ".persistence",
                        ROOT_PACKAGE + ".ui"
                )
        );
    }

    @Test
    @DisplayName("ui package does not depend directly on repositories or persistence")
    void uiDoesNotDependOnRepositoriesOrPersistence() throws IOException {
        assertNoForbiddenImports(
                "ui",
                Set.of(
                        ROOT_PACKAGE + ".repository",
                        ROOT_PACKAGE + ".persistence"
                )
        );
    }

    private static void assertNoForbiddenImports(String packageSegment, Set<String> forbiddenPrefixes)
            throws IOException {
        List<Violation> violations = new ArrayList<>();
        for (JavaSource source : productionSources(packageSegment)) {
            for (String importedType : source.importedTypes()) {
                if (isForbidden(importedType, forbiddenPrefixes)
                        && !TEMPORARY_ALLOWED_IMPORTS.contains(
                                new AllowedImport(source.relativePath(), importedType))) {
                    violations.add(new Violation(source.relativePath(), importedType));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Forbidden architecture imports found:%n%s".formatted(formatViolations(violations))
        );
    }

    private static boolean isForbidden(String importedType, Set<String> forbiddenPrefixes) {
        for (String forbiddenPrefix : forbiddenPrefixes) {
            if (importedType.equals(forbiddenPrefix) || importedType.startsWith(forbiddenPrefix + ".")) {
                return true;
            }
        }
        return false;
    }

    private static List<JavaSource> productionSources(String packageSegment) throws IOException {
        Path packagePath = ROOT_PACKAGE_PATH.resolve(packageSegment);
        try (Stream<Path> paths = Files.walk(packagePath)) {
            return paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(ArchitectureBoundaryTest::readJavaSource)
                    .toList();
        }
    }

    private static JavaSource readJavaSource(Path path) {
        try {
            List<String> lines = Files.readAllLines(path);
            return new JavaSource(
                    ROOT_PACKAGE_PATH.relativize(path).toString().replace('\\', '/'),
                    importedTypes(lines)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read Java source: " + path, exception);
        }
    }

    private static List<String> importedTypes(List<String> lines) {
        List<String> imports = new ArrayList<>();
        for (String line : lines) {
            Matcher matcher = IMPORT_PATTERN.matcher(line.strip());
            if (matcher.matches()) {
                imports.add(matcher.group(1));
            }
        }
        return imports;
    }

    private static String formatViolations(List<Violation> violations) {
        StringBuilder message = new StringBuilder();
        for (Violation violation : violations) {
            message.append(System.lineSeparator())
                    .append("- ")
                    .append(violation.relativePath())
                    .append(" imports ")
                    .append(violation.importedType());
        }
        return message.toString();
    }

    record AllowedImport(String relativePath, String importedType) {
    }

    record JavaSource(String relativePath, List<String> importedTypes) {
    }

    record Violation(String relativePath, String importedType) {
    }
}
