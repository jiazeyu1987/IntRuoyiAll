from pathlib import Path
import re


def test_schedule_order_service_injects_workstation_mapper():
    source = Path(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/"
        "service/pro/scheduleorder/MesProScheduleOrderServiceImpl.java"
    ).read_text(encoding="utf-8")

    assert re.search(
        r"@Resource\s+private\s+MesMdWorkstationMapper\s+workstationMapper\s*;",
        source,
    ), "MesProScheduleOrderServiceImpl must inject MesMdWorkstationMapper before building schedule resource snapshots"
