package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncWatermarkRespVO;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTriggerTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.sync.admin.ErpKingdeeSyncAdminService;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncCommand;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRunResult;
import cn.iocoder.yudao.module.erp.service.sync.runtime.ErpKingdeeSyncRuntimeService;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobPageReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.service.job.JobService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.client.MesMdClientDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.vendor.MesMdVendorDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import cn.iocoder.yudao.module.mes.service.md.client.MesMdClientService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.iocoder.yudao.module.mes.service.md.vendor.MesMdVendorService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.sync.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_MANUAL_OPERATION_FORBIDDEN;

@Tag(name = "Admin - MES Work Order")
@RestController
@RequestMapping("/mes/pro/work-order")
@Validated
public class MesProWorkOrderController {
    @Resource private MesProWorkOrderService workOrderService;
    @Resource private MesMdItemService itemService;
    @Resource private MesMdClientService clientService;
    @Resource private MesMdVendorService vendorService;
    @Resource private MesMdUnitMeasureService unitMeasureService;
    @Resource private MesKingdeeProductionOrderSyncService kingdeeProductionOrderSyncService;
    @Resource private ErpKingdeeSyncRuntimeService kingdeeSyncRuntimeService;
    @Resource private ErpKingdeeSyncAdminService kingdeeSyncAdminService;
    @Resource private JobService jobService;
    @Resource private MesKingdeeProductionOrderCreateService kingdeeProductionOrderCreateService;
    @Resource private MesKingdeeWorkOrderBomSyncService kingdeeWorkOrderBomSyncService;
    @Resource private MesKingdeeProductionMaterialListMapper productionMaterialListMapper;

