package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeBomClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeBomLine;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.itemconsume.MesWmItemConsumeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.outsourceissue.MesWmOutsourceIssueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.productissue.MesWmProductIssueMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdProductBomService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_ERP_BOM_SYNC_DOWNSTREAM_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_ERP_BOM_SYNC_ITEM_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_ERP_BOM_SYNC_MULTI_VERSION;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_ERP_BOM_SYNC_NOT_FOUND;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_ERP_BOM_SYNC_PRODUCT_CODE_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_ERP_BOM_SYNC_RECURSIVE_ITEM;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_ERP_BOM_SYNC_STATUS_INVALID;

@Service
@Validated
@RequiredArgsConstructor
public class MesKingdeeWorkOrderBomSyncServiceImpl implements MesKingdeeWorkOrderBomSyncService {

    private static final int ERP_BOM_QUANTITY_SCALE = 6;

    private final ErpKingdeeBomClient bomClient;
    private final ErpKingdeeConfigService kingdeeConfigService;
    private final MesProWorkOrderService workOrderService;
    private final MesProWorkOrderBomMapper workOrderBomMapper;
    private final MesMdItemMapper itemMapper;
    private final MesMdProductBomService productBomService;
    private final MesWmProductIssueMapper productIssueMapper;
    private final MesWmOutsourceIssueMapper outsourceIssueMapper;
    private final MesWmItemConsumeMapper itemConsumeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesKingdeeWorkOrderBomSyncResult syncErpBom(Long workOrderId) {
        MesProWorkOrderDO workOrder = workOrderService.validateWorkOrderExists(workOrderId);
        validateSyncStatus(workOrder);
        validateNoDownstreamUsage(workOrderId);

        MesMdItemDO productItem = itemMapper.selectById(workOrder.getProductId());
        if (productItem == null || StrUtil.isBlank(productItem.getCode())) {
            throw ServiceExceptionUtil.exception(PRO_WORK_ORDER_ERP_BOM_SYNC_PRODUCT_CODE_MISSING);
        }

        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        List<ErpKingdeeBomLine> bomLines = bomClient.fetchApprovedBomByParentMaterialNumber(
                kingdeeProperties, productItem.getCode());
        if (CollUtil.isEmpty(bomLines)) {
            throw ServiceExceptionUtil.exception(PRO_WORK_ORDER_ERP_BOM_SYNC_NOT_FOUND, productItem.getCode());
        }

        Map<String, List<ErpKingdeeBomLine>> versionMap = groupByVersion(bomLines);
        if (versionMap.size() > 1) {
            throw ServiceExceptionUtil.exception(PRO_WORK_ORDER_ERP_BOM_SYNC_MULTI_VERSION,
                    productItem.getCode(), String.join(", ", versionMap.keySet()));
        }
        Map.Entry<String, List<ErpKingdeeBomLine>> versionEntry = versionMap.entrySet().iterator().next();

        List<MesProWorkOrderBomDO> newBomRows = buildWorkOrderBomRows(workOrder, versionEntry.getValue());
        workOrderBomMapper.deleteByWorkOrderId(workOrderId);
        if (CollUtil.isNotEmpty(newBomRows)) {
            workOrderBomMapper.insertBatch(newBomRows);
        }

        MesKingdeeWorkOrderBomSyncResult result = new MesKingdeeWorkOrderBomSyncResult();
        result.setWorkOrderId(workOrderId);
        result.setErpBomVersion(versionEntry.getKey());
        result.setSyncedBomCount(newBomRows.size());
        return result;
    }

    private void validateSyncStatus(MesProWorkOrderDO workOrder) {
        Integer status = workOrder.getStatus();
        if (!MesProWorkOrderStatusEnum.PREPARE.getStatus().equals(status)
                && !MesProWorkOrderStatusEnum.CONFIRMED.getStatus().equals(status)) {
            throw ServiceExceptionUtil.exception(PRO_WORK_ORDER_ERP_BOM_SYNC_STATUS_INVALID);
        }
    }

    private void validateNoDownstreamUsage(Long workOrderId) {
        if (productIssueMapper.selectCountByWorkOrderId(workOrderId) > 0
                || outsourceIssueMapper.selectCountByWorkOrderId(workOrderId) > 0
                || itemConsumeMapper.selectCountByWorkOrderId(workOrderId) > 0) {
            throw ServiceExceptionUtil.exception(PRO_WORK_ORDER_ERP_BOM_SYNC_DOWNSTREAM_EXISTS);
        }
    }

    private Map<String, List<ErpKingdeeBomLine>> groupByVersion(List<ErpKingdeeBomLine> bomLines) {
        Map<String, List<ErpKingdeeBomLine>> versionMap = new LinkedHashMap<>();
        for (ErpKingdeeBomLine line : bomLines) {
            versionMap.computeIfAbsent(line.getBomVersion(), key -> new ArrayList<>()).add(line);
        }
        return versionMap;
    }

    private List<MesProWorkOrderBomDO> buildWorkOrderBomRows(MesProWorkOrderDO workOrder, List<ErpKingdeeBomLine> bomLines) {
        Set<String> missingCodes = new LinkedHashSet<>();
        List<MesProWorkOrderBomDO> result = new ArrayList<>(bomLines.size());
        for (ErpKingdeeBomLine bomLine : bomLines) {
            MesMdItemDO localItem = itemMapper.selectByCode(bomLine.getChildMaterialNumber());
            if (localItem == null) {
                missingCodes.add(bomLine.getChildMaterialNumber());
                continue;
            }
            if (CollUtil.isNotEmpty(productBomService.getProductBomListByItemId(localItem.getId()))) {
                throw ServiceExceptionUtil.exception(PRO_WORK_ORDER_ERP_BOM_SYNC_RECURSIVE_ITEM, localItem.getCode());
            }
            result.add(new MesProWorkOrderBomDO()
                    .setWorkOrderId(workOrder.getId())
                    .setItemId(localItem.getId())
                    .setQuantity(calculateBomQuantity(workOrder.getQuantity(), bomLine))
                    .setRemark("ERP BOM版本: " + bomLine.getBomVersion()));
        }
        if (CollUtil.isNotEmpty(missingCodes)) {
            throw ServiceExceptionUtil.exception(PRO_WORK_ORDER_ERP_BOM_SYNC_ITEM_MISSING,
                    String.join(", ", missingCodes));
        }
        return result;
    }

    private BigDecimal calculateBomQuantity(BigDecimal workOrderQuantity, ErpKingdeeBomLine bomLine) {
        BigDecimal quantity = workOrderQuantity
                .multiply(bomLine.getNumerator())
                .divide(bomLine.getDenominator(), ERP_BOM_QUANTITY_SCALE, RoundingMode.HALF_UP);
        quantity = quantity.stripTrailingZeros();
        return quantity.scale() < 0 ? quantity.setScale(0) : quantity;
    }

}
