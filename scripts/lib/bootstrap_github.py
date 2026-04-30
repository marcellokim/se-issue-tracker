#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from pathlib import Path
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[2]
LABELS_FILE = ROOT / "config/github/labels.json"
MILESTONES_FILE = ROOT / "config/github/milestones.json"
PROJECT_VARIABLE_NAME = "PROJECT_URL"
REPO_BOOLEAN_SETTINGS = {
    "allow_auto_merge": True,
    "delete_branch_on_merge": True,
}


def run(cmd: list[str], *, check: bool = True, capture_output: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(cmd, cwd=ROOT, check=check, text=True, capture_output=capture_output)


def detect_repo() -> str:
    env_repo = os.getenv("GH_REPO")
    if env_repo:
        return env_repo

    remote = run(["git", "remote", "get-url", "origin"]).stdout.strip()
    if remote.startswith("https://github.com/"):
        return remote.removeprefix("https://github.com/").removesuffix(".git")
    if remote.startswith("git@github.com:"):
        return remote.removeprefix("git@github.com:").removesuffix(".git")
    raise SystemExit(f"origin remote에서 GitHub 저장소를 확인할 수 없습니다: {remote}")


def gh_json(args: list[str]) -> object:
    output = run(["gh", *args]).stdout
    return json.loads(output) if output.strip() else None


def gh_api(method: str, path: str, fields: dict[str, str] | None = None) -> object:
    cmd = ["gh", "api", "-X", method, path]
    for key, value in (fields or {}).items():
        cmd.extend(["-f", f"{key}={value}"])
    output = run(cmd).stdout
    return json.loads(output) if output.strip() else None


def sync_labels(repo: str) -> list[str]:
    desired = json.loads(LABELS_FILE.read_text())
    current = gh_json(["api", f"repos/{repo}/labels?per_page=100"])
    current_by_name = {item["name"]: item for item in current}
    summary: list[str] = []

    for label in desired:
        name = label["name"]
        if name in current_by_name:
            gh_api(
                "PATCH",
                f"repos/{repo}/labels/{quote(name, safe='')}",
                {
                    "new_name": name,
                    "color": label["color"],
                    "description": label["description"],
                },
            )
            summary.append(f"라벨 갱신: {name}")
        else:
            gh_api(
                "POST",
                f"repos/{repo}/labels",
                {
                    "name": name,
                    "color": label["color"],
                    "description": label["description"],
                },
            )
            summary.append(f"라벨 생성: {name}")

    return summary


def sync_repo_settings(repo: str) -> list[str]:
    current = gh_json(["api", f"repos/{repo}"])
    summary: list[str] = []

    for field, expected in REPO_BOOLEAN_SETTINGS.items():
        actual = current.get(field)
        if actual == expected:
            summary.append(f"저장소 설정 유지: {field}={expected}")
            continue

        gh_api("PATCH", f"repos/{repo}", {field: str(expected).lower()})
        summary.append(f"저장소 설정 갱신: {field}={expected}")

    return summary


def sync_milestones(repo: str) -> list[str]:
    desired = json.loads(MILESTONES_FILE.read_text())
    current = gh_json(["api", f"repos/{repo}/milestones?state=all&per_page=100"])
    current_titles = {item["title"] for item in current}
    summary: list[str] = []

    for milestone in desired:
        if milestone["title"] in current_titles:
            current_item = next(item for item in current if item["title"] == milestone["title"])
            if current_item.get("description") == milestone["description"]:
                summary.append(f"마일스톤 유지: {milestone['title']}")
                continue
            gh_api(
                "PATCH",
                f"repos/{repo}/milestones/{current_item['number']}",
                {
                    "title": milestone["title"],
                    "description": milestone["description"],
                },
            )
            summary.append(f"마일스톤 설명 갱신: {milestone['title']}")
            continue
        gh_api(
            "POST",
            f"repos/{repo}/milestones",
            {
                "title": milestone["title"],
                "description": milestone["description"],
            },
        )
        summary.append(f"마일스톤 생성: {milestone['title']}")

    return summary


def list_projects(owner: str) -> list[dict[str, object]]:
    data = gh_json(["project", "list", "--owner", owner, "--format", "json"])
    return data.get("projects", []) if isinstance(data, dict) else []


def ensure_project(owner: str, title: str, create_if_missing: bool) -> tuple[str, str | None]:
    for project in list_projects(owner):
        if project.get("title") == title:
            return f"Project 유지: {title}", project.get("url")

    if not create_if_missing:
        return f"Project 생성을 건너뜀 (--create-project 사용 시 생성): {title}", None

    run(["gh", "project", "create", "--owner", owner, "--title", title])
    for project in list_projects(owner):
        if project.get("title") == title:
            return f"Project 생성: {title}", project.get("url")

    return f"Project 생성: {title}", None


def set_project_variable(repo: str, project_url: str | None) -> str:
    if not project_url:
        return f"저장소 변수 {PROJECT_VARIABLE_NAME} 설정 건너뜀 (Project URL 확인 불가)"

    run(["gh", "variable", "set", PROJECT_VARIABLE_NAME, "--repo", repo, "--body", project_url])
    return f"저장소 변수 설정: {PROJECT_VARIABLE_NAME}={project_url}"


def main() -> int:
    parser = argparse.ArgumentParser(description="GitHub 라벨, 마일스톤, Project를 초기 설정합니다.")
    parser.add_argument("--repo", help="owner/repo를 직접 지정합니다. 기본값은 origin remote에서 확인합니다.")
    parser.add_argument("--project-title", default="SE 2026-1 텀프로젝트", help="확인하거나 생성할 Project 제목")
    parser.add_argument("--create-project", action="store_true", help="GitHub Project가 없으면 생성합니다")
    args = parser.parse_args()

    repo = args.repo or detect_repo()
    owner = repo.split("/", 1)[0]

    print(f"저장소: {repo}")
    print(f"소유자: {owner}")

    for item in sync_labels(repo):
        print(f"- {item}")

    for item in sync_milestones(repo):
        print(f"- {item}")

    for item in sync_repo_settings(repo):
        print(f"- {item}")

    project_result, project_url = ensure_project(owner, args.project_title, args.create_project)
    print(f"- {project_result}")
    print(f"- {set_project_variable(repo, project_url)}")

    print("\n수동으로 확인할 항목:")
    print("1. main/dev 브랜치 보호 규칙을 확인합니다")
    print("2. 브랜치 보호 필수 체크가 Gradle CI의 'build' job과 맞는지 확인합니다")
    print("3. GitHub Project 보기/필드를 대기, 준비됨, 진행 중, 리뷰 중, 완료 흐름에 맞춥니다")
    print("4. CODEOWNERS placeholder를 실제 팀원 GitHub ID로 교체합니다")
    return 0


if __name__ == "__main__":
    sys.exit(main())
