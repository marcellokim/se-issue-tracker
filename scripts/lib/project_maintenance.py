#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PROJECT_TITLE = "SE 2026-1 텀프로젝트"
STATUS_FIELD = "Status"

STATUS_BY_LABEL = {
    "status:backlog": "대기",
    "status:ready": "준비됨",
    "status:in-progress": "진행 중",
    "status:review": "리뷰 중",
    "status:done": "완료",
}
STATUS_LABELS = set(STATUS_BY_LABEL)

REQUIRED_FILES = [
    "SE_Term_Project_2026-1.pdf",
    "README.md",
    ".pr_agent.toml",
    ".gemini/config.yaml",
    ".gemini/styleguide.md",
    ".github/PULL_REQUEST_TEMPLATE.md",
    ".github/copilot-instructions.md",
    ".github/workflows/add-to-project.yml",
    ".github/workflows/codeql.yml",
    ".github/workflows/gradle.yml",
    ".github/workflows/pr-metadata.yml",
    ".github/workflows/project-maintenance.yml",
    ".github/workflows/workflow-guard.yml",
    "config/github/labels.json",
    "config/github/milestones.json",
    ".githooks/commit-msg",
    ".githooks/pre-commit",
    ".githooks/pre-push",
    "docs/assumptions.md",
    "docs/automation-playbook.md",
    "docs/project-management-plan.md",
    "docs/requirements-traceability.md",
    "docs/team-setup-manual.md",
    "scripts/audit-project.sh",
    "scripts/lib/git-refs.sh",
    "scripts/lib/python.sh",
    "scripts/sync-project-board.sh",
    "scripts/open-pr.sh",
    "scripts/start-task.sh",
    "scripts/validate-workflow-guard.sh",
    "scripts/validate-public-attribution.sh",
]

DB_STANDARD_FILES = [
    "README.md",
    "docs/assumptions.md",
    "docs/project-management-plan.md",
    "docs/requirements-traceability.md",
    "config/github/milestones.json",
]

FORBIDDEN_OLD_STORAGE_PHRASES = [
    "파일 기반 영속 저장소",
    "파일 저장/로드 테스트",
    "DB 도입 전까지",
]


@dataclass
class CheckResult:
    ok: bool
    message: str


@dataclass
class ProjectContext:
    owner: str
    project_id: str
    project_number: int
    status_field_id: str
    status_option_by_name: dict[str, str]
    item_by_url: dict[str, dict[str, object]]


def run(cmd: list[str], *, check: bool = False, env: dict[str, str] | None = None) -> subprocess.CompletedProcess[str]:
    completed = subprocess.run(cmd, cwd=ROOT, text=True, capture_output=True, env=env)
    if check and completed.returncode != 0:
        command = " ".join(cmd)
        stderr = completed.stderr.strip()
        stdout = completed.stdout.strip()
        details = stderr or stdout or f"exit code {completed.returncode}"
        raise SystemExit(f"명령 실패: {command}\n{details}")
    return completed


def fail(message: str) -> CheckResult:
    return CheckResult(False, message)


def pass_(message: str) -> CheckResult:
    return CheckResult(True, message)


def detect_repo() -> str:
    env_repo = os.getenv("GH_REPO")
    if env_repo:
        return env_repo

    remote = run(["git", "remote", "get-url", "origin"], check=True).stdout.strip()
    if remote.startswith("https://github.com/"):
        return remote.removeprefix("https://github.com/").removesuffix(".git")
    if remote.startswith("git@github.com:"):
        return remote.removeprefix("git@github.com:").removesuffix(".git")
    raise SystemExit(f"origin remote에서 GitHub 저장소를 확인할 수 없습니다: {remote}")


def gh_json(args: list[str]) -> object:
    completed = run(["gh", *args], check=True)
    return json.loads(completed.stdout) if completed.stdout.strip() else None


def gh_available() -> bool:
    return run(["gh", "auth", "status"]).returncode == 0


def parse_issue_number_from_branch(branch: str) -> int | None:
    match = re.match(r"^(?:feat|fix|docs|test|ci|chore|refactor|feature)/([0-9]+)-", branch)
    return int(match.group(1)) if match else None


