from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
INT_RUOYI_ROOT = REPO_ROOT.parent
FRONTEND_RUNTIME_CONTROL = (
    INT_RUOYI_ROOT
    / "yudao-ui-admin-vue3"
    / "src"
    / "views"
    / "infra"
    / "runtime-control"
    / "index.vue"
)
BACKEND_ACTION_SOURCE = (
    REPO_ROOT
    / "yudao-module-infra"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "infra"
    / "service"
    / "runtimecontrol"
    / "RuntimeControlOperationAction.java"
)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_mark_release_tested_ui_no_longer_requires_manual_release_tag():
    vue_text = read_text(FRONTEND_RUNTIME_CONTROL)

    assert "operationRequiresReleaseTag" in vue_text
    assert "['publish-test', 'promote-prod', 'promote-backup'].includes(action)" in vue_text
    assert "mark-release-tested" in vue_text
    assert "当前测试服发布包" in vue_text
    assert "testCurrentReleaseTag || '无'" in vue_text
    assert "验证结论" in vue_text
    assert "operationDialog.testConclusion" in vue_text


def test_mark_release_tested_backend_still_routes_to_same_script_mode():
    action_text = read_text(BACKEND_ACTION_SOURCE)

    assert "MARK_RELEASE_TESTED(\"mark-release-tested\", \"标记测试通过\", \"test\"" in action_text
    assert "markReleaseTestedArguments" in action_text
    assert 'args.add("mark-tested")' in action_text
    assert 'args.add("-TestConclusion")' in action_text
    assert 'args.add("-SelectedRecoverySetCandidateId")' in action_text
    assert 'args.add("-RecoverySetManifestHash")' in action_text
