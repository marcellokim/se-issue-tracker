package com.github.marcellokim.issuetracker.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.persistence.jdbc.JdbcDashboardSummaryRepository;
import com.github.marcellokim.issuetracker.persistence.jdbc.JdbcRepositoryFactory;
import com.github.marcellokim.issuetracker.persistence.jdbc.JdbcUserRepository;
import com.github.marcellokim.issuetracker.repository.DashboardSummaryRepository;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.service.CommentIdProvider;
import com.github.marcellokim.issuetracker.service.CurrentUserSession;
import com.github.marcellokim.issuetracker.service.IssueIdProvider;
import com.github.marcellokim.issuetracker.service.PasswordHashing;
import com.github.marcellokim.issuetracker.technical.CommentIdGenerator;
import com.github.marcellokim.issuetracker.technical.IssueIdGenerator;
import com.github.marcellokim.issuetracker.technical.PasswordHasher;
import com.github.marcellokim.issuetracker.technical.SessionStore;
import com.github.marcellokim.issuetracker.technical.SystemClock;
import java.io.IOException;
import java.lang.reflect.Constructor;
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

    private static final Set<AllowedReference> ALLOWED_REFERENCES = Set.of();

    @Test
    @DisplayName("domain package does not depend on outer layers")
    void domainDoesNotDependOnOuterLayers() throws IOException {
        assertNoForbiddenReferences(
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
        assertNoForbiddenReferences(
                "service",
                Set.of(
                        ROOT_PACKAGE + ".controller",
                        ROOT_PACKAGE + ".persistence",
                        ROOT_PACKAGE + ".ui",
                        ROOT_PACKAGE + ".config",
                        ROOT_PACKAGE + ".technical"
                )
        );
    }

    @Test
    @DisplayName("technical implementations satisfy service ports")
    void technicalImplementationsSatisfyServicePorts() {
        assertTrue(Clock.class.isAssignableFrom(SystemClock.class));
        assertTrue(CommentIdProvider.class.isAssignableFrom(CommentIdGenerator.class));
        assertTrue(IssueIdProvider.class.isAssignableFrom(IssueIdGenerator.class));
        assertTrue(CurrentUserSession.class.isAssignableFrom(SessionStore.class));
        assertTrue(PasswordHashing.class.isAssignableFrom(PasswordHasher.class));
    }

    @Test
    @DisplayName("domain and service packages keep runtime sources behind ports")
    void domainAndServicesKeepRuntimeSourcesBehindPorts() throws IOException {
        assertNoForbiddenSourceText(
                Set.of("domain", "service"),
                Set.of(
                        "LocalDateTime.now(",
                        "Instant.now(",
                        "System.currentTimeMillis(",
                        "UUID.randomUUID(",
                        "Math.random(",
                        "new Random("
                )
        );
    }

    @Test
    @DisplayName("runtime source scan ignores comments")
    void runtimeSourceScanIgnoresComments() {
        JavaSource source = new JavaSource(
                "domain/Sample.java",
                List.of(
                        "package com.github.marcellokim.issuetracker.domain;",
                        "// UUID.randomUUID( is documentation only.",
                        "/* LocalDateTime.now( belongs in the example text.",
                        " * System.currentTimeMillis( is not executable here.",
                        " */",
                        "final class Sample {",
                        "    void method() {",
                        "        String value = \"safe\";",
                        "    }",
                        "}"
                ),
                List.of()
        );

        List<Violation> violations = runtimeSourceViolations(
                source,
                Set.of("UUID.randomUUID(", "LocalDateTime.now(", "System.currentTimeMillis(")
        );

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("runtime source scan reports executable calls")
    void runtimeSourceScanReportsExecutableCalls() {
        JavaSource source = new JavaSource(
                "domain/Sample.java",
                List.of(
                        "package com.github.marcellokim.issuetracker.domain;",
                        "final class Sample {",
                        "    Object id() {",
                        "        return UUID.randomUUID();",
                        "    }",
                        "}"
                ),
                List.of()
        );

        List<Violation> violations = runtimeSourceViolations(source, Set.of("UUID.randomUUID("));

        assertEquals(1, violations.size());
        assertEquals(4, violations.get(0).lineNumber());
    }

    @Test
    @DisplayName("jdbc user repository depends on password hashing port")
    void jdbcUserRepositoryDependsOnPasswordHashingPort() throws NoSuchMethodException {
        Constructor<JdbcUserRepository> repositoryConstructor = JdbcUserRepository.class.getConstructor(
                DatabaseConnectionProvider.class,
                PasswordHashing.class
        );
        Constructor<JdbcRepositoryFactory> factoryConstructor = JdbcRepositoryFactory.class.getConstructor(
                DatabaseConnectionProvider.class,
                PasswordHashing.class
        );

        assertEquals(PasswordHashing.class, repositoryConstructor.getParameterTypes()[1]);
        assertEquals(PasswordHashing.class, factoryConstructor.getParameterTypes()[1]);
    }

    @Test
    @DisplayName("jdbc repositories do not create password hashing implementations")
    void jdbcRepositoriesDoNotCreatePasswordHasherImplementation() throws IOException {
        assertNoSourceText(
                ROOT_PACKAGE_PATH.resolve("persistence/jdbc/JdbcRepositoryFactory.java"),
                ROOT_PACKAGE + ".technical.PasswordHasher",
                "new PasswordHasher");
        assertNoSourceText(
                ROOT_PACKAGE_PATH.resolve("persistence/jdbc/JdbcUserRepository.java"),
                ROOT_PACKAGE + ".technical.PasswordHasher",
                "new PasswordHasher");
    }

    @Test
    @DisplayName("jdbc repository factory exposes dashboard summary repository")
    void jdbcRepositoryFactoryExposesDashboardSummaryRepository() {
        DatabaseConnectionProvider connectionProvider = () -> {
            throw new AssertionError("Connection should not be opened while creating repositories.");
        };
        JdbcRepositoryFactory factory = new JdbcRepositoryFactory(connectionProvider, new PasswordHasher());
        DashboardSummaryRepository repository = factory.dashboardSummaries();

        assertTrue(repository instanceof JdbcDashboardSummaryRepository);
    }

    @Test
    @DisplayName("controllers delegate to services instead of repositories or persistence")
    void controllersDoNotDependOnRepositoriesOrPersistence() throws IOException {
        assertNoForbiddenReferences(
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
        assertNoForbiddenReferences(
                "ui",
                Set.of(
                        ROOT_PACKAGE + ".repository",
                        ROOT_PACKAGE + ".persistence"
                )
        );
    }

    @Test
    @DisplayName("cli package avoids repository and persistence imports once it exists")
    void cliPackageAvoidsRepositoriesOrPersistenceOncePresent() throws IOException {
        assertNoForbiddenReferences(
                "cli",
                Set.of(
                        ROOT_PACKAGE + ".repository",
                        ROOT_PACKAGE + ".persistence"
                )
        );
    }

    private static void assertNoForbiddenReferences(String packageSegment, Set<String> forbiddenPrefixes)
            throws IOException {
        List<Violation> violations = new ArrayList<>();
        for (JavaSource source : productionSources(packageSegment)) {
            for (SourceReference importedType : source.importedTypes()) {
                if (isForbidden(importedType.reference(), forbiddenPrefixes)
                        && !ALLOWED_REFERENCES.contains(
                                new AllowedReference(source.relativePath(), importedType.reference()))) {
                    violations.add(new Violation(
                            source.relativePath(),
                            importedType.lineNumber(),
                            "imports",
                            importedType.reference()
                    ));
                }
            }
            for (SourceReference reference : forbiddenFullyQualifiedReferences(source, forbiddenPrefixes)) {
                if (!ALLOWED_REFERENCES.contains(
                        new AllowedReference(source.relativePath(), reference.reference()))) {
                    violations.add(new Violation(
                            source.relativePath(),
                            reference.lineNumber(),
                            "references",
                            reference.reference()
                    ));
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Forbidden architecture references found:%n%s".formatted(formatViolations(violations))
        );
    }

    private static void assertNoSourceText(Path sourcePath, String... forbiddenTexts) throws IOException {
        String source = Files.readString(sourcePath);
        for (String forbiddenText : forbiddenTexts) {
            assertTrue(
                    !source.contains(forbiddenText),
                    () -> sourcePath + " must not contain " + forbiddenText
            );
        }
    }

    private static void assertNoForbiddenSourceText(Set<String> packageSegments, Set<String> forbiddenTexts)
            throws IOException {
        List<Violation> violations = new ArrayList<>();
        for (String packageSegment : packageSegments) {
            for (JavaSource source : productionSources(packageSegment)) {
                violations.addAll(runtimeSourceViolations(source, forbiddenTexts));
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Forbidden runtime sources found:%n%s".formatted(formatViolations(violations))
        );
    }

    private static List<Violation> runtimeSourceViolations(JavaSource source, Set<String> forbiddenTexts) {
        List<Violation> violations = new ArrayList<>();
        boolean inBlockComment = false;
        for (int index = 0; index < source.lines().size(); index++) {
            CodeLine codeLine = executablePart(source.lines().get(index), inBlockComment);
            inBlockComment = codeLine.inBlockComment();
            for (String forbiddenText : forbiddenTexts) {
                if (codeLine.text().contains(forbiddenText)) {
                    violations.add(new Violation(
                            source.relativePath(),
                            index + 1,
                            "uses runtime source",
                            forbiddenText
                    ));
                }
            }
        }
        return violations;
    }

    private static CodeLine executablePart(String line, boolean startsInBlockComment) {
        StringBuilder code = new StringBuilder();
        boolean inBlockComment = startsInBlockComment;
        int cursor = 0;
        while (cursor < line.length()) {
            if (inBlockComment) {
                int commentEnd = line.indexOf("*/", cursor);
                if (commentEnd < 0) {
                    return new CodeLine(code.toString(), true);
                }
                cursor = commentEnd + 2;
                inBlockComment = false;
                continue;
            }

            int lineCommentStart = line.indexOf("//", cursor);
            int blockCommentStart = line.indexOf("/*", cursor);
            if (lineCommentStart < 0 && blockCommentStart < 0) {
                code.append(line.substring(cursor));
                break;
            }
            if (lineCommentStart >= 0 && (blockCommentStart < 0 || lineCommentStart < blockCommentStart)) {
                code.append(line, cursor, lineCommentStart);
                break;
            }
            code.append(line, cursor, blockCommentStart);
            cursor = blockCommentStart + 2;
            inBlockComment = true;
        }
        return new CodeLine(code.toString(), inBlockComment);
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
        Path packagePath = MAIN_SOURCE_ROOT
                .resolve(ROOT_PACKAGE.replace('.', '/'))
                .resolve(packageSegment);
        if (!Files.exists(packagePath)) {
            if ("cli".equals(packageSegment) || "ui".equals(packageSegment)) {
                return List.of();
            }
            throw new AssertionError("Production package is missing: " + packageSegment);
        }
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
                    lines,
                    importedTypes(lines)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read Java source: " + path, exception);
        }
    }

    private static List<SourceReference> importedTypes(List<String> lines) {
        List<SourceReference> imports = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            Matcher matcher = IMPORT_PATTERN.matcher(line.strip());
            if (matcher.matches()) {
                imports.add(new SourceReference(index + 1, matcher.group(1)));
            }
        }
        return imports;
    }

    private static List<SourceReference> forbiddenFullyQualifiedReferences(
            JavaSource source,
            Set<String> forbiddenPrefixes
    ) {
        List<SourceReference> references = new ArrayList<>();
        for (int index = 0; index < source.lines().size(); index++) {
            String line = source.lines().get(index).strip();
            if (isDeclarationLine(line)) {
                continue;
            }
            for (String forbiddenPrefix : forbiddenPrefixes) {
                if (line.contains(forbiddenPrefix + ".")) {
                    // import 없이 FQCN을 직접 쓰는 우회도 같은 경계 위반이므로 본문 라인까지 검사함.
                    references.add(new SourceReference(index + 1, forbiddenPrefix));
                }
            }
        }
        return references;
    }

    private static boolean isDeclarationLine(String line) {
        return line.startsWith("package ") || line.startsWith("import ");
    }

    private static String formatViolations(List<Violation> violations) {
        StringBuilder message = new StringBuilder();
        for (Violation violation : violations) {
            message.append(System.lineSeparator())
                    .append("- ")
                    .append(violation.relativePath())
                    .append(":")
                    .append(violation.lineNumber())
                    .append(" ")
                    .append(violation.kind())
                    .append(" ")
                    .append(violation.reference());
        }
        return message.toString();
    }

    record AllowedReference(String relativePath, String reference) {
    }

    record JavaSource(String relativePath, List<String> lines, List<SourceReference> importedTypes) {
    }

    record SourceReference(int lineNumber, String reference) {
    }

    record Violation(String relativePath, int lineNumber, String kind, String reference) {
    }

    record CodeLine(String text, boolean inBlockComment) {
    }
}