def parse_issue_numbers_from_body(body: str) -> list[int]:
    return parse_issue_numbers_with_keywords(body, r"close[sd]?|fix(?:e[sd])?|resolve[sd]?|refs?")


def parse_closing_issue_numbers_from_body(body: str) -> list[int]:
    return parse_issue_numbers_with_keywords(body, r"close[sd]?|fix(?:e[sd])?|resolve[sd]?")


def parse_issue_numbers_with_keywords(body: str, keywords: str) -> list[int]:
    numbers: list[int] = []
    pattern = re.compile(rf"\b(?:{keywords}):?\s+#([0-9]+)", re.IGNORECASE)
    for match in pattern.finditer(body):
        number = int(match.group(1))
        if number not in numbers:
            numbers.append(number)
    return numbers


def is_bot_login(login: str) -> bool:
    return login.endswith("[bot]") or login in {"dependabot", "renovate"}


def project_number_from_url(project_url: str) -> int | None:
    match = re.search(r"/projects/(\d+)$", project_url)
    return int(match.group(1)) if match else None


def resolve_project(repo: str, owner: str, project_number: int | None, project_title: str) -> tuple[str, int, str]:
    if project_number is None:
        env_url = os.getenv("PROJECT_URL", "")
        project_number = project_number_from_url(env_url) if env_url else None

    if project_number is None:
        variable = run(["gh", "variable", "get", "PROJECT_URL", "--repo", repo])
        if variable.returncode == 0:
            project_number = project_number_from_url(variable.stdout.strip())

    if project_number is None:
        projects = gh_json(["project", "list", "--owner", owner, "--format", "json", "--limit", "100"])
        for project in projects.get("projects", []):
            if project.get("title") == project_title:
                project_number = int(project["number"])
                break

    if project_number is None:
        raise SystemExit("GitHub 프로젝트 번호를 찾을 수 없습니다. PROJECT_URL 변수 또는 --project-number를 확인하세요.")

    view = gh_json(["project", "view", str(project_number), "--owner", owner, "--format", "json"])
    return str(view["id"]), project_number, str(view.get("readme", ""))


def load_project_context(repo: str, owner: str, project_number: int | None, project_title: str) -> tuple[ProjectContext, str]:
    project_id, resolved_number, readme = resolve_project(repo, owner, project_number, project_title)
    fields = gh_json(["project", "field-list", str(resolved_number), "--owner", owner, "--format", "json", "--limit", "100"])
    status_field = next((field for field in fields["fields"] if field["name"] == STATUS_FIELD), None)
    if not status_field:
        raise SystemExit("프로젝트 Status 필드를 찾을 수 없습니다.")

    project_items = gh_json(["project", "item-list", str(resolved_number), "--owner", owner, "--format", "json", "--limit", "200"])
    item_by_url = {
        item["content"]["url"]: item
        for item in project_items.get("items", [])
        if item.get("content", {}).get("url")
    }
    context = ProjectContext(
        owner=owner,
        project_id=project_id,
        project_number=resolved_number,
        status_field_id=status_field["id"],
        status_option_by_name={option["name"]: option["id"] for option in status_field["options"]},
        item_by_url=item_by_url,
    )
    return context, readme


def label_names(item: dict[str, object]) -> set[str]:
    return {label["name"] for label in item.get("labels", [])}


def expected_issue_status(issue: dict[str, object]) -> str | None:
    labels = label_names(issue)
    if issue.get("state") == "CLOSED":
        return "완료"
    for label, status in STATUS_BY_LABEL.items():
        if label in labels:
            return status
    return None


def project_status(issue: dict[str, object]) -> str | None:
    for item in issue.get("projectItems", []):
        status = item.get("status")
        if isinstance(status, dict) and status.get("name"):
            return str(status["name"])
    return None


def required_file_checks() -> list[CheckResult]:
    return [
        pass_(f"필수 파일 확인: {path}") if (ROOT / path).exists() else fail(f"필수 파일 누락: {path}")
        for path in REQUIRED_FILES
    ]


