from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImpl.java"


def _method_body(source: str, method_name: str) -> str:
    marker = f"private List<ShiftWindow> {method_name}"
    start = source.index(marker)
    brace = source.index("{", start)
    depth = 0
    for index in range(brace, len(source)):
        char = source[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return source[start:index + 1]
    raise AssertionError(f"method {method_name} body not found")


def test_route_process_capacity_window_is_derived_from_required_minutes():
    source = SOURCE.read_text(encoding="utf-8")
    body = _method_body(source, "buildRouteProcessShiftWindows")

    assert "ROUTE_PROCESS_WINDOW_DAYS" not in source
    assert "remainingMinutes > 0 || windows.isEmpty()" in body
    assert "resolveCalendarShiftMode" not in body
    assert "remainingMinutes -= windowMinutes" in body


def test_latest_start_uses_actual_planned_minutes_for_route_process_windows():
    source = SOURCE.read_text(encoding="utf-8")
    assert "routeWindowStart, plannedMinutes)" in source
    assert "routeWindowMinutes * ROUTE_PROCESS_WINDOW_DAYS" not in source