    @PostMapping("/create") @Operation(summary = "Create work order") @PreAuthorize("@ss.hasPermission('mes:pro-work-order:create')")
    public CommonResult<Long> createWorkOrder(@Valid @RequestBody MesProWorkOrderSaveReqVO createReqVO) { throw manualOperationForbidden(); }
    @PutMapping("/update") @Operation(summary = "Create work order") @PreAuthorize("@ss.hasPermission('mes:pro-work-order:update')")
    public CommonResult<Boolean> updateWorkOrder(@Valid @RequestBody MesProWorkOrderSaveReqVO updateReqVO) { throw manualOperationForbidden(); }
    @DeleteMapping("/delete") @Operation(summary = "Create work order") @Parameter(name = "id", description = "ID", required = true) @PreAuthorize("@ss.hasPermission('mes:pro-work-order:delete')")
    public CommonResult<Boolean> deleteWorkOrder(@RequestParam("id") Long id) { throw manualOperationForbidden(); }
    @GetMapping("/get") @Operation(summary = "Create work order") @Parameter(name = "id", description = "ID", required = true, example = "1024") @PreAuthorize("@ss.hasPermission('mes:pro-work-order:query')")
    public CommonResult<MesProWorkOrderRespVO> getWorkOrder(@RequestParam("id") Long id) { MesProWorkOrderDO workOrder = workOrderService.getWorkOrder(id); return success(workOrder == null ? null : buildWorkOrderRespVOList(ListUtil.of(workOrder)).get(0)); }
    @GetMapping("/page") @Operation(summary = "Get work order page") @PreAuthorize("@ss.hasPermission('mes:pro-work-order:query')")
    public CommonResult<PageResult<MesProWorkOrderRespVO>> getWorkOrderPage(@Valid MesProWorkOrderPageReqVO pageReqVO) { PageResult<MesProWorkOrderDO> pageResult = workOrderService.getWorkOrderPage(pageReqVO); return success(new PageResult<>(buildWorkOrderRespVOList(pageResult.getList()), pageResult.getTotal())); }
    @GetMapping("/product-name-options") @Operation(summary = "查询生产工单产品名称候选") @PreAuthorize("@ss.hasPermission('mes:pro-work-order:query')")
    public CommonResult<List<String>> getWorkOrderProductNameOptions(@RequestParam(value = "keyword", required = false) String keyword) { return success(workOrderService.getWorkOrderProductNameOptions(keyword)); }
    @GetMapping("/temporary-freeze-status") @Operation(summary = "Get Kingdee production order sync status") @PreAuthorize("@ss.hasPermission('mes:pro-work-order:query')")
    public CommonResult<MesProWorkOrderTemporaryFreezeStatusRespVO> getTemporaryFreezeStatus() { return success(workOrderService.getTemporaryFreezeStatus()); }
    @PutMapping("/temporary-freeze") @Operation(summary = "Get Kingdee production order sync status") @PreAuthorize("@ss.hasPermission('mes:pro-work-order:update')")
    public CommonResult<MesProWorkOrderTemporaryFreezeStatusRespVO> updateTemporaryFreeze(@Valid @RequestBody MesProWorkOrderTemporaryFreezeReqVO reqVO) { throw manualOperationForbidden(); }
    @PutMapping("/update-temporary-frozen") @Operation(summary = "Update single work order temporary freeze status") @PreAuthorize("@ss.hasPermission('mes:pro-work-order:update')")
    public CommonResult<Boolean> updateWorkOrderTemporaryFrozen(@Valid @RequestBody MesProWorkOrderUpdateTemporaryFrozenReqVO reqVO) { throw manualOperationForbidden(); }
    @GetMapping("/export-excel") @Operation(summary = "Export work order Excel") @PreAuthorize("@ss.hasPermission('mes:pro-work-order:export')") @ApiAccessLog(operateType = EXPORT)
    public void exportWorkOrderExcel(@Valid MesProWorkOrderPageReqVO pageReqVO, HttpServletResponse response) throws IOException { pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE); List<MesProWorkOrderDO> list = workOrderService.getWorkOrderPage(pageReqVO).getList(); ExcelUtils.write(response, "work-orders.xls", "data", MesProWorkOrderRespVO.class, buildWorkOrderRespVOList(list)); }
    @PutMapping("/confirm") @Operation(summary = "Confirm work order") @Parameter(name = "id", description = "ID", required = true) @PreAuthorize("@ss.hasPermission('mes:pro-work-order:update')")
    public CommonResult<Boolean> confirmWorkOrder(@RequestParam("id") Long id) { throw manualOperationForbidden(); }
    @PutMapping("/finish") @Operation(summary = "Confirm work order") @Parameter(name = "id", description = "ID", required = true) @PreAuthorize("@ss.hasPermission('mes:pro-work-order:update')")
    public CommonResult<Boolean> finishWorkOrder(@RequestParam("id") Long id) { throw manualOperationForbidden(); }
    @PutMapping("/cancel") @Operation(summary = "Confirm work order") @Parameter(name = "id", description = "ID", required = true) @PreAuthorize("@ss.hasPermission('mes:pro-work-order:update')")
    public CommonResult<Boolean> cancelWorkOrder(@RequestParam("id") Long id) { throw manualOperationForbidden(); }
    @GetMapping("/sync-status") @Operation(summary = "Get Kingdee production order sync status") @PreAuthorize("@ss.hasPermission('mes:pro-work-order:query')")
    public CommonResult<MesProWorkOrderSyncStatusRespVO> getKingdeeSyncStatus() { return success(buildKingdeeSyncStatus()); }
    @PostMapping("/sync-kingdee") @Operation(summary = "Sync production orders from Kingdee K3Cloud") @PreAuthorize("@ss.hasAnyPermissions('mes:pro-work-order:create', 'mes:pro-schedule-order:create')")
    public CommonResult<MesKingdeeProductionOrderSyncRespVO> syncKingdeeWorkOrders() { LocalDateTime windowEnd = LocalDateTime.now(); AtomicReference<MesKingdeeProductionOrderSyncResult> resultReference = new AtomicReference<>(); kingdeeSyncRuntimeService.executeSync(ErpKingdeeSyncCommand.builder().syncType(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER).triggerType(ErpKingdeeSyncTriggerTypeEnum.MANUAL).forceInitialWindowStart(true).initialWindowStart(windowEnd.toLocalDate().minusYears(1).atStartOfDay()).windowEnd(windowEnd).build(), context -> { MesKingdeeProductionOrderSyncResult result = kingdeeProductionOrderSyncService.syncWorkOrders(context); resultReference.set(result); return ErpKingdeeSyncRunResult.success(windowEnd, result.getCreatedCount(), result.getUpdatedCount() + result.getFinishedCount() + result.getCanceledCount(), result.getSkippedCount(), 0); }); return success(BeanUtils.toBean(resultReference.get(), MesKingdeeProductionOrderSyncRespVO.class)); }
    @PostMapping("/{id}/create-kingdee-production-order") @Operation(summary = "Create test Kingdee production order for one work order") @Parameter(name = "id", description = "Work order ID", required = true) @PreAuthorize("@ss.hasPermission('mes:pro-work-order:create-erp')")
    public CommonResult<MesKingdeeProductionOrderCreateRespVO> createKingdeeProductionOrder(@PathVariable("id") Long id) { MesKingdeeProductionOrderCreateResult result = kingdeeProductionOrderCreateService.createAndSubmitProductionOrder(id); return success(BeanUtils.toBean(result, MesKingdeeProductionOrderCreateRespVO.class)); }
    @PostMapping("/{id}/sync-erp-bom") @Operation(summary = "Sync ERP BOM for one work order") @Parameter(name = "id", description = "Work order ID", required = true) @PreAuthorize("@ss.hasPermission('mes:pro-work-order:update')")
    public CommonResult<MesKingdeeWorkOrderBomSyncRespVO> syncErpBom(@PathVariable("id") Long id) { MesKingdeeWorkOrderBomSyncResult result = kingdeeWorkOrderBomSyncService.syncErpBom(id); return success(BeanUtils.toBean(result, MesKingdeeWorkOrderBomSyncRespVO.class)); }

    private MesProWorkOrderSyncStatusRespVO buildKingdeeSyncStatus() {
        MesProWorkOrderSyncStatusRespVO respVO = new MesProWorkOrderSyncStatusRespVO();
        respVO.setSyncType(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER.getType());
        JobDO autoSyncJob = findProductionOrderSyncJob();
        respVO.setAutoSyncConfigured(autoSyncJob != null);
        if (autoSyncJob != null) {
            respVO.setAutoSyncJobId(autoSyncJob.getId());
            respVO.setAutoSyncJobName(autoSyncJob.getName());
            respVO.setAutoSyncCronExpression(autoSyncJob.getCronExpression());
            respVO.setAutoSyncEnabled(CommonStatusEnum.ENABLE.getStatus().equals(autoSyncJob.getStatus()));
        } else {
            respVO.setAutoSyncEnabled(Boolean.FALSE);
        }
        ErpKingdeeSyncRunRespVO latestRun = findLatestProductionOrderRun();
        ErpKingdeeSyncWatermarkRespVO watermark = findProductionOrderWatermark();
        respVO.setLastSuccessTime(watermark == null ? null : watermark.getLastSuccessTime());
        if (latestRun == null) { respVO.setLatestStatus(respVO.getAutoSyncConfigured() ? "CONFIGURED_PENDING_RUN" : "AUTO_SYNC_NOT_CONFIGURED"); return respVO; }
        respVO.setLatestRunTime(latestRun.getStartedAt());
        respVO.setLatestFinishedTime(latestRun.getEndedAt());
        respVO.setLatestTriggerType(latestRun.getTriggerType());
        respVO.setLatestCreatedCount(defaultZero(latestRun.getCreatedCount()));
        respVO.setLatestUpdatedCount(defaultZero(latestRun.getUpdatedCount()));
        respVO.setLatestSkippedCount(defaultZero(latestRun.getSkippedCount()));
        respVO.setLatestFailedCount(defaultZero(latestRun.getFailedCount()));
        respVO.setLatestFailureMessage(latestRun.getFailureMessage());
        respVO.setLatestStatus(resolveLatestStatus(latestRun));
        return respVO;
    }
    private JobDO findProductionOrderSyncJob() { JobPageReqVO pageReqVO = new JobPageReqVO(); pageReqVO.setPageNo(1); pageReqVO.setPageSize(100); pageReqVO.setHandlerName("KingdeeProductionOrderSyncJob"); PageResult<JobDO> pageResult = jobService.getJobPage(pageReqVO); return pageResult.getList().stream().findFirst().orElse(null); }
    private ErpKingdeeSyncRunRespVO findLatestProductionOrderRun() { ErpKingdeeSyncRunPageReqVO pageReqVO = new ErpKingdeeSyncRunPageReqVO(); pageReqVO.setPageNo(1); pageReqVO.setPageSize(1); pageReqVO.setSyncType(ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER.getType()); PageResult<ErpKingdeeSyncRunRespVO> pageResult = kingdeeSyncAdminService.getRunPage(pageReqVO); return pageResult.getList().stream().findFirst().orElse(null); }
    private ErpKingdeeSyncWatermarkRespVO findProductionOrderWatermark() { return kingdeeSyncAdminService.getWatermarks().stream().filter(item -> Objects.equals(item.getSyncType(), ErpKingdeeSyncTypeEnum.PRODUCTION_ORDER.getType())).findFirst().orElse(null); }
    private String resolveLatestStatus(ErpKingdeeSyncRunRespVO latestRun) { if (latestRun.getStatus() == null) return "UNKNOWN"; if (Objects.equals(latestRun.getStatus(), 30)) return "FAILED"; if (Objects.equals(latestRun.getStatus(), 10)) return "RUNNING"; int changedCount = defaultZero(latestRun.getCreatedCount()) + defaultZero(latestRun.getUpdatedCount()); return changedCount == 0 ? "SUCCESS_NO_NEW_RECORDS" : "SUCCESS"; }
    private int defaultZero(Integer value) { return value == null ? 0 : value; }
    private List<MesProWorkOrderRespVO> buildWorkOrderRespVOList(List<MesProWorkOrderDO> list) { if (CollUtil.isEmpty(list)) return Collections.emptyList(); Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(convertSet(list, MesProWorkOrderDO::getProductId)); Map<Long, MesMdClientDO> clientMap = clientService.getClientMap(convertSet(list, MesProWorkOrderDO::getClientId)); Map<Long, MesMdVendorDO> vendorMap = vendorService.getVendorMap(convertSet(list, MesProWorkOrderDO::getVendorId)); Map<Long, MesMdUnitMeasureDO> unitMeasureMap = unitMeasureService.getUnitMeasureMap(convertSet(itemMap.values(), MesMdItemDO::getUnitMeasureId)); Map<Long, MesProWorkOrderDO> parentMap = workOrderService.getWorkOrderMap(convertSet(list, MesProWorkOrderDO::getParentId)); Map<Long, List<MesKingdeeProductionMaterialListDO>> materialListMap = buildMaterialListMapByWorkOrderId(list); return BeanUtils.toBean(list, MesProWorkOrderRespVO.class, vo -> { MapUtils.findAndThen(itemMap, vo.getProductId(), item -> { vo.setProductName(item.getName()).setProductCode(item.getCode()).setProductSpecification(item.getSpecification()); MapUtils.findAndThen(unitMeasureMap, item.getUnitMeasureId(), unitMeasure -> vo.setUnitMeasureName(unitMeasure.getName())); }); MapUtils.findAndThen(clientMap, vo.getClientId(), client -> vo.setClientName(client.getName()).setClientCode(client.getCode())); MapUtils.findAndThen(vendorMap, vo.getVendorId(), vendor -> vo.setVendorName(vendor.getName()).setVendorCode(vendor.getCode())); MapUtils.findAndThen(parentMap, vo.getParentId(), parent -> vo.setParentCode(parent.getCode())); List<MesKingdeeProductionMaterialListDO> materialRows = materialListMap.get(vo.getId()); if (CollUtil.isNotEmpty(materialRows)) { LinkedHashSet<String> billNos = new LinkedHashSet<>(convertList(materialRows, MesKingdeeProductionMaterialListDO::getSourceBillNo)); billNos.remove(null); billNos.remove(""); vo.setProductionMaterialListCount((long) billNos.size()); vo.setProductionMaterialListSummary(billNos.isEmpty() ? null : String.join("、", billNos)); } else { vo.setProductionMaterialListCount(0L); vo.setProductionMaterialListSummary(null); } }); }
    private Map<Long, List<MesKingdeeProductionMaterialListDO>> buildMaterialListMapByWorkOrderId(List<MesProWorkOrderDO> list) { List<Long> workOrderIds = convertList(list, MesProWorkOrderDO::getId); if (CollUtil.isEmpty(workOrderIds)) { return Collections.emptyMap(); } return productionMaterialListMapper.selectListByWorkOrderIds(workOrderIds).stream().filter(item -> item.getWorkOrderId() != null).collect(java.util.stream.Collectors.groupingBy(MesKingdeeProductionMaterialListDO::getWorkOrderId, LinkedHashMap::new, java.util.stream.Collectors.toList())); }
    private RuntimeException manualOperationForbidden() { return exception(PRO_WORK_ORDER_MANUAL_OPERATION_FORBIDDEN); }
}
