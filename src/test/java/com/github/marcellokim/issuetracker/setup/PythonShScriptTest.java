package com.github.marcellokim.issuetracker.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Python 실행 파일 탐색 라이브러리 (python.sh)")
class PythonShScriptTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("PYTHON_EXECUTABLE이 유효한 명령어면 그 값을 출력하고 성공한다")
    void usesPythonExecutableEnvVarWhenCommandExists() throws IOException, InterruptedException {
        var result = resolveExecutable(Map.of("PYTHON_EXECUTABLE", "bash"));

        assertEquals(0, result.exitCode(), "유효한 PYTHON_EXECUTABLE 설정 시 성공해야 합니다: " + result.output());
        assertEquals("bash", result.output().strip());
    }

    @Test
    @DisplayName("PYTHON_EXECUTABLE이 존재하지 않는 명령어면 실패한다")
    void failsWhenPythonExecutableEnvVarPointsToNonexistentCommand() throws IOException, InterruptedException {
        var result = resolveExecutable(Map.of("PYTHON_EXECUTABLE", "nonexistent_python_xyz_12345"));

        assertNotEquals(0, result.exitCode(), "존재하지 않는 PYTHON_EXECUTABLE 설정 시 실패해야 합니다");
    }

    @Test
    @DisplayName("PYTHON_EXECUTABLE이 존재하지 않으면 오류 메시지를 출력한다")
    void printsErrorMessageWhenPythonExecutableNotFound() throws IOException, InterruptedException {
        var result = resolveExecutable(Map.of("PYTHON_EXECUTABLE", "nonexistent_python_xyz_12345"));

        assertTrue(
                result.output().contains("PYTHON_EXECUTABLE"),
                "오류 메시지에 PYTHON_EXECUTABLE이 언급되어야 합니다: " + result.output()
        );
    }

    @Test
    @DisplayName("PATH에 python3가 있으면 python3를 선택한다")
    void selectsPython3WhenAvailableInPath() throws IOException, InterruptedException {
        Path fakePython3 = createFakeExecutable("python3");

        var result = resolveExecutable(Map.of("PATH", fakePython3.getParent().toString()));

        assertEquals(0, result.exitCode(), "python3가 PATH에 있으면 성공해야 합니다: " + result.output());
        assertEquals("python3", result.output().strip());
    }

    @Test
    @DisplayName("python3가 없고 python만 있으면 python을 선택한다")
    void selectsPythonWhenPython3NotAvailable() throws IOException, InterruptedException {
        Path fakePython = createFakeExecutable("python");

        var result = resolveExecutable(Map.of("PATH", fakePython.getParent().toString()));

        assertEquals(0, result.exitCode(), "python이 PATH에 있으면 성공해야 합니다: " + result.output());
        assertEquals("python", result.output().strip());
    }

    @Test
    @DisplayName("python3와 python이 없고 py만 있으면 py를 선택한다")
    void selectsPyWhenOnlyPyAvailable() throws IOException, InterruptedException {
        Path fakePy = createFakeExecutable("py");

        var result = resolveExecutable(Map.of("PATH", fakePy.getParent().toString()));

        assertEquals(0, result.exitCode(), "py가 PATH에 있으면 성공해야 합니다: " + result.output());
        assertEquals("py", result.output().strip());
    }

    @Test
    @DisplayName("python3, python, py 모두 없으면 실패하고 오류 메시지를 출력한다")
    void failsWhenNoPythonExecutableFoundInPath() throws IOException, InterruptedException {
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectories(emptyDir);

        var result = resolveExecutable(Map.of("PATH", emptyDir.toString()));

        assertNotEquals(0, result.exitCode(), "Python이 전혀 없으면 실패해야 합니다");
        assertTrue(
                result.output().contains("python3") && result.output().contains("python"),
                "오류 메시지에 후보 목록이 있어야 합니다: " + result.output()
        );
    }

    @Test
    @DisplayName("python3가 있어도 PYTHON_EXECUTABLE 환경변수가 우선한다")
    void pythonExecutableEnvVarTakesPriorityOverFallback() throws IOException, InterruptedException {
        Path fakePython3 = createFakeExecutable("python3");
        Path fakeBash = createFakeExecutable("fakepython");

        var result = resolveExecutable(Map.of(
                "PYTHON_EXECUTABLE", "fakepython",
                "PATH", fakePython3.getParent().toString() + ":" + fakeBash.getParent().toString()
        ));

        assertEquals(0, result.exitCode(), "PYTHON_EXECUTABLE이 우선되어야 합니다: " + result.output());
        assertEquals("fakepython", result.output().strip());
    }

    private Path createFakeExecutable(String name) throws IOException {
        Path execDir = Files.createTempDirectory(tempDir, "bin-");
        Path exec = execDir.resolve(name);
        Files.writeString(exec, "#!/bin/sh\nexit 0\n");
        Files.setPosixFilePermissions(exec, PosixFilePermissions.fromString("rwxr-xr-x"));
        return exec;
    }

    private ResolveResult resolveExecutable(Map<String, String> extraEnv) throws IOException, InterruptedException {
        var processBuilder = new ProcessBuilder(
                "bash", "-c", ". scripts/lib/python.sh && resolve_python_executable"
        );
        processBuilder.directory(Path.of(".").toAbsolutePath().normalize().toFile());
        processBuilder.redirectErrorStream(true);

        Map<String, String> env = processBuilder.environment();
        env.putAll(extraEnv);

        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        return new ResolveResult(exitCode, output);
    }

    record ResolveResult(int exitCode, String output) {
    }
}