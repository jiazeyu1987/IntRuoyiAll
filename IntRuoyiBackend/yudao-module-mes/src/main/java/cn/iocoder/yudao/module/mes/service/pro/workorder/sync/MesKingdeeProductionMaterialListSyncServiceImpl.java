package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionMaterialList;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionMaterialListClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class MesKingdeeProductionMaterialListSyncServiceImpl implements MesKingdeeProductionMaterialListSyncService {

    private final ErpKingdeeProductionMaterialListClient client;
    private final ErpKingdeeConfigService configService;
    private final MesKingdeeProductionMaterialListMapper materialListMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProWorkOrderBomMapper workOrderBomMapper;
    private final MesMdItemMapper itemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesKingdeeProductionMaterialListSyncResult syncModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd) {
        ErpKingdeeProperties properties = configService.getEffectiveProperties();
        properties.validateProductionOrderSyncConfig();
        List<ErpKingdeeProductionMaterialList> rows =
                client.fetchProductionMaterialListsModifiedBetween(properties, windowStart, windowEnd);
        return syncRows(rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesKingdeeProductionMaterialListSyncResult syncByProductionOrderNos(Collection<String> productionOrderNos) {
        ErpKingdeeProperties properties = configService.getEffectiveProperties();
        properties.validateProductionOrderSyncConfig();
        List<ErpKingdeeProductionMaterialList> rows =
                client.fetchProductionMaterialListsByProductionOrderNos(properties, productionOrderNos);
        return syncRows(rows);
    }

    private MesKingdeeProductionMaterialListSyncResult syncRows(List<ErpKingdeeProductionMaterialList> rows) {
        MesKingdeeProductionMaterialListSyncResult result = new MesKingdeeProductionMaterialListSyncResult();
        for (ErpKingdeeProductionMaterialList row : rows) {
            List<MesProWorkOrderDO> workOrders = TenantUtils.executeIgnore(() ->
                    workOrderMapper.selectListByCodes(Collections.singleton(row.getProductionOrderNo())));
            if (workOrders.isEmpty()) {
                upsertRow(row, null, result);
                continue;
            }
            for (MesProWorkOrderDO workOrder : workOrders) {
                TenantUtils.execute(workOrder.getTenantId(), () -> upsertRow(row, workOrder, result));
            }
        }
        return result;
    }

    private void upsertRow(ErpKingdeeProductionMaterialList row, MesProWorkOrderDO workOrder,
                           MesKingdeeProductionMaterialListSyncResult result) {
        MesKingdeeProductionMaterialListDO existing = materialListMapper.selectBySourceLine(row.getBillNo(),
                row.getProductionOrderNo(), row.getProductionOrderLineNo(), row.getChildMaterialCode());
        MesKingdeeProductionMaterialListDO mapped = buildRow(row, workOrder);
        if (existing == null) {
            materialListMapper.insert(mapped);
            result.addCreated(mapped.getId());
            return;
        }
        mapped.setId(existing.getId());
        materialListMapper.updateById(mapped);
        result.addUpdated(existing.getId());
    }

    private MesKingdeeProductionMaterialListDO buildRow(ErpKingdeeProductionMaterialList row, MesProWorkOrderDO workOrder) {
        MesMdItemDO productItem = resolveProductItem(row, workOrder);
        MesMdItemDO childItem = itemMapper.selectByCode(row.getChildMaterialCode());
        String productCode = requireProductCode(row, productItem);
        MesKingdeeProductionMaterialListDO target = MesKingdeeProductionMaterialListDO.builder()
                .sourceFormId(row.getFormId())
                .sourceBillNo(row.getBillNo())
                .sourceEntryId(row.getEntryId())
                .productCode(productCode)
                .productionOrderNo(row.getProductionOrderNo())
                .productionOrderLineNo(row.getProductionOrderLineNo())
                .productionOrderStatus(row.getProductionOrderStatus())
                .childMaterialCode(row.getChildMaterialCode())
                .childMaterialName(row.getChildMaterialName())
                .childMaterialSpecification(row.getChildMaterialSpecification())
                .childMaterialType(row.getChildMaterialType())
                .numerator(row.getNumerator())
                .denominator(row.getDenominator())
                .childUnitName(row.getChildUnitName())
                .requiredQuantity(row.getRequiredQuantity())
                .issueMethod(row.getIssueMethod())
                .demandTime(row.getDemandTime())
                .sourceModifyTime(row.getSourceModifyTime())
                .lastSyncTime(LocalDateTime.now())
                .rawPayload(row.getRawPayload())
                .build();
        if (workOrder != null) {
            target.setWorkOrderId(workOrder.getId());
            target.setWorkOrderCode(workOrder.getCode());
            target.setProductId(workOrder.getProductId());
            target.setWorkOrderBomId(resolveWorkOrderBomId(workOrder.getId(), childItem));
        } else if (productItem != null) {
            target.setProductId(productItem.getId());
        }
        if (childItem != null) {
            target.setChildMaterialId(childItem.getId());
        }
        return target;
    }

    private MesMdItemDO resolveProductItem(ErpKingdeeProductionMaterialList row, MesProWorkOrderDO workOrder) {
        if (StrUtil.isNotBlank(row.getProductCode())) {
            return itemMapper.selectByCode(row.getProductCode());
        }
        if (workOrder == null || workOrder.getProductId() == null) {
            return null;
        }
        return itemMapper.selectById(workOrder.getProductId());
    }

    private String requireProductCode(ErpKingdeeProductionMaterialList row, MesMdItemDO productItem) {
        if (StrUtil.isNotBlank(row.getProductCode())) {
            return row.getProductCode();
        }
        if (productItem != null && StrUtil.isNotBlank(productItem.getCode())) {
            return productItem.getCode();
        }
        throw new IllegalStateException("ERP production material list missing product code and local work order product: "
                + row.getProductionOrderNo());
    }

    private Long resolveWorkOrderBomId(Long workOrderId, MesMdItemDO childItem) {
        if (workOrderId == null || childItem == null) {
            return null;
        }
        List<MesProWorkOrderBomDO> bomRows = workOrderBomMapper.selectListByWorkOrderId(workOrderId);
        if (bomRows == null) {
            return null;
        }
        for (MesProWorkOrderBomDO bomRow : bomRows) {
            if (childItem.getId().equals(bomRow.getItemId())) {
                return bomRow.getId();
            }
        }
        return null;
    }

}
