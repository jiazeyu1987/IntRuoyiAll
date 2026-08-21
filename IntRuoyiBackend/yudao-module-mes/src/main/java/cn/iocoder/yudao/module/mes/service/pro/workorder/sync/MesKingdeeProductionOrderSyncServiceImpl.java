package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrder;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncContext;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemTypeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDiffDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionOrderSyncRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemTypeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.unitmeasure.MesMdUnitMeasureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderDiffMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionOrderSyncRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.md.MesMdItemTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderDiffStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderSourceTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderTypeEnum;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID;

@Service
@Validated
@RequiredArgsConstructor
public class MesKingdeeProductionOrderSyncServiceImpl implements MesKingdeeProductionOrderSyncService {

    private static final String DEFAULT_ITEM_TYPE_CODE = "KINGDEE_PRODUCT";
    private static final String DEFAULT_ITEM_TYPE_NAME = "Kingdee Imported Product";
    private static final String KINGDEE_VOID_DOCUMENT_STATUS = "Z";
    private static final String KINGDEE_FINISHED_STATUS = "5";

    private final ErpKingdeeProductionOrderClient productionOrderClient;
    private final ErpKingdeeConfigService kingdeeConfigService;
    private final MesProWorkOrderService workOrderService;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesKingdeeProductionOrderSyncRecordMapper syncRecordMapper;
    private final MesProScheduleOrderMapper scheduleOrderMapper;
    private final MesProScheduleOrderDiffMapper scheduleOrderDiffMapper;
    private final MesMdItemMapper itemMapper;
    private final MesMdItemTypeMapper itemTypeMapper;
    private final MesMdUnitMeasureMapper unitMeasureMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesKingdeeProductionOrderSyncResult syncWorkOrders() {
        LocalDateTime windowEnd = LocalDateTime.now();
        return syncWorkOrders(ErpKingdeeSyncContext.builder()
                .initialSync(true)
                .windowStart(windowEnd.toLocalDate().minusYears(1).atStartOfDay())
                .windowEnd(windowEnd)
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesKingdeeProductionOrderSyncResult syncWorkOrders(ErpKingdeeSyncContext context) {
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validateBaseConfig();
        List<ErpKingdeeProductionOrder> productionOrders = fetchProductionOrders(kingdeeProperties, context);
        MesKingdeeProductionOrderSyncResult result = new MesKingdeeProductionOrderSyncResult();
        Set<String> processedSourceKeys = new LinkedHashSet<>();
        Set<String> processedWorkOrderCodes = new LinkedHashSet<>();
        for (ErpKingdeeProductionOrder productionOrder : productionOrders) {
            if (isKingdeeVoided(productionOrder)) {
                continue;
            }
            String sourceKey = buildSourceKey(productionOrder);
            if (!processedSourceKeys.add(sourceKey)) {
                result.addSkipped(sourceKey);
                continue;
            }
            String workOrderCode = buildWorkOrderCode(productionOrder);
            if (!processedWorkOrderCodes.add(workOrderCode)) {
                result.addSkipped(sourceKey);
                continue;
            }
            MesKingdeeProductionOrderSyncRecordDO syncRecord =
                    syncRecordMapper.selectBySourceKey(productionOrder.getFid(), productionOrder.getMaterialNumber());
            MesProWorkOrderDO existingWorkOrder = resolveExistingWorkOrder(syncRecord, sourceKey, workOrderCode);
            Long productId = ensureMesItem(productionOrder);
            if (existingWorkOrder == null) {
                Long workOrderId = workOrderService.createWorkOrder(buildCreateReqVO(productionOrder, productId));
                workOrderMapper.updateById(buildErpSnapshotUpdate(workOrderId, productionOrder));
                updateWorkOrderStatus(workOrderId);
                saveOrUpdateSyncRecord(syncRecord, productionOrder, workOrderId);
                result.addCreated(workOrderId);
                finishWorkOrderIfKingdeeFinished(productionOrder, new MesProWorkOrderDO()
                        .setId(workOrderId)
                        .setStatus(MesProWorkOrderStatusEnum.CONFIRMED.getStatus()), result);
                continue;
            }
            syncExistingWorkOrder(existingWorkOrder, productionOrder, productId);
            saveOrUpdateSyncRecord(syncRecord, productionOrder, existingWorkOrder.getId());
            result.addUpdated(existingWorkOrder.getId());
            finishWorkOrderIfKingdeeFinished(productionOrder, existingWorkOrder, result);
        }
        syncInactiveWorkOrders(kingdeeProperties, result, processedWorkOrderCodes);
        return result;
    }

    private List<ErpKingdeeProductionOrder> fetchProductionOrders(ErpKingdeeProperties kingdeeProperties,
                                                                  ErpKingdeeSyncContext context) {
        if (context.isInitialSync()) {
            return productionOrderClient.fetchProductionOrdersByBillDateRange(kingdeeProperties,
                    context.getWindowStart().toLocalDate(), context.getWindowEnd().toLocalDate());
        }
        return productionOrderClient.fetchProductionOrdersModifiedBetween(kingdeeProperties,
                context.getWindowStart(), context.getWindowEnd());
    }

    private void syncExistingWorkOrder(MesProWorkOrderDO existingWorkOrder,
                                       ErpKingdeeProductionOrder productionOrder,
                                       Long productId) {
        MesProWorkOrderDO updated = buildUpdatedWorkOrder(existingWorkOrder, productionOrder, productId);
        if (!hasWorkOrderChanged(existingWorkOrder, updated)) {
            return;
        }
        workOrderMapper.updateById(updated);
        createScheduleOrderDiffIfNeeded(existingWorkOrder, updated);
    }

    private MesProWorkOrderDO resolveExistingWorkOrder(MesKingdeeProductionOrderSyncRecordDO syncRecord,
                                                       String sourceKey,
                                                       String workOrderCode) {
        MesProWorkOrderDO sourceLinkedWorkOrder = resolveSourceLinkedWorkOrder(syncRecord, sourceKey);
        MesProWorkOrderDO workOrderByCode = workOrderService.getWorkOrder(workOrderCode);
        if (sourceLinkedWorkOrder != null && workOrderByCode != null
                && !Objects.equals(sourceLinkedWorkOrder.getId(), workOrderByCode.getId())) {
            throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "production order source key conflicts with existing work order code: "
                            + sourceKey + " -> " + workOrderCode);
        }
        return sourceLinkedWorkOrder != null ? sourceLinkedWorkOrder : workOrderByCode;
    }

    private MesProWorkOrderDO resolveSourceLinkedWorkOrder(MesKingdeeProductionOrderSyncRecordDO syncRecord,
                                                           String sourceKey) {
        if (syncRecord == null) {
            return null;
        }
        if (syncRecord.getWorkOrderId() == null) {
            throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "production order sync record workOrderId is blank: " + syncRecord.getId()
                            + ", sourceKey=" + sourceKey);
        }
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(syncRecord.getWorkOrderId());
        if (workOrder == null) {
            throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "production order sync record points to missing work order: " + syncRecord.getId()
                            + ", sourceKey=" + sourceKey);
        }
        return workOrder;
    }

    private MesProWorkOrderSaveReqVO buildCreateReqVO(ErpKingdeeProductionOrder productionOrder, Long productId) {
        MesProWorkOrderSaveReqVO reqVO = new MesProWorkOrderSaveReqVO();
        String workOrderCode = buildWorkOrderCode(productionOrder);
        reqVO.setCode(workOrderCode);
        reqVO.setName(productionOrder.getMaterialName());
        reqVO.setType(MesProWorkOrderTypeEnum.SELF.getType());
        reqVO.setOrderSourceType(resolveSourceType(productionOrder));
        reqVO.setOrderSourceCode(resolveOrderSourceCode(productionOrder, workOrderCode));
        reqVO.setProductId(productId);
        reqVO.setQuantity(productionOrder.getQuantity());
        reqVO.setQuantityProduced(BigDecimal.ZERO);
        reqVO.setQuantityChanged(BigDecimal.ZERO);
        reqVO.setQuantityScheduled(BigDecimal.ZERO);
        reqVO.setBatchCode(StrUtil.trimToNull(productionOrder.getBatchNumber()));
        reqVO.setRequestDate(resolveRequestDate(productionOrder));
        reqVO.setRemark("Kingdee K3Cloud production order: " + productionOrder.getBillNo());
        return reqVO;
    }

    private Integer resolveSourceType(ErpKingdeeProductionOrder productionOrder) {
        return StrUtil.isNotBlank(productionOrder.getSourceBillNo())
                ? MesProWorkOrderSourceTypeEnum.ORDER.getType()
                : MesProWorkOrderSourceTypeEnum.STORE.getType();
    }

    private String resolveOrderSourceCode(ErpKingdeeProductionOrder productionOrder, String workOrderCode) {
        String sourceBillNo = StrUtil.trimToNull(productionOrder.getSourceBillNo());
        if (StrUtil.equals(sourceBillNo, workOrderCode)) {
            return null;
        }
        return sourceBillNo;
    }

    private LocalDateTime resolveRequestDate(ErpKingdeeProductionOrder productionOrder) {
        if (productionOrder.getPlannedEndDate() != null) {
            return productionOrder.getPlannedEndDate();
        }
        if (productionOrder.getPlannedStartDate() != null) {
            return productionOrder.getPlannedStartDate();
        }
        if (productionOrder.getBillDate() != null) {
            return productionOrder.getBillDate();
        }
        throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                "production order request date is blank for bill " + productionOrder.getBillNo());
    }

    private Long ensureMesItem(ErpKingdeeProductionOrder productionOrder) {
        MesMdItemDO item = itemMapper.selectByCode(productionOrder.getMaterialNumber());
        if (item != null) {
            return item.getId();
        }
        item = new MesMdItemDO();
        item.setCode(productionOrder.getMaterialNumber());
        item.setName(productionOrder.getMaterialName());
        item.setSpecification(productionOrder.getMaterialSpecification());
        item.setUnitMeasureId(ensureUnitMeasure(productionOrder));
        item.setItemTypeId(ensureItemType());
        item.setStatus(CommonStatusEnum.ENABLE.getStatus());
        item.setSafeStockFlag(Boolean.FALSE);
        item.setMinStock(BigDecimal.ZERO);
        item.setMaxStock(BigDecimal.ZERO);
        item.setHighValue(Boolean.FALSE);
        item.setBatchFlag(Boolean.FALSE);
        item.setRemark("Kingdee imported item");
        itemMapper.insert(item);
        return item.getId();
    }

    private Long ensureItemType() {
        MesMdItemTypeDO itemType = itemTypeMapper.selectByParentIdAndCode(MesMdItemTypeDO.PARENT_ID_ROOT, DEFAULT_ITEM_TYPE_CODE);
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

    private Long ensureUnitMeasure(ErpKingdeeProductionOrder productionOrder) {
        String unitCode = StrUtil.blankToDefault(productionOrder.getUnitCode(), productionOrder.getUnitName());
        String unitName = StrUtil.blankToDefault(productionOrder.getUnitName(), unitCode);
        if (StrUtil.isBlank(unitCode) || StrUtil.isBlank(unitName)) {
            throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "production order unit is blank for material " + productionOrder.getMaterialNumber());
        }
        MesMdUnitMeasureDO unitMeasure = unitMeasureMapper.selectByCode(unitCode);
        if (unitMeasure == null) {
            unitMeasure = unitMeasureMapper.selectByName(unitName);
        }
        if (unitMeasure != null) {
            return unitMeasure.getId();
        }
        unitMeasure = new MesMdUnitMeasureDO();
        unitMeasure.setCode(unitCode);
        unitMeasure.setName(unitName);
        unitMeasure.setPrimaryFlag(Boolean.TRUE);
        unitMeasure.setChangeRate(BigDecimal.ONE);
        unitMeasure.setStatus(CommonStatusEnum.ENABLE.getStatus());
        unitMeasure.setRemark("Kingdee imported unit");
        unitMeasureMapper.insert(unitMeasure);
        return unitMeasure.getId();
    }

    private void updateWorkOrderStatus(Long workOrderId) {
        MesProWorkOrderDO update = new MesProWorkOrderDO()
                .setId(workOrderId)
                .setStatus(MesProWorkOrderStatusEnum.CONFIRMED.getStatus());
        workOrderMapper.updateById(update);
    }

    private String buildWorkOrderCode(ErpKingdeeProductionOrder productionOrder) {
        if (StrUtil.isBlank(productionOrder.getBillNo())) {
            throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "production order billNo is blank for fid " + productionOrder.getFid());
        }
        return productionOrder.getBillNo();
    }

    private String buildSourceKey(ErpKingdeeProductionOrder productionOrder) {
        if (StrUtil.isBlank(productionOrder.getFid())) {
            throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "production order fid is blank for bill " + productionOrder.getBillNo());
        }
        if (StrUtil.isBlank(productionOrder.getMaterialNumber())) {
            throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "production order materialNumber is blank for fid " + productionOrder.getFid());
        }
        return productionOrder.getFid() + ":" + productionOrder.getMaterialNumber();
    }

    private MesProWorkOrderDO buildUpdatedWorkOrder(MesProWorkOrderDO existingWorkOrder,
                                                    ErpKingdeeProductionOrder productionOrder,
                                                    Long productId) {
        String workOrderCode = buildWorkOrderCode(productionOrder);
        return new MesProWorkOrderDO()
                .setId(existingWorkOrder.getId())
                .setCode(workOrderCode)
                .setName(productionOrder.getMaterialName())
                .setType(MesProWorkOrderTypeEnum.SELF.getType())
                .setOrderSourceType(resolveSourceType(productionOrder))
                .setOrderSourceCode(resolveOrderSourceCode(productionOrder, workOrderCode))
                .setProductId(productId)
                .setQuantity(productionOrder.getQuantity())
                .setBatchCode(resolveBatchCode(existingWorkOrder, productionOrder))
                .setWorkshopName(StrUtil.trimToNull(productionOrder.getWorkshopName()))
                .setBomVersion(StrUtil.trimToNull(productionOrder.getBomVersion()))
                .setPickMode(StrUtil.trimToNull(productionOrder.getPickMode()))
                .setAuxiliaryCode(StrUtil.trimToNull(productionOrder.getAuxiliaryCode()))
                .setBusinessStatus(StrUtil.trimToNull(productionOrder.getBusinessStatus()))
                .setDrawingNumber(StrUtil.trimToNull(productionOrder.getDrawingNumber()))
                .setScheduleStatus(StrUtil.trimToNull(productionOrder.getScheduleStatus()))
                .setPlannedStartTime(productionOrder.getPlannedStartDate())
                .setPlannedEndTime(productionOrder.getPlannedEndDate())
                .setRequestDate(resolveRequestDate(productionOrder))
                .setRemark("Kingdee K3Cloud production order: " + productionOrder.getBillNo());
    }

    private String resolveBatchCode(MesProWorkOrderDO existingWorkOrder, ErpKingdeeProductionOrder productionOrder) {
        String erpBatchCode = StrUtil.trimToNull(productionOrder.getBatchNumber());
        return erpBatchCode != null ? erpBatchCode : StrUtil.trimToNull(existingWorkOrder.getBatchCode());
    }

    private MesProWorkOrderDO buildErpSnapshotUpdate(Long workOrderId,
                                                     ErpKingdeeProductionOrder productionOrder) {
        return new MesProWorkOrderDO()
                .setId(workOrderId)
                .setWorkshopName(StrUtil.trimToNull(productionOrder.getWorkshopName()))
                .setBomVersion(StrUtil.trimToNull(productionOrder.getBomVersion()))
                .setPickMode(StrUtil.trimToNull(productionOrder.getPickMode()))
                .setAuxiliaryCode(StrUtil.trimToNull(productionOrder.getAuxiliaryCode()))
                .setBusinessStatus(StrUtil.trimToNull(productionOrder.getBusinessStatus()))
                .setDrawingNumber(StrUtil.trimToNull(productionOrder.getDrawingNumber()))
                .setScheduleStatus(StrUtil.trimToNull(productionOrder.getScheduleStatus()))
                .setPlannedStartTime(productionOrder.getPlannedStartDate())
                .setPlannedEndTime(productionOrder.getPlannedEndDate());
    }

    private boolean hasWorkOrderChanged(MesProWorkOrderDO existingWorkOrder, MesProWorkOrderDO updatedWorkOrder) {
        return !Objects.equals(existingWorkOrder.getName(), updatedWorkOrder.getName())
                || !Objects.equals(existingWorkOrder.getOrderSourceType(), updatedWorkOrder.getOrderSourceType())
                || !Objects.equals(existingWorkOrder.getOrderSourceCode(), updatedWorkOrder.getOrderSourceCode())
                || !Objects.equals(existingWorkOrder.getProductId(), updatedWorkOrder.getProductId())
                || compareDecimal(existingWorkOrder.getQuantity(), updatedWorkOrder.getQuantity()) != 0
                || !Objects.equals(existingWorkOrder.getBatchCode(), updatedWorkOrder.getBatchCode())
                || !Objects.equals(existingWorkOrder.getWorkshopName(), updatedWorkOrder.getWorkshopName())
                || !Objects.equals(existingWorkOrder.getBomVersion(), updatedWorkOrder.getBomVersion())
                || !Objects.equals(existingWorkOrder.getPickMode(), updatedWorkOrder.getPickMode())
                || !Objects.equals(existingWorkOrder.getAuxiliaryCode(), updatedWorkOrder.getAuxiliaryCode())
                || !Objects.equals(existingWorkOrder.getBusinessStatus(), updatedWorkOrder.getBusinessStatus())
                || !Objects.equals(existingWorkOrder.getDrawingNumber(), updatedWorkOrder.getDrawingNumber())
                || !Objects.equals(existingWorkOrder.getScheduleStatus(), updatedWorkOrder.getScheduleStatus())
                || !Objects.equals(existingWorkOrder.getPlannedStartTime(), updatedWorkOrder.getPlannedStartTime())
                || !Objects.equals(existingWorkOrder.getPlannedEndTime(), updatedWorkOrder.getPlannedEndTime())
                || !Objects.equals(existingWorkOrder.getRequestDate(), updatedWorkOrder.getRequestDate())
                || !Objects.equals(existingWorkOrder.getRemark(), updatedWorkOrder.getRemark());
    }

    private void createScheduleOrderDiffIfNeeded(MesProWorkOrderDO existingWorkOrder, MesProWorkOrderDO updatedWorkOrder) {
        MesProScheduleOrderDO scheduleOrder = scheduleOrderMapper.selectEffectiveByWorkOrderId(existingWorkOrder.getId());
        if (scheduleOrder == null) {
            return;
        }
        Map<String, Object> oldValue = buildDiffPayload(existingWorkOrder);
        Map<String, Object> newValue = buildDiffPayload(updatedWorkOrder);
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        scheduleOrderDiffMapper.insert(MesProScheduleOrderDiffDO.builder()
                .scheduleOrderId(scheduleOrder.getId())
                .workOrderId(existingWorkOrder.getId())
                .diffType("ERP_WORK_ORDER_SYNC")
                .oldValueJson(JsonUtils.toJsonString(oldValue))
                .newValueJson(JsonUtils.toJsonString(newValue))
                .status(MesProScheduleOrderDiffStatusEnum.PENDING.getStatus())
                .remark("Kingdee nightly sync detected work order snapshot drift")
                .build());
        scheduleOrderMapper.updateById(new MesProScheduleOrderDO()
                .setId(scheduleOrder.getId())
                .setDiffStatus(MesProScheduleOrderDiffStatusEnum.PENDING.getStatus()));
    }

    private Map<String, Object> buildDiffPayload(MesProWorkOrderDO workOrder) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", workOrder.getName());
        payload.put("orderSourceType", workOrder.getOrderSourceType());
        payload.put("orderSourceCode", workOrder.getOrderSourceCode());
        payload.put("productId", workOrder.getProductId());
        payload.put("quantity", workOrder.getQuantity());
        payload.put("batchCode", workOrder.getBatchCode());
        payload.put("workshopName", workOrder.getWorkshopName());
        payload.put("bomVersion", workOrder.getBomVersion());
        payload.put("pickMode", workOrder.getPickMode());
        payload.put("auxiliaryCode", workOrder.getAuxiliaryCode());
        payload.put("businessStatus", workOrder.getBusinessStatus());
        payload.put("drawingNumber", workOrder.getDrawingNumber());
        payload.put("scheduleStatus", workOrder.getScheduleStatus());
        payload.put("plannedStartTime", workOrder.getPlannedStartTime());
        payload.put("plannedEndTime", workOrder.getPlannedEndTime());
        payload.put("requestDate", workOrder.getRequestDate());
        payload.put("remark", workOrder.getRemark());
        return payload;
    }

    private void saveOrUpdateSyncRecord(MesKingdeeProductionOrderSyncRecordDO syncRecord,
                                        ErpKingdeeProductionOrder productionOrder,
                                        Long workOrderId) {
        if (syncRecord == null) {
            syncRecordMapper.insert(new MesKingdeeProductionOrderSyncRecordDO()
                    .setSourceFid(productionOrder.getFid())
                    .setSourceBillNo(productionOrder.getBillNo())
                    .setSourceMaterialNumber(productionOrder.getMaterialNumber())
                    .setWorkOrderId(workOrderId));
            return;
        }
        syncRecordMapper.updateById(new MesKingdeeProductionOrderSyncRecordDO()
                .setId(syncRecord.getId())
                .setSourceBillNo(productionOrder.getBillNo())
                .setWorkOrderId(workOrderId));
    }

    private void syncInactiveWorkOrders(ErpKingdeeProperties kingdeeProperties,
                                        MesKingdeeProductionOrderSyncResult result,
                                        Set<String> activeWorkOrderCodes) {
        List<MesKingdeeProductionOrderSyncRecordDO> syncRecords = syncRecordMapper.selectList();
        if (syncRecords == null || syncRecords.isEmpty()) {
            return;
        }
        Map<String, List<MesKingdeeProductionOrderSyncRecordDO>> recordsByBillNo = new LinkedHashMap<>();
        List<String> billNosToVerify = new ArrayList<>();
        for (MesKingdeeProductionOrderSyncRecordDO syncRecord : syncRecords) {
            Long workOrderId = syncRecord.getWorkOrderId();
            if (workOrderId == null) {
                throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                        "production order sync record workOrderId is blank: " + syncRecord.getId());
            }
            String sourceBillNo = StrUtil.trimToNull(syncRecord.getSourceBillNo());
            if (sourceBillNo == null) {
                throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                        "production order sync record sourceBillNo is blank: " + syncRecord.getId());
            }
            if (activeWorkOrderCodes.contains(sourceBillNo)) {
                continue;
            }
            if (!recordsByBillNo.containsKey(sourceBillNo)) {
                recordsByBillNo.put(sourceBillNo, new ArrayList<>());
                billNosToVerify.add(sourceBillNo);
            }
            recordsByBillNo.get(sourceBillNo).add(syncRecord);
        }
        if (billNosToVerify.isEmpty()) {
            return;
        }
        List<ErpKingdeeProductionOrder> statusOrders =
                productionOrderClient.fetchProductionOrdersByBillNos(kingdeeProperties, billNosToVerify);
        Map<String, ErpKingdeeProductionOrder> statusOrderByBillNo = new LinkedHashMap<>();
        for (ErpKingdeeProductionOrder statusOrder : statusOrders) {
            statusOrderByBillNo.putIfAbsent(statusOrder.getBillNo(), statusOrder);
        }
        Set<Long> changedWorkOrderIds = new LinkedHashSet<>();
        for (String billNo : billNosToVerify) {
            ErpKingdeeProductionOrder statusOrder = statusOrderByBillNo.get(billNo);
            for (MesKingdeeProductionOrderSyncRecordDO syncRecord : recordsByBillNo.get(billNo)) {
                if (!changedWorkOrderIds.add(syncRecord.getWorkOrderId())) {
                    continue;
                }
                syncInactiveWorkOrder(syncRecord, statusOrder, result);
            }
        }
    }

    private void syncInactiveWorkOrder(MesKingdeeProductionOrderSyncRecordDO syncRecord,
                                       ErpKingdeeProductionOrder statusOrder,
                                       MesKingdeeProductionOrderSyncResult result) {
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(syncRecord.getWorkOrderId());
        if (workOrder == null) {
            throw ServiceExceptionUtil.exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "production order sync record points to missing work order: " + syncRecord.getId());
        }
        if (isKingdeeVoided(statusOrder)) {
            cancelSyncedWorkOrder(workOrder, result);
            return;
        }
        if (isKingdeeFinished(statusOrder)) {
            finishSyncedWorkOrder(workOrder, result);
        }
    }

    private void finishWorkOrderIfKingdeeFinished(ErpKingdeeProductionOrder productionOrder,
                                                  MesProWorkOrderDO workOrder,
                                                  MesKingdeeProductionOrderSyncResult result) {
        if (isKingdeeFinished(productionOrder)) {
            finishSyncedWorkOrder(workOrder, result);
        }
    }

    private boolean isKingdeeVoided(ErpKingdeeProductionOrder statusOrder) {
        return statusOrder == null
                || KINGDEE_VOID_DOCUMENT_STATUS.equals(statusOrder.getDocumentStatus());
    }

    private boolean isKingdeeFinished(ErpKingdeeProductionOrder statusOrder) {
        return KINGDEE_FINISHED_STATUS.equals(statusOrder.getStatus());
    }

    private void cancelSyncedWorkOrder(MesProWorkOrderDO workOrder,
                                       MesKingdeeProductionOrderSyncResult result) {
        if (Objects.equals(workOrder.getStatus(), MesProWorkOrderStatusEnum.CANCELED.getStatus())
                || Objects.equals(workOrder.getStatus(), MesProWorkOrderStatusEnum.FINISHED.getStatus())) {
            return;
        }
        if (Objects.equals(workOrder.getStatus(), MesProWorkOrderStatusEnum.CONFIRMED.getStatus())) {
            workOrderService.cancelWorkOrder(workOrder.getId());
        } else {
            workOrderMapper.updateById(new MesProWorkOrderDO()
                    .setId(workOrder.getId())
                    .setStatus(MesProWorkOrderStatusEnum.CANCELED.getStatus())
                    .setCancelDate(LocalDateTime.now()));
        }
        result.addCanceled(workOrder.getId());
    }

    private void finishSyncedWorkOrder(MesProWorkOrderDO workOrder,
                                       MesKingdeeProductionOrderSyncResult result) {
        if (Objects.equals(workOrder.getStatus(), MesProWorkOrderStatusEnum.FINISHED.getStatus())
                || Objects.equals(workOrder.getStatus(), MesProWorkOrderStatusEnum.CANCELED.getStatus())) {
            return;
        }
        if (Objects.equals(workOrder.getStatus(), MesProWorkOrderStatusEnum.CONFIRMED.getStatus())) {
            workOrderService.finishWorkOrder(workOrder.getId());
        } else {
            workOrderMapper.updateById(new MesProWorkOrderDO()
                    .setId(workOrder.getId())
                    .setStatus(MesProWorkOrderStatusEnum.FINISHED.getStatus())
                    .setFinishDate(LocalDateTime.now()));
        }
        result.addFinished(workOrder.getId());
    }

    private int compareDecimal(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

}
