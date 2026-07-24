from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CONTROLLER_PATH = (
    ROOT
    / "yudao-module-bpm"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "bpm"
    / "controller"
    / "admin"
    / "task"
    / "BpmProcessInstanceController.java"
)
ERROR_CODE_PATH = (
    ROOT
    / "yudao-module-bpm"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "bpm"
    / "enums"
    / "ErrorCodeConstants.java"
)


def read_controller() -> str:
    assert CONTROLLER_PATH.exists(), "missing BpmProcessInstanceController"
    return CONTROLLER_PATH.read_text(encoding="utf-8")


def test_bpm_process_instance_full_detail_is_gated_by_bpm_admin_or_participant() -> None:
    text = read_controller()

    assert "PermissionApi" in text
    assert "RoleCodeEnum.BPM_ADMIN.getCode()" in text
    assert "hasAnyRolesOrSuperAdmin(loginUserId, RoleCodeEnum.BPM_ADMIN.getCode())" in text
    assert "assertCanReadProcessInstance(getLoginUserId(), processInstance)" in text
    assert "assertCanReadProcessInstance(getLoginUserId(), historicProcessInstance)" in text
    assert "if (StrUtil.isNotBlank(reqVO.getProcessInstanceId()))" in text
    assert "assertCanReadProcessInstance(getLoginUserId(), reqVO.getProcessInstanceId())" in text
    assert "hasTaskParticipant(loginUserId, processInstance.getId())" in text
    assert "PROCESS_INSTANCE_ACCESS_DENIED" in text


def test_bpm_process_instance_model_and_print_routes_are_not_public_full_detail_paths() -> None:
    text = read_controller()

    model_method = text.split("getProcessInstanceBpmnModelView(", 1)[0].rsplit("@GetMapping", 1)[1]
    print_method = text.split("getProcessInstancePrintData(", 1)[0].rsplit("@GetMapping", 1)[1]

    assert "@PreAuthorize(\"@ss.hasPermission('bpm:process-instance:query')\")" in model_method
    assert "assertCanReadProcessInstance(getLoginUserId(), id)" in text
    assert "@PreAuthorize(\"@ss.hasPermission('bpm:process-instance:query')\")" in print_method
    assert "assertCanReadProcessInstance(getLoginUserId(), historicProcessInstance)" in text


def test_bpm_access_denied_error_code_is_explicit() -> None:
    text = ERROR_CODE_PATH.read_text(encoding="utf-8")

    assert "PROCESS_INSTANCE_ACCESS_DENIED" in text
    assert "流程实例访问失败，只有发起人、处理人或 BPM 管理员可查看该流程" in text
