package com.github.marcellokim.issuetracker.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("프로젝트 정합성 스크립트")
class ProjectMaintenanceScriptTest {

    @Test
    @DisplayName("dev로 머지된 PR은 closingIssuesReferences가 비어도 본문 Closes를 기준으로 이슈 완료 후보를 찾는다")
    void mergedDevPrCompletionFallsBackToBodyClosesReferences() throws IOException, InterruptedException {
        String script = """
                import importlib.util
                import sys
                import types

                spec = importlib.util.spec_from_file_location("project_maintenance", "scripts/lib/project_maintenance.py")
                project_maintenance = importlib.util.module_from_spec(spec)
                sys.modules[spec.name] = project_maintenance
                spec.loader.exec_module(project_maintenance)

                def fake_gh_json(args):
                    if args[:2] == ["pr", "list"]:
                        fields = set(args[args.index("--json") + 1].split(","))
                        assert "body" in fields, args
                        assert "closingIssuesReferences" in fields, args
                        return [{
                            "number": 9001,
                            "title": "comment contract",
                            "body": "## 관련 이슈\\n- Closes #9102\\n",
                            "closingIssuesReferences": []
                        }, {
                            "number": 9002,
                            "title": "reference only",
                            "body": "## 참고\\n- Refs #9103\\n",
                            "closingIssuesReferences": []
                        }, {
                            "number": 9003,
                            "title": "invalid closing reference",
                            "body": "## 관련 이슈\\n- Closes: #9999\\n",
                            "closingIssuesReferences": []
                        }]
                    if args[:3] == ["issue", "view", "9102"]:
                        return {
                            "number": 9102,
                            "title": "IssueController 등록과 코멘트 API 구현",
                            "state": "OPEN",
                            "labels": [{"name": "status:review"}]
                        }
                    if args[:3] == ["issue", "view", "9999"]:
                        raise SystemExit("issue not found")
                    raise AssertionError(args)

                project_maintenance.gh_json = fake_gh_json
                project_maintenance.run = lambda *args, **kwargs: types.SimpleNamespace(returncode=0, stdout="", stderr="")

                changes = []
                project_maintenance.sync_issue_completion_from_merged_dev_prs(
                    "marcellokim/se-issue-tracker",
                    dry_run=True,
                    changes=changes
                )

                assert changes == [
                    "DRY-RUN merged dev PR #9001 linked issue 완료 처리: #9102 IssueController 등록과 코멘트 API 구현"
                ], changes
                """;

        ScriptResult result = runPython(script);

        assertEquals(0, result.exitCode(), result.output());
    }

    private ScriptResult runPython(String script) throws IOException, InterruptedException {
        var processBuilder = new ProcessBuilder("python3", "-c", script);
        processBuilder.directory(Path.of(".").toAbsolutePath().normalize().toFile());
        processBuilder.redirectErrorStream(true);

        var process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        return new ScriptResult(exitCode, output);
    }

    record ScriptResult(int exitCode, String output) {
    }
}
