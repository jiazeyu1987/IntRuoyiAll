from pathlib import Path


SERVICE = Path(
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/"
    "MesProAutoScheduleServiceImpl.java"
)


def read(path: Path) -> str:
    assert path.exists(), f"required file missing: {path}"
    return path.read_text(encoding="utf-8")


def method_body(source: str, signature: str) -> str:
    start = source.find(signature)
    assert start >= 0, f"method signature missing: {signature}"
    brace_start = source.find("{", start)
    assert brace_start >= 0, f"method body missing: {signature}"
    depth = 0
    for index in range(brace_start, len(source)):
        char = source[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return source[brace_start : index + 1]
    raise AssertionError(f"method body not closed: {signature}")


def test_replan_explanation_summary_uses_generated_task_range_not_apply_summary() -> None:
    source = read(SERVICE)
    body = method_body(source, "private MesProReplanExplanationRespVO.Summary buildExplanationSummary(")

    assert "buildGeneratedTaskTimeRange(computation)" in body
    assert "applyRespVO.getSummary()" not in body


def test_generated_task_time_range_is_derived_from_new_tasks_only() -> None:
    source = read(SERVICE)
    body = method_body(source, "private TimeRange buildGeneratedTaskTimeRange(")

    assert "computation.generatedTasks.stream()" in body
    assert ".map(task -> task.startTime)" in body
    assert ".map(task -> task.endTime)" in body
    assert "computation.finalSteps" not in body
    assert "computation.preservedTasks" not in body

