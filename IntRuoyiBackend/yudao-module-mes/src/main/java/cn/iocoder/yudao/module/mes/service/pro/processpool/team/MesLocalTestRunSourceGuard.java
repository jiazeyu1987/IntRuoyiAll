package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.*;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.testdata.MesLocalTestRunManifestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** Explicit local source provenance. This never grants production, QA, signature or release authority. */
@Component
@RequiredArgsConstructor
public class MesLocalTestRunSourceGuard {
    public static final String SOURCE = "LOCAL_TEST_RUN";
    public static final String STATUS = "LOCAL_TEST";
    private final MesLocalTestRunManifestMapper manifests;
    private final MesProWorkOrderMapper orders;
    private final ErpKingdeeProductionPickListMapper picks;
    private final ErpKingdeeProductionPickListItemMapper pickItems;
    private final ErpKingdeeProductionReplenishmentListMapper replenishments;
    private final ErpKingdeeProductionReplenishmentListItemMapper replenishmentItems;
    private final MesFixedTestOrderModeGuard mode;
    private final MesKingdeeProductionMaterialListMapper materials;

    public boolean isLocalInput(MesProWorkOrderDO order) {
        return order != null && (order.getOrderSourceCode() != null && order.getOrderSourceCode().startsWith(SOURCE)
                || manifests.selectByWorkOrderId(order.getId()) != null);
    }

    public void requireProductionAdmission(MesProWorkOrderDO order) {
        if (validate(order, true)) {
            require(Objects.equals(order.getStatus(),
                    cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum.CONFIRMED.getStatus()), "CONFIRMED_ORDER_REQUIRED");
        }
    }

    public boolean validateOrderId(Long workOrderId, boolean progression) {
        return validate(orders.selectById(workOrderId), progression);
    }

