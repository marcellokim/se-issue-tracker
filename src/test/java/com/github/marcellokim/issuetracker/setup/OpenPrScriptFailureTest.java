package com.github.marcellokim.issuetracker.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Open PR script failure recovery")
class OpenPrScriptFailureTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("stops before review status when current status lookup fails")
    void stopsBeforeMutationWhenStatusLabelLookupFails() throws IOException, InterruptedException {
        var fixture = createFixture();

        var result = fixture.run("issue-view-fails");

        assertNotEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("상태 라벨 조회에 실패"), result.output());
        assertTrue(!Files.readString(fixture.log()).contains("issue edit"), "라벨 조회 실패 후 상태를 변경하면 안 됩니다.");
    }

    @Test
    @DisplayName("restores previous status when pre-create status update fails")
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

    @Test
    @DisplayName("does not add review after saved status recheck fails")
    void stopsBeforeAddingReviewWhenPreCreateStatusRelookupFails() throws IOException, InterruptedException {
        var fixture = createFixture();

        var result = fixture.run("second-label-view-fails");
        var commandLog = Files.readString(fixture.log());

        assertNotEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("상태 라벨 조회에 실패"), result.output());
        assertTrue(!commandLog.contains("issue edit 86 --add-label status:review"), commandLog);
    }

    @Test
    @DisplayName("restores previous status when PR creation fails")
    void restoresPreviousStatusWhenPullRequestCreationFails() throws IOException, InterruptedException {
        var fixture = createFixture();

        var result = fixture.run("pr-create-fails");
        var commandLog = Files.readString(fixture.log());

        assertNotEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("이슈 #86 상태 라벨을 이전 상태로 복구"), result.output());
        assertTrue(commandLog.contains("issue edit 86 --remove-label status:todo"), commandLog);
        assertTrue(commandLog.contains("issue edit 86 --add-label status:review"), commandLog);
        assertTrue(commandLog.contains("gh pr create --base dev"), commandLog);
        assertTrue(commandLog.contains("issue edit 86 --remove-label status:review"), commandLog);
        assertTrue(commandLog.contains("issue edit 86 --add-label status:todo"), commandLog);
        assertTrue(commandLog.contains("sync-project-board"), commandLog);
    }

    @Test
    @DisplayName("builds PR title from branch type and cleaned issue title")
    void createsPullRequestTitleFromBranchTypeAndIssueTitle() throws IOException, InterruptedException {
        var fixture = createFixture();

        var result = fixture.run("normal", "feature/86-open-pr-status-race", "[feat] PR 자동화 레이스 수정");
        var commandLog = Files.readString(fixture.log());

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(commandLog.contains("--title feat: PR 자동화 레이스 수정"), commandLog);
    }

    @Test
    @DisplayName("removes only standard type prefixes from PR titles")
    void preservesNonConventionIssueTitlePrefixes() throws IOException, InterruptedException {
        var scopedTitleFixture = createFixture();
        var scopedTitleResult = scopedTitleFixture.run("normal", "docs/86-api-title", "docs(api): 로그인 흐름 구현");
        var scopedTitleCommandLog = Files.readString(scopedTitleFixture.log());

        assertEquals(0, scopedTitleResult.exitCode(), scopedTitleResult.output());
        assertTrue(scopedTitleCommandLog.contains("--title docs: 로그인 흐름 구현"), scopedTitleCommandLog);

        var apiTitleFixture = createFixture();
        var apiTitleResult = apiTitleFixture.run("normal", "docs/86-api-title", "API: 로그인 흐름 구현");
        var apiTitleCommandLog = Files.readString(apiTitleFixture.log());

        assertEquals(0, apiTitleResult.exitCode(), apiTitleResult.output());
        assertTrue(apiTitleCommandLog.contains("--title docs: API: 로그인 흐름 구현"), apiTitleCommandLog);

        var urgentTitleFixture = createFixture();
        var urgentTitleResult = urgentTitleFixture.run("normal", "fix/86-login-flow", "[긴급] 로그인 흐름 수정");
        var urgentTitleCommandLog = Files.readString(urgentTitleFixture.log());

        assertEquals(0, urgentTitleResult.exitCode(), urgentTitleResult.output());
        assertTrue(urgentTitleCommandLog.contains("--title fix: [긴급] 로그인 흐름 수정"), urgentTitleCommandLog);
    }

    @Test
    @DisplayName("does not use Bash 4 lowercase expansion")
    void avoidsBashFourOnlyLowercaseExpansion() throws IOException {
        var script = Files.readString(Path.of("scripts/open-pr.sh"));

        assertFalse(script.contains(",,}"), "macOS 기본 Bash 3.2에서 깨지는 lowercase expansion을 쓰면 안 됩니다.");
    }

    private Fixture createFixture() throws IOException {
        var repo = Files.createTempDirectory(tempDir, "repo-");
        var bin = Files.createTempDirectory(tempDir, "bin-");
        var log = Files.createTempFile(tempDir, "commands-", ".log");

        Files.createDirectories(repo.resolve("scripts/lib"));
        var labelLookupCount = Files.createTempFile(tempDir, "label-lookups-", ".count");
        var labelState = Files.createTempFile(tempDir, "labels-", ".txt");
        Files.writeString(labelLookupCount, "0");
        Files.writeString(labelState, "status:todo\n");

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
        writeExecutable(repo.resolve("scripts/lib/project_maintenance.py"), """
                #!/usr/bin/env bash
                echo "project_maintenance.py $*" >> "$OPEN_PR_LOG"
                exit 0
                """);
        writeExecutable(bin.resolve("git"), """
                #!/usr/bin/env bash
                echo "git $*" >> "$OPEN_PR_LOG"
                case "$*" in
                  "rev-parse --show-toplevel") echo "$OPEN_PR_REPO";;
                  "rev-parse --abbrev-ref HEAD") echo "${OPEN_PR_BRANCH:-chore/86-open-pr-status-race}";;
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
                  echo "${OPEN_PR_ISSUE_TITLE:-PR 자동화 레이스 수정}"
                  exit 0
                fi
                if [[ "$1 $2" == "issue view" && "$*" == *"--json labels"* ]]; then
                  count="$(cat "$OPEN_PR_LABEL_LOOKUP_COUNT")"
                  count="$((count + 1))"
                  echo "$count" > "$OPEN_PR_LABEL_LOOKUP_COUNT"
                  if [[ "$OPEN_PR_GH_MODE" == "second-label-view-fails" && "$count" -ge 2 ]]; then
                    echo "label lookup failed" >&2
                    exit 17
                  fi
                  if [[ "$OPEN_PR_GH_MODE" == "issue-view-fails" ]]; then
                    echo "label lookup failed" >&2
                    exit 17
                  fi
                  cat "$OPEN_PR_LABEL_STATE"
                  exit 0
                fi
                if [[ "$1 $2" == "issue edit" ]]; then
                  if [[ "$OPEN_PR_GH_MODE" == "add-review-fails" && "$*" == *"--add-label status:review"* ]]; then
                    echo "add review failed" >&2
                    exit 18
                  fi
                  if [[ "${4:-}" == "--remove-label" ]]; then
                    grep -vxF "${5:-}" "$OPEN_PR_LABEL_STATE" > "$OPEN_PR_LABEL_STATE.tmp" || true
                    mv "$OPEN_PR_LABEL_STATE.tmp" "$OPEN_PR_LABEL_STATE"
                  fi
                  if [[ "${4:-}" == "--add-label" ]] && ! grep -qxF "${5:-}" "$OPEN_PR_LABEL_STATE"; then
                    echo "${5:-}" >> "$OPEN_PR_LABEL_STATE"
                  fi
                  exit 0
                fi
                if [[ "$1 $2" == "pr view" ]]; then
                  exit 1
                fi
                if [[ "$1 $2" == "pr create" ]]; then
                  if [[ "$OPEN_PR_GH_MODE" == "pr-create-fails" ]]; then
                    echo "pr create failed" >&2
                    exit 19
                  fi
                  echo "https://github.com/marcellokim/se-issue-tracker/pull/90"
                  exit 0
                fi
                echo "unexpected gh command: $*" >&2
                exit 21
                """);

        return new Fixture(repo, bin, log, labelLookupCount, labelState);
    }

    private void writeExecutable(Path path, String content) throws IOException {
        Files.writeString(path, content);
        assertTrue(path.toFile().setExecutable(true), () -> "실행 권한 설정 실패: " + path);
    }

    record Fixture(Path repo, Path bin, Path log, Path labelLookupCount, Path labelState) {
        ScriptResult run(String mode) throws IOException, InterruptedException {
            var command = "PATH=" + shellQuote(bashPath(bin)) + ":$PATH; export PATH; bash scripts/open-pr.sh";
            var processBuilder = new ProcessBuilder("bash", "-lc", command);
            processBuilder.directory(repo.toFile());
            processBuilder.redirectErrorStream(true);

            Map<String, String> environment = processBuilder.environment();
            environment.put("OPEN_PR_REPO", bashPath(repo));
            environment.put("OPEN_PR_LOG", bashPath(log));
            environment.put("OPEN_PR_GH_MODE", mode);
            environment.put("OPEN_PR_LABEL_LOOKUP_COUNT", bashPath(labelLookupCount));
            environment.put("OPEN_PR_LABEL_STATE", bashPath(labelState));

            var process = processBuilder.start();
            var output = new String(process.getInputStream().readAllBytes());
            var exitCode = process.waitFor();
            return new ScriptResult(exitCode, output);
        }

        ScriptResult run(String mode, String branch, String issueTitle) throws IOException, InterruptedException {
            var command = "PATH=" + shellQuote(bashPath(bin)) + ":$PATH; export PATH; bash scripts/open-pr.sh";
            var processBuilder = new ProcessBuilder("bash", "-lc", command);
            processBuilder.directory(repo.toFile());
            processBuilder.redirectErrorStream(true);

            Map<String, String> environment = processBuilder.environment();
            environment.put("OPEN_PR_REPO", bashPath(repo));
            environment.put("OPEN_PR_LOG", bashPath(log));
            environment.put("OPEN_PR_GH_MODE", mode);
            environment.put("OPEN_PR_LABEL_LOOKUP_COUNT", bashPath(labelLookupCount));
            environment.put("OPEN_PR_LABEL_STATE", bashPath(labelState));
            environment.put("OPEN_PR_BRANCH", branch);
            environment.put("OPEN_PR_ISSUE_TITLE", issueTitle);

            var process = processBuilder.start();
            var output = new String(process.getInputStream().readAllBytes());
            var exitCode = process.waitFor();
            return new ScriptResult(exitCode, output);
        }

        private static String bashPath(Path path) {
            String absolutePath = path.toAbsolutePath().normalize().toString().replace('\\', '/');
            if (absolutePath.length() > 2 && absolutePath.charAt(1) == ':') {
                return "/" + Character.toLowerCase(absolutePath.charAt(0)) + absolutePath.substring(2);
            }
            return absolutePath;
        }

        private static String shellQuote(String value) {
            return "'" + value.replace("'", "'\"'\"'") + "'";
        }
    }

    record ScriptResult(int exitCode, String output) {
    }
}