def json_file_checks() -> list[CheckResult]:
    results: list[CheckResult] = []
    for path in ["config/github/labels.json", "config/github/milestones.json"]:
        try:
            json.loads((ROOT / path).read_text(encoding="utf-8"))
            results.append(pass_(f"JSON 문법 확인: {path}"))
        except json.JSONDecodeError as exc:
            results.append(fail(f"JSON 문법 오류: {path}: {exc}"))
    return results


def db_standard_checks() -> list[CheckResult]:
    results: list[CheckResult] = []

    for path in DB_STANDARD_FILES:
        text = (ROOT / path).read_text(encoding="utf-8")
        if "DB" in text and ("persistence" in text or "영속 저장소" in text or "저장소" in text):
            results.append(pass_(f"DB 표준 문구 확인: {path}"))
        else:
            results.append(fail(f"DB 표준 문구 부족: {path}"))
    return results


def stale_storage_phrase_checks() -> list[CheckResult]:
    results: list[CheckResult] = []

    searchable = ["README.md", "docs", "config/github"]
    for phrase in FORBIDDEN_OLD_STORAGE_PHRASES:
        hits: list[str] = []
        for root in searchable:
            base = ROOT / root
            files = [base] if base.is_file() else base.rglob("*.md")
            for file in files:
                if phrase in file.read_text(encoding="utf-8"):
                    hits.append(str(file.relative_to(ROOT)))
        results.append(pass_(f"과거 저장소 문구 없음: {phrase}") if not hits else fail(f"과거 저장소 문구 잔존: {phrase}: {', '.join(hits)}"))
    return results


def shell_syntax_checks() -> list[CheckResult]:
    results: list[CheckResult] = []

    for script in sorted((ROOT / "scripts").rglob("*.sh")):
        syntax = run(["bash", "-n", str(script.relative_to(ROOT))])
        results.append(pass_(f"shell 문법 확인: {script.name}") if syntax.returncode == 0 else fail(f"shell 문법 오류: {script.name}: {syntax.stderr.strip()}"))
    return results


def branch_alignment_checks() -> list[CheckResult]:
    fetch = run(["git", "fetch", "origin", "main", "dev", "--quiet"])
    if fetch.returncode != 0:
        return [fail(f"origin/main, origin/dev fetch 실패: {fetch.stderr.strip()}")]
    ancestry = run(["git", "merge-base", "--is-ancestor", "origin/main", "origin/dev"])
    if ancestry.returncode == 0:
        return [pass_("origin/main이 origin/dev의 조상입니다")]
    return [fail("origin/dev가 origin/main을 포함하지 않습니다")]


def local_checks(skip_git_branches: bool) -> list[CheckResult]:
    results: list[CheckResult] = []
    results.extend(required_file_checks())
    results.extend(json_file_checks())
    results.extend(db_standard_checks())
    results.extend(stale_storage_phrase_checks())
    results.extend(shell_syntax_checks())
    attribution = run(["bash", "scripts/validate-public-attribution.sh", "--all"])
    if attribution.returncode == 0:
        results.append(pass_("공개 이력 표기 정책 확인"))
    else:
        results.append(fail(f"공개 이력 표기 정책 위반: {attribution.stderr.strip()}"))
    if not skip_git_branches:
        results.extend(branch_alignment_checks())
    return results


def project_readme_checks(repo: str, owner: str, project_number: int | None, project_title: str) -> tuple[ProjectContext, list[CheckResult]]:
    context, readme = load_project_context(repo, owner, project_number, project_title)
    results: list[CheckResult] = []
    results.append(pass_(f"GitHub 프로젝트 확인: {owner}/{context.project_number} ({context.project_id})"))

    if "SE_Term_Project_2026-1.pdf" in readme and "DB 기반 persistence" in readme:
        results.append(pass_("프로젝트 설명의 PDF 원문/DB 표준 문구 확인"))
    else:
        results.append(fail("프로젝트 설명에 PDF 원문 또는 DB 표준 문구가 부족합니다"))
    return context, results


