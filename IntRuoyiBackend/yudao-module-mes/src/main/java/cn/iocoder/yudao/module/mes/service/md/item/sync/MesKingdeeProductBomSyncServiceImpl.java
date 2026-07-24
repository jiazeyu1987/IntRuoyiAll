package cn.iocoder.yudao.module.mes.service.md.item.sync;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeBomClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeBomLine;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdProductBomDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdProductBomMapper;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderBomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_ITEM_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_PRODUCT_BOM_ERP_SYNC_ITEM_CODE_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_PRODUCT_BOM_ERP_SYNC_ITEM_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_PRODUCT_BOM_ERP_SYNC_MULTI_VERSION;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_PRODUCT_BOM_ERP_SYNC_NOT_FOUND;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_PRODUCT_BOM_ERP_SYNC_RECURSIVE_ITEM;

@Service
@Validated
@RequiredArgsConstructor
public class MesKingdeeProductBomSyncServiceImpl implements MesKingdeeProductBomSyncService {

    private static final int ERP_BOM_QUANTITY_SCALE = 6;

    private static final String ITEM_CODE_MISSING_MESSAGE =
            "\u5f53\u524d\u7269\u6599/\u4ea7\u54c1\u7f16\u7801\u7f3a\u5931\uff0c\u65e0\u6cd5\u6267\u884c ERP \u540c\u6b65 BOM";
    private static final String BOM_NOT_FOUND_MESSAGE =
            "ERP \u4e2d\u672a\u627e\u5230\u7269\u6599/\u4ea7\u54c1\u7f16\u7801 {} \u7684\u5df2\u5ba1\u6838 BOM";
    private static final String MULTI_VERSION_MESSAGE =
            "ERP \u4e2d\u7269\u6599/\u4ea7\u54c1\u7f16\u7801 {} \u547d\u4e2d\u4e86\u591a\u4e2a\u5df2\u5ba1\u6838 BOM \u7248\u672c\uff1a{}";
    private static final String ITEM_MISSING_MESSAGE =
            "ERP BOM \u5b50\u9879\u7269\u6599\u672a\u6620\u5c04\u5230\u672c\u5730 MES \u7269\u6599\uff1a{}";
    private static final String PARENT_ITEM_MISSING_MESSAGE =
            "ERP BOM \u7236\u9879\u7269\u6599\u672a\u6620\u5c04\u5230\u672c\u5730 MES \u7269\u6599\uff1a{}";
    private static final String RECURSIVE_ITEM_MESSAGE =
            "ERP BOM \u5b50\u9879\u7269\u6599 {} \u5728\u672c\u5730\u4ecd\u914d\u7f6e\u4e86\u4e0b\u7ea7 BOM\uff0c\u7981\u6b62\u540c\u6b65";
    private static final String ERP_BOM_REMARK_PREFIX = "ERP BOM\u7248\u672c: ";

