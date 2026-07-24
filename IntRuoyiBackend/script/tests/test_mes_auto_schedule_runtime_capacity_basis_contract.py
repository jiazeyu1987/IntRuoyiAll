from pathlib import Path


REQ_VO = Path(
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/schedule/vo/"
    "MesProAutoSchedulePreviewReqVO.java"
)
AUTO_SCHEDULE_SERVICE = Path(
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/"
    "MesProAutoScheduleServiceImpl.java"
)
NIGHTLY_REPLAN_SERVICE = Path(
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/"
    "MesProNightlyReplanServiceImpl.java"
)
ROUTE_SCHEDULE_CONFIG_REQ = Path(
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/vo/"
    "scheduleconfig/MesProRouteScheduleConfigSaveReqVO.java"
)


def read(path: Path) -> str:
    assert path.exists(), f"required file missing: {path}"
    return path.read_text(encoding="utf-8")


def test_auto_schedule_request_names_planned_actual_as_runtime_capacity_basis():
    source = read(REQ_VO)

    assert "private String runtimeCapacityBasis;" in source
    assert "运行时产能基准不能为空" in source
    assert "private String capacityMode;" not in source


def test_auto_schedule_services_read_runtime_capacity_basis_not_route_capacity_mode():
    auto_schedule_source = read(AUTO_SCHEDULE_SERVICE)
    nightly_source = read(NIGHTLY_REPLAN_SERVICE)

    assert "reqVO.getRuntimeCapacityBasis()" in auto_schedule_source
    assert "reqVO.getCapacityMode()" not in auto_schedule_source
    assert "reqVO.setRuntimeCapacityBasis(CAPACITY_MODE_PLANNED)" in nightly_source
    assert "reqVO.setCapacityMode(CAPACITY_MODE_PLANNED)" not in nightly_source


def test_route_schedule_strategy_keeps_capacity_mode_contract():
    source = read(ROUTE_SCHEDULE_CONFIG_REQ)

    assert "private String capacityMode;" in source
    assert "runtimeCapacityBasis" not in source
