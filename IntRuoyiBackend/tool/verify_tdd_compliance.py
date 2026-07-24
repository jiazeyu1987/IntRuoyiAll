from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path
from typing import Iterable


AREA_MESSAGES = {
    "backend": "backend production changes require a changed Java test under src/test/.",
    "script": "repo tooling changes under script/ or sql/mysql/ require a changed script test under script/tests/.",
    "tool": "tooling production changes require a changed tooling test under tool/tests/.",
}

EVIDENCE_MESSAGE = (
    "TDD evidence must include a RED line with FAIL and a GREEN line with PASS in execution-log.md."
)

BACKEND_EXTENSIONS = {".java", ".kt", ".groovy", ".xml", ".yml", ".yaml", ".properties"}
SCRIPT_EXTENSIONS = {".ps1", ".py", ".sql", ".sh"}
TOOL_EXTENSIONS = {".py"}


def evaluate_compliance(
    changed_paths: Iterable[str | Path],
    *,
    repo_root: Path,
    task_dir: Path | None,
) -> list[str]:
    paths = [_normalize_path(path, repo_root) for path in changed_paths]
    production_areas = {
        area for path in paths for area in [_production_area_for(path)] if area is not None
    }
    if not production_areas:
        return []

    test_areas = {area for path in paths for area in [_test_area_for(path)] if area is not None}
    findings = [AREA_MESSAGES[area] for area in sorted(production_areas - test_areas)]

    evidence_log = _resolve_evidence_log(repo_root, task_dir)
    if evidence_log is None:
        findings.append("production changes require --task-dir pointing to a task directory.")
    elif not evidence_log.exists():
        findings.append(f"production changes require {evidence_log} to exist.")
    elif not _has_red_green_evidence(evidence_log):
        findings.append(EVIDENCE_MESSAGE)

    return findings


def collect_changed_paths(repo_root: Path, *, all_changed: bool) -> list[str]:
    args = ["git", "diff", "--name-only", "--diff-filter=ACMRTD"]
    if all_changed:
        args.append("HEAD")
    else:
        args.append("--cached")
    result = subprocess.run(
        args,
        cwd=repo_root,
        capture_output=True,
        encoding="utf-8",
        text=True,
        check=False,
    )
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or "git diff failed")
    paths = [line.strip() for line in result.stdout.splitlines() if line.strip()]
    if all_changed:
        untracked_result = subprocess.run(
            ["git", "ls-files", "--others", "--exclude-standard"],
            cwd=repo_root,
            capture_output=True,
            encoding="utf-8",
            text=True,
            check=False,
        )
        if untracked_result.returncode != 0:
            raise RuntimeError(untracked_result.stderr.strip() or "git ls-files failed")
        seen = set(paths)
        for line in untracked_result.stdout.splitlines():
            path = line.strip()
            if path and path not in seen:
                paths.append(path)
                seen.add(path)
    return paths


def build_pre_commit_hook(*, script_path: str = "tool/verify_tdd_compliance.py") -> str:
    return "\n".join(
        [
            "#!/bin/sh",
            "# IntRuoyi strict TDD gate",
            'if [ -z "$TDD_TASK_DIR" ]; then',
            '  echo "TDD compliance failed: set TDD_TASK_DIR to the task directory path before committing." >&2',
            "  exit 1",
            "fi",
            f'python "{script_path}" --task-dir "$TDD_TASK_DIR"',
            "",
        ]
    )


