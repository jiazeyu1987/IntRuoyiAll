from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
PROJECT_ROOT = REPO_ROOT.parent
FRONTEND_ROOT = PROJECT_ROOT / "yudao-ui-admin-vue3"

GUARD_DOC_PATH = PROJECT_ROOT / "docs" / "engineering" / "stable-identity-version-binding-guard.md"
BPM_POLICY_VO_PATH = (
    REPO_ROOT
    / "yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/controller/admin/formcenter/vo/FormPolicySlotReqVO.java"
)
BPM_RUNTIME_PATH = (
    REPO_ROOT
    / "yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java"
)
BPM_POLICY_PAGE_PATH = FRONTEND_ROOT / "src/views/form-center/policy/index.vue"
DCC_ASSIGNMENT_SERVICE_PATH = (
    REPO_ROOT
    / "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/assignment/"
    / "DccProjectCodeAssignmentServiceImpl.java"
)
EDHR_BATCH_EXECUTION_SERVICE_PATH = (
    REPO_ROOT
    / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
    / "MesProEdhrBatchExecutionServiceImpl.java"
)
ROUTE_VERSION_MAPPER_PATH = (
    REPO_ROOT
    / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/route/MesProRouteVersionMapper.java"
)


def read(path: Path) -> str:
    assert path.exists(), f"missing required contract file: {path}"
    return path.read_text(encoding="utf-8")


def between(source: str, start: str, end: str) -> str:
    start_index = source.index(start)
    end_index = source.index(end, start_index)
    return source[start_index:end_index]


def test_engineering_guard_documents_stable_identity_and_freeze_boundaries() -> None:
    text = read(GUARD_DOC_PATH)

    for keyword in [
        "配置身份绑定",
        "运行实例冻结",
        "BPM 表单中心",
        "DCC 项目代码分配",
        "eDHR 批记录",
        "排产",
        "禁止 fallback 到旧版本",
    ]:
        assert keyword in text


def test_bpm_policy_save_surface_uses_stable_template_identity() -> None:
    vo = read(BPM_POLICY_VO_PATH)
    policy_page = read(BPM_POLICY_PAGE_PATH)

    assert "private Long templateId;" in vo
    assert "templateVersionId" not in vo
    assert "templateId" in policy_page
    assert "templateVersionId" not in policy_page


def test_bpm_runtime_resolves_latest_published_template_from_policy_identity() -> None:
    runtime = read(BPM_RUNTIME_PATH)
    to_policy_slot = between(runtime, "private FormPolicySlot toPolicySlot", "private List<FormPolicySlot>")
    resolve_runtime = between(runtime, "private FormTemplateVersionDO requireLatestPublishedTemplateVersion", "private void requireStatus")

    assert "slotReqVO.getTemplateId()" in to_policy_slot
    assert "selectLatestPublishedByTemplateId" in to_policy_slot
    assert "FormTemplateVersionRef.of(version.getId(), String.valueOf(version.getTemplateId())" in to_policy_slot
    assert "parseTemplateId(slot, slot.getTemplateVersionRef())" in resolve_runtime
    assert "selectLatestPublishedByTemplateId(tenantId, templateId)" in resolve_runtime
    assert "templateVersionId" not in to_policy_slot


def test_dcc_project_code_assignment_resolves_latest_file_by_master_identity() -> None:
    service = read(DCC_ASSIGNMENT_SERVICE_PATH)
    scope_check = between(service, "private DccProjectCodeAssignmentFileDO resolveCurrentApprovedAssignmentFile", "private Map<Long, DccControlledFileDO>")
    list_resolution = between(service, "private Map<Long, DccControlledFileDO> selectLatestApprovedFileMapByMasterId", "private DccProjectCodeDO validateProjectCode")

    assert "requestedFile.getMasterId()" in scope_check
    assert "controlledFileMapper.selectLatestApprovedByMasterId(requestedFile.getMasterId())" in scope_check
    assert "selectByAssignmentIdAndMasterId(assignmentId, requestedFile.getMasterId())" in scope_check
    assert "selectByAssignmentIdAndControlledFileId" not in scope_check
    assert "assignmentFile.getMasterId()" in list_resolution
    assert "controlledFileMapper.selectLatestApprovedByMasterId(masterId)" in list_resolution
    assert "latestApprovedFile == null" in list_resolution


def test_edhr_route_form_binding_requires_definition_identity_and_latest_approved_member() -> None:
    service = read(EDHR_BATCH_EXECUTION_SERVICE_PATH)
    binding_resolution = between(service, "private ResolvedRouteFormBinding resolveLatestApprovedRouteFormBinding", "private String normalizeFormSlotType")

    assert "record.getBatchRecordDefinitionId()" in binding_resolution
    assert "boundReport.getBatchRecordDefinitionId()" in binding_resolution
    assert "if (definitionId == null)" in binding_resolution
    assert "selectLatestApprovedByDefinitionId(definitionId)" in binding_resolution
    assert "resolveLatestApprovedMemberReport(" in binding_resolution
    assert "boundReport.getSourceTableIndex() == null" in binding_resolution
    assert "selectListByDefinitionIdAndVersionId(definitionId, latestVersionId)" in binding_resolution
    assert "boundReport.getSourceTableIndex()" in binding_resolution
    assert "normalizeFormSlotType(report.getFormSlotType())" in binding_resolution
    assert "matches.size() != 1" in binding_resolution


def test_schedule_runtime_freezes_only_active_route_version() -> None:
    mapper = read(ROUTE_VERSION_MAPPER_PATH)
    select_active = between(mapper, "default MesProRouteVersionDO selectActiveByRouteId", "default MesProRouteVersionDO selectByRouteIdAndVersionNo")

    assert ".eq(MesProRouteVersionDO::getRouteId, routeId)" in select_active
    assert ".eq(MesProRouteVersionDO::getActive, Boolean.TRUE)" in select_active
    assert ".eq(MesProRouteVersionDO::getLifecycleStatus, STATUS_ACTIVE)" in select_active
    assert 'String STATUS_ACTIVE = "ACTIVE";' in mapper
