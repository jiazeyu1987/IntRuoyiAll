from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
CONTROLLER_PATH = (
    REPO_ROOT
    / "yudao-module-mes"
    / "src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesActiveOrderDossierFileController.java"
)


def read_controller() -> str:
    assert CONTROLLER_PATH.exists(), "missing active order dossier file controller"
    return CONTROLLER_PATH.read_text(encoding="utf-8")


def test_batch_execution_upload_permission_can_list_and_upload_dossier_files() -> None:
    source = read_controller()

    assert (
        "@ss.hasAnyPermissions('mes:pro-process-pool-team-leader:query', "
        "'mes:pro-production-release:query', 'mes:pro-edhr-batch-execution:upload')"
    ) in source
    assert (
        "@ss.hasAnyPermissions('mes:pro-process-pool-team-leader:maintain', "
        "'mes:pro-production-release:pqc-approve', 'mes:pro-edhr-batch-execution:upload')"
    ) in source


def test_batch_execution_upload_permission_does_not_delete_dossier_files() -> None:
    source = read_controller()
    delete_start = source.index('@PostMapping("/delete")')
    delete_block = source[delete_start: source.index('public CommonResult<Boolean> delete', delete_start)]

    assert "mes:pro-edhr-batch-execution:upload" not in delete_block
