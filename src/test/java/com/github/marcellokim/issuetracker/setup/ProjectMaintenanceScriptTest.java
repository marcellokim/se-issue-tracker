package com.github.marcellokim.issuetracker.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Project maintenance script")
class ProjectMaintenanceScriptTest {

    @Test
    @DisplayName("finds completed issues from Closes text when merged dev PR lacks closing references")
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
                        }, {
                            "number": 9004,
                            "title": "qualified closing reference",
                            "body": "## 관련 이슈\\n- Resolves marcellokim/se-issue-tracker#9104\\n",
                            "closingIssuesReferences": []
                        }, {
                            "number": 9005,
                            "title": "external qualified reference",
                            "body": "## 관련 이슈\\n- Closes other/repo#9105\\n",
                            "closingIssuesReferences": []
                        }]
                    if args[:3] == ["issue", "view", "9102"]:
                        return {
                            "number": 9102,
                            "title": "IssueController 등록과 코멘트 API 구현",
                            "state": "OPEN",
                            "labels": [{"name": "status:review"}]
                        }
                    if args[:3] == ["issue", "view", "9104"]:
                        return {
                            "number": 9104,
                            "title": "fully qualified closing reference",
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
                    "DRY-RUN merged dev PR #9001 linked issue 완료 처리: #9102 IssueController 등록과 코멘트 API 구현",
                    "DRY-RUN merged dev PR #9004 linked issue 완료 처리: #9104 fully qualified closing reference"
                ], changes
                """;

        ScriptResult result = runPython(script);

        assertEquals(0, result.exitCode(), result.output());
    }

    @Test
    @DisplayName("does not treat PR-number Closes text as a completed issue")
    void mergedDevPrCompletionIgnoresPullRequestReferencesFromBody() throws IOException, InterruptedException {
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
                        return [{
                            "number": 9007,
                            "title": "metadata loop",
                            "body": "## 관련 이슈\\n- Closes #9107\\n",
                            "closingIssuesReferences": []
                        }]
                    if args[:3] == ["issue", "view", "9107"]:
                        return {
                            "number": 9107,
                            "title": "already merged pull request",
                            "state": "MERGED",
                            "url": "https://github.com/marcellokim/se-issue-tracker/pull/9107",
                            "labels": [{"name": "status:done"}]
                        }
                    raise AssertionError(args)

                project_maintenance.gh_json = fake_gh_json
                project_maintenance.run = lambda *args, **kwargs: types.SimpleNamespace(returncode=0, stdout="", stderr="")

                changes = []
                project_maintenance.sync_issue_completion_from_merged_dev_prs(
                    "marcellokim/se-issue-tracker",
                    dry_run=True,
                    changes=changes
                )

                assert changes == [], changes
                """;

        ScriptResult result = runPython(script);

        assertEquals(0, result.exitCode(), result.output());
    }

    @Test
    @DisplayName("does not hide non-not-found failures during fallback issue lookup")
    void mergedDevPrCompletionReraisesNonMissingIssueLookupFailures() throws IOException, InterruptedException {
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
                        return [{
                            "number": 9006,
                            "title": "cli lookup failure",
                            "body": "## 관련 이슈\\n- Closes #9998\\n",
                            "closingIssuesReferences": []
                        }]
                    if args[:3] == ["issue", "view", "9998"]:
                        raise SystemExit("rate limit exceeded")
                    raise AssertionError(args)

                project_maintenance.gh_json = fake_gh_json
                project_maintenance.run = lambda *args, **kwargs: types.SimpleNamespace(returncode=0, stdout="", stderr="")

                changes = []
                project_maintenance.sync_issue_completion_from_merged_dev_prs(
                    "marcellokim/se-issue-tracker",
                    dry_run=True,
                    changes=changes
                )
                """;

        ScriptResult result = runPython(script);

        assertTrue(result.exitCode() != 0, result.output());
        assertTrue(result.output().contains("rate limit exceeded"), result.output());
    }

    @Test
    @DisplayName("project item context finds existing items on boards over 200 entries")
    void projectContextLoadsMoreThanTwoHundredItems() throws IOException, InterruptedException {
        String script = """
                import importlib.util
                import sys

                spec = importlib.util.spec_from_file_location("project_maintenance", "scripts/lib/project_maintenance.py")
                project_maintenance = importlib.util.module_from_spec(spec)
                sys.modules[spec.name] = project_maintenance
                spec.loader.exec_module(project_maintenance)

                def fake_gh_json(args):
                    if args[:2] == ["project", "view"]:
                        return {"id": "PVT_project", "readme": ""}
                    if args[:2] == ["project", "field-list"]:
                        return {
                            "fields": [{
                                "id": "PVTSSF_status",
                                "name": "Status",
                                "options": [{"id": "option_review", "name": "리뷰 중"}]
                            }]
                        }
                    if args[:2] == ["project", "item-list"]:
                        limit = int(args[args.index("--limit") + 1])
                        assert limit == int(project_maintenance.PROJECT_ITEM_LIST_LIMIT), args
                        return {
                            "items": [{
                                "id": "PVTI_recent",
                                "content": {"url": "https://github.com/marcellokim/se-issue-tracker/pull/262"}
                            }]
                        }
                    raise AssertionError(args)

                project_maintenance.gh_json = fake_gh_json

                context, _ = project_maintenance.load_project_context(
                    "marcellokim/se-issue-tracker",
                    "@me",
                    1,
                    "SE 2026-1 텀프로젝트"
                )

                assert "https://github.com/marcellokim/se-issue-tracker/pull/262" in context.item_by_url
                """;

        ScriptResult result = runPython(script);

        assertEquals(0, result.exitCode(), result.output());
    }

    private ScriptResult runPython(String script) throws IOException, InterruptedException {
        Path scriptFile = Files.createTempFile("project-maintenance-test-", ".py");
        Files.writeString(scriptFile, script, StandardCharsets.UTF_8);
        try {
            var processBuilder = new ProcessBuilder("python3", scriptFile.toString());
            processBuilder.directory(Path.of(".").toAbsolutePath().normalize().toFile());
            processBuilder.redirectErrorStream(true);

            var process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            return new ScriptResult(exitCode, output);
        } finally {
            Files.deleteIfExists(scriptFile);
        }
    }

    record ScriptResult(int exitCode, String output) {
    }
}
