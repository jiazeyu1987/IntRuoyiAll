from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "yudao-module-mes" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao" / "module" / "mes" / "service" / "pro" / "simulation" / "stage4" / "MesStage4DossierUploadSimulationServiceImpl.java"


def test_stage4_owned_attachment_marker_rehashes_the_changed_ledger_row():
    text = SOURCE.read_text(encoding="utf-8")
    start = text.index("    private void markAttachmentOwned(")
    end = text.index("\n    private IndependentBatchExecutionInputFixture", start)
    block = text[start:end]

    marker_index = block.index("setReasonText(SIMULATION_MARKER")
    rehash_index = block.index("setAttachmentHash", marker_index)
    update_index = block.index("attachmentMapper.updateById", marker_index)
    assert rehash_index < update_index
    assert "MesProEdhrSpecialNodeAttachmentHasher.attachmentHash" in block


def test_stage4_independent_receipt_records_formal_backfill_ids_and_rehashes():
    text = SOURCE.read_text(encoding="utf-8")
    batch_backfill_index = text.index(
        "MesProcessPoolActiveOrderCompletionBackfillDO batchRecordBackfill ="
    )
    process_backfill_index = text.index(
        "MesProcessPoolActiveOrderCompletionBackfillDO processInspectionBackfill =",
        batch_backfill_index,
    )
    trace_source_index = text.index(
        "MesProEdhrBatchProvisioningRecordDO provisioningRecord =", process_backfill_index
    )
    trace_source_end = text.index(
        "FormalStage4Source source =", trace_source_index
    )
    block = text[process_backfill_index:trace_source_end]

    assert ".setBatchRecordId(batchRecordBackfill.getId())" in block
    assert ".setProcessInspectionId(processInspectionBackfill.getId())" in block
    assert ".setBatchRecordSourceIdsJson(JSON.toJSONString(List.of(batchRecordBackfill.getId())))" in block
    assert ".setProcessInspectionSourceIdsJson(JSON.toJSONString(List.of(processInspectionBackfill.getId())))" in block
    assert "MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt)" in block
    assert "completionReceiptMapper.insert(receipt)" in block
    assert "MesProcessPoolActiveOrderCompletionReceiptDO persistedReceipt = completionReceiptMapper.selectById(receipt.getId())" in block
    assert "persistedReceipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(persistedReceipt))" in block
    assert "completionReceiptMapper.updateById(persistedReceipt)" in block
    assert ".setSourceCredentialHash(persistedReceipt.getReceiptHash())" in block
    assert "STAGE4_INDEPENDENT_COMPLETION_RECEIPT_PERSISTENCE_MISMATCH" not in block


def test_stage4_independent_route_assigns_archive_to_simulation_actor():
    text = SOURCE.read_text(encoding="utf-8")
    assert 'setScopeType("ROUTE")' in text
    assert 'setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE)' in text
    assert '.setAssigneeUserId(actorUserId)' in text
    assert '.setCandidateSourceType("USER")' in text
    assert '.setCandidateSourceId(actorUserId)' in text
    assert ".setDueMinutes(1440)" in text
    assert "assignmentRuleMapper.insert(archiveRule)" in text
