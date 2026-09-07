from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SERVICE_PATH = (
    "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/"
    "MesTeamLeaderActiveOrderSimulationService.java"
)


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def test_stage1_material_payload_uses_selected_device_parameter_readings():
    service = read(SERVICE_PATH)

    assert "MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper" in service
    assert "resolveSimulationDeviceParameterReadings(snapshot, leaderUserId, defaultDevice)" in service
    assert "buildSimulationDeviceParameterReading(" in service
    assert "resolveSimulationParameterValue(rule)" in service
    assert 'detail.put("deviceParameterReadings", deviceParameterReadings);' in service
    assert 'JsonUtils.toJsonString(deviceParameterReadings)' in service


def test_stage1_no_longer_hardcodes_empty_device_parameter_readings():
    service = read(SERVICE_PATH)
    output_method = service[
        service.index("private Map<String, Object> outputMaterialDetail("):
        service.index("private Map<String, Object> materialIdentity(")
    ]
    material_entry_method = service[
        service.index("private MesProFeedbackMaterialCreateCommand.Entry toSimulationMaterialEntry("):
        service.index("private SimulationDevice resolveDefaultSimulationDevice(")
    ]

    assert 'detail.put("deviceParameterReadings", List.of())' not in output_method
    assert "JsonUtils.toJsonString(List.of())" not in material_entry_method


if __name__ == "__main__":
    test_stage1_material_payload_uses_selected_device_parameter_readings()
    test_stage1_no_longer_hardcodes_empty_device_parameter_readings()
    print("PASS: stage1 simulation device parameters static contract")

