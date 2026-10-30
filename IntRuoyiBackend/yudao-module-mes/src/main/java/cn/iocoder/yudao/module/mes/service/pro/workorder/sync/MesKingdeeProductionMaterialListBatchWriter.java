package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionMaterialList;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRunResult;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeTransactionService;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/** One ERP page, including its committed counters, is one independent transaction. */
@Service
@RequiredArgsConstructor
public class MesKingdeeProductionMaterialListBatchWriter {

    private static final int WRITE_BATCH_SIZE = 200;
    private final MesKingdeeProductionMaterialListMapper materialListMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProWorkOrderBomMapper workOrderBomMapper;
    private final MesMdItemMapper itemMapper;
    private final ErpKingdeeSyncRuntimeTransactionService runtimeTransactionService;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ErpKingdeeSyncRunResult writePage(Long runId, List<ErpKingdeeProductionMaterialList> rows) {
        Objects.requireNonNull(runId, "runId");
        if (rows.isEmpty() || rows.size() > 1000) {
            throw new IllegalArgumentException("生产用料清单批次必须为 1 至 1000 行");
        }
        Long requestingTenant = TenantContextHolder.getRequiredTenantId();
        Set<String> orderNos = rows.stream().map(ErpKingdeeProductionMaterialList::getProductionOrderNo)
                .collect(Collectors.toSet());
        Map<String, List<MesProWorkOrderDO>> orders = TenantUtils.executeIgnore(() ->
                workOrderMapper.selectListByCodes(orderNos)).stream()
                .collect(Collectors.groupingBy(MesProWorkOrderDO::getCode));
        // Preserve routing to every matching work order's tenant, or the requesting tenant when unmatched.
        Map<Long, List<Target>> tenants = new TreeMap<>();
        for (ErpKingdeeProductionMaterialList row : rows) {
            List<MesProWorkOrderDO> matches = orders.get(row.getProductionOrderNo());
            if (matches == null) {
                tenants.computeIfAbsent(requestingTenant, ignored -> new ArrayList<>()).add(new Target(row, null));
            } else {
                for (MesProWorkOrderDO order : matches) {
                    tenants.computeIfAbsent(Objects.requireNonNull(order.getTenantId(), "工单缺少租户"),
                            ignored -> new ArrayList<>()).add(new Target(row, order.getId()));
                }
            }
        }
        int[] counts = new int[3];
        tenants.forEach((tenantId, targets) -> TenantUtils.execute(tenantId, () -> {
            ErpKingdeeSyncRunResult result = writeTenant(targets);
            counts[0] = Math.addExact(counts[0], result.getCreatedCount());
            counts[1] = Math.addExact(counts[1], result.getUpdatedCount());
            counts[2] = Math.addExact(counts[2], result.getSkippedCount());
        }));
        ErpKingdeeSyncRunResult result = ErpKingdeeSyncRunResult.success(null, counts[0], counts[1], counts[2], 0);
        // TenantUtils has restored the requesting tenant; failure here rolls back all tenants in this page.
        runtimeTransactionService.recordProductionMaterialListProgress(runId, result);
        return result;
    }

