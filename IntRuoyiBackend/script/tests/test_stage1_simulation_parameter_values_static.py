from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SERVICE_PATH = (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/"
    "MesTeamLeaderActiveOrderSimulationService.java"
)


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def test_stage1_numeric_parameter_text_uses_generated_value_not_standard_range():
    service = read(SERVICE_PATH)
    text_method = service[
        service.index("private String resolveSimulationParameterText("):
        service.index("private String resolveSimulationParameterStatus(")
    ]

    assert "formatSimulationParameterValue(value)" in text_method
    assert text_method.index("formatSimulationParameterValue(value)") < text_method.index("getDefaultText()")
    assert text_method.index("formatSimulationParameterValue(value)") < text_method.index("getStandardText()")
    assert 'return value == null ? null : value.toPlainString();' not in text_method


def test_stage1_numeric_parameter_value_strips_trailing_zeros_for_submission_text():
    service = read(SERVICE_PATH)

    assert "private String formatSimulationParameterValue(BigDecimal value)" in service
    assert "stripTrailingZeros().toPlainString()" in service
    assert 'reading.put("textValue", resolveSimulationParameterText(rule, value));' in service


if __name__ == "__main__":
    test_stage1_numeric_parameter_text_uses_generated_value_not_standard_range()
    test_stage1_numeric_parameter_value_strips_trailing_zeros_for_submission_text()
    print("PASS: stage1 simulation parameter values static contract")
