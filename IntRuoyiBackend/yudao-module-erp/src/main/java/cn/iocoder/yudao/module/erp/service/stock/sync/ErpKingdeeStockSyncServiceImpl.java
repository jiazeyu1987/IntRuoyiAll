package cn.iocoder.yudao.module.erp.service.stock.sync;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductCategoryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpKingdeeWarehouseSyncRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductCategoryMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUnitMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpKingdeeWarehouseSyncRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeInventoryClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeInventoryRow;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID;

@Service
@Validated
@RequiredArgsConstructor
public class ErpKingdeeStockSyncServiceImpl implements ErpKingdeeStockSyncService {

    private static final String DEFAULT_CATEGORY_CODE = "KINGDEE_STOCK_PRODUCT";
    private static final String DEFAULT_CATEGORY_NAME = "Kingdee Imported Stock Product";

    private final ErpKingdeeInventoryClient inventoryClient;
    private final ErpKingdeeConfigService kingdeeConfigService;
    private final ErpStockMapper stockMapper;
    private final ErpWarehouseMapper warehouseMapper;
    private final ErpKingdeeWarehouseSyncRecordMapper warehouseSyncRecordMapper;
    private final ErpProductMapper productMapper;
    private final ErpProductCategoryMapper productCategoryMapper;
    private final ErpProductUnitMapper productUnitMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeStockSyncResult syncStocks() {
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validateBaseConfig();
        List<ErpKingdeeInventoryRow> rows = inventoryClient.fetchInventoryRows(kingdeeProperties);
        return syncRows(rows, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeStockSyncResult syncStocksFullSkipExisting() {
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validateBaseConfig();
        List<ErpKingdeeInventoryRow> rows = inventoryClient.fetchInventoryRows(kingdeeProperties);
        return syncRows(rows, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeStockSyncResult syncStocksModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd) {
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validateBaseConfig();
        List<ErpKingdeeInventoryRow> rows = inventoryClient.fetchInventoryRowsModifiedBetween(
                kingdeeProperties, windowStart, windowEnd);
        return syncRows(rows, false);
    }

    private ErpKingdeeStockSyncResult syncRows(List<ErpKingdeeInventoryRow> rows, boolean skipExisting) {
        Map<String, Long> warehouseIds = new HashMap<>();
        Map<String, ErpKingdeeInventoryRow> rowsByMaterial = new LinkedHashMap<>();
        for (ErpKingdeeInventoryRow row : rows) {
            rowsByMaterial.putIfAbsent(row.getMaterialNumber(), row);
        }
        Map<String, Long> productIds = prepareProductIds(rowsByMaterial);
        Map<String, ErpStockDO> aggregated = new LinkedHashMap<>();
        for (ErpKingdeeInventoryRow row : rows) {
            Long warehouseId = warehouseIds.computeIfAbsent(
                    row.getStockOrgNumber() + ":" + row.getWarehouseNumber(),
                    key -> ensureWarehouse(row));
            Long productId = productIds.get(row.getMaterialNumber());
            if (productId == null) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                        "inventory product is missing for material " + row.getMaterialNumber());
            }
            String aggregateKey = productId + ":" + warehouseId;
            ErpStockDO stock = aggregated.computeIfAbsent(aggregateKey,
                    key -> new ErpStockDO().setProductId(productId).setWarehouseId(warehouseId).setCount(BigDecimal.ZERO));
            stock.setCount(stock.getCount().add(row.getQuantity()));
        }
        ErpKingdeeStockSyncResult result = new ErpKingdeeStockSyncResult();
        upsertStocks(aggregated, skipExisting, result);
        result.setSyncedCount(aggregated.size());
        return result;
    }

    private void upsertStocks(Map<String, ErpStockDO> aggregated, boolean skipExisting,
                              ErpKingdeeStockSyncResult result) {
        List<ErpStockDO> stocksToCreate = new ArrayList<>();
        for (ErpStockDO stock : aggregated.values()) {
            ErpStockDO existing = stockMapper.selectByProductIdAndWarehouseId(stock.getProductId(), stock.getWarehouseId());
            if (existing == null) {
                stocksToCreate.add(stock);
                continue;
            }
            if (skipExisting) {
                result.addSkipped();
                continue;
            }
            stockMapper.updateById(new ErpStockDO()
                    .setId(existing.getId())
                    .setProductId(existing.getProductId())
                    .setWarehouseId(existing.getWarehouseId())
                    .setCount(stock.getCount()));
        }
        if (!stocksToCreate.isEmpty()) {
            stockMapper.insertBatch(stocksToCreate);
        }
    }

    private Long ensureWarehouse(ErpKingdeeInventoryRow row) {
        ErpKingdeeWarehouseSyncRecordDO syncRecord = warehouseSyncRecordMapper
                .selectBySourceKey(row.getStockOrgNumber(), row.getWarehouseNumber());
        if (syncRecord != null) {
            return syncRecord.getWarehouseId();
        }
        String warehouseName = buildWarehouseDisplayName(row);
        ErpWarehouseDO warehouse = warehouseMapper.selectByName(warehouseName);
        if (warehouse == null) {
            warehouse = new ErpWarehouseDO();
            warehouse.setName(warehouseName);
            warehouse.setAddress(row.getStockOrgName());
            warehouse.setSort(0L);
            warehouse.setRemark("Kingdee warehouse " + row.getWarehouseNumber());
            warehouse.setStatus(CommonStatusEnum.ENABLE.getStatus());
            warehouse.setDefaultStatus(Boolean.FALSE);
            warehouseMapper.insert(warehouse);
        }
        warehouseSyncRecordMapper.insert(new ErpKingdeeWarehouseSyncRecordDO()
                .setSourceStockOrgNumber(row.getStockOrgNumber())
                .setSourceStockOrgName(row.getStockOrgName())
                .setSourceWarehouseNumber(row.getWarehouseNumber())
                .setSourceWarehouseName(row.getWarehouseName())
                .setWarehouseId(warehouse.getId()));
        return warehouse.getId();
    }

    private String buildWarehouseDisplayName(ErpKingdeeInventoryRow row) {
        String orgName = StrUtil.blankToDefault(row.getStockOrgName(), row.getStockOrgNumber());
        String warehouseName = StrUtil.blankToDefault(row.getWarehouseName(), row.getWarehouseNumber());
        return orgName + " / " + warehouseName;
    }

    private Map<String, Long> prepareProductIds(Map<String, ErpKingdeeInventoryRow> rowsByMaterial) {
        Map<String, Long> productIds = new HashMap<>();
        if (rowsByMaterial.isEmpty()) {
            return productIds;
        }
        List<String> materialNumbers = new ArrayList<>(rowsByMaterial.keySet());
        List<ErpProductDO> existingProducts = productMapper.selectListByBarCodes(materialNumbers);
        for (ErpProductDO product : existingProducts) {
            productIds.put(product.getBarCode(), product.getId());
        }
        if (productIds.size() == rowsByMaterial.size()) {
            return productIds;
        }

        Long categoryId = ensureProductCategory();
        List<ErpProductDO> productsToCreate = new ArrayList<>();
        List<String> missingMaterialNumbers = new ArrayList<>();
        for (Map.Entry<String, ErpKingdeeInventoryRow> entry : rowsByMaterial.entrySet()) {
            if (productIds.containsKey(entry.getKey())) {
                continue;
            }
            ErpKingdeeInventoryRow row = entry.getValue();
            productsToCreate.add(buildProduct(row, categoryId));
            missingMaterialNumbers.add(entry.getKey());
        }
        productMapper.insertBatch(productsToCreate, 1000);
        List<ErpProductDO> createdProducts = productMapper.selectListByBarCodes(missingMaterialNumbers);
        for (ErpProductDO product : createdProducts) {
            productIds.put(product.getBarCode(), product.getId());
        }
        for (String materialNumber : missingMaterialNumbers) {
            if (!productIds.containsKey(materialNumber)) {
                throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                        "inventory product create failed for material " + materialNumber);
            }
        }
        return productIds;
    }