    private final ErpKingdeeBomClient bomClient;
    private final ErpKingdeeConfigService kingdeeConfigService;
    private final MesMdItemMapper itemMapper;
    private final MesMdProductBomMapper productBomMapper;
    private final MesProWorkOrderBomService workOrderBomService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesKingdeeProductBomSyncResult syncErpBom(Long itemId) {
        MesMdItemDO item = itemMapper.selectById(itemId);
        if (item == null) {
            throw exception(MD_ITEM_NOT_EXISTS);
        }
        if (StrUtil.isBlank(item.getCode())) {
            throw exception0(MD_PRODUCT_BOM_ERP_SYNC_ITEM_CODE_MISSING.getCode(), ITEM_CODE_MISSING_MESSAGE);
        }

        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        List<ErpKingdeeBomLine> bomLines = bomClient.fetchApprovedBomByParentMaterialNumber(
                kingdeeProperties, item.getCode());
        if (CollUtil.isEmpty(bomLines)) {
            throw exception0(MD_PRODUCT_BOM_ERP_SYNC_NOT_FOUND.getCode(), BOM_NOT_FOUND_MESSAGE, item.getCode());
        }

        Map<String, List<ErpKingdeeBomLine>> versionMap = groupByVersion(bomLines);
        if (versionMap.size() > 1) {
            throw exception0(MD_PRODUCT_BOM_ERP_SYNC_MULTI_VERSION.getCode(),
                    MULTI_VERSION_MESSAGE, item.getCode(), String.join(", ", versionMap.keySet()));
        }
        Map.Entry<String, List<ErpKingdeeBomLine>> versionEntry = versionMap.entrySet().iterator().next();

        List<MesMdProductBomDO> newBomRows = buildProductBomRows(itemId, versionEntry.getValue());
        productBomMapper.deleteByItemId(itemId);
        if (CollUtil.isNotEmpty(newBomRows)) {
            productBomMapper.insertBatch(newBomRows);
        }

        MesKingdeeProductBomSyncResult result = new MesKingdeeProductBomSyncResult();
        result.setItemId(itemId);
        result.setErpBomVersion(versionEntry.getKey());
        result.setSyncedBomCount(newBomRows.size());
        result.setSyncedParentCount(1);
        result.setRecalculatedWorkOrderCount(workOrderBomService.regenerateOpenWorkOrderBomByProductIds(List.of(itemId)));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesKingdeeProductBomSyncResult syncBomLinesModifiedBetween(LocalDateTime windowStart,
                                                                      LocalDateTime windowEnd) {
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        List<ErpKingdeeBomLine> changedBomLines =
                bomClient.fetchBomLinesModifiedBetween(kingdeeProperties, windowStart, windowEnd);
        MesKingdeeProductBomSyncResult result = new MesKingdeeProductBomSyncResult();
        if (CollUtil.isEmpty(changedBomLines)) {
            result.setSyncedParentCount(0);
            result.setSyncedBomCount(0);
            result.setRecalculatedWorkOrderCount(0);
            return result;
        }

        int syncedParentCount = 0;
        int syncedBomCount = 0;
        Set<Long> changedParentItemIds = new LinkedHashSet<>();
        for (Map.Entry<String, List<ErpKingdeeBomLine>> parentEntry : groupByParentMaterial(changedBomLines).entrySet()) {
            MesMdItemDO parentItem = itemMapper.selectByCode(parentEntry.getKey());
            if (parentItem == null) {
                throw exception0(MD_PRODUCT_BOM_ERP_SYNC_ITEM_MISSING.getCode(),
                        PARENT_ITEM_MISSING_MESSAGE, parentEntry.getKey());
            }
            Map.Entry<String, List<ErpKingdeeBomLine>> versionEntry =
                    resolveSingleVersion(parentItem.getCode(), parentEntry.getValue());
            List<MesMdProductBomDO> newBomRows = buildProductBomRows(parentItem.getId(), versionEntry.getValue());
            productBomMapper.deleteByItemId(parentItem.getId());
            if (CollUtil.isNotEmpty(newBomRows)) {
                productBomMapper.insertBatch(newBomRows);
            }
            syncedParentCount++;
            syncedBomCount += newBomRows.size();
            changedParentItemIds.add(parentItem.getId());
        }
        result.setSyncedParentCount(syncedParentCount);
        result.setSyncedBomCount(syncedBomCount);
        result.setRecalculatedWorkOrderCount(
                workOrderBomService.regenerateOpenWorkOrderBomByProductIds(changedParentItemIds));
        return result;
    }

    private Map<String, List<ErpKingdeeBomLine>> groupByVersion(List<ErpKingdeeBomLine> bomLines) {
        Map<String, List<ErpKingdeeBomLine>> versionMap = new LinkedHashMap<>();
        for (ErpKingdeeBomLine line : bomLines) {
            versionMap.computeIfAbsent(line.getBomVersion(), key -> new ArrayList<>()).add(line);
        }
        return versionMap;
    }

    private Map<String, List<ErpKingdeeBomLine>> groupByParentMaterial(List<ErpKingdeeBomLine> bomLines) {
        Map<String, List<ErpKingdeeBomLine>> parentMap = new LinkedHashMap<>();
        for (ErpKingdeeBomLine line : bomLines) {
            parentMap.computeIfAbsent(line.getParentMaterialNumber(), key -> new ArrayList<>()).add(line);
        }
        return parentMap;
    }

    private Map.Entry<String, List<ErpKingdeeBomLine>> resolveSingleVersion(String itemCode,
                                                                            List<ErpKingdeeBomLine> bomLines) {
        Map<String, List<ErpKingdeeBomLine>> versionMap = groupByVersion(bomLines);
        if (versionMap.size() > 1) {
            throw exception0(MD_PRODUCT_BOM_ERP_SYNC_MULTI_VERSION.getCode(),
                    MULTI_VERSION_MESSAGE, itemCode, String.join(", ", versionMap.keySet()));
        }
        return versionMap.entrySet().iterator().next();
    }

    private List<MesMdProductBomDO> buildProductBomRows(Long itemId, List<ErpKingdeeBomLine> bomLines) {
        Set<String> missingCodes = new LinkedHashSet<>();
        List<MesMdProductBomDO> result = new ArrayList<>(bomLines.size());
        for (ErpKingdeeBomLine bomLine : bomLines) {
            MesMdItemDO localItem = itemMapper.selectByCode(bomLine.getChildMaterialNumber());
            if (localItem == null) {
                missingCodes.add(bomLine.getChildMaterialNumber());
                continue;
            }
            if (CollUtil.isNotEmpty(productBomMapper.selectByItemId(localItem.getId()))) {
                throw exception0(MD_PRODUCT_BOM_ERP_SYNC_RECURSIVE_ITEM.getCode(),
                        RECURSIVE_ITEM_MESSAGE, localItem.getCode());
            }
            result.add(MesMdProductBomDO.builder()
                    .itemId(itemId)
                    .bomItemId(localItem.getId())
                    .quantity(calculateBomQuantity(bomLine))
                    .status(CommonStatusEnum.ENABLE.getStatus())
                    .remark(ERP_BOM_REMARK_PREFIX + bomLine.getBomVersion())
                    .build());
        }
        if (CollUtil.isNotEmpty(missingCodes)) {
            throw exception0(MD_PRODUCT_BOM_ERP_SYNC_ITEM_MISSING.getCode(),
                    ITEM_MISSING_MESSAGE, String.join(", ", missingCodes));
        }
        return result;
    }

    private BigDecimal calculateBomQuantity(ErpKingdeeBomLine bomLine) {
        BigDecimal quantity = bomLine.getNumerator()
                .divide(bomLine.getDenominator(), ERP_BOM_QUANTITY_SCALE, RoundingMode.HALF_UP);
        quantity = quantity.stripTrailingZeros();
        return quantity.scale() < 0 ? quantity.setScale(0) : quantity;
    }

}