    public boolean validate(MesProWorkOrderDO order, boolean progression) {
        require(order != null && order.getId() != null, "ORDER_REQUIRED");
        var manifest = manifests.selectByWorkOrderId(order.getId());
        boolean marked = order.getOrderSourceCode() != null && order.getOrderSourceCode().startsWith(SOURCE);
        if (manifest == null) {
            require(!marked, "MANIFEST_REQUIRED");
            return false;
        }
        if (progression) mode.requireEnabled(SecurityFrameworkUtils.getLoginUserId());
        Long tenant = TenantContextHolder.getTenantId();
        require(tenant != null && !TenantContextHolder.isIgnore() && Objects.equals(tenant, order.getTenantId())
                && Objects.equals(tenant, manifest.getTenantId()), "TENANT_MISMATCH");
        require(SOURCE.equals(manifest.getSource()) && manifest.getId() != null
                && Objects.equals(order.getOrderSourceCode(), SOURCE + ":" + manifest.getId())
                && Objects.equals(order.getId(), manifest.getWorkOrderId())
                && manifest.getRunId() != null && manifest.getRunId().matches("[A-Za-z0-9][A-Za-z0-9_-]{2,39}")
                && manifest.getSlot() != null && Set.of("A", "B").contains(manifest.getSlot())
                && Objects.equals(order.getCode(), "LT-" + manifest.getRunId() + "-" + manifest.getSlot())
                && Objects.equals(order.getProductId(), manifest.getMesProductId())
                && sameDecimal(order.getQuantity(), manifest.getQuantity()) && manifest.getQuantity().signum() > 0
                && Objects.equals(order.getBatchCode(), manifest.getBatchCode())
                && manifest.getRequestId() != null && manifest.getProjectId() != null
                && manifest.getCatalogId() != null && manifest.getRelationId() != null
                && manifest.getInputHash() != null && manifest.getInputHash().matches("[0-9a-f]{64}")
                && manifest.getIncludeReplenishment() != null, "MANIFEST_ORDER_IDENTITY_MISMATCH");
        require(Objects.equals(manifest.getInputHash(),
                cn.iocoder.yudao.module.mes.service.pro.testdata.MesLocalTestRunInputHash.hash(manifest)), "INPUT_HASH_MISMATCH");
        var pick = picks.selectById(manifest.getPickHeaderId());
        require(pick != null && Objects.equals(tenant, pick.getTenantId())
                && SOURCE.equals(pick.getSourceFormId()) && STATUS.equals(pick.getDocumentStatus())
                && Objects.equals(pick.getSourceFid(), SOURCE + ":" + manifest.getId() + ":P")
                && Objects.equals(pick.getSourceBillNo(), order.getCode() + "-P"), "PICK_HEADER_MISMATCH");
        var pItems = pickItems.selectListByPickListIds(List.of(pick.getId()));
        require(pItems != null && !pItems.isEmpty(), "PICK_ITEMS_REQUIRED");
        requireExactIds(manifest.getPickItemIdsJson(), pItems.stream().map(ErpKingdeeProductionPickListItemDO::getId).toList());
        requireExactIds(manifest.getPickItemIdsJson(), pickItems.selectListByProductionOrderNo(order.getCode())
                .stream().map(ErpKingdeeProductionPickListItemDO::getId).toList());
        for (var item : pItems) {
            require(Objects.equals(tenant, item.getTenantId()) && Objects.equals(item.getProductionPickListId(), pick.getId())
                    && Objects.equals(item.getProductionOrderNo(), order.getCode()) && SOURCE.equals(item.getSourceFormId())
                    && Objects.equals(item.getSourceFid(), pick.getSourceFid())
                    && Objects.equals(item.getSourceBillNo(), pick.getSourceBillNo())
                    && item.getSourceEntryId() != null && item.getSourceEntryId().matches("[1-9][0-9]*") && Objects.equals(item.getSourceLineKey(), pick.getSourceFid() + ":" + item.getSourceEntryId()),
                    "PICK_ITEM_MISMATCH");
        }
        require(Objects.equals(manifest.getPickSnapshotHash(), pickSnapshotHash(pick, pItems)), "PICK_HASH_MISMATCH");
        var rItems = replenishmentItems.selectListByProductionOrderNo(order.getCode());
        requireExactIds(manifest.getReplenishmentItemIdsJson(), rItems.stream().map(ErpKingdeeProductionReplenishmentListItemDO::getId).toList());
        if (!manifest.getIncludeReplenishment()) {
            require(rItems.isEmpty() && manifest.getReplenishmentHeaderId() == null
                    && manifest.getReplenishmentSnapshotHash() == null, "UNEXPECTED_REPLENISHMENT");
        } else {
            var header = replenishments.selectById(manifest.getReplenishmentHeaderId());
            require(header != null && !rItems.isEmpty() && Objects.equals(tenant, header.getTenantId())
                    && SOURCE.equals(header.getSourceFormId()) && STATUS.equals(header.getDocumentStatus())
                    && Objects.equals(header.getSourceFid(), SOURCE + ":" + manifest.getId() + ":R")
                    && Objects.equals(header.getSourceBillNo(), order.getCode() + "-R"), "REPLENISHMENT_HEADER_MISMATCH");
            requireExactIds(manifest.getReplenishmentItemIdsJson(), replenishmentItems
                    .selectListByReplenishmentListIds(List.of(header.getId())).stream()
                    .map(ErpKingdeeProductionReplenishmentListItemDO::getId).toList());
            for (var item : rItems) {
                require(Objects.equals(tenant, item.getTenantId()) && Objects.equals(item.getProductionReplenishmentListId(), header.getId())
                        && Objects.equals(item.getProductionOrderNo(), order.getCode()) && SOURCE.equals(item.getSourceFormId())
                        && Objects.equals(item.getSourceFid(), header.getSourceFid())
                        && Objects.equals(item.getSourceBillNo(), header.getSourceBillNo())
                        && item.getSourceEntryId() != null && item.getSourceEntryId().matches("[1-9][0-9]*") && Objects.equals(item.getSourceLineKey(), header.getSourceFid() + ":" + item.getSourceEntryId()),
                        "REPLENISHMENT_ITEM_MISMATCH");
            }
            require(Objects.equals(manifest.getReplenishmentSnapshotHash(), replenishmentSnapshotHash(header, rItems)),
                    "REPLENISHMENT_HASH_MISMATCH");
        }
        var materialRows = materials.selectListByProductionOrderNo(order.getCode());
        require(materialRows != null && !materialRows.isEmpty(), "MATERIAL_ITEMS_REQUIRED");
        requireExactIds(manifest.getMaterialItemIdsJson(), materialRows.stream().map(MesKingdeeProductionMaterialListDO::getId).toList());
        for (var row : materialRows) {
            require(SOURCE.equals(row.getSourceFormId()) && STATUS.equals(row.getProductionOrderStatus())
                    && Objects.equals(row.getSourceBillNo(), order.getCode() + "-M")
                    && Objects.equals(row.getProductionOrderNo(), order.getCode())
                    && Objects.equals(row.getWorkOrderCode(), order.getCode())
                    && Objects.equals(row.getWorkOrderId(), order.getId())
                    && Objects.equals(row.getProductId(), order.getProductId())
                    && row.getWorkOrderBomId() != null && row.getWorkOrderBomId() > 0
                    && Objects.equals(row.getSourceEntryId(), row.getWorkOrderBomId().toString()), "MATERIAL_ITEM_MISMATCH");
        }
        require(Objects.equals(manifest.getMaterialSnapshotHash(), materialSnapshotHash(materialRows)), "MATERIAL_HASH_MISMATCH");
        return true;
    }