    private ErpProductDO buildProduct(ErpKingdeeInventoryRow row, Long categoryId) {
        ErpProductDO product = new ErpProductDO();
        product.setName(StrUtil.blankToDefault(row.getMaterialName(), row.getMaterialNumber()));
        product.setBarCode(row.getMaterialNumber());
        product.setCategoryId(categoryId);
        product.setUnitId(ensureProductUnit(row.getUnitName(), row.getMaterialNumber()));
        product.setStatus(CommonStatusEnum.ENABLE.getStatus());
        product.setStandard(row.getMaterialSpecification());
        return product;
    }

    private Long ensureProductCategory() {
        ErpProductCategoryDO category = productCategoryMapper.selectByCode(DEFAULT_CATEGORY_CODE);
        if (category != null) {
            return category.getId();
        }
        category = new ErpProductCategoryDO();
        category.setParentId(ErpProductCategoryDO.PARENT_ID_ROOT);
        category.setName(DEFAULT_CATEGORY_NAME);
        category.setCode(DEFAULT_CATEGORY_CODE);
        category.setSort(0);
        category.setStatus(CommonStatusEnum.ENABLE.getStatus());
        productCategoryMapper.insert(category);
        return category.getId();
    }

    private Long ensureProductUnit(String unitName, String materialNumber) {
        if (StrUtil.isBlank(unitName)) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "inventory unit is blank for material " + materialNumber);
        }
        ErpProductUnitDO unit = productUnitMapper.selectByName(unitName);
        if (unit != null) {
            return unit.getId();
        }
        unit = new ErpProductUnitDO();
        unit.setName(unitName);
        unit.setStatus(CommonStatusEnum.ENABLE.getStatus());
        productUnitMapper.insert(unit);
        return unit.getId();
    }

}