def milestone_remote_checks(repo: str) -> list[CheckResult]:
    results: list[CheckResult] = []
    desired_milestones = json.loads((ROOT / "config/github/milestones.json").read_text(encoding="utf-8"))
    remote_milestones = gh_json(["api", f"repos/{repo}/milestones?state=all&per_page=100"])
    remote_by_title = {item["title"]: item for item in remote_milestones}
    for desired in desired_milestones:
        remote = remote_by_title.get(desired["title"])
        if not remote:
            results.append(fail(f"원격 마일스톤 누락: {desired['title']}"))
        elif remote.get("description") != desired["description"]:
            results.append(fail(f"원격 마일스톤 설명 불일치: {desired['title']}"))
        else:
            results.append(pass_(f"원격 마일스톤 일치: {desired['title']}"))
    return results


def traceability_issue_checks(issues: list[dict[str, object]]) -> list[CheckResult]:
    results: list[CheckResult] = []
    seen_issue_numbers = {issue["number"] for issue in issues}

    traceability = (ROOT / "docs/requirements-traceability.md").read_text(encoding="utf-8")
    linked_issue_numbers = set()
    for issue_url_match, hash_match in re.findall(r"/issues/(\d+)|#(\d+)", traceability):
        raw_number = issue_url_match or hash_match
        if raw_number:
            linked_issue_numbers.add(int(raw_number))

    for number in sorted(linked_issue_numbers):
        results.append(pass_(f"추적표 이슈 존재: #{number}") if number in seen_issue_numbers else fail(f"추적표 이슈가 원격에 없습니다: #{number}"))
    return results


def issue_metadata_checks(issues: list[dict[str, object]]) -> list[CheckResult]:
    results: list[CheckResult] = []
    for issue in issues:
        number = int(issue["number"])
        if number < 13:
            continue
        labels = label_names(issue)
        status_labels = sorted(labels.intersection(STATUS_BY_LABEL))
        type_labels = sorted(label for label in labels if label.startswith("type:"))
        if len(status_labels) == 1:
            results.append(pass_(f"이슈 상태 라벨 확인: #{number} {status_labels[0]}"))
        else:
            results.append(fail(f"이슈 상태 라벨은 정확히 1개여야 합니다: #{number} {status_labels}"))
        if type_labels:
            results.append(pass_(f"이슈 type 라벨 확인: #{number}"))
        else:
            results.append(fail(f"이슈 type 라벨 누락: #{number}"))
        if issue.get("milestone"):
            results.append(pass_(f"이슈 마일스톤 확인: #{number}"))
        else:
            results.append(fail(f"이슈 마일스톤 누락: #{number}"))
        expected = expected_issue_status(issue)
        actual = project_status(issue)
        if expected and actual == expected:
            results.append(pass_(f"프로젝트 상태 일치: #{number} {actual}"))
        else:
            results.append(fail(f"프로젝트 상태 불일치: #{number} expected={expected} actual={actual}"))
    return results


def issue18_db_checks(repo: str) -> list[CheckResult]:
    issue18 = gh_json(["issue", "view", "18", "--repo", repo, "--json", "title,body,labels,milestone,projectItems"])
    issue18_text = f"{issue18.get('title', '')}\n{issue18.get('body', '')}"
    if all(token in issue18_text for token in ["DB", "schema", "seed", "JDBC"]):
        return [pass_("이슈 #18 DB 표준 상세 내용 확인")]
    return [fail("이슈 #18에 DB/schema/seed/JDBC 기준이 부족합니다")]


def github_checks(repo: str, owner: str, project_number: int | None, project_title: str) -> list[CheckResult]:
    if not gh_available():
        return [fail("GitHub CLI 인증이 필요합니다: gh auth login")]

    _, results = project_readme_checks(repo, owner, project_number, project_title)
    issues = gh_json(["issue", "list", "--repo", repo, "--state", "all", "--limit", "200", "--json", "number,title,state,labels,milestone,projectItems,url"])
    results.extend(milestone_remote_checks(repo))
    results.extend(traceability_issue_checks(issues))
    results.extend(issue_metadata_checks(issues))
    results.extend(issue18_db_checks(repo))
    return results


