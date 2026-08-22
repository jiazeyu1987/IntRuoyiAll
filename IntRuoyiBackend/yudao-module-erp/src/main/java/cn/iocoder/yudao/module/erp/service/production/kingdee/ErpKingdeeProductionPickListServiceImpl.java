package cn.iocoder.yudao.module.erp.service.production.kingdee;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionPickListPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionPickListRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionPickList;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionPickListClient;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionPickListSyncResult;
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
public class ErpKingdeeProductionPickListServiceImpl
        implements ErpKingdeeProductionPickListService {

    @Resource
    private ErpKingdeeProductionPickListClient productionPickListClient;
    @Resource
    private ErpKingdeeConfigService kingdeeConfigService;
    @Resource
    private ErpKingdeeProductionPickListMapper productionPickListMapper;
    @Resource
    private ErpKingdeeProductionPickListItemMapper productionPickListItemMapper;

    @Override
    public PageResult<ErpProductionPickListRespVO> getPage(
            ErpProductionPickListPageReqVO pageReqVO) {
        PageResult<ErpKingdeeProductionPickListDO> pageResult =
                productionPickListMapper.selectPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpKingdeeProductionPickListItemDO> itemList =
                productionPickListItemMapper.selectListByPickListIds(
                        convertSet(pageResult.getList(), ErpKingdeeProductionPickListDO::getId));
        Map<Long, List<ErpKingdeeProductionPickListItemDO>> itemMap =
                convertMultiMap(itemList,
                        ErpKingdeeProductionPickListItemDO::getProductionPickListId);
        return BeanUtils.toBean(pageResult, ErpProductionPickListRespVO.class, pickList -> {
            List<ErpProductionPickListRespVO.Item> items = BeanUtils.toBean(
                    itemMap.get(pickList.getId()), ErpProductionPickListRespVO.Item.class);
            pickList.setItems(items);
            pickList.setMaterialNames(joinDistinct(items.stream()
                    .map(ErpProductionPickListRespVO.Item::getMaterialName)
                    .toList()));
            pickList.setProductionOrderNos(joinDistinct(items.stream()
                    .map(ErpProductionPickListRespVO.Item::getProductionOrderNo)
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
    public ErpKingdeeProductionPickListSyncResult syncAll() {
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        properties.validateBaseConfig();
        return syncPickLists(productionPickListClient.fetchProductionPickLists(properties), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeProductionPickListSyncResult syncAllSkipExisting() {
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        properties.validateBaseConfig();
        return syncPickLists(productionPickListClient.fetchProductionPickLists(properties), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeProductionPickListSyncResult syncModifiedBetween(
            LocalDateTime windowStart, LocalDateTime windowEnd) {
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        properties.validateBaseConfig();
        return syncPickLists(productionPickListClient
                .fetchProductionPickListsModifiedBetween(properties, windowStart, windowEnd), false);
    }

    private ErpKingdeeProductionPickListSyncResult syncPickLists(
            List<ErpKingdeeProductionPickList> pickLists, boolean skipExisting) {
        LocalDateTime now = LocalDateTime.now();
        ErpKingdeeProductionPickListSyncResult result =
                new ErpKingdeeProductionPickListSyncResult();
        for (ErpKingdeeProductionPickList pickList : pickLists) {
            ErpKingdeeProductionPickListDO existing =
                    productionPickListMapper.selectBySource(
                            ErpKingdeeProductionPickList.FORM_ID, pickList.getFid());
            ErpKingdeeProductionPickListDO record = buildRecord(pickList, now);
            if (skipExisting && existing != null) {
                result.addSkipped(pickList.getFid());
                continue;
            }
            if (existing == null) {
                productionPickListMapper.insert(record);
                result.addCreated();
            } else {
                record.setId(existing.getId());
                productionPickListMapper.updateById(record);
                result.addUpdated();
            }
            upsertItems(record.getId(), pickList, now);
        }
        return result;
    }

    private ErpKingdeeProductionPickListDO buildRecord(
            ErpKingdeeProductionPickList pickList, LocalDateTime now) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpKingdeeProductionPickListDO record = ErpKingdeeProductionPickListDO.builder()
                .sourceFormId(ErpKingdeeProductionPickList.FORM_ID)
                .sourceFid(pickList.getFid())
                .sourceBillNo(pickList.getBillNo())
                .billDate(pickList.getBillDate())
                .documentStatus(pickList.getDocumentStatus())
                .stockOrgNumber(pickList.getStockOrgNumber())
                .stockOrgName(pickList.getStockOrgName())
                .productionOrgNumber(pickList.getProductionOrgNumber())
                .productionOrgName(pickList.getProductionOrgName())
                .ownerNumber(pickList.getOwnerNumber())
                .ownerName(pickList.getOwnerName())
                .departmentNumber(pickList.getDepartmentNumber())
                .departmentName(pickList.getDepartmentName())
                .description(pickList.getDescription())
                .sourceModifyTime(pickList.getSourceModifyTime())
                .lastSyncTime(now)
                .rawPayload(JsonUtils.toJsonString(pickList))
                .build();
        record.setTenantId(tenantId);
        return record;
    }

    private void upsertItems(Long pickListId, ErpKingdeeProductionPickList pickList,
                             LocalDateTime now) {
        productionPickListItemMapper.deleteByProductionPickListId(pickListId);
        for (ErpKingdeeProductionPickList.Line line : pickList.getLines()) {
            String sourceLineKey = pickList.getFid() + "|" + line.getEntryId();
            ErpKingdeeProductionPickListItemDO record =
                    buildItemRecord(pickListId, pickList, line, sourceLineKey, now);
            productionPickListItemMapper.insert(record);
        }
    }

    private ErpKingdeeProductionPickListItemDO buildItemRecord(
            Long pickListId, ErpKingdeeProductionPickList pickList,
            ErpKingdeeProductionPickList.Line line, String sourceLineKey,
            LocalDateTime now) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpKingdeeProductionPickListItemDO record = ErpKingdeeProductionPickListItemDO.builder()
                .productionPickListId(pickListId)
                .sourceFormId(ErpKingdeeProductionPickList.FORM_ID)
                .sourceFid(pickList.getFid())
                .sourceEntryId(line.getEntryId())
                .sourceLineKey(sourceLineKey)
                .sourceBillNo(pickList.getBillNo())
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
                .sourceModifyTime(pickList.getSourceModifyTime())
                .lastSyncTime(now)
                .rawPayload(JsonUtils.toJsonString(line))
                .build();
        record.setTenantId(tenantId);
        return record;
    }

}
