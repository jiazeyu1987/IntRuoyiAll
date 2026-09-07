from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
SIMULATION_SERVICE = ROOT / "IntRuoyiBackend" / "yudao-module-mes" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "mes" / "service" / "pro" / "processpool" / "team" / "MesTeamLeaderActiveOrderSimulationService.java"
DETAIL_SERVICE = ROOT / "IntRuoyiBackend" / "yudao-module-mes" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "mes" / "service" / "pro" / "processpool" / "team" / "MesTeamLeaderActiveOrderDetailServiceImpl.java"
STAGE1_SERVICE = ROOT / "IntRuoyiBackend" / "yudao-module-mes" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "mes" / "service" / "pro" / "simulation" / "stage1" / "MesStage1ActiveOrderCompleteSimulationServiceImpl.java"


def main() -> None:
    source = SIMULATION_SERVICE.read_text(encoding="utf-8")
    required = [
        'detail.put("selectedDevices"',
        '"inMeteringValidityPeriod", Boolean.TRUE',
        'detail.put("clearanceConfirmations"',
        'clearanceConfirmation("workplace", "清场")',
        'clearanceConfirmation("material", "物料")',
        'clearanceConfirmation("cleaning", "清洁")',
    ]
    missing = [item for item in required if item not in source]
    if missing:
        raise AssertionError("Stage1 simulation device/clearance status contract missing: " + ", ".join(missing))
    detail_source = DETAIL_SERVICE.read_text(encoding="utf-8")
    detail_required = [
        'setClearanceConfirmations(resolveClearanceConfirmations(detail.get("clearanceConfirmations")',
        'return resolveClearanceConfirmations(payload.get("clearanceConfirmations"), activeOrderId);',
    ]
    detail_missing = [item for item in detail_required if item not in detail_source]
    if detail_missing:
        raise AssertionError("Active order detail material clearance contract missing: " + ", ".join(detail_missing))
    stage1_source = STAGE1_SERVICE.read_text(encoding="utf-8")
    if ".simulateActiveOrderCompletion(" not in stage1_source:
        raise AssertionError("Stage1 button simulation must delegate to active-order completion simulation service")
    print("PASS: stage1 simulation device and clearance status static contract")


if __name__ == "__main__":
    main()
