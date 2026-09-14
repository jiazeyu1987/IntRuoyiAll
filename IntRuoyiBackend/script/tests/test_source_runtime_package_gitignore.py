from pathlib import Path
import subprocess


REPO_ROOT = Path(__file__).resolve().parents[3]
FILTER_SOURCE = (
    "IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/"
    "bpm/formcenter/runtime/FormTemplateJimuReportSaveSyncFilter.java"
)


def run_git(*args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["git", "-C", str(REPO_ROOT), *args],
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        check=False,
    )


def test_runtime_java_package_source_is_not_hidden_by_gitignore() -> None:
    check_ignore = run_git("check-ignore", "-q", "--", FILTER_SOURCE)

    assert check_ignore.returncode == 1, (
        "Java source packages named runtime must not be ignored as runtime output. "
        f"git check-ignore output: {check_ignore.stdout}"
    )


def test_jimu_save_sync_filter_source_is_tracked_for_release_worktrees() -> None:
    tracked = run_git("ls-files", "--error-unmatch", "--", FILTER_SOURCE)

    assert tracked.returncode == 0, (
        "Release worktrees only contain committed files; the Jimu save sync filter "
        f"must be tracked. git ls-files output: {tracked.stdout}"
    )