def print_results(results: list[CheckResult], quiet: bool) -> int:
    failed = [result for result in results if not result.ok]
    if not quiet:
        for result in results:
            prefix = "PASS" if result.ok else "FAIL"
            print(f"[{prefix}] {result.message}")
    if failed:
        print(f"\n{len(failed)}개 항목이 실패했습니다.", file=sys.stderr)
        return 1
    if not quiet:
        print(f"\n자동화 정합성 점검 통과: {len(results)}개 항목")
    return 0


def audit(args: argparse.Namespace) -> int:
    repo = args.repo or detect_repo()
    owner = args.owner or repo.split("/", 1)[0]
    results = local_checks(args.skip_git_branches or args.local_only)
    if not args.local_only:
        results.extend(github_checks(repo, owner, args.project_number, args.project_title))
    return print_results(results, args.quiet)


def item_status_from_labels(labels: list[str], state: str) -> str:
    if state in {"CLOSED", "MERGED"}:
        return "완료"
    for label, status in STATUS_BY_LABEL.items():
        if label in labels:
            return status
    return "대기"


def pr_project_status(pr: dict[str, object]) -> str:
    if pr.get("state") != "OPEN":
        return "완료"
    if pr.get("isDraft"):
        return "진행 중"
    return "리뷰 중"


def print_github_errors(failures: list[str]) -> int:
    for failure in failures:
        print(f"::error::{failure}", file=sys.stderr)
    return 1


def ensure_project_item(context: ProjectContext, url: str, dry_run: bool, changes: list[str]) -> dict[str, object] | None:
    if url in context.item_by_url:
        return context.item_by_url[url]
    if dry_run:
        changes.append(f"DRY-RUN add item: {url}")
        return None

    added = gh_json([
        "project", "item-add",
        str(context.project_number),
        "--owner", context.owner,
        "--url", url,
        "--format", "json",
    ])
    item = {"id": added.get("id"), "content": {"url": url}, "status": ""}
    context.item_by_url[url] = item
    changes.append(f"add item: {url}")
    return item


def append_issue_link_if_missing(pr_number: int, pr: dict[str, object], issue_number: int, repo: str, dry_run: bool, changes: list[str]) -> None:
    body = str(pr.get("body") or "")
    if issue_number in parse_issue_numbers_from_body(body):
        return

    if dry_run:
        changes.append(f"DRY-RUN PR #{pr_number} body에 Closes #{issue_number} 추가")
        return

    if "## 관련 이슈" in body:
        new_body = body.rstrip() + f"\n- Closes #{issue_number}\n"
    else:
        section = f"## 관련 이슈\n- Closes #{issue_number}\n"
        new_body = (body.rstrip() + "\n\n" + section) if body.strip() else section
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", delete=False) as body_file:
        body_file.write(new_body)
        body_file_path = body_file.name
    try:
        run(["gh", "pr", "edit", str(pr_number), "--repo", repo, "--body-file", body_file_path], check=True)
    finally:
        Path(body_file_path).unlink(missing_ok=True)
    changes.append(f"PR #{pr_number} body에 Closes #{issue_number} 추가")


def sync_pr_assignees(
    pr_number: int,
    repo: str,
    pr: dict[str, object],
    issue: dict[str, object],
    dry_run: bool,
    changes: list[str],
    failures: list[str],
) -> None:
    current = {assignee["login"] for assignee in pr.get("assignees", [])}
    desired = {assignee["login"] for assignee in issue.get("assignees", [])}
    author = pr.get("author") or {}
    author_login = str(author.get("login") or "")
    if not desired and author_login and not is_bot_login(author_login):
        desired.add(author_login)

    missing = sorted(desired - current)
    if not missing:
        if current:
            return
        failures.append(f"PR #{pr_number} assignee를 정할 수 없습니다. linked issue assignee 또는 PR 작성자를 확인하세요.")
        return

    if dry_run:
        changes.append(f"DRY-RUN PR #{pr_number} assignee 추가: {', '.join(missing)}")
        return

    run(["gh", "pr", "edit", str(pr_number), "--repo", repo, "--add-assignee", ",".join(missing)], check=True)
    changes.append(f"PR #{pr_number} assignee 추가: {', '.join(missing)}")


