package cn.iocoder.yudao.module.erp.service.product.sync;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductCategoryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductCategoryMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUnitMapper;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeMaterial;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeMaterialClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID;

@Service
@Validated
@RequiredArgsConstructor
public class ErpKingdeeProductSyncServiceImpl implements ErpKingdeeProductSyncService {

    private final ErpKingdeeMaterialClient materialClient;
    private final ErpKingdeeConfigService kingdeeConfigService;
    private final ErpProductMapper productMapper;
    private final ErpProductCategoryMapper productCategoryMapper;
    private final ErpProductUnitMapper productUnitMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeProductSyncResult syncProducts() {
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validateProductSyncConfig();
        List<ErpKingdeeMaterial> materials = materialClient.fetchMaterials(kingdeeProperties);
        return syncMaterials(materials, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeProductSyncResult syncProductsFullSkipExisting() {
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validateProductSyncConfig();
        List<ErpKingdeeMaterial> materials = materialClient.fetchMaterials(kingdeeProperties);
        return syncMaterials(materials, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeProductSyncResult syncProductsModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd) {
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validateProductSyncConfig();
        List<ErpKingdeeMaterial> materials =
                materialClient.fetchMaterialsModifiedBetween(kingdeeProperties, windowStart, windowEnd);
        return syncMaterials(materials, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeProductSyncResult syncProductsByNumbers(Collection<String> materialNumbers) {
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validateProductSyncConfig();
        List<ErpKingdeeMaterial> materials = materialClient.fetchMaterialsByNumbers(kingdeeProperties, materialNumbers);
        return syncMaterials(materials, false);
    }

    private ErpKingdeeProductSyncResult syncMaterials(List<ErpKingdeeMaterial> materials,
                                                      boolean skipExisting) {
        Map<String, ErpKingdeeMaterial> materialsByNumber = new LinkedHashMap<>();
        for (ErpKingdeeMaterial material : materials) {
            materialsByNumber.putIfAbsent(material.getMaterialNumber(), material);
        }
        List<ErpProductDO> existingProducts = productMapper.selectListByBarCodes(materialsByNumber.keySet());
        Map<String, ErpProductDO> existingProductMap = new LinkedHashMap<>();
        for (ErpProductDO product : existingProducts) {
            existingProductMap.put(product.getBarCode(), product);
        }

        Map<String, Long> categoryIds = new LinkedHashMap<>();
        Map<String, Long> unitIds = new LinkedHashMap<>();
        List<ErpProductDO> productsToCreate = new ArrayList<>();
        List<ErpProductDO> productsToUpdate = new ArrayList<>();
        ErpKingdeeProductSyncResult result = new ErpKingdeeProductSyncResult();
        for (ErpKingdeeMaterial material : materialsByNumber.values()) {
            Long categoryId = ensureCategoryId(material, categoryIds);
            Long unitId = ensureUnitId(material, unitIds);
            Integer status = resolveStatus(material);
            ErpProductDO existingProduct = existingProductMap.get(material.getMaterialNumber());
            if (existingProduct == null) {
                productsToCreate.add(buildCreateProduct(material, categoryId, unitId, status));
                result.addCreated(material.getMaterialNumber());
                continue;
            }
            if (skipExisting) {
                result.addSkipped(material.getMaterialNumber());
                continue;
            }
            ErpProductDO updateProduct = buildUpdateProduct(existingProduct, material, categoryId, unitId, status);
            if (updateProduct == null) {
                result.addSkipped(material.getMaterialNumber());
                continue;
            }
            productsToUpdate.add(updateProduct);
            result.addUpdated(material.getMaterialNumber());
        }
        if (CollUtil.isNotEmpty(productsToCreate)) {
            productMapper.insertBatch(productsToCreate, 1000);
        }
        if (CollUtil.isNotEmpty(productsToUpdate)) {
            productMapper.updateBatch(productsToUpdate, 1000);
        }
        return result;
    }

    private ErpProductDO buildCreateProduct(ErpKingdeeMaterial material,
                                            Long categoryId,
                                            Long unitId,
                                            Integer status) {
        ErpProductDO product = new ErpProductDO();
        product.setName(material.getMaterialName());
        product.setBarCode(material.getMaterialNumber());
        product.setCategoryId(categoryId);
        product.setUnitId(unitId);
        product.setStatus(status);
        product.setStandard(StrUtil.emptyToNull(material.getSpecification()));
        return product;
    }

    private ErpProductDO buildUpdateProduct(ErpProductDO existingProduct,
                                            ErpKingdeeMaterial material,
                                            Long categoryId,
                                            Long unitId,
                                            Integer status) {
        String specification = StrUtil.emptyToNull(material.getSpecification());
        if (Objects.equals(existingProduct.getName(), material.getMaterialName())
                && Objects.equals(existingProduct.getCategoryId(), categoryId)
                && Objects.equals(existingProduct.getUnitId(), unitId)
                && Objects.equals(existingProduct.getStatus(), status)
                && Objects.equals(existingProduct.getStandard(), specification)) {
            return null;
        }
        ErpProductDO update = new ErpProductDO();
        update.setId(existingProduct.getId());
        update.setName(material.getMaterialName());
        update.setCategoryId(categoryId);
        update.setUnitId(unitId);
        update.setStatus(status);
        update.setStandard(specification);
        return update;
    }

    private Long ensureCategoryId(ErpKingdeeMaterial material, Map<String, Long> categoryIds) {
        return categoryIds.computeIfAbsent(material.getCategoryCode(), key -> {
            ErpProductCategoryDO category = productCategoryMapper.selectByCode(key);
            if (category != null) {
                return category.getId();
            }
            category = new ErpProductCategoryDO();
            category.setParentId(ErpProductCategoryDO.PARENT_ID_ROOT);
            category.setName(material.getCategoryName());
            category.setCode(material.getCategoryCode());
            category.setSort(0);
            category.setStatus(CommonStatusEnum.ENABLE.getStatus());
            productCategoryMapper.insert(category);
            return category.getId();
        });
    }

    private Long ensureUnitId(ErpKingdeeMaterial material, Map<String, Long> unitIds) {
        return unitIds.computeIfAbsent(material.getUnitName(), key -> {
            ErpProductUnitDO unit = productUnitMapper.selectByName(key);
            if (unit != null) {
                return unit.getId();
            }
            unit = new ErpProductUnitDO();
            unit.setName(key);
            unit.setStatus(CommonStatusEnum.ENABLE.getStatus());
            productUnitMapper.insert(unit);
            return unit.getId();
        });
    }

    private Integer resolveStatus(ErpKingdeeMaterial material) {
        return switch (StrUtil.nullToEmpty(material.getForbidStatus())) {
            case "A" -> CommonStatusEnum.ENABLE.getStatus();
            case "B" -> CommonStatusEnum.DISABLE.getStatus();
            default -> throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "unsupported material forbid status: " + material.getForbidStatus());
        };
    }

}
