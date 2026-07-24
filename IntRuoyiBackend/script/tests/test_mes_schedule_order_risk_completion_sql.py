from pathlib import Path


MAPPER_PATH = (
    Path(__file__).resolve().parents[2]
    / "yudao-module-mes"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "mes"
    / "dal"
    / "mysql"
    / "pro"
    / "scheduleorder"
    / "MesProScheduleOrderMapper.java"
)


def test_nightly_replan_orders_by_promise_date_before_priority():
    source = MAPPER_PATH.read_text(encoding="utf-8")
    method_start = source.index("selectListForNightlyReplan")
    method_end = source.index("selectListWithoutRoute", method_start)
    method = source[method_start:method_end]

    promise_index = method.index("orderByAsc(MesProScheduleOrderDO::getPromiseDate)")
    priority_index = method.index("orderByAsc(MesProScheduleOrderDO::getPriorityNo)")
    id_index = method.index("orderByAsc(MesProScheduleOrderDO::getId)")

    assert promise_index < priority_index < id_index