def sync_pr_milestone(
    pr_number: int,
    repo: str,
    pr: dict[str, object],
    issue: dict[str, object],
    dry_run: bool,
    changes: list[str],
    failures: list[str],
) -> None:
    current = pr.get("milestone") or {}
    current_title = current.get("title") if isinstance(current, dict) else None
    issue_milestone = issue.get("milestone") or {}
    desired_title = issue_milestone.get("title") if isinstance(issue_milestone, dict) else None
    if not desired_title:
        failures.append(f"linked issue #{issue['number']} milestone이 없어 PR #{pr_number} milestone을 자동 지정할 수 없습니다.")
        return

    if current_title == desired_title:
        return

    if dry_run:
        changes.append(f"DRY-RUN PR #{pr_number} milestone 설정: {desired_title}")
        return

    run(["gh", "pr", "edit", str(pr_number), "--repo", repo, "--milestone", str(desired_title)], check=True)
    changes.append(f"PR #{pr_number} milestone 설정: {desired_title}")


def sync_single_pr_metadata(args: argparse.Namespace) -> int:
    repo = args.repo or detect_repo()
    owner = args.owner or repo.split("/", 1)[0]
    if not gh_available():
        raise SystemExit("GitHub CLI 인증이 필요합니다: gh auth login")

    pr = gh_json([
        "pr", "view", str(args.pr),
        "--repo", repo,
        "--json", "number,title,body,author,headRefName,assignees,milestone,projectItems,closingIssuesReferences,url,state,isDraft",
    ])
    pr_number = int(pr["number"])
    author = pr.get("author") or {}
    author_login = str(author.get("login") or "")
    if args.skip_bots and is_bot_login(author_login):
        if not args.quiet:
            print(f"bot PR 메타데이터 자동 보정 건너뜀: PR #{pr_number} author={author_login}")
        return 0

    linked_numbers = [int(issue["number"]) for issue in pr.get("closingIssuesReferences", [])]
    linked_numbers.extend(parse_issue_numbers_from_body(str(pr.get("body") or "")))
    if not linked_numbers:
        branch_issue = parse_issue_number_from_branch(str(pr.get("headRefName") or ""))
        if branch_issue is not None:
            linked_numbers.append(branch_issue)
    linked_numbers = list(dict.fromkeys(linked_numbers))

    failures: list[str] = []
    changes: list[str] = []
    if not linked_numbers:
        failures.append(f"PR #{pr_number} linked issue를 찾을 수 없습니다. 브랜치명을 feat/<issue>-<slug>로 만들거나 PR 본문에 Closes #N을 추가하세요.")
    if len(linked_numbers) > 1:
        failures.append(f"PR #{pr_number} linked issue 후보가 여러 개입니다: {linked_numbers}. 하나의 대표 이슈만 남겨주세요.")

    if failures:
        return print_github_errors(failures)

    issue_number = linked_numbers[0]
    issue = gh_json(["issue", "view", str(issue_number), "--repo", repo, "--json", "number,title,assignees,milestone,url"])

    append_issue_link_if_missing(pr_number, pr, issue_number, repo, args.dry_run, changes)
    sync_pr_assignees(pr_number, repo, pr, issue, args.dry_run, changes, failures)
    sync_pr_milestone(pr_number, repo, pr, issue, args.dry_run, changes, failures)

    context, _ = load_project_context(repo, owner, args.project_number, args.project_title)
    item = ensure_project_item(context, str(pr["url"]), args.dry_run, changes)
    if item:
        set_project_status(context, item, pr_project_status(pr), f"PR #{pr_number} {pr['title']}", dry_run=args.dry_run, quiet=args.quiet, changes=changes)

    if failures:
        return print_github_errors(failures)

    if changes:
        for change in changes:
            print(f"- {change}")
    elif not args.quiet:
        print(f"PR #{pr_number} 메타데이터 변경 없음")
    return 0


