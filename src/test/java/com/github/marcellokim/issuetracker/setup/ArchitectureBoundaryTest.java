package com.github.marcellokim.issuetracker.setup;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.marcellokim.issuetracker.persistence.DatabaseConnectionProvider;
import com.github.marcellokim.issuetracker.persistence.jdbc.JdbcRepositoryFactory;
import com.github.marcellokim.issuetracker.persistence.jdbc.JdbcUserRepository;
import com.github.marcellokim.issuetracker.service.Clock;
import com.github.marcellokim.issuetracker.service.CommentIdProvider;
import com.github.marcellokim.issuetracker.service.CurrentUserSession;
import com.github.marcellokim.issuetracker.service.PasswordHashing;
import com.github.marcellokim.issuetracker.technical.CommentIdGenerator;
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

    /*
     * 임시 아키텍처 부채 표시자.
     * 각 흐름이 의도한 service/presenter 경계로 이동하면 예외 제거 필요.
     */
    private static final Set<AllowedReference> TEMPORARY_ALLOWED_REFERENCES = Set.of();

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
        assertTrue(CurrentUserSession.class.isAssignableFrom(SessionStore.class));
        assertTrue(PasswordHashing.class.isAssignableFrom(PasswordHasher.class));
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

        assertTrue(repositoryConstructor.getParameterTypes()[1].equals(PasswordHashing.class));
        assertTrue(factoryConstructor.getParameterTypes()[1].equals(PasswordHashing.class));
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
                        && !TEMPORARY_ALLOWED_REFERENCES.contains(
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
                if (!TEMPORARY_ALLOWED_REFERENCES.contains(
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
            // Task 1 guard 선설치 후 Task 2 cli 패키지 생성 전까지만 빈 검사 대상 허용.
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
}
