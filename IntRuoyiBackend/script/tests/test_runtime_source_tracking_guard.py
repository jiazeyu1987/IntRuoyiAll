import subprocess
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2].parent

RUNTIME_SOURCE_FILES = [
    "IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeService.java",
    "IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterBpmEventBridge.java",
    "IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java",
    "IntRuoyiBackend/yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/sync/runtime/ErpKingdeeSyncCommand.java",
    "IntRuoyiBackend/yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/sync/runtime/ErpKingdeeSyncContext.java",
    "IntRuoyiBackend/yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/sync/runtime/ErpKingdeeSyncRunResult.java",
    "IntRuoyiBackend/yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/sync/runtime/ErpKingdeeSyncRuntimeService.java",
    "IntRuoyiBackend/yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/sync/runtime/ErpKingdeeSyncRuntimeServiceImpl.java",
    "IntRuoyiBackend/yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/sync/runtime/ErpKingdeeSyncTask.java",
]

FORM_CENTER_RUNTIME_IMPL = (
    REPO_ROOT
    / "IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java"
)
def test_backend_runtime_java_sources_are_present_and_not_ignored() -> None:
    missing = [path for path in RUNTIME_SOURCE_FILES if not (REPO_ROOT / path).exists()]
    assert not missing, "missing runtime Java source files: " + ", ".join(missing)

    ignored = []
    for path in RUNTIME_SOURCE_FILES:
        result = subprocess.run(
            ["git", "check-ignore", "--quiet", "--", path],
            cwd=REPO_ROOT,
            check=False,
        )
        if result.returncode == 0:
            ignored.append(path)
        else:
            assert result.returncode == 1, f"git check-ignore failed for {path}: {result.returncode}"
    assert not ignored, "runtime Java source files must not be ignored by Git: " + ", ".join(ignored)

def test_form_center_runtime_resolves_plugin_beans_lazily() -> None:
    source = FORM_CENTER_RUNTIME_IMPL.read_text(encoding="utf-8")

    assert "ObjectProvider<FormBusinessEffectExecutor>" in source
    assert "ObjectProvider<FormControlledActionLifecycleAdapter>" in source
    assert "private List<FormBusinessEffectExecutor> effectExecutors =" not in source
    assert "private List<FormControlledActionLifecycleAdapter> lifecycleAdapters =" not in source
