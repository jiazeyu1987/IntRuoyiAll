package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Read-only authoritative resolver shared by frontline batch lookup and completion freezing. */
@Component
public class MesFormalProductionPickListSourceResolver {

    private final MesProWorkOrderMapper workOrderMapper;
    private final ErpKingdeeProductionPickListMapper pickListMapper;
    private final ErpKingdeeProductionPickListItemMapper pickListItemMapper;

    public MesFormalProductionPickListSourceResolver(
            MesProWorkOrderMapper workOrderMapper,
            ErpKingdeeProductionPickListMapper pickListMapper,
            ErpKingdeeProductionPickListItemMapper pickListItemMapper) {
        this.workOrderMapper = workOrderMapper;
        this.pickListMapper = pickListMapper;
        this.pickListItemMapper = pickListItemMapper;
    }

    public Resolution resolve(Long workOrderId) {
        MesProWorkOrderDO workOrder = workOrderId == null ? null : workOrderMapper.selectById(workOrderId);
        if (workOrder == null || StrUtil.isBlank(workOrder.getCode())) {
            throw invalid("WORK_ORDER_CODE_REQUIRED:" + workOrderId);
        }
        String productionOrderNo = StrUtil.trim(workOrder.getCode());
        List<ErpKingdeeProductionPickListItemDO> discovered =
                pickListItemMapper.selectListByProductionOrderNo(productionOrderNo);
        if (discovered == null || discovered.isEmpty()) {
            throw invalid("FORMAL_PICK_LIST_REQUIRED:" + productionOrderNo);
        }
        if (discovered.stream().anyMatch(item -> item == null || item.getProductionPickListId() == null)) {
            throw invalid("PICK_LIST_ID_REQUIRED:" + productionOrderNo);
        }
        Map<Long, List<ErpKingdeeProductionPickListItemDO>> grouped = discovered.stream()
                .collect(Collectors.groupingBy(ErpKingdeeProductionPickListItemDO::getProductionPickListId,
                        LinkedHashMap::new, Collectors.toList()));
        List<Source> sources = grouped.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> validateSource(productionOrderNo, entry.getKey(), entry.getValue()))
                .toList();
        String resolutionHash = DigestUtil.sha256Hex(productionOrderNo + "|" + sources.stream()
                .map(Source::hash).collect(Collectors.joining("|")));
        return new Resolution(workOrder, productionOrderNo, sources, resolutionHash);
    }

    private Source validateSource(String productionOrderNo, Long pickListId,
                                  List<ErpKingdeeProductionPickListItemDO> items) {
        ErpKingdeeProductionPickListDO header = pickListMapper.selectById(pickListId);
        if (header == null || StrUtil.isBlank(header.getSourceFid()) || StrUtil.isBlank(header.getSourceBillNo())
                || !"C".equalsIgnoreCase(header.getDocumentStatus()) || items == null || items.isEmpty()) {
            throw invalid("FORMAL_PICK_LIST_HEADER_OR_STATUS_INVALID:" + pickListId);
        }
        if (items.stream().anyMatch(item -> item.getId() == null
                || !Objects.equals(item.getProductionPickListId(), pickListId)
                || !Objects.equals(StrUtil.trim(item.getProductionOrderNo()), productionOrderNo)
                || StrUtil.isBlank(item.getSourceFid()) || StrUtil.isBlank(item.getSourceEntryId())
                || StrUtil.isBlank(item.getSourceLineKey()) || StrUtil.isBlank(item.getMaterialNumber()))) {
            throw invalid("FORMAL_PICK_LIST_ITEM_INVALID:" + pickListId);
        }
        List<ErpKingdeeProductionPickListItemDO> ordered = items.stream()
                .sorted(Comparator.comparing(ErpKingdeeProductionPickListItemDO::getSourceEntryId)
                        .thenComparing(ErpKingdeeProductionPickListItemDO::getId)).toList();
        if (ordered.stream().map(ErpKingdeeProductionPickListItemDO::getSourceEntryId)
                .distinct().count() != ordered.size()) {
            throw invalid("FORMAL_PICK_LIST_ITEM_ID_DUPLICATED:" + pickListId);
        }
        return new Source(header, ordered, snapshotHash(header, ordered));
    }

    public static String snapshotHash(ErpKingdeeProductionPickListDO header,
                                      List<ErpKingdeeProductionPickListItemDO> items) {
        String seed = normalized(header.getId()) + "|" + normalized(header.getSourceFormId()) + "|"
                + normalized(header.getSourceFid()) + "|" + normalized(header.getSourceBillNo()) + "|"
                + normalized(header.getBillDate()) + "|" + normalized(header.getDocumentStatus()) + "|"
                + normalized(header.getStockOrgNumber()) + "|" + normalized(header.getStockOrgName()) + "|"
                + normalized(header.getProductionOrgNumber()) + "|" + normalized(header.getProductionOrgName()) + "|"
                + normalized(header.getOwnerNumber()) + "|" + normalized(header.getOwnerName()) + "|"
                + normalized(header.getDepartmentNumber()) + "|" + normalized(header.getDepartmentName()) + "|"
                + normalized(header.getDescription()) + "|" + normalized(header.getSourceModifyTime()) + "|"
                + items.stream().map(MesFormalProductionPickListSourceResolver::itemSeed)
                .collect(Collectors.joining("||"));
        return DigestUtil.sha256Hex(seed);
    }

    public static String itemSnapshotHash(ErpKingdeeProductionPickListItemDO item) {
        return DigestUtil.sha256Hex(itemSeed(item));
    }

    private static String itemSeed(ErpKingdeeProductionPickListItemDO item) {
        return String.join("|", normalized(item.getId()), normalized(item.getProductionPickListId()),
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

    private MesFormalProductionPickListSourceException invalid(String reason) {
        return new MesFormalProductionPickListSourceException(reason);
    }

    public record Resolution(MesProWorkOrderDO workOrder, String productionOrderNo,
                             List<Source> sources, String hash) {
    }

    public record Source(ErpKingdeeProductionPickListDO header,
                         List<ErpKingdeeProductionPickListItemDO> items, String hash) {
    }
}