    private ErpKingdeeSyncRunResult writeTenant(List<Target> targets) {
        Set<Long> orderIds = targets.stream().map(Target::workOrderId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        Map<Long, MesProWorkOrderDO> orders = new HashMap<>();
        if (!orderIds.isEmpty()) {
            workOrderMapper.selectListByIdsForUpdate(orderIds).forEach(order -> orders.put(order.getId(), order));
            if (!orders.keySet().equals(orderIds)) {
                throw new IllegalStateException("生产工单已删除或租户归属已变化，当前批次未提交");
            }
        }
        Map<SourceKey, MesKingdeeProductionMaterialListDO> keys = new LinkedHashMap<>();
        targets.forEach(target -> keys.putIfAbsent(SourceKey.of(target.row()), sourceKey(target.row())));
        Map<SourceKey, MesKingdeeProductionMaterialListDO> existing = new HashMap<>();
        materialListMapper.selectBySourceLines(keys.values()).forEach(row -> {
            if (existing.put(SourceKey.of(row), row) != null) {
                throw new IllegalStateException("生产用料清单存在重复业务标识，当前批次未提交");
            }
        });

        Set<String> itemCodes = new HashSet<>();
        targets.stream().filter(target -> needsWrite(existing.get(SourceKey.of(target.row())), target.row()))
                .forEach(target -> {
                    itemCodes.add(target.row().getProductCode());
                    itemCodes.add(target.row().getChildMaterialCode());
                });
        Map<String, MesMdItemDO> items = new HashMap<>();
        if (!itemCodes.isEmpty()) {
            itemMapper.selectListByCodes(itemCodes).forEach(item -> {
                if (items.put(item.getCode(), item) != null) {
                    throw new IllegalStateException("租户内存在重复物料编码：" + item.getCode());
                }
            });
        }
        Map<BomKey, Long> boms = new HashMap<>();
        if (!orderIds.isEmpty() && !itemCodes.isEmpty()) {
            Set<Long> itemIds = items.values().stream().map(MesMdItemDO::getId).collect(Collectors.toSet());
            if (!itemIds.isEmpty()) {
                for (MesProWorkOrderBomDO bom : workOrderBomMapper.selectListByWorkOrderIdsAndItemIds(orderIds, itemIds)) {
                    boms.putIfAbsent(new BomKey(bom.getWorkOrderId(), bom.getItemId()), bom.getId());
                }
            }
        }
        Map<SourceKey, MesKingdeeProductionMaterialListDO> inserts = new LinkedHashMap<>();
        Map<SourceKey, MesKingdeeProductionMaterialListDO> updates = new LinkedHashMap<>();
        int skipped = 0;
        for (Target target : targets) {
            ErpKingdeeProductionMaterialList row = target.row();
            SourceKey key = SourceKey.of(row);
            MesKingdeeProductionMaterialListDO previous = existing.get(key);
            if (!needsWrite(previous, row)) {
                skipped++;
                continue;
            }
            MesProWorkOrderDO order = orders.get(target.workOrderId());
            if (order != null && !row.getProductionOrderNo().equals(order.getCode())) {
                throw new IllegalStateException("生产工单编号已变化，当前批次未提交");
            }
            MesMdItemDO child = items.get(row.getChildMaterialCode());
            Long bomId = child == null ? null : boms.get(new BomKey(target.workOrderId(), child.getId()));
            MesKingdeeProductionMaterialListDO mapped = MesKingdeeProductionMaterialListRowMapping.map(
                    row, order, items.get(row.getProductCode()), child, bomId);
            if (previous == null || inserts.containsKey(key)) {
                inserts.put(key, mapped);
            } else {
                mapped.setId(previous.getId());
                updates.put(key, mapped);
            }
            existing.put(key, mapped);
        }
        if (!inserts.isEmpty()) {
            requireBatchSuccess(materialListMapper.insert(inserts.values(), WRITE_BATCH_SIZE), inserts.size());
        }
        if (!updates.isEmpty()) {
            requireBatchSuccess(materialListMapper.updateById(updates.values(), WRITE_BATCH_SIZE), updates.size());
        }
        return ErpKingdeeSyncRunResult.success(null, inserts.size(), updates.size(), skipped, 0);
    }

    private static void requireBatchSuccess(List<org.apache.ibatis.executor.BatchResult> results, int expected) {
        int statements = 0;
        for (org.apache.ibatis.executor.BatchResult result : results) {
            for (int affected : result.getUpdateCounts()) {
                if (affected <= 0 && affected != java.sql.Statement.SUCCESS_NO_INFO) {
                    throw new IllegalStateException("生产用料清单批量写入未成功，当前批次未提交");
                }
                statements++;
            }
        }
        if (statements != expected) {
            throw new IllegalStateException("生产用料清单批量写入数量不一致，当前批次未提交");
        }
    }

    private static boolean needsWrite(MesKingdeeProductionMaterialListDO existing, ErpKingdeeProductionMaterialList row) {
        return existing == null || MesKingdeeProductionMaterialListRowMapping.shouldBackfillErpColumns(existing, row);
    }

    private static MesKingdeeProductionMaterialListDO sourceKey(ErpKingdeeProductionMaterialList row) {
        return new MesKingdeeProductionMaterialListDO().setSourceBillNo(row.getBillNo())
                .setProductionOrderNo(row.getProductionOrderNo()).setProductionOrderLineNo(row.getProductionOrderLineNo())
                .setChildMaterialCode(row.getChildMaterialCode());
    }

    private record Target(ErpKingdeeProductionMaterialList row, Long workOrderId) { }
    private record BomKey(Long workOrderId, Long itemId) { }
    private record SourceKey(String billNo, String orderNo, Integer lineNo, String childCode) {
        static SourceKey of(ErpKingdeeProductionMaterialList row) {
            return new SourceKey(row.getBillNo(), row.getProductionOrderNo(), row.getProductionOrderLineNo(), row.getChildMaterialCode());
        }
        static SourceKey of(MesKingdeeProductionMaterialListDO row) {
            return new SourceKey(row.getSourceBillNo(), row.getProductionOrderNo(), row.getProductionOrderLineNo(), row.getChildMaterialCode());
        }
    }
}
