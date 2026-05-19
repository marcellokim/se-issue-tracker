package com.github.marcellokim.issuetracker.setup;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("표준 PR 스크립트 실패 복구")
class OpenPrScriptFailureTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("기존 상태 라벨 조회 실패는 review 상태 변경 전에 중단한다")
    void stopsBeforeMutationWhenStatusLabelLookupFails() throws IOException, InterruptedException {
        var fixture = createFixture();

        var result = fixture.run("issue-view-fails");

        assertNotEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("상태 라벨 조회에 실패"), result.output());
        assertTrue(!Files.readString(fixture.log()).contains("issue edit"), "라벨 조회 실패 후 상태를 변경하면 안 됩니다.");
    }

    @Test
    @DisplayName("PR 생성 전 상태 변경이 실패하면 기존 상태 라벨을 복구한다")
    void restoresPreviousStatusWhenPreCreateStatusMutationFails() throws IOException, InterruptedException {
        var fixture = createFixture();

        var result = fixture.run("add-review-fails");
        var commandLog = Files.readString(fixture.log());

        assertNotEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("이슈 #86 상태 라벨을 이전 상태로 복구"), result.output());
        assertTrue(commandLog.contains("issue edit 86 --remove-label status:todo"), commandLog);
        assertTrue(commandLog.contains("issue edit 86 --add-label status:todo"), commandLog);
        assertTrue(commandLog.contains("sync-project-board"), commandLog);
    }

    private Fixture createFixture() throws IOException {
        var repo = tempDir.resolve("repo");
        var bin = tempDir.resolve("bin");
        var log = tempDir.resolve("commands.log");

        Files.createDirectories(repo.resolve("scripts/lib"));
        Files.createDirectories(bin);
        Files.createFile(log);

        Files.copy(Path.of("scripts/open-pr.sh"), repo.resolve("scripts/open-pr.sh"));
        Files.copy(Path.of("scripts/lib/git-refs.sh"), repo.resolve("scripts/lib/git-refs.sh"));

        writeExecutable(repo.resolve("gradlew"), """
                #!/usr/bin/env bash
                echo "gradlew $*" >> "$OPEN_PR_LOG"
                exit 0
                """);
        writeExecutable(repo.resolve("scripts/sync-project-board.sh"), """
                #!/usr/bin/env bash
                echo "sync-project-board" >> "$OPEN_PR_LOG"
                exit 0
                """);
        writeExecutable(bin.resolve("git"), """
                #!/usr/bin/env bash
                echo "git $*" >> "$OPEN_PR_LOG"
                case "$*" in
                  "rev-parse --show-toplevel") echo "$OPEN_PR_REPO";;
                  "rev-parse --abbrev-ref HEAD") echo "chore/86-open-pr-status-race";;
                  "status --porcelain") ;;
                  "config --get-all remote.origin.fetch") echo "+refs/heads/*:refs/remotes/origin/*";;
                  "fetch origin dev --quiet") ;;
                  "merge-base --is-ancestor origin/dev HEAD") ;;
                  "push -u origin HEAD") ;;
                  *) echo "unexpected git command: $*" >&2; exit 20;;
                esac
                """);
        writeExecutable(bin.resolve("gh"), """
                #!/usr/bin/env bash
                echo "gh $*" >> "$OPEN_PR_LOG"
                if [[ "$1 $2" == "auth status" ]]; then
                  exit 0
                fi
                if [[ "$1 $2" == "issue view" && "$*" == *"--json title"* ]]; then
                  echo "PR 자동화 레이스 수정"
                  exit 0
                fi
                if [[ "$1 $2" == "issue view" && "$*" == *"--json labels"* ]]; then
                  if [[ "$OPEN_PR_GH_MODE" == "issue-view-fails" ]]; then
                    echo "label lookup failed" >&2
                    exit 17
                  fi
                  echo "status:todo"
                  exit 0
                fi
                if [[ "$1 $2" == "issue edit" ]]; then
                  if [[ "$OPEN_PR_GH_MODE" == "add-review-fails" && "$*" == *"--add-label status:review"* ]]; then
                    echo "add review failed" >&2
                    exit 18
                  fi
                  exit 0
                fi
                if [[ "$1 $2" == "pr view" ]]; then
                  exit 1
                fi
                if [[ "$1 $2" == "pr create" ]]; then
                  echo "https://github.com/marcellokim/se-issue-tracker/pull/90"
                  exit 0
                fi
                echo "unexpected gh command: $*" >&2
                exit 21
                """);

        return new Fixture(repo, bin, log);
    }

    private void writeExecutable(Path path, String content) throws IOException {
        Files.writeString(path, content);
        assertTrue(path.toFile().setExecutable(true), () -> "실행 권한 설정 실패: " + path);
    }

    record Fixture(Path repo, Path bin, Path log) {
        ScriptResult run(String mode) throws IOException, InterruptedException {
            var processBuilder = new ProcessBuilder("bash", "scripts/open-pr.sh");
            processBuilder.directory(repo.toFile());
            processBuilder.redirectErrorStream(true);

            Map<String, String> environment = processBuilder.environment();
            environment.put("PATH", bin + System.getProperty("path.separator") + environment.get("PATH"));
            environment.put("OPEN_PR_REPO", repo.toString());
            environment.put("OPEN_PR_LOG", log.toString());
            environment.put("OPEN_PR_GH_MODE", mode);

            var process = processBuilder.start();
            var output = new String(process.getInputStream().readAllBytes());
            var exitCode = process.waitFor();
            return new ScriptResult(exitCode, output);
        }
    }

    record ScriptResult(int exitCode, String output) {
    }
}
