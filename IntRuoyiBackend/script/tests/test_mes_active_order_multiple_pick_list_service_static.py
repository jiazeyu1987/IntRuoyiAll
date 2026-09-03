from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def read(relative_path: str) -> str:
    return (REPO_ROOT / relative_path).read_text(encoding="utf-8")


def test_version_upgrade_copies_every_bound_pick_list_snapshot() -> None:
    source = read(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/"
        "MesTeamLeaderActiveOrderVersionUpgradeServiceImpl.java"
    )

    assert "selectListByActiveOrderId(sourceActiveOrder.getId())" in source
    assert "for (int index = 0; index < oldPickListBindings.size(); index++)" in source
    assert "copyPickListBinding(oldPickListBindings.get(index)" in source
    assert "pickListBindingItemMapper.selectListByBindingId(source.getId())" in source


def test_stage4_uses_batch_origin_pick_list_binding_id() -> None:
    source = read(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage4/"
        "MesStage4DossierUploadSimulationServiceImpl.java"
    )

    assert "selectListByBatchExecutionId(batch.getId())" in source
    assert "pickListBindingMapper.selectById(origin.getPickListBindingId())" in source
    assert "Objects.equals(binding.getPickListId(), origin.getPickListId())" in source


def test_stage_cleanup_deletes_every_bound_pick_list_snapshot() -> None:
    stage2_5 = read(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage2_5/"
        "MesStage2_5BackfillBatchExecutionSimulationServiceImpl.java"
    )
    stage5 = read(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage5/"
        "MesStage5FinalReleaseSimulationServiceImpl.java"
    )

    assert "for (MesProcessPoolActiveOrderPickListBindingDO binding : bindingMapper" in stage2_5
    assert ".selectListByActiveOrderId(activeOrder.getId()))" in stage2_5
    assert "for (MesProcessPoolActiveOrderPickListBindingDO activeOrderBinding : bindings)" in stage5
    assert "bindingMapper.deleteById(activeOrderBinding.getId())" in stage5


def test_authoritative_context_keeps_pick_list_set_not_first_binding() -> None:
    resolver = read(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
        "MesBatchExecutionAuthoritativeContextResolver.java"
    )
    context = read(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
        "MesBatchExecutionAuthoritativeContext.java"
    )
    command = read(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
        "MesBatchExecutionProvisionCommand.java"
    )

    assert "pickListBindings.get(0)" not in resolver
    assert "private List<MesProcessPoolActiveOrderPickListBindingDO> pickListBindings;" in context
    assert "private List<MesBatchExecutionPickListSource> pickListSources;" in command
    assert ".setPickListSources(toPickListSources(pickListBindings))" in resolver


def test_entry_trace_and_release_validate_all_pick_list_sources() -> None:
    entry_contract = read(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
        "MesBatchExecutionEntryContractService.java"
    )
    txc = read(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
        "MesProEdhrBatchTraceTxCProducer.java"
    )
    entry_metadata = read(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
        "MesProEdhrBatchExecutionServiceImpl.java"
    )
    release_validator = read(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/productionrelease/core/"
        "MesReleaseFinalizationValidator.java"
    )

    assert "validatePickListSources(" in entry_contract
    assert "metadata.put(\"pickListSources\"" in entry_metadata
    assert "parsePickListSources(metadata)" in txc
    assert "validatePickListSources(command, receipt)" in release_validator
    assert "Objects.equals(command.getPickListBindingId(), receipt.getPickListBindingId())" not in release_validator
