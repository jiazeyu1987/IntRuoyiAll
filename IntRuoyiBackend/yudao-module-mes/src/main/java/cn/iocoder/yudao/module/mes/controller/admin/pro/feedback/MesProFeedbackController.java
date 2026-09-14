package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportAttributeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportBatchSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportConfirmBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackSubmitService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackImportRecordService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.importer.ThirdPartyFeedbackImportPayload;
import cn.iocoder.yudao.module.mes.service.pro.feedback.importer.ThirdPartyFeedbackImportResult;
import cn.iocoder.yudao.module.mes.service.pro.feedback.importer.ThirdPartyFeedbackImportService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.task.MesProTaskService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.framework.common.util.collection.MapUtils.findAndThen;


@Tag(name = "管理后台 - MES 生产报工")
@RestController
@RequestMapping("/mes/pro/feedback")
@Validated
public class MesProFeedbackController {

    @Resource
    private MesProFeedbackService feedbackService;
    @Resource
    private MesProFrontlineFeedbackSubmitService frontlineFeedbackSubmitService;
    @Resource
    private MesMdWorkstationService workstationService;
    @Resource
    private MesProRouteService routeService;
    @Resource
    private MesProProcessService processService;
    @Resource
    private MesProWorkOrderService workOrderService;
    @Resource
    private MesMdItemService itemService;
    @Resource
    private MesMdUnitMeasureService unitMeasureService;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesProTaskService taskService;
    @Resource
    private ThirdPartyFeedbackImportService thirdPartyFeedbackImportService;
    @Resource
    private MesProFeedbackImportRecordService feedbackImportRecordService;

    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建生产报工")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:create')")
    public CommonResult<Long> createFeedback(@Valid @RequestBody MesProFeedbackSaveReqVO createReqVO) {
        return success(feedbackService.createFeedback(createReqVO));
    }

    @PostMapping("/frontline/submit")
    @Operation(summary = "一线报工与记录本一体提交")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:create')")
    public CommonResult<MesProFrontlineFeedbackSubmitRespVO> frontlineSubmit(
            @Valid @RequestBody MesProFrontlineFeedbackSubmitReqVO reqVO) {
        return success(frontlineFeedbackSubmitService.submit(reqVO));
    }

    @PostMapping("/import-third-party-xlsx")
    @Operation(summary = "导入第三方生产报工 Excel")
    @Parameters({
            @Parameter(name = "file", description = "第三方报工 Excel 文件", required = true)
    })
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:create')")
    public CommonResult<ThirdPartyFeedbackImportResult> importThirdPartyXlsx(@RequestParam("file") MultipartFile file) {
        return success(thirdPartyFeedbackImportService.importWorkbook(file));
    }

    @PostMapping("/import-direct-work-report-xlsx")
    @Operation(summary = "导入李萍报工单 Excel 并直接创建报工")
    @Parameters({
            @Parameter(name = "file", description = "李萍报工单 Excel 文件", required = true)
    })
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:create')")
    public CommonResult<ThirdPartyFeedbackImportResult> importDirectWorkReportXlsx(@RequestParam("file") MultipartFile file) {
        return success(thirdPartyFeedbackImportService.importDirectWorkReportWorkbook(file));
    }

    @PostMapping("/simulate-import-third-party-xlsx")
    @Operation(summary = "模拟导入第三方生产报工 Excel")
    @Parameter(name = "processCount", description = "随机模拟的订单工序数量", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:create')")
    public CommonResult<ThirdPartyFeedbackImportResult> simulateThirdPartyXlsxImport(
            @RequestParam("processCount") @Min(1) @Max(20) Integer processCount) {
        return success(thirdPartyFeedbackImportService.simulateImportWorkbook(processCount));
    }

    @GetMapping("/import-record/page")
    @Operation(summary = "分页查询第三方报工待归属记录")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<PageResult<MesProFeedbackImportRecordRespVO>> getImportRecordPage(
            @Valid MesProFeedbackImportRecordPageReqVO pageReqVO) {
        return success(feedbackImportRecordService.getImportRecordPage(pageReqVO));
    }

    @GetMapping("/import-record/batch-summary")
    @Operation(summary = "查询第三方报工当前批次摘要")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<MesProFeedbackImportBatchSummaryRespVO> getImportRecordBatchSummary(
            @RequestParam("importRecordIds") List<Long> importRecordIds) {
        return success(feedbackImportRecordService.getImportRecordBatchSummary(importRecordIds));
    }

    @GetMapping("/import-record/candidates")
    @Operation(summary = "查询第三方报工归属候选排产工序")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<List<MesProFeedbackImportCandidateRespVO>> getImportRecordCandidates(
            @RequestParam("importRecordId") Long importRecordId) {
        return success(feedbackImportRecordService.getAttributionCandidates(importRecordId));
    }

    @PostMapping("/import-record/attribute")
    @Operation(summary = "确认第三方报工归属并创建正式报工")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:update')")
    public CommonResult<Long> attributeImportRecord(@Valid @RequestBody MesProFeedbackImportAttributeReqVO reqVO) {
        return success(feedbackImportRecordService.attributeImportRecord(reqVO));
    }

    @PostMapping("/import-record/reattribute")
    @Operation(summary = "修改第三方报工归属并重建草稿正式报工")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:update')")
    public CommonResult<Long> reattributeImportRecord(@Valid @RequestBody MesProFeedbackImportAttributeReqVO reqVO) {
        return success(feedbackImportRecordService.reattributeImportRecord(reqVO));
    }

    @PostMapping("/import-record/confirm-batch")
    @Operation(summary = "整批确认第三方导入报工")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:update')")
    public CommonResult<Boolean> confirmImportRecordBatch(
            @Valid @RequestBody MesProFeedbackImportConfirmBatchReqVO reqVO) {
        feedbackImportRecordService.confirmImportRecordBatch(reqVO);
        return success(true);
    }

    @PutMapping("/update")
    @Operation(summary = "更新生产报工")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:update')")
    public CommonResult<Boolean> updateFeedback(@Valid @RequestBody MesProFeedbackSaveReqVO updateReqVO) {
        feedbackService.updateFeedback(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除生产报工")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:delete')")
    public CommonResult<Boolean> deleteFeedback(@RequestParam("id") Long id) {
        feedbackService.deleteFeedback(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得生产报工")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<MesProFeedbackRespVO> getFeedback(@RequestParam("id") Long id) {
        MesProFeedbackDO feedback = feedbackService.getFeedback(id);
        if (feedback == null) {
            return success(null);
        }
        return success(buildFeedbackRespVOList(ListUtil.of(feedback)).get(0));
    }

    @GetMapping("/page")
    @Operation(summary = "获得生产报工分页")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<PageResult<MesProFeedbackRespVO>> getFeedbackPage(@Valid MesProFeedbackPageReqVO pageReqVO) {
        PageResult<MesProFeedbackDO> pageResult = feedbackService.getFeedbackPage(pageReqVO);
        return success(new PageResult<>(buildFeedbackRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出生产报工 Excel")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportFeedbackExcel(@Valid MesProFeedbackPageReqVO pageReqVO,
                                     HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MesProFeedbackDO> list = feedbackService.getFeedbackPage(pageReqVO).getList();
        List<MesProFeedbackRespVO> voList = buildFeedbackRespVOList(list);
        ExcelUtils.write(response, "生产报工.xls", "数据", MesProFeedbackRespVO.class, voList);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交报工")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:update')")
    public CommonResult<Boolean> submitFeedback(@RequestParam("id") Long id) {
        feedbackService.submitFeedback(id);
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "驳回报工")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:update')")
    public CommonResult<Boolean> rejectFeedback(@RequestParam("id") Long id) {
        feedbackService.rejectFeedback(id);
        return success(true);
    }

    @PutMapping("/approve")
    @Operation(summary = "审批报工")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:approve')")
    public CommonResult<Boolean> approveFeedback(@RequestParam("id") Long id) {
        return success(feedbackService.approveFeedback(id));
    }

    // ==================== 拼接 VO ====================

    private List<MesProFeedbackRespVO> buildFeedbackRespVOList(List<MesProFeedbackDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 1.1 任务（获取 taskCode）
        Map<Long, MesProTaskDO> taskMap = taskService.getTaskMap(
                convertSet(list, MesProFeedbackDO::getTaskId));
        // 1.2 物料（直接从 DO.itemId）、计量单位（从 item.unitMeasureId）
        Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(
                convertSet(list, MesProFeedbackDO::getItemId));
        Map<Long, MesMdUnitMeasureDO> unitMeasureMap = unitMeasureService.getUnitMeasureMap(
                convertSet(itemMap.values(), MesMdItemDO::getUnitMeasureId));
        // 1.3 工单
        Map<Long, MesProWorkOrderDO> workOrderMap = workOrderService.getWorkOrderMap(
                convertSet(list, MesProFeedbackDO::getWorkOrderId));
        // 1.4 工作站
        Map<Long, MesMdWorkstationDO> workstationMap = workstationService.getWorkstationMap(
                convertSet(list, MesProFeedbackDO::getWorkstationId));
        // 1.5 工艺路线
        Set<Long> routeIds = convertSet(list, MesProFeedbackDO::getRouteId);
        Map<Long, MesProRouteDO> routeMap = routeService.getRouteMapIgnoreDeleted(routeIds);
        // 1.6 工序
        Map<Long, MesProProcessDO> processMap = processService.getProcessMap(
                new ArrayList<>(convertSet(list, MesProFeedbackDO::getProcessId)));
        // 1.7 工序的 checkFlag：批量查询后直接构建 routeId -> processId -> checkFlag 的双层 Map
        List<MesProRouteProcessDO> allRouteProcesses = routeProcessService.getRouteProcessListByRouteIdsIgnoreDeleted(routeIds);
        Map<Long, Map<Long, Boolean>> routeProcessCheckFlagMap = new HashMap<>();
        for (MesProRouteProcessDO rp : allRouteProcesses) {
            routeProcessCheckFlagMap
                    .computeIfAbsent(rp.getRouteId(), k -> new HashMap<>())
                    .put(rp.getProcessId(), Boolean.TRUE.equals(rp.getCheckFlag()));
        }
        // 1.8 报工人/审核人
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSetByFlatMap(list, feedback ->
                        Stream.of(feedback.getFeedbackUserId(), feedback.getApproveUserId())));
        Map<Long, MesProFeedbackImportRecordDO> importRecordMap = feedbackImportRecordService.getImportRecordMapByFeedbacks(list);

        // 2. 拼接 VO
        return BeanUtils.toBean(list, MesProFeedbackRespVO.class, vo -> {
            // 工作站
            findAndThen(workstationMap, vo.getWorkstationId(), ws ->
                    vo.setWorkstationCode(ws.getCode()).setWorkstationName(ws.getName()));
            // 工艺路线
            findAndThen(routeMap, vo.getRouteId(), route ->
                    vo.setRouteCode(route.getCode()));
            // 工序
            findAndThen(processMap, vo.getProcessId(), process ->
                    vo.setProcessCode(process.getCode()).setProcessName(process.getName()));
            // checkFlag
            findAndThen(routeProcessCheckFlagMap, vo.getRouteId(), processCheckMap ->
                    findAndThen(processCheckMap, vo.getProcessId(), vo::setCheckFlag));
            // 工单
            findAndThen(workOrderMap, vo.getWorkOrderId(), wo ->
                    vo.setWorkOrderCode(wo.getCode()).setWorkOrderName(wo.getName()));
            // 任务
            findAndThen(taskMap, vo.getTaskId(), task ->
                    vo.setTaskCode(task.getCode()));
            // 物料 → 单位
            findAndThen(itemMap, vo.getItemId(), item -> {
                vo.setItemCode(item.getCode()).setItemName(item.getName()).setItemSpecification(item.getSpecification())
                        .setUnitMeasureId(item.getUnitMeasureId());
                findAndThen(unitMeasureMap, item.getUnitMeasureId(), unit ->
                        vo.setUnitMeasureName(unit.getName()));
            });
            // 报工人、审核人
            findAndThen(userMap, vo.getFeedbackUserId(), user ->
                    vo.setFeedbackUserNickname(user.getNickname()));
            findAndThen(userMap, vo.getApproveUserId(), user ->
                    vo.setApproveUserNickname(user.getNickname()));
            findAndThen(importRecordMap, vo.getId(), importRecord -> {
                vo.setSourceImportRecordId(importRecord.getId())
                        .setSourceImportFileName(importRecord.getSourceFileName())
                        .setSourceImportSheetName(importRecord.getSheetName())
                        .setSourceImportRowNo(importRecord.getRowNo())
                        .setSourceImportAttributionTime(importRecord.getUpdateTime());
                fillExcelDisplayFields(vo, importRecord);
            });
            vo.setApprovalImpactText(buildApprovalImpactText(vo));
        });
    }

    private void fillExcelDisplayFields(MesProFeedbackRespVO vo, MesProFeedbackImportRecordDO importRecord) {
        ThirdPartyFeedbackImportPayload payload = JsonUtils.parseObject(
                importRecord.getSourcePayloadJson(), ThirdPartyFeedbackImportPayload.class);
        if (payload == null) {
            return;
        }
        vo.setExcelProductCode(payload.getItemCode())
                .setExcelProductName(payload.getItemName())
                .setExcelProcessCode(payload.getProcessCode())
                .setExcelProcessName(payload.getProcessName())
                .setExcelDepartment(payload.getDepartment())
                .setExcelEmployeeNo(payload.getFeedbackUserCode())
                .setExcelEmployeeName(payload.getFeedbackUserName())
                .setExcelSectionLeader(payload.getApproverName())
                .setExcelFeedbackTime(payload.getFeedbackTime());
    }

    private String buildApprovalImpactText(MesProFeedbackRespVO vo) {
        if (vo.getSourceImportRecordId() != null || vo.getScheduleOrderProcessId() != null) {
            return "归属会按所选订单工序生成草稿正式报工；提交正式报工后回写排产进度；审批仅确认质量/合规结果，不会改变归属工序或重复扣减剩余数量。";
        }
        return "审批仅确认质量/合规结果，通过后进入质检/入库/完成判断。";
    }

}
