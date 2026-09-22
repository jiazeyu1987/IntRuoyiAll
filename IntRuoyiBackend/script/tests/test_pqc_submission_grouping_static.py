from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
MES_JAVA = ROOT / "IntRuoyiBackend" / "yudao-module-mes" / "src" / "main" / "java"
MES_RESOURCES = ROOT / "IntRuoyiBackend" / "yudao-module-mes" / "src" / "main" / "resources"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_timeline_contract_exposes_formal_group_sources():
    read_do = read(
        MES_JAVA
        / "cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/ProcessPoolTimelineEventReadDO.java"
    )
    response_vo = read(
        MES_JAVA
        / "cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/vo/ProcessPoolTimelineEventRespVO.java"
    )
    assert "groupedEventIds" in read_do
    assert "groupedOriginalPayloadJsons" in response_vo


def test_timeline_mapper_groups_only_by_explicit_submission_identity():
    mapper = read(
        MES_RESOURCES
        / "mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml"
    )
    assert "pqcSubmissionGroupId" in mapper
    assert "COUNT(DISTINCT" in mapper
    assert "MAX(group_event.id)" in mapper
    assert "selectPqcSubmissionGroupPayloadsByGroupIds" in mapper


def test_formal_frontline_submit_carries_one_group_id_across_task_payloads():
    frontend = read(
        ROOT
        / "IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue"
    )
    assert "pqcSubmissionGroupId" in frontend
    assert "crypto.randomUUID()" in frontend


def test_stage1_simulation_carries_one_group_id_for_same_submission_action():
    simulation = read(
        MES_JAVA
        / "cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderSimulationService.java"
    )
    assert "pqcSubmissionGroupId" in simulation
    assert "simulationRunId" in simulation
