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
        return syncRows(rows);
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
        return syncRows(rows);
    }

    private int syncRows(List<ErpKingdeeInventoryRow> rows) {
        LocalDateTime now = LocalDateTime.now();
        for (ErpKingdeeInventoryRow row : rows) {
            upsert(row, now);
        }
        return rows.size();
    }

    private void upsert(ErpKingdeeInventoryRow row, LocalDateTime now) {
        String sourceLineKey = row.getStockOrgNumber() + "|" + row.getWarehouseNumber() + "|"
                + row.getMaterialNumber() + "|" + row.getLotNumber();
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
            return;
        }
        record.setId(existing.getId());
        inventoryListMapper.updateById(record);
    }

}