    private static void requireExactIds(String json, List<Long> actual) {
        List<Long> expected = JsonUtils.parseArray(json, Long.class);
        require(expected != null && !expected.contains(null) && actual != null && !actual.contains(null) && expected.size() == new HashSet<>(expected).size()
                && actual.size() == new HashSet<>(actual).size() && expected.size() == actual.size()
                && new HashSet<>(expected).equals(new HashSet<>(actual)), "SOURCE_ITEM_SET_MISMATCH");
    }

    public static String pickSnapshotHash(ErpKingdeeProductionPickListDO header, List<ErpKingdeeProductionPickListItemDO> items) {
        var ordered = items.stream().sorted(Comparator.comparing(ErpKingdeeProductionPickListItemDO::getId)).toList();
        return DigestUtil.sha256Hex("LOCAL_TEST_SOURCE_V1|" + header.getTenantId() + "|"
                + ordered.stream().map(item -> normalized(item.getTenantId())).collect(Collectors.joining("|")) + "|"
                + MesFormalProductionPickListSourceResolver.snapshotHash(header, ordered));
    }

    public static String replenishmentSnapshotHash(ErpKingdeeProductionReplenishmentListDO header,
                                                   List<ErpKingdeeProductionReplenishmentListItemDO> items) {
        var ordered = items.stream().sorted(Comparator.comparing(ErpKingdeeProductionReplenishmentListItemDO::getId)).toList();
        return DigestUtil.sha256Hex("LOCAL_TEST_SOURCE_V1|" + header.getTenantId() + "|"
                + ordered.stream().map(item -> normalized(item.getTenantId())).collect(Collectors.joining("|")) + "|"
                + replenishmentFieldsHash(header, ordered));
    }

