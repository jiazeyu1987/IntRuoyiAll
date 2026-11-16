package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
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

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING;

/** Read-only authoritative resolver shared by frontline batch lookup and completion freezing. */
@Component
public class MesFormalProductionPickListSourceResolver {

    private final MesLocalTestRunSourceGuard localSources;
    private final MesProWorkOrderMapper workOrderMapper;
    private final ErpKingdeeProductionPickListMapper pickListMapper;
    private final ErpKingdeeProductionPickListItemMapper pickListItemMapper;

    public MesFormalProductionPickListSourceResolver(
            MesProWorkOrderMapper workOrderMapper,
            ErpKingdeeProductionPickListMapper pickListMapper,
            ErpKingdeeProductionPickListItemMapper pickListItemMapper,
            MesLocalTestRunSourceGuard localSources) {
        this.localSources = localSources;
        this.workOrderMapper = workOrderMapper;
        this.pickListMapper = pickListMapper;
        this.pickListItemMapper = pickListItemMapper;
    }

    public Resolution resolve(Long workOrderId) {
        MesProWorkOrderDO workOrder = workOrderId == null ? null : workOrderMapper.selectById(workOrderId);
        if (workOrder == null || StrUtil.isBlank(workOrder.getCode())) {
            throw invalid("WORK_ORDER_CODE_REQUIRED:" + workOrderId);
        }
        boolean localTest = localSources.validate(workOrder, true);
        String productionOrderNo = StrUtil.trim(workOrder.getCode());
        List<ErpKingdeeProductionPickListItemDO> discovered =
                pickListItemMapper.selectListByProductionOrderNo(productionOrderNo);
        if (discovered == null || discovered.isEmpty()) {
            throw new ServiceException(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING.getCode(),
                    "当前系统缺少生产订单(" + productionOrderNo + ")对应的领料单,请在确保ERP里存在然后在系统里进行同步");
        }
        if (discovered.stream().anyMatch(item -> item == null || item.getProductionPickListId() == null)) {
            throw invalid("PICK_LIST_ID_REQUIRED:" + productionOrderNo);
        }
        Map<Long, List<ErpKingdeeProductionPickListItemDO>> grouped = discovered.stream()
                .collect(Collectors.groupingBy(ErpKingdeeProductionPickListItemDO::getProductionPickListId,
                        LinkedHashMap::new, Collectors.toList()));
        List<Source> sources = grouped.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> validateSource(productionOrderNo, entry.getKey(), entry.getValue(), localTest))
                .toList();
        String resolutionHash = DigestUtil.sha256Hex(productionOrderNo + "|" + sources.stream()
                .map(Source::hash).collect(Collectors.joining("|")));
        return new Resolution(workOrder, productionOrderNo, sources, resolutionHash);
    }

    private Source validateSource(String productionOrderNo, Long pickListId,
                                  List<ErpKingdeeProductionPickListItemDO> items, boolean localTest) {
        ErpKingdeeProductionPickListDO header = pickListMapper.selectById(pickListId);
        if (header == null || StrUtil.isBlank(header.getSourceFid()) || StrUtil.isBlank(header.getSourceBillNo())
                || !(localTest ? MesLocalTestRunSourceGuard.STATUS.equals(header.getDocumentStatus())
                    && MesLocalTestRunSourceGuard.SOURCE.equals(header.getSourceFormId())
                    : "C".equalsIgnoreCase(header.getDocumentStatus())
                    && !MesLocalTestRunSourceGuard.SOURCE.equals(header.getSourceFormId()))
                || items == null || items.isEmpty()) {
            throw invalid("FORMAL_PICK_LIST_HEADER_OR_STATUS_INVALID:" + pickListId);
        }
        if (items.stream().anyMatch(item -> item.getId() == null
                || !Objects.equals(item.getProductionPickListId(), pickListId)
                || !Objects.equals(StrUtil.trim(item.getProductionOrderNo()), productionOrderNo)
                || StrUtil.isBlank(item.getSourceFid()) || StrUtil.isBlank(item.getSourceEntryId())
                || StrUtil.isBlank(item.getSourceLineKey()) || StrUtil.isBlank(item.getMaterialNumber())
                || !localTest && MesLocalTestRunSourceGuard.SOURCE.equals(item.getSourceFormId()))) {
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
