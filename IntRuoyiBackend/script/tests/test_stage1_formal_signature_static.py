from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
SERVICE = ROOT / "IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderSimulationService.java"
TEST = ROOT / "IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderSimulationServiceTest.java"
SIGNATURE_TEST = ROOT / "IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionSignatureServiceTest.java"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_stage1_simulation_uses_formal_signature_service():
    source = read(SERVICE)
    assert "MesProBatchRecordExecutionSignatureService" in source
    assert "recordStage1SimulationSignature" in source
    assert "nextSimulationSignatureId" not in source
    assert "simulationSignatureSnapshot" not in source
    assert "recordProductionSubmitSignature" not in source
    assert 'PRODUCTION_SUBMIT' in source
    assert 'PQC_SUBMIT' in source
    assert 'TEAM_LEADER_REVIEW' in source


def test_stage1_regression_verifies_signature_ids_from_service():
    source = read(TEST)
    assert "signatureService" in source
    assert "stage1SimulationShouldPersistFormalSignatureRecordsForAllSubmitAndReviewActions" in source
    assert "recordStage1SimulationSignature" in source
    assert "10001L" in source
    assert "10002L" in source
    assert "10003L" in source


def test_signature_service_persists_stage1_login_session_signature():
    source = read(SIGNATURE_TEST)
    assert "recordStage1SimulationSignature_persistsFormalLoginSessionSignatureRecord" in source
    assert '"LOGIN_SESSION"' in source
    assert '"MES_ACTIVE_ORDER_SIMULATION"' in source
    assert "isPasswordMatch(any(), any())" in source