def install_pre_commit_hook(repo_root: Path, *, force: bool = False, script_path: str | None = None) -> Path:
    hook_path = _git_path(repo_root, "hooks/pre-commit")
    hook_text = build_pre_commit_hook(script_path=script_path or _relative_script_path(repo_root))
    if hook_path.exists():
        current = hook_path.read_text(encoding="utf-8")
        if current != hook_text and not force:
            raise RuntimeError(f"pre-commit hook already exists at {hook_path}; rerun with --force to replace it.")
    hook_path.parent.mkdir(parents=True, exist_ok=True)
    hook_path.write_text(hook_text, encoding="utf-8", newline="\n")
    hook_path.chmod(0o755)
    return hook_path


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Fail fast when Ruoyi production changes do not carry strict TDD evidence."
    )
    parser.add_argument("--repo", default=".", help="Git repository root. Defaults to cwd.")
    parser.add_argument(
        "--task-dir",
        help="Task directory containing execution-log.md with explicit RED and GREEN evidence.",
    )
    parser.add_argument(
        "--all-changed",
        action="store_true",
        help="Check all changes against HEAD instead of only staged changes.",
    )
    parser.add_argument(
        "--paths",
        nargs="+",
        help="Explicit changed paths to check. When provided, git diff is not used.",
    )
    parser.add_argument(
        "--install-hook",
        action="store_true",
        help="Install the strict TDD pre-commit hook into this git repository.",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Replace an existing pre-commit hook when used with --install-hook.",
    )
    args = parser.parse_args(argv)

    repo_root = Path(args.repo).resolve()
    task_dir = Path(args.task_dir) if args.task_dir else None

    try:
        if args.install_hook:
            hook_path = install_pre_commit_hook(repo_root, force=args.force)
            print(f"Installed strict TDD pre-commit hook: {hook_path}")
            return 0
        changed_paths = args.paths or collect_changed_paths(repo_root, all_changed=args.all_changed)
        findings = evaluate_compliance(changed_paths, repo_root=repo_root, task_dir=task_dir)
    except RuntimeError as exc:
        print(f"TDD compliance check could not run: {exc}", file=sys.stderr)
        return 2

    if findings:
        print("TDD compliance failed:")
        for finding in findings:
            print(f"- {finding}")
        return 1

    if changed_paths:
        print("TDD compliance passed.")
    else:
        print("TDD compliance passed: no changed paths.")
    return 0


def _normalize_path(path: str | Path, repo_root: Path) -> str:
    candidate = Path(path)
    if candidate.is_absolute():
        try:
            candidate = candidate.resolve().relative_to(repo_root.resolve())
        except ValueError:
            return str(candidate).replace("\\", "/")
    return str(candidate).replace("\\", "/").lstrip("./")


def _production_area_for(path: str) -> str | None:
    normalized = path.replace("\\", "/")
    suffix = Path(normalized).suffix.lower()

    if normalized.startswith("tool/") and not normalized.startswith("tool/tests/") and suffix in TOOL_EXTENSIONS:
        return "tool"
    if normalized.startswith("script/") and not normalized.startswith("script/tests/") and suffix in SCRIPT_EXTENSIONS:
        return "script"
    if normalized.startswith("sql/") and suffix == ".sql":
        return "script"
    if "/src/main/" in normalized and suffix in BACKEND_EXTENSIONS:
        return "backend"
    return None


def _test_area_for(path: str) -> str | None:
    normalized = path.replace("\\", "/")
    if normalized.startswith("tool/tests/"):
        return "tool"
    if normalized.startswith("script/tests/"):
        return "script"
    if "/src/test/" in normalized:
        return "backend"
    return None


def _resolve_evidence_log(repo_root: Path, task_dir: Path | None) -> Path | None:
    if task_dir is None:
        return None
    if task_dir.is_absolute():
        return task_dir / "execution-log.md"
    return repo_root / task_dir / "execution-log.md"


def _git_path(repo_root: Path, pathspec: str) -> Path:
    result = subprocess.run(
        ["git", "rev-parse", "--git-path", pathspec],
        cwd=repo_root,
        capture_output=True,
        encoding="utf-8",
        text=True,
        check=False,
    )
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or "git rev-parse failed")
    git_path = Path(result.stdout.strip())
    if git_path.is_absolute():
        return git_path
    return repo_root / git_path


def _relative_script_path(repo_root: Path) -> str:
    script = Path(__file__).resolve()
    try:
        return script.relative_to(repo_root.resolve()).as_posix()
    except ValueError:
        return Path(os.path.relpath(script, repo_root.resolve())).as_posix()


def _has_red_green_evidence(evidence_log: Path) -> bool:
    text = evidence_log.read_text(encoding="utf-8")
    red = re.search(
        r"^\s*(?:-\s*)?RED\s*:.*\bFAIL(?:ED|URE)?\b",
        text,
        re.IGNORECASE | re.MULTILINE,
    )
    green = re.search(
        r"^\s*(?:-\s*)?GREEN\s*:.*\bPASS(?:ED)?\b",
        text,
        re.IGNORECASE | re.MULTILINE,
    )
    return red is not None and green is not None


if __name__ == "__main__":
    raise SystemExit(main())
