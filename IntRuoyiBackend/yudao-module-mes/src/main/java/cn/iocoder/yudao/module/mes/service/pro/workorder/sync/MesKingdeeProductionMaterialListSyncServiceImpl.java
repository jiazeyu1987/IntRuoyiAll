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
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRunResult;
import lombok.extern.slf4j.Slf4j;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
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
    private final MesKingdeeProductionMaterialListBatchWriter batchWriter;

    @Override
    public ErpKingdeeSyncRunResult syncAllSkipExisting(Long runId) {
        return syncAllSkipExisting(runId, null, null);
    }

    @Override
    public ErpKingdeeSyncRunResult syncAllSkipExisting(Long runId, LocalDateTime windowStart, LocalDateTime windowEnd) {
        java.util.Objects.requireNonNull(runId, "生产用料清单同步任务编号不能为空");
        ErpKingdeeProperties properties = configService.getEffectiveProperties();
        properties.validateProductionOrderSyncConfig();
        int[] counts = new int[3];
        long[] sourceRows = {0};
        try {
            java.util.function.Consumer<List<ErpKingdeeProductionMaterialList>> consumer = page -> {
                ErpKingdeeSyncRunResult result = windowStart == null ? batchWriter.writePage(runId, page)
                        : batchWriter.writePage(runId, page, true);
                counts[0] = Math.addExact(counts[0], result.getCreatedCount());
                counts[1] = Math.addExact(counts[1], result.getUpdatedCount());
                counts[2] = Math.addExact(counts[2], result.getSkippedCount());
                sourceRows[0] += page.size();
                log.info("生产用料清单批次已提交: runId={}, sourceRows={}, created={}, updated={}, skipped={}",
                        runId, sourceRows[0], counts[0], counts[1], counts[2]);
            };
            if (windowStart == null) {
                client.forEachProductionMaterialListPage(properties, consumer);
            } else {
                client.forEachProductionMaterialListPage(properties, windowStart, windowEnd, consumer);
            }
        } catch (RuntimeException failure) {
            throw new IllegalStateException("生产用料清单全量同步失败，已提交源数据 " + sourceRows[0]
                    + " 行；已提交批次保留，重新全量同步会跳过已有记录。原因：" + failure.getMessage(), failure);
        }
        return ErpKingdeeSyncRunResult.success(null, counts[0], counts[1], counts[2], 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesKingdeeProductionMaterialListSyncResult syncModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd) {
        ErpKingdeeProperties properties = configService.getEffectiveProperties();
        properties.validateProductionOrderSyncConfig();
        List<ErpKingdeeProductionMaterialList> rows =
                client.fetchProductionMaterialListsModifiedBetween(properties, windowStart, windowEnd);
        return syncRows(rows, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesKingdeeProductionMaterialListSyncResult syncByProductionOrderNos(Collection<String> productionOrderNos) {
        ErpKingdeeProperties properties = configService.getEffectiveProperties();
        properties.validateProductionOrderSyncConfig();
        List<ErpKingdeeProductionMaterialList> rows =
                client.fetchProductionMaterialListsByProductionOrderNos(properties, productionOrderNos);
        return syncRows(rows, false);
    }

    private MesKingdeeProductionMaterialListSyncResult syncRows(
            List<ErpKingdeeProductionMaterialList> rows, boolean skipExisting) {
        MesKingdeeProductionMaterialListSyncResult result = new MesKingdeeProductionMaterialListSyncResult();
        for (ErpKingdeeProductionMaterialList row : rows) {
            List<MesProWorkOrderDO> workOrders = TenantUtils.executeIgnore(() ->
                    workOrderMapper.selectListByCodes(Collections.singleton(row.getProductionOrderNo())));
            if (workOrders.isEmpty()) {
                upsertRow(row, null, result, skipExisting);
                continue;
            }
            for (MesProWorkOrderDO workOrder : workOrders) {
                TenantUtils.execute(workOrder.getTenantId(), () -> upsertRow(row, workOrder, result, skipExisting));
            }
        }
        return result;
    }

    private void upsertRow(ErpKingdeeProductionMaterialList row, MesProWorkOrderDO workOrder,
                           MesKingdeeProductionMaterialListSyncResult result, boolean skipExisting) {
        if (workOrder != null) {
            workOrder = workOrderMapper.selectByIdForUpdate(workOrder.getId());
            if (workOrder == null) {
                throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception(
                        cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_NOT_EXISTS);
            }
        }
        MesKingdeeProductionMaterialListDO existing = materialListMapper.selectBySourceLine(row.getBillNo(),
                row.getProductionOrderNo(), row.getProductionOrderLineNo(), row.getChildMaterialCode());
        if (skipExisting && existing != null && !MesKingdeeProductionMaterialListRowMapping.shouldBackfillErpColumns(existing, row)) {
            result.addSkipped(row.getBillNo() + "|" + row.getProductionOrderNo() + "|"
                    + row.getProductionOrderLineNo() + "|" + row.getChildMaterialCode());
            return;
        }
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
        return MesKingdeeProductionMaterialListRowMapping.map(row, workOrder, productItem, childItem,
                workOrder == null ? null : resolveWorkOrderBomId(workOrder.getId(), childItem));
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


    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncByCode(String code) {
        ErpKingdeeProperties properties = configService.getEffectiveProperties();
        List<ErpKingdeeProductionMaterialList> rows = client.fetchProductionMaterialListsByCode(properties, code);
        // A manual synchronization belongs only to the current tenant.
        MesKingdeeProductionMaterialListSyncResult result = new MesKingdeeProductionMaterialListSyncResult();
        for (ErpKingdeeProductionMaterialList row : rows) {
            List<MesProWorkOrderDO> orders = workOrderMapper.selectListByCodes(Collections.singleton(row.getProductionOrderNo()));
            if (orders.isEmpty()) { upsertRow(row, null, result, false); }
            else { for (MesProWorkOrderDO order : orders) { upsertRow(row, order, result, false); } }
        }
        return result.getCreatedCount() + result.getUpdatedCount();
    }

}
