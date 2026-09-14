from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def test_detail_contract_returns_replenishment_material_collection():
    domain = read(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/"
        "MesTeamLeaderActiveOrderDetail.java"
    )
    vo = read(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/"
        "MesTeamLeaderActiveOrderDetailRespVO.java"
    )
    controller = read(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/"
        "MesProcessPoolTeamLeaderController.java"
    )
    service = read(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/"
        "MesTeamLeaderActiveOrderDetailServiceImpl.java"
    )

    assert "List<SupplementMaterialDetail> supplementMaterials" in domain
    assert "class SupplementMaterialDetail" in vo
    assert ".setSupplementMaterials(process.getSupplementMaterials().stream()" in controller
    assert "selectListByProductionOrderNo(workOrderCode)" in service
    assert ".setSourceReplenishmentListIds(" in service
    assert ".setSourceReplenishmentListNos(" in service
    assert ".setSourceReplenishmentListItemIds(" in service
    assert "computeIfAbsent(key, ignored -> new SupplementAccumulator" in service


def test_stage1_clones_and_cleans_replenishment_lists_for_generated_work_order():
    service = read(
        "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage1/"
        "MesStage1ActiveOrderCompleteSimulationServiceImpl.java"
    )

    assert "cloneReplenishmentLists(templateWorkOrder.getCode(), workOrder.getCode()" in service
    assert "selectListByProductionOrderNo(sourceWorkOrderCode)" in service
    assert ".setProductionOrderNo(targetWorkOrderCode)" in service
    assert "STAGE1-RL-" in service
    assert "cleanupCopiedReplenishmentLists(runId)" in service
    assert "deleteByProductionReplenishmentListId" in service
