package cn.iocoder.yudao.module.mes.service.md.item.sync;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductUnitService;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemTypeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemTypeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.unitmeasure.MesMdUnitMeasureMapper;
import cn.iocoder.yudao.module.mes.enums.md.MesMdItemTypeEnum;
import lombok.RequiredArgsConstructor;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID;

@Service
@Validated
@RequiredArgsConstructor
public class MesKingdeeItemSyncServiceImpl implements MesKingdeeItemSyncService {

    static final String DEFAULT_ITEM_TYPE_CODE = "KINGDEE_PRODUCT";
    static final String DEFAULT_ITEM_TYPE_NAME = "金蝶同步产品";
    private static final String DEFAULT_ITEM_REMARK = "ERP imported item";
    private static final String DEFAULT_UNIT_REMARK = "ERP imported unit";

    private final ErpProductMapper productMapper;
    private final ErpProductUnitService productUnitService;
    private final MesMdItemMapper itemMapper;
    private final MesMdItemTypeMapper itemTypeMapper;
    private final MesMdUnitMeasureMapper unitMeasureMapper;
    private final SqlSessionTemplate sqlSessionTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesKingdeeItemSyncResult syncItems() {
        List<ErpProductDO> products = productMapper.selectList();
        return syncProducts(products, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesKingdeeItemSyncResult syncItemsByProductCodes(Collection<String> productCodes) {
        LinkedHashSet<String> normalizedCodes = normalizeProductCodes(productCodes);
        if (normalizedCodes.isEmpty()) {
            return new MesKingdeeItemSyncResult();
        }
        sqlSessionTemplate.clearCache();
        return syncProducts(productMapper.selectListByBarCodes(normalizedCodes), false);
    }

    private MesKingdeeItemSyncResult syncProducts(List<ErpProductDO> products, boolean disableMissingItems) {
        Map<Long, ErpProductUnitDO> productUnitMap = productUnitService.getProductUnitMap(products.stream()
                .map(ErpProductDO::getUnitId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        Map<String, ErpProductDO> productMap = new LinkedHashMap<>();
        for (ErpProductDO product : products) {
            String productCode = StrUtil.trimToEmpty(product.getBarCode());
            if (StrUtil.isBlank(productCode)) {
                throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                        "erp product bar code is blank");
            }
            if (productMap.putIfAbsent(productCode, product) != null) {
                throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                        "duplicate erp product bar code: " + productCode);
            }
        }

        List<MesMdItemDO> existingItems = disableMissingItems ? itemMapper.selectListAll()
                : itemMapper.selectListByCodes(productMap.keySet());
        Map<String, MesMdItemDO> existingItemMap = new LinkedHashMap<>();
        for (MesMdItemDO item : existingItems) {
            existingItemMap.putIfAbsent(item.getCode(), item);
        }

        Long itemTypeId = ensureProductItemTypeId();
        Map<String, Long> unitIds = new LinkedHashMap<>();
        MesKingdeeItemSyncResult result = new MesKingdeeItemSyncResult();
        for (ErpProductDO product : productMap.values()) {
            Long unitMeasureId = ensureUnitMeasureId(product, productUnitMap, unitIds);
            Integer status = resolveStatus(product);
            MesMdItemDO existingItem = existingItemMap.remove(product.getBarCode());
            if (existingItem == null) {
                itemMapper.insert(buildCreateItem(product, itemTypeId, unitMeasureId, status));
                result.addCreated();
                continue;
            }
            MesMdItemDO updateItem = buildUpdateItem(existingItem, product, itemTypeId, unitMeasureId, status);
            if (updateItem == null) {
                result.addSkipped();
                continue;
            }
            itemMapper.updateById(updateItem);
            result.addUpdated();
        }

        if (!disableMissingItems) {
            return result;
        }
        Set<String> syncedCodes = new LinkedHashSet<>(productMap.keySet());
        for (MesMdItemDO existingItem : existingItemMap.values()) {
            if (!Objects.equals(existingItem.getItemTypeId(), itemTypeId)) {
                continue;
            }
            if (syncedCodes.contains(existingItem.getCode())) {
                continue;
            }
            if (CommonStatusEnum.isDisable(existingItem.getStatus())) {
                result.addSkipped();
                continue;
            }
            itemMapper.updateById(new MesMdItemDO()
                    .setId(existingItem.getId())
                    .setStatus(CommonStatusEnum.DISABLE.getStatus()));
            result.addDisabled();
        }
        return result;
    }

    private LinkedHashSet<String> normalizeProductCodes(Collection<String> productCodes) {
        if (productCodes == null || productCodes.isEmpty()) {
            return new LinkedHashSet<>();
        }
        LinkedHashSet<String> normalizedCodes = new LinkedHashSet<>();
        for (String productCode : productCodes) {
            String normalizedCode = StrUtil.trimToNull(productCode);
            if (normalizedCode == null) {
                throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                        "erp product code is blank");
            }
            normalizedCodes.add(normalizedCode);
        }
        return normalizedCodes;
    }

    private MesMdItemDO buildCreateItem(ErpProductDO product, Long itemTypeId,
                                        Long unitMeasureId, Integer status) {
        MesMdItemDO item = new MesMdItemDO();
        item.setCode(product.getBarCode());
        item.setName(product.getName());
        item.setSpecification(StrUtil.emptyToNull(product.getStandard()));
        item.setUnitMeasureId(unitMeasureId);
        item.setItemTypeId(itemTypeId);
        item.setStatus(status);
        item.setSafeStockFlag(Boolean.FALSE);
        item.setMinStock(BigDecimal.ZERO);
        item.setMaxStock(BigDecimal.ZERO);
        item.setHighValue(Boolean.FALSE);
        item.setBatchFlag(Boolean.FALSE);
        item.setRemark(DEFAULT_ITEM_REMARK);
        return item;
    }

    private MesMdItemDO buildUpdateItem(MesMdItemDO existingItem, ErpProductDO product,
                                        Long itemTypeId, Long unitMeasureId, Integer status) {
        String specification = StrUtil.emptyToNull(product.getStandard());
        if (Objects.equals(existingItem.getCode(), product.getBarCode())
                && Objects.equals(existingItem.getName(), product.getName())
                && Objects.equals(existingItem.getSpecification(), specification)
                && Objects.equals(existingItem.getUnitMeasureId(), unitMeasureId)
                && Objects.equals(existingItem.getItemTypeId(), itemTypeId)
                && Objects.equals(existingItem.getStatus(), status)) {
            return null;
        }
        MesMdItemDO update = new MesMdItemDO();
        update.setId(existingItem.getId());
        update.setCode(product.getBarCode());
        update.setName(product.getName());
        update.setSpecification(specification);
        update.setUnitMeasureId(unitMeasureId);
        update.setItemTypeId(itemTypeId);
        update.setStatus(status);
        return update;
    }

    private Long ensureProductItemTypeId() {
        MesMdItemTypeDO itemType = itemTypeMapper.selectByParentIdAndCode(
                MesMdItemTypeDO.PARENT_ID_ROOT, DEFAULT_ITEM_TYPE_CODE);
        if (itemType != null) {
            return itemType.getId();
        }
        itemType = new MesMdItemTypeDO();
        itemType.setParentId(MesMdItemTypeDO.PARENT_ID_ROOT);
        itemType.setCode(DEFAULT_ITEM_TYPE_CODE);
        itemType.setName(DEFAULT_ITEM_TYPE_NAME);
        itemType.setItemOrProduct(MesMdItemTypeEnum.PRODUCT.getValue());
        itemType.setSort(0);
        itemType.setStatus(CommonStatusEnum.ENABLE.getStatus());
        itemTypeMapper.insert(itemType);
        return itemType.getId();
    }

    private Long ensureUnitMeasureId(ErpProductDO product,
                                     Map<Long, ErpProductUnitDO> productUnitMap,
                                     Map<String, Long> unitIds) {
        if (product.getUnitId() == null) {
            throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "erp product unit id is blank for product " + product.getBarCode());
        }
        ErpProductUnitDO productUnit = productUnitMap.get(product.getUnitId());
        if (productUnit == null) {
            throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "erp product unit is missing for product " + product.getBarCode());
        }
        String unitName = StrUtil.trimToEmpty(productUnit.getName());
        if (StrUtil.isBlank(unitName)) {
            throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "erp product unit name is blank for product " + product.getBarCode());
        }
        return unitIds.computeIfAbsent(unitName, key -> {
            MesMdUnitMeasureDO unitMeasure = unitMeasureMapper.selectByCode(key);
            if (unitMeasure == null) {
                unitMeasure = unitMeasureMapper.selectByName(key);
            }
            if (unitMeasure != null) {
                return unitMeasure.getId();
            }
            unitMeasure = new MesMdUnitMeasureDO();
            unitMeasure.setCode(key);
            unitMeasure.setName(key);
            unitMeasure.setPrimaryFlag(Boolean.TRUE);
            unitMeasure.setChangeRate(BigDecimal.ONE);
            unitMeasure.setStatus(CommonStatusEnum.ENABLE.getStatus());
            unitMeasure.setRemark(DEFAULT_UNIT_REMARK);
            unitMeasureMapper.insert(unitMeasure);
            return unitMeasure.getId();
        });
    }

    private Integer resolveStatus(ErpProductDO product) {
        Integer status = product.getStatus();
        if (Objects.equals(status, CommonStatusEnum.ENABLE.getStatus())
                || Objects.equals(status, CommonStatusEnum.DISABLE.getStatus())) {
            return status;
        }
        throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                "unsupported erp product status: " + status + ", product code: " + product.getBarCode());
    }

}
