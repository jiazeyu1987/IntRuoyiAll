package cn.iocoder.yudao.module.erp.service.production.kingdee;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionReplenishmentListPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionReplenishmentListRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListMapper;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionReplenishmentList;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionReplenishmentListClient;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionReplenishmentListSyncResult;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Service
@Validated
public class ErpKingdeeProductionReplenishmentListServiceImpl
        implements ErpKingdeeProductionReplenishmentListService {

    @Resource
    private ErpKingdeeProductionReplenishmentListClient productionReplenishmentListClient;
    @Resource
    private ErpKingdeeConfigService kingdeeConfigService;
    @Resource
    private ErpKingdeeProductionReplenishmentListMapper productionReplenishmentListMapper;
    @Resource
    private ErpKingdeeProductionReplenishmentListItemMapper productionReplenishmentListItemMapper;

    @Override
    public PageResult<ErpProductionReplenishmentListRespVO> getPage(
            ErpProductionReplenishmentListPageReqVO pageReqVO) {
        List<Long> productionReplenishmentListIds = null;
        if (StrUtil.isNotBlank(pageReqVO.getProductionOrderNo())) {
            productionReplenishmentListIds = productionReplenishmentListItemMapper
                    .selectReplenishmentListIdsByProductionOrderNo(pageReqVO.getProductionOrderNo());
            if (CollUtil.isEmpty(productionReplenishmentListIds)) {
                return PageResult.empty();
            }
        }
        PageResult<ErpKingdeeProductionReplenishmentListDO> pageResult =
                productionReplenishmentListMapper.selectPageByProductionReplenishmentListIds(pageReqVO,
                        productionReplenishmentListIds);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpKingdeeProductionReplenishmentListItemDO> itemList =
                productionReplenishmentListItemMapper.selectListByReplenishmentListIds(
                        convertSet(pageResult.getList(), ErpKingdeeProductionReplenishmentListDO::getId));
        Map<Long, List<ErpKingdeeProductionReplenishmentListItemDO>> itemMap =
                convertMultiMap(itemList,
                        ErpKingdeeProductionReplenishmentListItemDO::getProductionReplenishmentListId);
        return BeanUtils.toBean(pageResult, ErpProductionReplenishmentListRespVO.class, replenishmentList -> {
            List<ErpProductionReplenishmentListRespVO.Item> items = BeanUtils.toBean(
                    itemMap.get(replenishmentList.getId()), ErpProductionReplenishmentListRespVO.Item.class);
            replenishmentList.setItems(items);
            replenishmentList.setMaterialNames(joinDistinct(items.stream()
                    .map(ErpProductionReplenishmentListRespVO.Item::getMaterialName)
                    .toList()));
            replenishmentList.setProductionOrderNos(joinDistinct(items.stream()
                    .map(ErpProductionReplenishmentListRespVO.Item::getProductionOrderNo)
                    .toList()));
        });
    }

    private String joinDistinct(List<String> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .collect(Collectors.joining("，"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeProductionReplenishmentListSyncResult syncAll(
            LocalDateTime windowStart, LocalDateTime windowEnd) {
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        properties.validateBaseConfig();
        return syncReplenishmentLists(productionReplenishmentListClient.fetchProductionReplenishmentLists(
                properties, windowStart, windowEnd), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeProductionReplenishmentListSyncResult syncAllSkipExisting(
            LocalDateTime windowStart, LocalDateTime windowEnd) {
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        properties.validateBaseConfig();
        return syncReplenishmentLists(productionReplenishmentListClient.fetchProductionReplenishmentLists(
                properties, windowStart, windowEnd), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeProductionReplenishmentListSyncResult syncModifiedBetween(
            LocalDateTime windowStart, LocalDateTime windowEnd) {
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        properties.validateBaseConfig();
        return syncReplenishmentLists(productionReplenishmentListClient
                .fetchProductionReplenishmentListsModifiedBetween(properties, windowStart, windowEnd), false);
    }

    private ErpKingdeeProductionReplenishmentListSyncResult syncReplenishmentLists(
            List<ErpKingdeeProductionReplenishmentList> replenishmentLists, boolean skipExisting) {
        LocalDateTime now = LocalDateTime.now();
        ErpKingdeeProductionReplenishmentListSyncResult result =
                new ErpKingdeeProductionReplenishmentListSyncResult();
        for (ErpKingdeeProductionReplenishmentList replenishmentList : replenishmentLists) {
            ErpKingdeeProductionReplenishmentListDO existing =
                    productionReplenishmentListMapper.selectBySource(
                            ErpKingdeeProductionReplenishmentList.FORM_ID, replenishmentList.getFid());
            ErpKingdeeProductionReplenishmentListDO record = buildRecord(replenishmentList, now);
            if (skipExisting && existing != null) {
                result.addSkipped(replenishmentList.getFid());
                continue;
            }
            if (existing == null) {
                productionReplenishmentListMapper.insert(record);
                result.addCreated();
            } else {
                record.setId(existing.getId());
                productionReplenishmentListMapper.updateById(record);
                result.addUpdated();
            }
            upsertItems(record.getId(), replenishmentList, now);
        }
        return result;
    }

    private ErpKingdeeProductionReplenishmentListDO buildRecord(
            ErpKingdeeProductionReplenishmentList replenishmentList, LocalDateTime now) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpKingdeeProductionReplenishmentListDO record = ErpKingdeeProductionReplenishmentListDO.builder()
                .sourceFormId(ErpKingdeeProductionReplenishmentList.FORM_ID)
                .sourceFid(replenishmentList.getFid())
                .sourceBillNo(replenishmentList.getBillNo())
                .billDate(replenishmentList.getBillDate())
                .documentStatus(replenishmentList.getDocumentStatus())
                .stockOrgNumber(replenishmentList.getStockOrgNumber())
                .stockOrgName(replenishmentList.getStockOrgName())
                .productionOrgNumber(replenishmentList.getProductionOrgNumber())
                .productionOrgName(replenishmentList.getProductionOrgName())
                .ownerNumber(replenishmentList.getOwnerNumber())
                .ownerName(replenishmentList.getOwnerName())
                .departmentNumber(replenishmentList.getDepartmentNumber())
                .departmentName(replenishmentList.getDepartmentName())
                .description(replenishmentList.getDescription())
                .sourceModifyTime(replenishmentList.getSourceModifyTime())
                .lastSyncTime(now)
                .rawPayload(JsonUtils.toJsonString(replenishmentList))
                .build();
        record.setTenantId(tenantId);
        return record;
    }

    private void upsertItems(Long replenishmentListId, ErpKingdeeProductionReplenishmentList replenishmentList,
                             LocalDateTime now) {
        productionReplenishmentListItemMapper.deleteByProductionReplenishmentListId(replenishmentListId);
        for (ErpKingdeeProductionReplenishmentList.Line line : replenishmentList.getLines()) {
            String sourceLineKey = replenishmentList.getFid() + "|" + line.getEntryId();
            ErpKingdeeProductionReplenishmentListItemDO record =
                    buildItemRecord(replenishmentListId, replenishmentList, line, sourceLineKey, now);
            productionReplenishmentListItemMapper.insert(record);
        }
    }

    private ErpKingdeeProductionReplenishmentListItemDO buildItemRecord(
            Long replenishmentListId, ErpKingdeeProductionReplenishmentList replenishmentList,
            ErpKingdeeProductionReplenishmentList.Line line, String sourceLineKey,
            LocalDateTime now) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpKingdeeProductionReplenishmentListItemDO record = ErpKingdeeProductionReplenishmentListItemDO.builder()
                .productionReplenishmentListId(replenishmentListId)
                .sourceFormId(ErpKingdeeProductionReplenishmentList.FORM_ID)
                .sourceFid(replenishmentList.getFid())
                .sourceEntryId(line.getEntryId())
                .sourceLineKey(sourceLineKey)
                .sourceBillNo(replenishmentList.getBillNo())
                .materialNumber(line.getMaterialNumber())
                .materialName(line.getMaterialName())
                .materialSpecification(line.getMaterialSpecification())
                .unitName(line.getUnitName())
                .requestedQuantity(line.getRequestedQuantity())
                .actualQuantity(line.getActualQuantity())
                .baseActualQuantity(line.getBaseActualQuantity())
                .warehouseNumber(line.getWarehouseNumber())
                .warehouseName(line.getWarehouseName())
                .stockLocationNumber(line.getStockLocationNumber())
                .stockLocationName(line.getStockLocationName())
                .lotNumber(line.getLotNumber())
                .productionOrderNo(line.getProductionOrderNo())
                .productionOrderLineNo(line.getProductionOrderLineNo())
                .productionMaterialListNo(line.getProductionMaterialListNo())
                .productionMaterialListLineNo(line.getProductionMaterialListLineNo())
                .workshopNumber(line.getWorkshopNumber())
                .workshopName(line.getWorkshopName())
                .stockStatusNumber(line.getStockStatusNumber())
                .stockStatusName(line.getStockStatusName())
                .sourceModifyTime(replenishmentList.getSourceModifyTime())
                .lastSyncTime(now)
                .rawPayload(JsonUtils.toJsonString(line))
                .build();
        record.setTenantId(tenantId);
        return record;
    }

}
