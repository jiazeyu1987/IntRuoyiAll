package cn.iocoder.yudao.module.erp.service.stock.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.ErpKingdeeInventoryListPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.ErpKingdeeInventoryListRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.kingdee.ErpKingdeeInventoryListDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.kingdee.ErpKingdeeInventoryListMapper;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeInventoryClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeInventoryRow;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
public class ErpKingdeeInventoryListServiceImpl implements ErpKingdeeInventoryListService {

    @Resource
    private ErpKingdeeInventoryClient inventoryClient;
    @Resource
    private ErpKingdeeConfigService kingdeeConfigService;
    @Resource
    private ErpKingdeeInventoryListMapper inventoryListMapper;

    @Override
    public PageResult<ErpKingdeeInventoryListRespVO> getPage(ErpKingdeeInventoryListPageReqVO pageReqVO) {
        return BeanUtils.toBean(inventoryListMapper.selectPage(pageReqVO), ErpKingdeeInventoryListRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncAll() {
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        properties.validateBaseConfig();
        List<ErpKingdeeInventoryRow> rows = inventoryClient.fetchInventoryRows(properties);
        return syncRows(rows, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeInventoryListSyncResult syncAllSkipExisting() {
        return syncAllSkipExisting(null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeInventoryListSyncResult syncAllSkipExisting(LocalDateTime windowStart, LocalDateTime windowEnd) {
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        properties.validateBaseConfig();
        List<ErpKingdeeInventoryRow> rows = (windowStart == null ? inventoryClient.fetchInventoryRows(properties)
                : inventoryClient.fetchInventoryRowsModifiedBetween(properties, windowStart, windowEnd));
        ErpKingdeeInventoryListSyncResult result = new ErpKingdeeInventoryListSyncResult();
        LocalDateTime now = LocalDateTime.now();
        for (ErpKingdeeInventoryRow row : rows) {
            if (upsert(row, now, true)) {
                result.addCreated();
            } else {
                result.addSkipped();
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd) {
        if (inventoryListMapper.selectCount() == 0) {
            return syncAll();
        }
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        properties.validateBaseConfig();
        List<ErpKingdeeInventoryRow> rows = inventoryClient.fetchInventoryRowsModifiedBetween(
                properties, windowStart, windowEnd);
        return syncRows(rows, false);
    }

    private int syncRows(List<ErpKingdeeInventoryRow> rows, boolean skipExisting) {
        LocalDateTime now = LocalDateTime.now();
        for (ErpKingdeeInventoryRow row : rows) {
            upsert(row, now, skipExisting);
        }
        return rows.size();
    }

    private String sourceLineKey(ErpKingdeeInventoryRow row) {
        return row.getStockOrgNumber() + "|" + row.getWarehouseNumber() + "|"
                + row.getMaterialNumber() + "|" + row.getLotNumber();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeInventoryListSyncResult syncAllWithUpdates() {
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        properties.validateBaseConfig();
        List<ErpKingdeeInventoryRow> rows = inventoryClient.fetchInventoryRows(properties);
        // The local list represents org/warehouse/material/lot, while ERP may split
        // that group across stock locations or owners. Sum the complete snapshot.
        java.util.Map<String, ErpKingdeeInventoryRow> grouped = new java.util.LinkedHashMap<>();
        for (ErpKingdeeInventoryRow row : rows) {
            String key = sourceLineKey(row);
            ErpKingdeeInventoryRow aggregate = grouped.get(key);
            if (aggregate == null) {
                grouped.put(key, BeanUtils.toBean(row, ErpKingdeeInventoryRow.class));
                continue;
            }
            if (!java.util.Objects.equals(aggregate.getUnitName(), row.getUnitName())) {
                throw new IllegalStateException("同一库存分组的基本单位不一致: " + key);
            }
            aggregate.setQuantity(aggregate.getQuantity().add(row.getQuantity()));
            if (row.getSourceModifyTime() != null && (aggregate.getSourceModifyTime() == null
                    || row.getSourceModifyTime().isAfter(aggregate.getSourceModifyTime()))) {
                aggregate.setSourceModifyTime(row.getSourceModifyTime());
            }
        }
        rows = new java.util.ArrayList<>(grouped.values());
        ErpKingdeeInventoryListSyncResult result = new ErpKingdeeInventoryListSyncResult();
        LocalDateTime now = LocalDateTime.now();
        for (ErpKingdeeInventoryRow row : rows) {
            boolean exists = inventoryListMapper.selectBySourceLine(sourceLineKey(row)) != null;
            upsert(row, now, false);
            if (exists) result.addUpdated(); else result.addCreated();
        }
        java.util.Set<String> sourceKeys = rows.stream().map(this::sourceLineKey).collect(java.util.stream.Collectors.toSet());
        for (ErpKingdeeInventoryListDO existing : inventoryListMapper.selectList()) {
            if (!sourceKeys.contains(existing.getSourceLineKey()) && existing.getQuantity().signum() != 0) {
                if (inventoryListMapper.updateById(new ErpKingdeeInventoryListDO().setId(existing.getId())
                        .setQuantity(java.math.BigDecimal.ZERO).setLastSyncTime(now)) != 1)
                    throw new IllegalStateException("ERP库存明细清零失败: " + existing.getId());
                result.addUpdated();
            }
        }
        return result;
    }

    private boolean upsert(ErpKingdeeInventoryRow row, LocalDateTime now, boolean skipExisting) {
        String sourceLineKey = sourceLineKey(row);
        ErpKingdeeInventoryListDO existing = inventoryListMapper.selectBySourceLine(sourceLineKey);
        ErpKingdeeInventoryListDO record = ErpKingdeeInventoryListDO.builder()
                .sourceFormId(ErpKingdeeInventoryRow.FORM_ID)
                .sourceLineKey(sourceLineKey)
                .materialNumber(row.getMaterialNumber())
                .materialName(row.getMaterialName())
                .materialSpecification(row.getMaterialSpecification())
                .warehouseNumber(row.getWarehouseNumber())
                .warehouseName(row.getWarehouseName())
                .lotNumber(row.getLotNumber())
                .unitName(row.getUnitName())
                .quantity(row.getQuantity())
                .stockOrgNumber(row.getStockOrgNumber())
                .stockOrgName(row.getStockOrgName())
                .sourceModifyTime(row.getSourceModifyTime())
                .lastSyncTime(now)
                .rawPayload(JsonUtils.toJsonString(row))
                .build();
        if (existing == null) {
            inventoryListMapper.insert(record);
            return true;
        }
        if (skipExisting) {
            return false;
        }
        record.setId(existing.getId());
        inventoryListMapper.updateById(record);
        return true;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncByCode(String code) {
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        return syncRows(inventoryClient.fetchInventoryRowsByCode(properties, code), false);
    }

}