def set_project_status(
    context: ProjectContext,
    item: dict[str, object],
    expected_status: str,
    label: str,
    *,
    dry_run: bool,
    quiet: bool,
    changes: list[str],
) -> None:
    actual_status = item.get("status")
    if actual_status == expected_status:
        if not quiet:
            print(f"[유지] {label}: {expected_status}")
        return

    if dry_run:
        changes.append(f"DRY-RUN status {label}: {actual_status} -> {expected_status}")
        return

    gh_json([
        "project", "item-edit",
        "--id", str(item["id"]),
        "--project-id", context.project_id,
        "--field-id", context.status_field_id,
        "--single-select-option-id", context.status_option_by_name[expected_status],
        "--format", "json",
    ])
    changes.append(f"status {label}: {actual_status} -> {expected_status}")


def sync_issue_items(context: ProjectContext, repo: str, *, dry_run: bool, quiet: bool, changes: list[str]) -> None:
    issues = gh_json(["issue", "list", "--repo", repo, "--state", "all", "--limit", "200", "--json", "number,title,state,labels,url"])
    for issue in issues:
        labels = [label["name"] for label in issue.get("labels", [])]
        expected = item_status_from_labels(labels, issue["state"])
        item = ensure_project_item(context, issue["url"], dry_run, changes)
        if item:
            set_project_status(context, item, expected, f"#{issue['number']} {issue['title']}", dry_run=dry_run, quiet=quiet, changes=changes)


def sync_issue_completion_from_merged_dev_prs(repo: str, *, dry_run: bool, changes: list[str]) -> None:
    prs = gh_json([
        "pr", "list",
        "--repo", repo,
        "--state", "merged",
        "--base", "dev",
        "--limit", "100",
        "--json", "number,title,body,closingIssuesReferences",
    ])
    completed_issue_numbers: dict[int, int] = {}
    repo_owner, repo_name = repo.split("/", 1)
    for pr in prs:
        for issue in pr.get("closingIssuesReferences", []):
            issue_repo = issue.get("repository") or {}
            issue_owner = issue_repo.get("owner") or {}
            if issue_repo.get("name") != repo_name or issue_owner.get("login") != repo_owner:
                continue
            completed_issue_numbers.setdefault(int(issue["number"]), int(pr["number"]))
        for issue_number in parse_closing_issue_numbers_from_body(str(pr.get("body") or "")):
            completed_issue_numbers.setdefault(issue_number, int(pr["number"]))

    for issue_number, pr_number in sorted(completed_issue_numbers.items()):
        issue = gh_json([
            "issue", "view", str(issue_number),
            "--repo", repo,
            "--json", "number,title,state,labels",
        ])
        labels = label_names(issue)
        labels_to_remove = sorted((labels & STATUS_LABELS) - {"status:done"})
        needs_done_label = "status:done" not in labels
        needs_close = issue.get("state") != "CLOSED"
        if not labels_to_remove and not needs_done_label and not needs_close:
            continue

        label = f"#{issue_number} {issue['title']}"
        if dry_run:
            changes.append(f"DRY-RUN merged dev PR #{pr_number} linked issue 완료 처리: {label}")
            continue

        if labels_to_remove:
            run(["gh", "issue", "edit", str(issue_number), "--repo", repo, "--remove-label", ",".join(labels_to_remove)], check=True)
        if needs_done_label:
            run(["gh", "issue", "edit", str(issue_number), "--repo", repo, "--add-label", "status:done"], check=True)
        if needs_close:
            run(["gh", "issue", "close", str(issue_number), "--repo", repo, "--reason", "completed"], check=True)
        changes.append(f"merged dev PR #{pr_number} linked issue 완료 처리: {label}")


def sync_pr_items(context: ProjectContext, repo: str, *, dry_run: bool, quiet: bool, changes: list[str]) -> None:
    prs = gh_json(["pr", "list", "--repo", repo, "--state", "all", "--limit", "100", "--json", "number,title,state,isDraft,url"])
    for pr in prs:
        expected = pr_project_status(pr)
        if pr["state"] != "OPEN" and pr["url"] not in context.item_by_url:
            continue
        item = ensure_project_item(context, pr["url"], dry_run, changes)
        if item:
            set_project_status(context, item, expected, f"PR #{pr['number']} {pr['title']}", dry_run=dry_run, quiet=quiet, changes=changes)