    private static String replenishmentFieldsHash(ErpKingdeeProductionReplenishmentListDO header,
                                      List<ErpKingdeeProductionReplenishmentListItemDO> items) {
        String seed = normalized(header.getId()) + "|" + normalized(header.getSourceFormId()) + "|"
                + normalized(header.getSourceFid()) + "|" + normalized(header.getSourceBillNo()) + "|"
                + normalized(header.getBillDate()) + "|" + normalized(header.getDocumentStatus()) + "|"
                + normalized(header.getStockOrgNumber()) + "|" + normalized(header.getStockOrgName()) + "|"
                + normalized(header.getProductionOrgNumber()) + "|" + normalized(header.getProductionOrgName()) + "|"
                + normalized(header.getOwnerNumber()) + "|" + normalized(header.getOwnerName()) + "|"
                + normalized(header.getDepartmentNumber()) + "|" + normalized(header.getDepartmentName()) + "|"
                + normalized(header.getDescription()) + "|" + normalized(header.getSourceModifyTime()) + "|"
                + items.stream().map(MesLocalTestRunSourceGuard::itemSeed)
                .collect(Collectors.joining("||"));
        return DigestUtil.sha256Hex(seed);
    }

    private static String itemSeed(ErpKingdeeProductionReplenishmentListItemDO item) {
        return String.join("|", normalized(item.getId()), normalized(item.getProductionReplenishmentListId()),
                normalized(item.getSourceFormId()), normalized(item.getSourceFid()),
                normalized(item.getSourceEntryId()), normalized(item.getSourceLineKey()),
                normalized(item.getSourceBillNo()), normalized(item.getMaterialNumber()),
                normalized(item.getMaterialName()), normalized(item.getMaterialSpecification()),
                normalized(item.getUnitName()), normalized(item.getRequestedQuantity()),
                normalized(item.getActualQuantity()), normalized(item.getBaseActualQuantity()),
                normalized(item.getWarehouseNumber()), normalized(item.getWarehouseName()),
                normalized(item.getStockLocationNumber()), normalized(item.getStockLocationName()),
                normalized(item.getLotNumber()), normalized(item.getProductionOrderNo()),
                normalized(item.getProductionOrderLineNo()), normalized(item.getProductionMaterialListNo()),
                normalized(item.getProductionMaterialListLineNo()), normalized(item.getWorkshopNumber()),
                normalized(item.getWorkshopName()), normalized(item.getStockStatusNumber()),
                normalized(item.getStockStatusName()), normalized(item.getSourceModifyTime()));
    }

    private static String normalized(Object value) {
        if (value instanceof java.math.BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return Objects.toString(value, "").trim();
    }

    public static String materialSnapshotHash(List<MesKingdeeProductionMaterialListDO> rows) {
        return DigestUtil.sha256Hex("LOCAL_TEST_MATERIAL_V1|" + rows.stream()
                .sorted(Comparator.comparing(MesKingdeeProductionMaterialListDO::getId))
                .map(row -> String.join("|", normalized(row.getId()),
                        normalized(row.getSourceFormId()),
                        normalized(row.getSourceBillNo()),
                        normalized(row.getSourceEntryId()),
                        normalized(row.getProductCode()),
                        normalized(row.getProductionOrderNo()),
                        normalized(row.getProductionOrderLineNo()),
                        normalized(row.getProductionOrderStatus()),
                        normalized(row.getChildMaterialCode()),
                        normalized(row.getChildMaterialName()),
                        normalized(row.getChildMaterialSpecification()),
                        normalized(row.getChildMaterialType()),
                        normalized(row.getNumerator()),
                        normalized(row.getDenominator()),
                        normalized(row.getChildUnitName()),
                        normalized(row.getDrawingNumber()),
                        normalized(row.getRequiredQuantity()),
                        normalized(row.getIssueMethod()),
                        normalized(row.getDemandTime()),
                        normalized(row.getWorkOrderId()),
                        normalized(row.getWorkOrderCode()),
                        normalized(row.getWorkOrderBomId()),
                        normalized(row.getProductId()),
                        normalized(row.getChildMaterialId()),
                        normalized(row.getSourceModifyTime())))
                .collect(Collectors.joining("||")));
    }

    private static boolean sameDecimal(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }

    private static void require(boolean condition, String reason) {
        if (!condition) throw new MesFormalProductionPickListSourceException("LOCAL_TEST_RUN:" + reason);
    }
}
