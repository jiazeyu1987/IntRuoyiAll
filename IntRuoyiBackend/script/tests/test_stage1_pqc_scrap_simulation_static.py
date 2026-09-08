from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
SIMULATION_SERVICE = ROOT / "IntRuoyiBackend" / "yudao-module-mes" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "mes" / "service" / "pro" / "processpool" / "team" / "MesTeamLeaderActiveOrderSimulationService.java"
FRONTEND_DETAIL = ROOT / "IntRuoyiFronted" / "src" / "views" / "mes" / "pro" / "processpool" / "components" / "ActiveOrderSubmissionDetailPanel.vue"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_stage1_pqc_simulation_writes_positive_scrap_quantity_to_pqc_payload():
    source = read(SIMULATION_SERVICE)

    required = [
        "private static final int SIMULATED_PQC_SCRAP_QUANTITY = 1;",
        "Integer scrapQuantity = simulatedPqcScrapQuantity(actualInspectionQuantity);",
        "String inspectionResult = simulatedPqcInspectionResult(scrapQuantity, pieceDetails);",
        '":scrapQuantity:" + scrapQuantity + ":inspectionResult:" + inspectionResult',
        "buildPqcRawPayload(activeOrder, task, actualInspectionQuantity, scrapQuantity, pieceDetails,",
        "inspectionResult,",
        'payload.put("scrapQuantity", scrapQuantity);',
        'payload.put("inspectionResult", inspectionResult);',
        "return MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE;",
    ]
    missing = [item for item in required if item not in source]
    assert not missing, "Stage1 PQC simulation scrap contract missing: " + ", ".join(missing)


def test_stage1_pqc_simulation_normalizes_already_confirmed_pqc_events():
    source = read(SIMULATION_SERVICE)

    required = [
        "normalizeConfirmedPqcSimulationSubmission(activeOrder, task, simulationStage, simulationRunId);",
        "private void normalizeConfirmedPqcSimulationSubmission(",
        "processPoolEventMapper.selectByIdForUpdate(task.getSubmittedEventId())",
        "normalizePqcSimulationPayload(event.getRawPayload(), scrapQuantity, inspectionResult)",
        ".setRawPayload(normalizedPayload)",
        ".setInspectionResult(inspectionResult)",
    ]
    missing = [item for item in required if item not in source]
    assert not missing, "Stage1 confirmed PQC normalization contract missing: " + ", ".join(missing)


def test_pqc_loss_report_uses_pqc_scrap_quantity_only():
    source = read(FRONTEND_DETAIL)

    assert "pqcLossReportRows" in source
    assert "submission.scrapQuantity" in source
    report_source = source[source.index("const pqcLossReportRows") : source.index("const pqcLossReportApprovalText")]
    assert "productionSubmissions" not in report_source