def sync_project(args: argparse.Namespace) -> int:
    repo = args.repo or detect_repo()
    owner = args.owner or repo.split("/", 1)[0]
    if not gh_available():
        raise SystemExit("GitHub CLI 인증이 필요합니다: gh auth login")

    context, _ = load_project_context(repo, owner, args.project_number, args.project_title)
    changes: list[str] = []
    sync_issue_completion_from_merged_dev_prs(repo, dry_run=args.dry_run, changes=changes)
    sync_issue_items(context, repo, dry_run=args.dry_run, quiet=args.quiet, changes=changes)
    sync_pr_items(context, repo, dry_run=args.dry_run, quiet=args.quiet, changes=changes)

    if changes:
        for change in changes:
            print(f"- {change}")
    elif not args.quiet:
        print("프로젝트 상태 변경 없음")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="과제 저장소 자동화 정합성을 점검하고 GitHub 프로젝트 상태를 정렬합니다.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    audit_parser = subparsers.add_parser("audit", help="로컬/원격 자동화 정합성을 점검합니다.")
    audit_parser.add_argument("--repo", help="owner/repo. 기본값은 origin remote입니다.")
    audit_parser.add_argument("--owner", help="프로젝트 owner. 기본값은 repo owner입니다.")
    audit_parser.add_argument("--project-number", type=int, help="GitHub 프로젝트 번호")
    audit_parser.add_argument("--project-title", default=PROJECT_TITLE)
    audit_parser.add_argument("--local-only", action="store_true", help="GitHub API 점검을 건너뜁니다.")
    audit_parser.add_argument("--skip-git-branches", action="store_true", help="origin/main, origin/dev 비교를 건너뜁니다.")
    audit_parser.add_argument("--quiet", action="store_true")
    audit_parser.set_defaults(func=audit)

    sync_parser = subparsers.add_parser("sync-project", help="이슈/PR 라벨과 상태를 GitHub 프로젝트에 반영합니다.")
    sync_parser.add_argument("--repo", help="owner/repo. 기본값은 origin remote입니다.")
    sync_parser.add_argument("--owner", help="프로젝트 owner. 기본값은 repo owner입니다.")
    sync_parser.add_argument("--project-number", type=int, help="GitHub 프로젝트 번호")
    sync_parser.add_argument("--project-title", default=PROJECT_TITLE)
    mode = sync_parser.add_mutually_exclusive_group()
    mode.add_argument("--apply", action="store_false", dest="dry_run", help="실제로 프로젝트 상태를 수정합니다.")
    mode.add_argument("--dry-run", action="store_true", default=True, help="변경 예정 내용만 출력합니다.")
    sync_parser.add_argument("--quiet", action="store_true")
    sync_parser.set_defaults(func=sync_project)

    pr_parser = subparsers.add_parser("sync-pr-metadata", help="PR linked issue, assignee, milestone, project metadata를 자동 보정합니다.")
    pr_parser.add_argument("--pr", type=int, required=True, help="PR 번호")
    pr_parser.add_argument("--repo", help="owner/repo. 기본값은 origin remote입니다.")
    pr_parser.add_argument("--owner", help="프로젝트 owner. 기본값은 repo owner입니다.")
    pr_parser.add_argument("--project-number", type=int, help="GitHub 프로젝트 번호")
    pr_parser.add_argument("--project-title", default=PROJECT_TITLE)
    pr_parser.add_argument("--skip-bots", action="store_true", help="bot 작성 PR은 이슈 기반 메타데이터 강제를 건너뜁니다.")
    mode = pr_parser.add_mutually_exclusive_group()
    mode.add_argument("--apply", action="store_false", dest="dry_run", help="실제로 PR 메타데이터를 수정합니다.")
    mode.add_argument("--dry-run", action="store_true", default=True, help="변경 예정 내용만 출력합니다.")
    pr_parser.add_argument("--quiet", action="store_true")
    pr_parser.set_defaults(func=sync_single_pr_metadata)

    args = parser.parse_args()
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
