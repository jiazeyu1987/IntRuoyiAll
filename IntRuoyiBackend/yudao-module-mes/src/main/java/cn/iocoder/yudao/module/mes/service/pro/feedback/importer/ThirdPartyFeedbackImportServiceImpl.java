package cn.iocoder.yudao.module.mes.service.pro.feedback.importer;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackImportRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.md.autocode.MesMdAutoCodeRuleCodeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO.ATTRIBUTION_STATUS_ATTRIBUTED;
import static cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO.ATTRIBUTION_STATUS_PENDING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_FILE_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_FILE_TYPE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_IMPORT_ROW_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_SIMULATE_CURRENT_USER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_SIMULATE_PROCESS_COUNT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_SIMULATE_SOURCE_NOT_ENOUGH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_SIMULATE_SOURCE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FEEDBACK_SIMULATE_WORKBOOK_BUILD_FAILED;

@Service
@Validated
public class ThirdPartyFeedbackImportServiceImpl implements ThirdPartyFeedbackImportService {

    private static final List<String> REQUIRED_HEADERS = List.of(
            "报工日期", "报工人编码", "报工人名称", "工段长", "生产订单号", "生产资源组", "生产资源", "派工单号",
            "产品编码", "产品名称", "规格", "模具编码", "工序编码", "工序名称", "所属部门", "报工数量", "支数",
            "公斤数", "实腔数", "全程时间", "生产定额", "工作时长", "注塑合模/组装公斤数", "注塑个数/组装个重", "操作"
    );
    private static final DateTimeFormatter FEEDBACK_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MIN_SIMULATED_FEEDBACK_QUANTITY = 100;
    private static final int MAX_SIMULATED_FEEDBACK_QUANTITY = 10_000;
    private static final int MIN_SIMULATED_PROCESS_COUNT = 1;
    private static final int MAX_SIMULATED_PROCESS_COUNT = 20;
    @Resource
    private ThirdPartyFeedbackExcelParser parser;
    @Resource
    private MesProFeedbackImportRecordMapper importRecordMapper;
    @Resource
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Resource
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Resource
    private MesProWorkOrderMapper workOrderMapper;
    @Resource
    private MesMdItemMapper itemMapper;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private MesProRouteProcessService routeProcessService;
    @Resource
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Resource
    private MesProTaskMapper taskMapper;
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private MesProFeedbackMapper feedbackMapper;
    @Resource
    private MesMdAutoCodeRecordService autoCodeRecordService;
    @Resource
    private MesProFeedbackService feedbackService;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ThirdPartyFeedbackImportResult importWorkbook(MultipartFile file) {
        validateFile(file);
        byte[] fileBytes = getFileBytes(file);
        return importWorkbook(StrUtil.blankToDefault(file.getOriginalFilename(), "third-party-feedback.xlsx"), fileBytes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ThirdPartyFeedbackImportResult importDirectWorkReportWorkbook(MultipartFile file) {
        validateFile(file);
        byte[] fileBytes = getFileBytes(file);
        return importDirectWorkReportWorkbook(StrUtil.blankToDefault(file.getOriginalFilename(), "direct-work-report.xlsx"), fileBytes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ThirdPartyFeedbackImportResult simulateImportWorkbook(Integer processCount) {
        int targetProcessCount = validateSimulatedProcessCount(processCount);
        AdminUserDO currentUser = resolveCurrentUser();
        List<SimulatedFeedbackSource> sources = resolveSimulatedFeedbackSources(targetProcessCount);
        Collections.shuffle(sources);
        List<SimulatedFeedbackSource> selectedSources = sources.subList(0, targetProcessCount);
        String uniqueToken = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String sheetName = "模拟报工-" + uniqueToken;
        String fileName = "simulated-third-party-feedback-" + uniqueToken + ".xlsx";
        byte[] workbookBytes = buildSimulatedWorkbook(selectedSources, currentUser, sheetName);
        return importWorkbook(fileName, workbookBytes);
    }

    private ThirdPartyFeedbackImportResult importWorkbook(String sourceFileName, byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw exception(PRO_FEEDBACK_IMPORT_FILE_EMPTY);
        }
        ThirdPartyFeedbackExcelParser.ImportWorkbookTemplate template =
                parser.detectTemplate(new ByteArrayInputStream(fileBytes));
        if (template == ThirdPartyFeedbackExcelParser.ImportWorkbookTemplate.LI_PING_DIRECT_WORK_REPORT) {
            return importDirectWorkReportWorkbook(sourceFileName, fileBytes);
        }
        String sourceFileSha256 = sha256Hex(fileBytes);
        ThirdPartyFeedbackExcelParseResult parseResult = parser.parse(new ByteArrayInputStream(fileBytes));
        List<Long> importRecordIds = new ArrayList<>();
        for (ThirdPartyFeedbackExcelRow row : parseResult.rows()) {
            if (importRecordMapper.selectBySourceFingerprint(sourceFileSha256, row.sheetName(), row.rowNo()) != null) {
                throw exception(PRO_FEEDBACK_IMPORT_ROW_DUPLICATE, row.sheetName(), row.rowNo());
            }
            MesProFeedbackImportRecordDO record = MesProFeedbackImportRecordDO.builder()
                    .sourceFileName(StrUtil.blankToDefault(sourceFileName, "third-party-feedback.xlsx"))
                    .sourceFileSha256(sourceFileSha256)
                    .sheetName(row.sheetName())
                    .rowNo(row.rowNo())
                    .feedbackId(0L)
                    .attributionStatus(ATTRIBUTION_STATUS_PENDING)
                    .taskCode(row.taskCode())
                    .workOrderCode(row.workOrderCode())
                    .itemCode(row.itemCode())
                    .processCode(row.processCode())
                    .sourcePayloadJson(JsonUtils.toJsonString(toPayload(row)))
                    .scheduleOrderId(null)
                    .scheduleOrderProcessId(null)
                    .candidateCount(null)
                    .remark(buildTraceRemark(row))
                    .build();
            importRecordMapper.insert(record);
            importRecordIds.add(record.getId());
        }

        ThirdPartyFeedbackImportResult result = new ThirdPartyFeedbackImportResult();
        result.setSheetCount(parseResult.sheetCount());
        result.setImportedCount(importRecordIds.size());
        result.setPendingCount(importRecordIds.size());
        result.setSubmittedCount(0);
        result.setSkippedRows(0);
        result.setFeedbackCodes(List.of());
        result.setImportRecordIds(importRecordIds);
        return result;
    }

    private ThirdPartyFeedbackImportResult importDirectWorkReportWorkbook(String sourceFileName, byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw exception(PRO_FEEDBACK_IMPORT_FILE_EMPTY);
        }
        String sourceFileSha256 = sha256Hex(fileBytes);
        DirectWorkReportExcelParseResult parseResult = parser.parseLiPingDirectWorkReport(new ByteArrayInputStream(fileBytes));
        List<DirectWorkReportExcelRow> rows = parseResult.rows();
        if (rows.isEmpty()) {
            ThirdPartyFeedbackImportResult result = new ThirdPartyFeedbackImportResult();
            result.setSheetCount(parseResult.sheetCount());
            result.setImportedCount(0);
            result.setPendingCount(0);
            result.setSubmittedCount(0);
            result.setSkippedRows(parseResult.skippedRows());
            result.setFeedbackCodes(List.of());
            result.setImportRecordIds(List.of());
            result.setDirectWorkReportDetails(List.of());
            result.setDirectWorkReportSkipWarnings(List.of());
            return result;
        }
        List<ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning> directWorkReportSkipWarnings = new ArrayList<>();
        int skippedRows = parseResult.skippedRows();
        UniqueWorkOrderLoadResult workOrderLoadResult = loadUniqueWorkOrders(rows);
        Map<String, MesProWorkOrderDO> workOrderMap = workOrderLoadResult.uniqueWorkOrders();
        List<DirectWorkReportExcelRow> rowsWithWorkOrder = new ArrayList<>();
        for (DirectWorkReportExcelRow row : rows) {
            if (!workOrderMap.containsKey(row.workOrderCode())) {
                skippedRows++;
                boolean duplicated = workOrderLoadResult.duplicatedCodes().contains(row.workOrderCode());
                directWorkReportSkipWarnings.add(buildDirectWorkReportSkipWarning(row, null, null, null,
                        Collections.emptyMap(), duplicated ? "WORK_ORDER_NOT_UNIQUE" : "WORK_ORDER_NOT_FOUND",
                        duplicated ? "生产订单号匹配到多个系统工单，无法唯一更新排产进度。"
                                : "生产工单未匹配到唯一系统工单，本行未更新排产进度。"));
                continue;
            }
            rowsWithWorkOrder.add(row);
        }
        if (rowsWithWorkOrder.isEmpty()) {
            ThirdPartyFeedbackImportResult result = new ThirdPartyFeedbackImportResult();
            result.setSheetCount(parseResult.sheetCount());
            result.setImportedCount(0);
            result.setPendingCount(0);
            result.setSubmittedCount(0);
            result.setSkippedRows(skippedRows);
            result.setFeedbackCodes(List.of());
            result.setImportRecordIds(List.of());
            result.setDirectWorkReportDetails(List.of());
            result.setDirectWorkReportSkipWarnings(directWorkReportSkipWarnings);
            return result;
        }
        Map<Long, MesMdItemDO> itemMap = loadWorkOrderItems(workOrderMap);
        UniqueScheduleOrderLoadResult scheduleOrderLoadResult = loadUniqueScheduleOrders(rowsWithWorkOrder, workOrderMap);
        Map<Long, MesProScheduleOrderDO> scheduleOrderMap = scheduleOrderLoadResult.uniqueScheduleOrders();
        Map<Long, List<MesProScheduleOrderProcessDO>> processMap = loadScheduleProcesses(scheduleOrderMap);
        Map<Long, MesProScheduleOrderDO> scheduleOrderById = scheduleOrderMap.values().stream()
                .collect(Collectors.toMap(MesProScheduleOrderDO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<Long, Long> scheduleProcessIdentityProcessIdMap =
                resolveScheduleProcessIdentityProcessIdMap(processMap, scheduleOrderById);
        Map<Long, MesProProcessDO> processDoMap = loadProcesses(processMap, scheduleProcessIdentityProcessIdMap);

        List<Long> importRecordIds = new ArrayList<>();
        List<String> feedbackCodes = new ArrayList<>();
        List<ThirdPartyFeedbackImportResult.DirectWorkReportDetail> directWorkReportDetails = new ArrayList<>();
        for (DirectWorkReportExcelRow row : rowsWithWorkOrder) {
            DirectProgressResolution resolution = resolveDirectProgressTarget(row, workOrderMap, scheduleOrderMap,
                    processMap, processDoMap, itemMap, scheduleProcessIdentityProcessIdMap,
                    scheduleOrderLoadResult.duplicatedWorkOrderIds());
            if (resolution.target() == null) {
                skippedRows++;
                if (resolution.skipWarning() != null) {
                    directWorkReportSkipWarnings.add(resolution.skipWarning());
                }
                continue;
            }
            DirectProgressTarget target = resolution.target();
            MesProScheduleOrderProcessDO beforeProgress = copyDirectProgressSnapshot(target.scheduleOrderProcess());
            if (!hasEnoughRemainingQuantity(row, beforeProgress)) {
                skippedRows++;
                directWorkReportSkipWarnings.add(buildDirectWorkReportSkipWarning(row, target.workOrder(),
                        target.scheduleOrder(), target.scheduleOrderProcess(), itemMap, "REMAINING_NOT_ENOUGH",
                        "报工数量超过当前工序剩余数量，本行未生成正式报工。"));
                continue;
            }
            DirectFeedbackContext directFeedbackContext = buildDirectFeedbackContext(row, target, itemMap);
            if (directFeedbackContext.skipWarning() != null) {
                skippedRows++;
                directWorkReportSkipWarnings.add(directFeedbackContext.skipWarning());
                continue;
            }
            MesProFeedbackImportRecordDO record = buildDirectImportRecord(sourceFileName, sourceFileSha256, row,
                    target.scheduleOrder().getId(), target.scheduleOrderProcess().getId(), 1, buildTraceRemark(row));
            importRecordMapper.insert(record);
            Long feedbackId = feedbackService.createFeedbackWithScheduleSnapshot(directFeedbackContext.request());
            feedbackMapper.updateById(new MesProFeedbackDO()
                    .setId(feedbackId)
                    .setSourceImportRecordId(record.getId()));
            importRecordMapper.updateById(MesProFeedbackImportRecordDO.builder()
                    .id(record.getId())
                    .feedbackId(feedbackId)
                    .attributionStatus(ATTRIBUTION_STATUS_ATTRIBUTED)
                    .attributionTargetType(MesProFeedbackImportRecordDO.ATTRIBUTION_TARGET_TYPE_CURRENT_ORDER)
                    .scheduleOrderId(target.scheduleOrder().getId())
                    .scheduleOrderProcessId(target.scheduleOrderProcess().getId())
                    .build());
            feedbackService.submitFeedback(feedbackId, true);
            Map<Long, MesProScheduleOrderProcessDO> recalculatedProcessMap =
                    recalculateDirectProgressForScheduleOrder(target.scheduleOrder());
            MesProScheduleOrderProcessDO afterProgress = recalculatedProcessMap.getOrDefault(
                    target.scheduleOrderProcess().getId(),
                    buildDirectProgressAfterSnapshot(beforeProgress, row.feedbackQuantity()));
            importRecordIds.add(record.getId());
            feedbackCodes.add(directFeedbackContext.feedbackCode());
            ThirdPartyFeedbackImportResult.DirectWorkReportDetail detail = buildDirectProgressDetail(row, target,
                    itemMap, beforeProgress, afterProgress, record.getId(), null, null);
            detail.setFeedbackCode(directFeedbackContext.feedbackCode());
            detail.setResultCode("FEEDBACK_SUBMITTED");
            detail.setResultMessage("已生成并提交正式报工，排产进度已按正式报工重算。");
            directWorkReportDetails.add(detail);
            target.scheduleOrderProcess().setReportedQuantity(afterProgress.getReportedQuantity());
            target.scheduleOrderProcess().setProgressPercent(afterProgress.getProgressPercent());
            target.scheduleOrderProcess().setRemainingQuantity(afterProgress.getRemainingQuantity());
        }

        ThirdPartyFeedbackImportResult result = new ThirdPartyFeedbackImportResult();
        result.setSheetCount(parseResult.sheetCount());
        result.setImportedCount(importRecordIds.size());
        result.setPendingCount(0);
        result.setSubmittedCount(feedbackCodes.size());
        result.setSkippedRows(skippedRows);
        result.setFeedbackCodes(feedbackCodes);
        result.setImportRecordIds(importRecordIds);
        result.setDirectWorkReportDetails(directWorkReportDetails);
        result.setDirectWorkReportSkipWarnings(directWorkReportSkipWarnings);
        return result;
    }

    private MesProFeedbackImportRecordDO buildDirectImportRecord(String sourceFileName,
                                                                 String sourceFileSha256,
                                                                 DirectWorkReportExcelRow row,
                                                                 Long scheduleOrderId,
                                                                 Long scheduleOrderProcessId,
                                                                 Integer candidateCount,
                                                                 String remark) {
        ThirdPartyFeedbackImportPayload payload = toPayload(row);
        return MesProFeedbackImportRecordDO.builder()
                .sourceFileName(sourceFileName)
                .sourceFileSha256(sourceFileSha256)
                .sheetName(row.sheetName())
                .rowNo(row.rowNo())
                .feedbackId(0L)
                .attributionStatus(ATTRIBUTION_STATUS_PENDING)
                .taskCode(row.taskCode())
                .workOrderCode(row.workOrderCode())
                .itemCode(row.itemCode())
                .processCode(row.processCode())
                .sourcePayloadJson(JsonUtils.toJsonString(payload))
                .scheduleOrderId(scheduleOrderId)
                .scheduleOrderProcessId(scheduleOrderProcessId)
                .candidateCount(candidateCount)
                .remark(remark)
                .build();
    }

    private String buildFailedTraceRemark(DirectWorkReportExcelRow row) {
        return buildTraceRemark(row)
                + "；归属失败：缺少唯一可用的 taskId -> scheduleOrderProcessId -> scheduleOrderId -> workOrderId 链路，或剩余数量/报工人/审批人不满足归属条件";
    }

    private AdminUserDO resolveCurrentUser() {
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        if (currentUserId == null) {
            throw exception(PRO_FEEDBACK_SIMULATE_CURRENT_USER_NOT_EXISTS);
        }
        AdminUserDO currentUser = adminUserMapper.selectById(currentUserId);
        if (currentUser == null || StrUtil.isBlank(currentUser.getUsername())) {
            throw exception(PRO_FEEDBACK_SIMULATE_CURRENT_USER_NOT_EXISTS);
        }
        return currentUser;
    }

    private int validateSimulatedProcessCount(Integer processCount) {
        if (processCount == null
                || processCount < MIN_SIMULATED_PROCESS_COUNT
                || processCount > MAX_SIMULATED_PROCESS_COUNT) {
            throw exception(PRO_FEEDBACK_SIMULATE_PROCESS_COUNT_INVALID);
        }
        return processCount;
    }

    private List<SimulatedFeedbackSource> resolveSimulatedFeedbackSources(int processCount) {
        List<MesProScheduleOrderProcessDO> processList = scheduleOrderProcessMapper.selectList(
                new LambdaQueryWrapperX<MesProScheduleOrderProcessDO>()
                        .eq(MesProScheduleOrderProcessDO::getEnabled, Boolean.TRUE)
                        .gt(MesProScheduleOrderProcessDO::getRemainingQuantity, BigDecimal.ZERO)
                        .orderByDesc(MesProScheduleOrderProcessDO::getId));
        List<SimulatedFeedbackSource> result = new ArrayList<>();
        for (MesProScheduleOrderProcessDO scheduleOrderProcess : processList) {
            MesProScheduleOrderDO scheduleOrder = scheduleOrderMapper.selectById(scheduleOrderProcess.getScheduleOrderId());
            if (!isAttributable(scheduleOrder) || StrUtil.isBlank(scheduleOrder.getErpWorkOrderCode())) {
                continue;
            }
            MesProTaskDO task = resolveTargetTask(scheduleOrderProcess.getId());
            if (task == null || StrUtil.isBlank(task.getCode())) {
                continue;
            }
            MesProWorkOrderDO workOrder = workOrderMapper.selectById(scheduleOrder.getWorkOrderId());
            if (workOrder == null || workOrder.getProductId() == null) {
                continue;
            }
            MesMdItemDO item = itemMapper.selectById(workOrder.getProductId());
            if (item == null || StrUtil.isBlank(item.getCode())) {
                continue;
            }
            MesProProcessDO process = processMapper.selectById(scheduleOrderProcess.getProcessId());
            if (process == null || StrUtil.isBlank(process.getCode()) || StrUtil.isBlank(process.getName())) {
                continue;
            }
            result.add(new SimulatedFeedbackSource(scheduleOrder, scheduleOrderProcess, workOrder, item, process, task));
        }
        if (result.isEmpty()) {
            throw exception(PRO_FEEDBACK_SIMULATE_SOURCE_NOT_EXISTS);
        }
        if (result.size() < processCount) {
            throw exception(PRO_FEEDBACK_SIMULATE_SOURCE_NOT_ENOUGH, processCount, result.size());
        }
        return result;
    }

    private boolean isAttributable(MesProScheduleOrderDO scheduleOrder) {
        if (scheduleOrder == null) {
            return false;
        }
        Integer status = scheduleOrder.getStatus();
        return !java.util.Objects.equals(status, MesProScheduleOrderStatusEnum.CANCELED.getStatus())
                && !java.util.Objects.equals(status, MesProScheduleOrderStatusEnum.FINISHED.getStatus());
    }

    private MesProTaskDO resolveTargetTask(Long scheduleOrderProcessId) {
        List<MesProTaskScheduleExtDO> extList = taskScheduleExtMapper.selectListByScheduleOrderProcessIds(List.of(scheduleOrderProcessId));
        if (extList == null || extList.isEmpty()) {
            return null;
        }
        List<Long> taskIds = extList.stream().map(MesProTaskScheduleExtDO::getTaskId).distinct().toList();
        List<MesProTaskDO> tasks = taskIds.isEmpty() ? Collections.emptyList() : taskMapper.selectListByIds(taskIds);
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    private byte[] buildSimulatedWorkbook(List<SimulatedFeedbackSource> sources, AdminUserDO currentUser, String sheetName) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(sheetName);
            var header = sheet.createRow(0);
            for (String headerName : REQUIRED_HEADERS) {
                header.createCell(header.getPhysicalNumberOfCells()).setCellValue(headerName);
            }
            String userDisplayName = StrUtil.blankToDefault(currentUser.getNickname(), currentUser.getUsername());
            for (int index = 0; index < sources.size(); index++) {
                writeSimulatedWorkbookRow(sheet.createRow(index + 1), sources.get(index), currentUser, userDisplayName);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw exception(PRO_FEEDBACK_SIMULATE_WORKBOOK_BUILD_FAILED, ex.getMessage());
        }
    }

    private void writeSimulatedWorkbookRow(org.apache.poi.ss.usermodel.Row row, SimulatedFeedbackSource source,
                                           AdminUserDO currentUser, String userDisplayName) {
        BigDecimal feedbackQuantity = resolveSimulatedFeedbackQuantity(source.scheduleOrderProcess());
        row.createCell(0).setCellValue(LocalDateTime.now().format(FEEDBACK_TIME_FORMATTER));
        row.createCell(1).setCellValue(currentUser.getUsername());
        row.createCell(2).setCellValue(userDisplayName);
        row.createCell(3).setCellValue(currentUser.getUsername());
        row.createCell(4).setCellValue(source.scheduleOrder().getErpWorkOrderCode());
        row.createCell(5).setCellValue("模拟报工");
        row.createCell(6).setCellValue(source.process().getName());
        row.createCell(7).setCellValue(source.task().getCode());
        row.createCell(8).setCellValue(source.item().getCode());
        row.createCell(9).setCellValue(StrUtil.blankToDefault(source.item().getName(), source.item().getCode()));
        row.createCell(10).setCellValue(StrUtil.blankToDefault(source.item().getSpecification(), ""));
        row.createCell(11).setCellValue("");
        row.createCell(12).setCellValue(source.process().getCode());
        row.createCell(13).setCellValue(source.process().getName());
        row.createCell(14).setCellValue("模拟报工");
        row.createCell(15).setCellValue(feedbackQuantity.toPlainString());
        row.createCell(24).setCellValue("模拟报工");
    }

    private BigDecimal resolveSimulatedFeedbackQuantity(MesProScheduleOrderProcessDO scheduleOrderProcess) {
        return BigDecimal.valueOf(ThreadLocalRandom.current()
                .nextInt(MIN_SIMULATED_FEEDBACK_QUANTITY, MAX_SIMULATED_FEEDBACK_QUANTITY + 1));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw exception(PRO_FEEDBACK_IMPORT_FILE_EMPTY);
        }
        String originalFilename = StrUtil.blankToDefault(file.getOriginalFilename(), "");
        if (!StrUtil.endWithIgnoreCase(originalFilename, ".xlsx")) {
            throw exception(PRO_FEEDBACK_IMPORT_FILE_TYPE_INVALID);
        }
    }

    private byte[] getFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception ex) {
            throw exception(PRO_FEEDBACK_IMPORT_FILE_EMPTY);
        }
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String buildTraceRemark(ThirdPartyFeedbackExcelRow row) {
        return StrUtil.format("third-party import file row: sheet={}, row={}, userName={}, resourceGroup={}, resource={}, mold={}, dept={}",
                row.sheetName(), row.rowNo(), row.feedbackUserName(), row.resourceGroup(), row.resourceName(), row.moldCode(), row.department());
    }

    private String buildTraceRemark(DirectWorkReportExcelRow row) {
        return StrUtil.format("direct work report import file row: sheet={}, row={}, userName={}, dept={}",
                row.sheetName(), row.rowNo(), row.feedbackUserName(), row.department());
    }

    private ThirdPartyFeedbackImportPayload toPayload(ThirdPartyFeedbackExcelRow row) {
        ThirdPartyFeedbackImportPayload payload = new ThirdPartyFeedbackImportPayload();
        payload.setSheetName(row.sheetName());
        payload.setRowNo(row.rowNo());
        payload.setFeedbackTime(row.feedbackTime());
        payload.setFeedbackUserCode(row.feedbackUserCode());
        payload.setFeedbackUserName(row.feedbackUserName());
        payload.setApproverName(row.approverName());
        payload.setWorkOrderCode(row.workOrderCode());
        payload.setResourceGroup(row.resourceGroup());
        payload.setResourceName(row.resourceName());
        payload.setTaskCode(row.taskCode());
        payload.setItemCode(row.itemCode());
        payload.setItemName(row.itemName());
        payload.setSpecification(row.specification());
        payload.setMoldCode(row.moldCode());
        payload.setProcessCode(row.processCode());
        payload.setProcessName(row.processName());
        payload.setDepartment(row.department());
        payload.setFeedbackQuantity(row.feedbackQuantity());
        return payload;
    }

    private ThirdPartyFeedbackImportPayload toPayload(DirectWorkReportExcelRow row) {
        ThirdPartyFeedbackImportPayload payload = new ThirdPartyFeedbackImportPayload();
        payload.setSheetName(row.sheetName());
        payload.setRowNo(row.rowNo());
        payload.setFeedbackTime(row.feedbackTime());
        payload.setFeedbackUserCode(row.feedbackUserCode());
        payload.setFeedbackUserName(row.feedbackUserName());
        payload.setApproverName(row.approverName());
        payload.setWorkOrderCode(row.workOrderCode());
        payload.setResourceGroup(row.department());
        payload.setResourceName(row.processName());
        payload.setTaskCode(row.taskCode());
        payload.setItemCode(row.itemCode());
        payload.setItemName(row.itemName());
        payload.setProcessCode(row.processCode());
        payload.setProcessName(row.processName());
        payload.setDepartment(row.department());
        payload.setFeedbackQuantity(row.feedbackQuantity());
        return payload;
    }

    private UniqueWorkOrderLoadResult loadUniqueWorkOrders(List<DirectWorkReportExcelRow> rows) {
        List<String> codes = rows.stream().map(DirectWorkReportExcelRow::workOrderCode).distinct().toList();
        Map<String, List<MesProWorkOrderDO>> grouped = workOrderMapper.selectListByCodes(codes).stream()
                .collect(Collectors.groupingBy(MesProWorkOrderDO::getCode, LinkedHashMap::new, Collectors.toList()));
        Map<String, MesProWorkOrderDO> result = new LinkedHashMap<>();
        Set<String> duplicatedCodes = grouped.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        for (DirectWorkReportExcelRow row : rows) {
            List<MesProWorkOrderDO> matches = grouped.getOrDefault(row.workOrderCode(), List.of());
            if (matches.isEmpty()) {
                continue;
            }
            if (matches.size() > 1) {
                continue;
            }
            result.put(row.workOrderCode(), matches.get(0));
        }
        return new UniqueWorkOrderLoadResult(result, duplicatedCodes);
    }

    private Map<Long, MesMdItemDO> loadWorkOrderItems(Map<String, MesProWorkOrderDO> workOrderMap) {
        List<Long> itemIds = workOrderMap.values().stream()
                .map(MesProWorkOrderDO::getProductId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return itemMapper.selectListByIds(itemIds).stream()
                .collect(Collectors.toMap(MesMdItemDO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private UniqueScheduleOrderLoadResult loadUniqueScheduleOrders(List<DirectWorkReportExcelRow> rows,
                                                                   Map<String, MesProWorkOrderDO> workOrderMap) {
        if (workOrderMap.isEmpty()) {
            return new UniqueScheduleOrderLoadResult(Collections.emptyMap(), Collections.emptySet());
        }
        Map<Long, List<MesProScheduleOrderDO>> grouped = scheduleOrderMapper.selectEffectiveListByWorkOrderIds(
                        workOrderMap.values().stream().map(MesProWorkOrderDO::getId).distinct().toList()).stream()
                .collect(Collectors.groupingBy(MesProScheduleOrderDO::getWorkOrderId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, MesProScheduleOrderDO> result = new LinkedHashMap<>();
        Set<Long> duplicatedWorkOrderIds = grouped.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        for (DirectWorkReportExcelRow row : rows) {
            MesProWorkOrderDO workOrder = workOrderMap.get(row.workOrderCode());
            List<MesProScheduleOrderDO> matches = grouped.getOrDefault(workOrder.getId(), List.of());
            if (matches.isEmpty()) {
                continue;
            }
            if (matches.size() > 1) {
                continue;
            }
            result.put(workOrder.getId(), matches.get(0));
        }
        return new UniqueScheduleOrderLoadResult(result, duplicatedWorkOrderIds);
    }

    private Map<Long, List<MesProScheduleOrderProcessDO>> loadScheduleProcesses(Map<Long, MesProScheduleOrderDO> scheduleOrderMap) {
        if (scheduleOrderMap.isEmpty()) {
            return Collections.emptyMap();
        }
        return scheduleOrderProcessMapper.selectListByScheduleOrderIds(
                        scheduleOrderMap.values().stream().map(MesProScheduleOrderDO::getId).toList()).stream()
                .collect(Collectors.groupingBy(MesProScheduleOrderProcessDO::getScheduleOrderId,
                        LinkedHashMap::new, Collectors.toList()));
    }

    private Map<Long, Long> resolveScheduleProcessIdentityProcessIdMap(
            Map<Long, List<MesProScheduleOrderProcessDO>> processMap,
            Map<Long, MesProScheduleOrderDO> scheduleOrderById) {
        Map<Long, Long> result = new LinkedHashMap<>();
        for (MesProScheduleOrderProcessDO scheduleOrderProcess : processMap.values().stream().flatMap(List::stream).toList()) {
            if (scheduleOrderProcess.getId() == null) {
                continue;
            }
            MesProScheduleOrderDO scheduleOrder = scheduleOrderById.get(scheduleOrderProcess.getScheduleOrderId());
            Long identityProcessId = resolveScheduleProcessIdentityProcessId(scheduleOrderProcess, scheduleOrder);
            if (identityProcessId != null) {
                result.put(scheduleOrderProcess.getId(), identityProcessId);
            }
        }
        return result;
    }

    private Long resolveScheduleProcessIdentityProcessId(MesProScheduleOrderProcessDO scheduleOrderProcess,
                                                         MesProScheduleOrderDO scheduleOrder) {
        if (scheduleOrderProcess.getRouteProcessId() == null) {
            return scheduleOrderProcess.getProcessId();
        }
        Long routeId = scheduleOrder == null ? null : scheduleOrder.getRouteId();
        MesProRouteProcessDO routeProcess = routeProcessService.resolveFrozenRouteProcess(
                scheduleOrderProcess.getRouteProcessId(), routeId, scheduleOrderProcess.getProcessId());
        return routeProcess.getProcessId();
    }

    private Map<Long, MesProProcessDO> loadProcesses(Map<Long, List<MesProScheduleOrderProcessDO>> processMap,
                                                     Map<Long, Long> scheduleProcessIdentityProcessIdMap) {
        List<Long> processIds = processMap.values().stream()
                .flatMap(List::stream)
                .flatMap(process -> java.util.stream.Stream.of(
                        process.getProcessId(),
                        scheduleProcessIdentityProcessIdMap.get(process.getId())))
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (processIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return processMapper.selectListByIds(processIds).stream()
                .collect(Collectors.toMap(MesProProcessDO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private void fillDirectWorkReportSourceDetail(ThirdPartyFeedbackImportResult.DirectWorkReportDetail detail,
                                                  DirectWorkReportExcelRow row) {
        detail.setSheetName(row.sheetName());
        detail.setRowNo(row.rowNo());
        detail.setWorkOrderCode(row.workOrderCode());
        detail.setProcessCode(row.processCode());
        detail.setProcessName(row.processName());
        detail.setFeedbackUserCode(row.feedbackUserCode());
        detail.setFeedbackUserName(row.feedbackUserName());
        detail.setApproverName(row.approverName());
    }

    private ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning buildDirectWorkReportSkipWarning(
            DirectWorkReportExcelRow row,
            MesProWorkOrderDO workOrder,
            MesProScheduleOrderDO scheduleOrder,
            MesProScheduleOrderProcessDO scheduleOrderProcess,
            Map<Long, MesMdItemDO> itemMap,
            String reasonCode,
            String reason) {
        MesMdItemDO item = workOrder == null ? null : itemMap.get(workOrder.getProductId());
        ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning warning = new ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning();
        warning.setSheetName(row.sheetName());
        warning.setRowNo(row.rowNo());
        warning.setWorkOrderCode(workOrder == null ? row.workOrderCode() : workOrder.getCode());
        warning.setScheduleOrderCode(scheduleOrder == null ? null : scheduleOrder.getCode());
        warning.setProductCode(item == null ? row.itemCode() : item.getCode());
        warning.setProductName(item == null ? row.itemName() : item.getName());
        warning.setProcessCode(scheduleOrderProcess == null ? row.processCode()
                : StrUtil.blankToDefault(scheduleOrderProcess.getProcessCode(), row.processCode()));
        warning.setProcessName(scheduleOrderProcess == null ? row.processName()
                : StrUtil.blankToDefault(scheduleOrderProcess.getProcessName(), row.processName()));
        warning.setFeedbackUserCode(row.feedbackUserCode());
        warning.setFeedbackUserName(row.feedbackUserName());
        warning.setApproverName(row.approverName());
        warning.setFeedbackQuantity(row.feedbackQuantity());
        if (scheduleOrderProcess != null) {
            warning.setReportedQuantity(defaultZero(scheduleOrderProcess.getReportedQuantity()));
            warning.setRemainingQuantity(defaultZero(scheduleOrderProcess.getRemainingQuantity()));
            warning.setProgressPercent(clampProgressPercent(scheduleOrderProcess.getProgressPercent()));
        }
        warning.setReasonCode(reasonCode);
        warning.setReason(reason);
        return warning;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal clampProgressPercent(BigDecimal value) {
        BigDecimal progressPercent = defaultZero(value).setScale(6, java.math.RoundingMode.HALF_UP);
        if (progressPercent.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(6);
        }
        BigDecimal maxProgressPercent = new BigDecimal("100.000000");
        if (progressPercent.compareTo(maxProgressPercent) > 0) {
            return maxProgressPercent;
        }
        return progressPercent;
    }

    private DirectProgressResolution resolveDirectProgressTarget(DirectWorkReportExcelRow row,
                                                                 Map<String, MesProWorkOrderDO> workOrderMap,
                                                                 Map<Long, MesProScheduleOrderDO> scheduleOrderMap,
                                                                 Map<Long, List<MesProScheduleOrderProcessDO>> processMap,
                                                                 Map<Long, MesProProcessDO> processDoMap,
                                                                 Map<Long, MesMdItemDO> itemMap,
                                                                 Map<Long, Long> scheduleProcessIdentityProcessIdMap,
                                                                 Set<Long> duplicatedScheduleWorkOrderIds) {
        MesProWorkOrderDO workOrder = workOrderMap.get(row.workOrderCode());
        if (workOrder == null) {
            return DirectProgressResolution.skip(buildDirectWorkReportSkipWarning(row, null, null, null, itemMap,
                    "WORK_ORDER_NOT_FOUND", "生产工单未匹配到唯一系统工单，本行未更新排产进度。"));
        }
        MesProScheduleOrderDO scheduleOrder = scheduleOrderMap.get(workOrder.getId());
        if (scheduleOrder == null) {
            boolean duplicated = duplicatedScheduleWorkOrderIds.contains(workOrder.getId());
            return DirectProgressResolution.skip(buildDirectWorkReportSkipWarning(row, workOrder, null, null, itemMap,
                    duplicated ? "SCHEDULE_ORDER_NOT_UNIQUE" : "SCHEDULE_ORDER_NOT_FOUND",
                    duplicated ? "生产工单存在多个有效排产工单，无法唯一更新排产进度。"
                            : "生产工单尚未生成有效排产工单，本行未更新排产进度。"));
        }
        List<MesProScheduleOrderProcessDO> processCodeCandidates = processMap.getOrDefault(scheduleOrder.getId(), List.of()).stream()
                .filter(process -> {
                    Long identityProcessId = scheduleProcessIdentityProcessIdMap.getOrDefault(
                            process.getId(), process.getProcessId());
                    MesProProcessDO processDO = processDoMap.get(identityProcessId);
                    return (processDO != null && StrUtil.equals(processDO.getCode(), row.processCode()))
                            || StrUtil.equals(process.getProcessCode(), row.processCode());
                })
                .toList();
        if (processCodeCandidates.isEmpty()) {
            return DirectProgressResolution.skip(buildDirectWorkReportSkipWarning(row, workOrder, scheduleOrder, null,
                    itemMap, "PROCESS_NOT_FOUND", "排产工单未找到工序编码匹配的工序，本行未更新排产进度。"));
        }
        List<MesProScheduleOrderProcessDO> candidates = processCodeCandidates.stream()
                .filter(process -> Boolean.TRUE.equals(process.getEnabled()))
                .toList();
        if (candidates.isEmpty()) {
            return DirectProgressResolution.skip(buildDirectWorkReportSkipWarning(row, workOrder, scheduleOrder,
                    processCodeCandidates.get(0), itemMap, "PROCESS_NOT_ENABLED",
                    "排产工单中的该工序未启用，本行未更新排产进度。"));
        }
        if (candidates.size() > 1) {
            return DirectProgressResolution.skip(buildDirectWorkReportSkipWarning(row, workOrder, scheduleOrder,
                    candidates.get(0), itemMap, "PROCESS_NOT_UNIQUE",
                    "排产工单存在多个可匹配工序，无法唯一更新排产进度。"));
        }
        if (row.feedbackQuantity() == null || row.feedbackQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return DirectProgressResolution.skip(buildDirectWorkReportSkipWarning(row, workOrder, scheduleOrder,
                    candidates.get(0), itemMap, "FEEDBACK_QUANTITY_INVALID",
                    "报工数量必须大于 0，本行未更新排产进度。"));
        }
        MesProScheduleOrderProcessDO scheduleOrderProcess = candidates.get(0);
        Long scheduleProcessIdentityProcessId = scheduleProcessIdentityProcessIdMap.getOrDefault(
                scheduleOrderProcess.getId(), scheduleOrderProcess.getProcessId());
        return DirectProgressResolution.target(
                new DirectProgressTarget(workOrder, scheduleOrder, scheduleOrderProcess, scheduleProcessIdentityProcessId));
    }

    private boolean hasEnoughRemainingQuantity(DirectWorkReportExcelRow row,
                                               MesProScheduleOrderProcessDO scheduleOrderProcess) {
        BigDecimal remainingQuantity = normalizeQuantity(scheduleOrderProcess.getRemainingQuantity());
        return remainingQuantity.compareTo(BigDecimal.ZERO) > 0
                && normalizeQuantity(row.feedbackQuantity()).compareTo(remainingQuantity) <= 0;
    }

    private DirectFeedbackContext buildDirectFeedbackContext(DirectWorkReportExcelRow row,
                                                             DirectProgressTarget target,
                                                             Map<Long, MesMdItemDO> itemMap) {
        MesProTaskDO task = resolveDirectFeedbackTask(row, target);
        if (task == null) {
            return DirectFeedbackContext.skip(buildDirectWorkReportSkipWarning(row, target.workOrder(),
                    target.scheduleOrder(), target.scheduleOrderProcess(), itemMap, "ACTIVE_TASK_NOT_FOUND",
                    "未找到与排产工序唯一匹配的未完成生产任务，本行未生成正式报工。"));
        }
        DirectUserResolution feedbackUser = resolveDirectFeedbackUser(row);
        if (feedbackUser.user() == null) {
            return DirectFeedbackContext.skip(buildDirectWorkReportSkipWarning(row, target.workOrder(),
                    target.scheduleOrder(), target.scheduleOrderProcess(), itemMap, feedbackUser.reasonCode(),
                    feedbackUser.reason()));
        }
        DirectUserResolution approveUser = resolveDirectApproveUser(row);
        if (approveUser.user() == null) {
            return DirectFeedbackContext.skip(buildDirectWorkReportSkipWarning(row, target.workOrder(),
                    target.scheduleOrder(), target.scheduleOrderProcess(), itemMap, approveUser.reasonCode(),
                    approveUser.reason()));
        }
        MesProRouteProcessDO routeProcess = routeProcessService.resolveFrozenRouteProcess(
                target.scheduleOrderProcess().getRouteProcessId(), target.scheduleOrder().getRouteId(),
                target.scheduleOrderProcess().getProcessId());
        boolean checkFlag = Boolean.TRUE.equals(routeProcess.getCheckFlag());
        String feedbackCode = autoCodeRecordService.generateAutoCode(MesMdAutoCodeRuleCodeEnum.PRO_FEEDBACK_CODE.getCode());
        MesProFeedbackSaveReqVO req = new MesProFeedbackSaveReqVO();
        req.setCode(feedbackCode);
        req.setType(MesProFeedbackTypeEnum.SELF.getType());
        req.setWorkstationId(task.getWorkstationId());
        req.setRouteId(task.getRouteId());
        req.setProcessId(routeProcess.getProcessId());
        req.setWorkOrderId(task.getWorkOrderId());
        req.setTaskId(task.getId());
        req.setItemId(task.getItemId());
        req.setScheduledQuantity(task.getQuantity());
        req.setFeedbackQuantity(row.feedbackQuantity());
        req.setFeedbackUserId(feedbackUser.user().getId());
        req.setFeedbackTime(row.feedbackTime());
        req.setApproveUserId(approveUser.user().getId());
        req.setRemark(buildTraceRemark(row));
        if (checkFlag) {
            req.setQualifiedQuantity(BigDecimal.ZERO);
            req.setUnqualifiedQuantity(BigDecimal.ZERO);
            req.setUncheckQuantity(row.feedbackQuantity());
        } else {
            req.setQualifiedQuantity(row.feedbackQuantity());
            req.setUnqualifiedQuantity(BigDecimal.ZERO);
            req.setUncheckQuantity(BigDecimal.ZERO);
        }
        req.setLaborScrapQuantity(BigDecimal.ZERO);
        req.setMaterialScrapQuantity(BigDecimal.ZERO);
        req.setOtherScrapQuantity(BigDecimal.ZERO);
        req.setScheduleOrderId(target.scheduleOrder().getId());
        req.setScheduleOrderProcessId(target.scheduleOrderProcess().getId());
        return DirectFeedbackContext.success(feedbackCode, req);
    }

    private MesProTaskDO resolveDirectFeedbackTask(DirectWorkReportExcelRow row,
                                                   DirectProgressTarget target) {
        List<MesProTaskScheduleExtDO> extList = taskScheduleExtMapper.selectListByScheduleOrderProcessIds(
                List.of(target.scheduleOrderProcess().getId()));
        if (extList == null || extList.isEmpty()) {
            return null;
        }
        List<Long> taskIds = extList.stream()
                .map(MesProTaskScheduleExtDO::getTaskId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<MesProTaskDO> tasks = taskIds.isEmpty() ? List.of() : taskMapper.selectListByIds(taskIds);
        List<MesProTaskDO> candidates = tasks.stream()
                .filter(Objects::nonNull)
                .filter(task -> !MesProTaskStatusEnum.isEndStatus(task.getStatus()))
                .filter(task -> Objects.equals(task.getWorkOrderId(), target.scheduleOrder().getWorkOrderId()))
                .filter(task -> task.getWorkstationId() != null)
                .filter(task -> task.getRouteId() != null)
                .filter(task -> task.getItemId() != null)
                .filter(task -> task.getQuantity() != null)
                .filter(task -> matchesDirectFeedbackTaskProcess(task, target))
                .toList();
        List<MesProTaskDO> taskCodeMatches = candidates.stream()
                .filter(task -> StrUtil.equals(task.getCode(), row.taskCode()))
                .toList();
        if (taskCodeMatches.size() == 1) {
            return taskCodeMatches.get(0);
        }
        if (taskCodeMatches.size() > 1) {
            return null;
        }
        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    private boolean matchesDirectFeedbackTaskProcess(MesProTaskDO task,
                                                     DirectProgressTarget target) {
        Long taskProcessId = task.getProcessId();
        Long scheduleProcessId = target.scheduleProcessIdentityProcessId() == null
                ? target.scheduleOrderProcess().getProcessId() : target.scheduleProcessIdentityProcessId();
        if (taskProcessId == null || scheduleProcessId == null) {
            return false;
        }
        if (taskProcessId <= 0
                && target.scheduleOrderProcess().getProcessId() != null
                && target.scheduleOrderProcess().getProcessId() <= 0) {
            return true;
        }
        List<Long> processIds = List.of(taskProcessId, scheduleProcessId).stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        Map<Long, Long> processIdentityMap = processIds.isEmpty() ? Map.of()
                : routeProcessService.getProcessIdentityMap(processIds);
        if (processIdentityMap == null) {
            processIdentityMap = Map.of();
        }
        Long taskIdentity = processIdentityMap.getOrDefault(taskProcessId, taskProcessId);
        Long scheduleIdentity = processIdentityMap.getOrDefault(scheduleProcessId, scheduleProcessId);
        return Objects.equals(taskIdentity, scheduleIdentity);
    }

    private DirectUserResolution resolveDirectFeedbackUser(DirectWorkReportExcelRow row) {
        if (StrUtil.isBlank(row.feedbackUserCode())) {
            return DirectUserResolution.fail("FEEDBACK_USER_NOT_FOUND", "报工人编码为空，本行未生成正式报工。");
        }
        AdminUserDO user = adminUserMapper.selectByUsername(row.feedbackUserCode());
        if (user == null) {
            return DirectUserResolution.fail("FEEDBACK_USER_NOT_FOUND",
                    "报工人编码未匹配到系统用户，本行未生成正式报工。");
        }
        return DirectUserResolution.success(user);
    }

    private DirectUserResolution resolveDirectApproveUser(DirectWorkReportExcelRow row) {
        if (StrUtil.isBlank(row.approverName())) {
            return DirectUserResolution.fail("APPROVER_NOT_FOUND", "审批人为空，本行未生成正式报工。");
        }
        AdminUserDO approverByUsername = adminUserMapper.selectByUsername(row.approverName());
        if (approverByUsername != null) {
            return DirectUserResolution.success(approverByUsername);
        }
        List<AdminUserDO> approvers = adminUserMapper.selectListByNicknamesExact(List.of(row.approverName()));
        if (approvers == null || approvers.isEmpty()) {
            return DirectUserResolution.fail("APPROVER_NOT_FOUND",
                    "审批人未匹配到唯一系统用户，本行未生成正式报工。");
        }
        if (approvers.size() > 1) {
            return DirectUserResolution.fail("APPROVER_NOT_UNIQUE",
                    "审批人匹配到多个系统用户，本行未生成正式报工。");
        }
        return DirectUserResolution.success(approvers.get(0));
    }

    private String resolveDirectProgressWarningCode(DirectWorkReportExcelRow row,
                                                    MesProScheduleOrderProcessDO scheduleOrderProcess) {
        BigDecimal remainingQuantity = defaultZero(scheduleOrderProcess.getRemainingQuantity());
        if (row.feedbackQuantity() != null && row.feedbackQuantity().compareTo(remainingQuantity) > 0) {
            return "OVER_REMAINING_QUANTITY";
        }
        return null;
    }

    private String buildDirectProgressWarningMessage(String warningCode,
                                                     MesProScheduleOrderProcessDO scheduleOrderProcess) {
        if (!StrUtil.equals("OVER_REMAINING_QUANTITY", warningCode)) {
            return null;
        }
        return "本次报工数量超过当前剩余数量，已按第三方报工结果累计排产进度。当前剩余数量："
                + defaultZero(scheduleOrderProcess.getRemainingQuantity()).stripTrailingZeros().toPlainString();
    }

    private ThirdPartyFeedbackImportResult.DirectWorkReportDetail buildDirectProgressDetail(
            DirectWorkReportExcelRow row,
            DirectProgressTarget target,
            Map<Long, MesMdItemDO> itemMap,
            MesProScheduleOrderProcessDO beforeProgress,
            MesProScheduleOrderProcessDO afterProgress,
            Long importRecordId,
            String warningCode,
            String warningMessage) {
        MesMdItemDO item = itemMap.get(target.workOrder().getProductId());
        ThirdPartyFeedbackImportResult.DirectWorkReportDetail detail = new ThirdPartyFeedbackImportResult.DirectWorkReportDetail();
        detail.setWorkOrderCode(target.workOrder().getCode());
        detail.setScheduleOrderCode(target.scheduleOrder().getCode());
        detail.setProductCode(item == null ? row.itemCode() : item.getCode());
        detail.setProductName(item == null ? row.itemName() : item.getName());
        detail.setProcessCode(StrUtil.blankToDefault(beforeProgress.getProcessCode(), row.processCode()));
        detail.setProcessName(StrUtil.blankToDefault(beforeProgress.getProcessName(), row.processName()));
        fillDirectWorkReportSourceDetail(detail, row);
        detail.setAttributionStatus(ATTRIBUTION_STATUS_ATTRIBUTED);
        detail.setFeedbackQuantity(row.feedbackQuantity());
        detail.setBeforeReportedQuantity(normalizeQuantity(beforeProgress.getReportedQuantity()));
        detail.setAfterReportedQuantity(normalizeQuantity(afterProgress.getReportedQuantity()));
        detail.setReportedQuantityDelta(detail.getAfterReportedQuantity().subtract(detail.getBeforeReportedQuantity()));
        detail.setBeforeProgressPercent(clampProgressPercent(beforeProgress.getProgressPercent()));
        detail.setAfterProgressPercent(clampProgressPercent(afterProgress.getProgressPercent()));
        detail.setProgressDeltaPercent(detail.getAfterProgressPercent().subtract(detail.getBeforeProgressPercent()));
        detail.setResultCode(StrUtil.blankToDefault(warningCode, "DIRECT_PROGRESS_UPDATED"));
        detail.setResultMessage(StrUtil.blankToDefault(warningMessage, "已更新排产进度。"));
        detail.setImportRecordId(importRecordId);
        return detail;
    }

    private Map<Long, MesProScheduleOrderProcessDO> recalculateDirectProgressForScheduleOrder(
            MesProScheduleOrderDO scheduleOrder) {
        MesProScheduleOrderDO currentScheduleOrder = scheduleOrderMapper.selectById(scheduleOrder.getId());
        if (currentScheduleOrder == null) {
            currentScheduleOrder = scheduleOrder;
        }
        List<MesProScheduleOrderProcessDO> processes =
                scheduleOrderProcessMapper.selectListByScheduleOrderId(scheduleOrder.getId());
        Map<Long, BigDecimal> completedByProcessId = sumFeedbackProgressByScheduleOrderProcessId(
                feedbackMapper.selectProgressListByScheduleOrderId(scheduleOrder.getId()));
        mergeQuantity(completedByProcessId, sumDirectProgressByScheduleOrderProcessId(scheduleOrder.getId()));
        Map<Long, MesProScheduleOrderProcessDO> result = new LinkedHashMap<>();
        for (MesProScheduleOrderProcessDO process : processes) {
            BigDecimal plannedQuantity = normalizeQuantity(process.getPlannedQuantity());
            BigDecimal reportedQuantity = completedByProcessId.getOrDefault(process.getId(), BigDecimal.ZERO).setScale(6);
            BigDecimal remainingQuantity = plannedQuantity.subtract(reportedQuantity).max(BigDecimal.ZERO).setScale(6);
            BigDecimal progressPercent = calculateProgressPercent(reportedQuantity, plannedQuantity);
            process.setReportedQuantity(reportedQuantity);
            process.setRemainingQuantity(remainingQuantity);
            process.setProgressPercent(progressPercent);
            scheduleOrderProcessMapper.updateProgress(process.getId(), reportedQuantity, remainingQuantity, progressPercent);
            result.put(process.getId(), process);
        }
        ProgressSummary summary = calculateProcessAggregateProgressSummary(
                resolveScheduleOrderTotalQuantity(currentScheduleOrder), processes);
        scheduleOrderMapper.updateProgressSummary(currentScheduleOrder.getId(), summary.totalQuantity(),
                summary.completedQuantity(), summary.uncompletedQuantity(), summary.progressPercent(),
                resolveDirectProgressStatus(currentScheduleOrder, summary));
        return result;
    }

    private Map<Long, BigDecimal> sumFeedbackProgressByScheduleOrderProcessId(List<MesProFeedbackDO> feedbackList) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        if (feedbackList == null) {
            return result;
        }
        Set<Integer> includedStatuses = Set.of(
                MesProFeedbackStatusEnum.FINISHED.getStatus(),
                MesProFeedbackStatusEnum.APPROVING.getStatus(),
                MesProFeedbackStatusEnum.UNCHECK.getStatus()
        );
        for (MesProFeedbackDO feedback : feedbackList) {
            if (feedback.getScheduleOrderProcessId() == null || !includedStatuses.contains(feedback.getStatus())) {
                continue;
            }
            result.merge(feedback.getScheduleOrderProcessId(), normalizeQuantity(feedback.getFeedbackQuantity()),
                    BigDecimal::add);
        }
        return result;
    }

    private Map<Long, BigDecimal> sumDirectProgressByScheduleOrderProcessId(Long scheduleOrderId) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        List<MesProFeedbackImportRecordDO> records =
                importRecordMapper.selectAppliedDirectProgressListByScheduleOrderId(scheduleOrderId);
        if (records == null) {
            return result;
        }
        for (MesProFeedbackImportRecordDO record : records) {
            if (record.getScheduleOrderProcessId() == null) {
                continue;
            }
            result.merge(record.getScheduleOrderProcessId(), normalizeQuantity(record.getProgressQuantity()),
                    BigDecimal::add);
        }
        return result;
    }

    private void mergeQuantity(Map<Long, BigDecimal> target, Map<Long, BigDecimal> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        source.forEach((key, value) -> target.merge(key, value, BigDecimal::add));
    }

    private MesProScheduleOrderProcessDO buildDirectProgressAfterSnapshot(MesProScheduleOrderProcessDO beforeProgress,
                                                                          BigDecimal feedbackQuantity) {
        BigDecimal plannedQuantity = normalizeQuantity(beforeProgress.getPlannedQuantity());
        BigDecimal reportedQuantity = normalizeQuantity(beforeProgress.getReportedQuantity())
                .add(normalizeQuantity(feedbackQuantity));
        BigDecimal remainingQuantity = plannedQuantity.subtract(reportedQuantity).max(BigDecimal.ZERO).setScale(6);
        BigDecimal progressPercent = calculateProgressPercent(reportedQuantity, plannedQuantity);
        return MesProScheduleOrderProcessDO.builder()
                .id(beforeProgress.getId())
                .scheduleOrderId(beforeProgress.getScheduleOrderId())
                .routeProcessId(beforeProgress.getRouteProcessId())
                .processId(beforeProgress.getProcessId())
                .processCode(beforeProgress.getProcessCode())
                .processName(beforeProgress.getProcessName())
                .enabled(beforeProgress.getEnabled())
                .plannedQuantity(beforeProgress.getPlannedQuantity())
                .reportedQuantity(reportedQuantity)
                .remainingQuantity(remainingQuantity)
                .progressPercent(progressPercent)
                .build();
    }

    private MesProScheduleOrderProcessDO copyDirectProgressSnapshot(MesProScheduleOrderProcessDO source) {
        return MesProScheduleOrderProcessDO.builder()
                .id(source.getId())
                .scheduleOrderId(source.getScheduleOrderId())
                .routeProcessId(source.getRouteProcessId())
                .processId(source.getProcessId())
                .processCode(source.getProcessCode())
                .processName(source.getProcessName())
                .enabled(source.getEnabled())
                .plannedQuantity(source.getPlannedQuantity())
                .reportedQuantity(source.getReportedQuantity())
                .remainingQuantity(source.getRemainingQuantity())
                .progressPercent(source.getProgressPercent())
                .build();
    }

    private BigDecimal normalizeQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(6) : value.setScale(6, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal calculateProgressPercent(BigDecimal completedQuantity, BigDecimal totalQuantity) {
        BigDecimal total = normalizeQuantity(totalQuantity);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(6);
        }
        return normalizeQuantity(completedQuantity).min(total).multiply(BigDecimal.valueOf(100))
                .divide(total, 6, java.math.RoundingMode.HALF_UP)
                .min(new BigDecimal("100.000000"))
                .setScale(6, java.math.RoundingMode.HALF_UP);
    }

    private ProgressSummary calculateProcessAggregateProgressSummary(BigDecimal scheduleOrderQuantity,
                                                                     List<MesProScheduleOrderProcessDO> processes) {
        List<MesProScheduleOrderProcessDO> enabledProcesses = processes.stream()
                .filter(process -> Boolean.TRUE.equals(process.getEnabled()))
                .toList();
        if (enabledProcesses.isEmpty()) {
            BigDecimal totalQuantity = normalizeQuantity(scheduleOrderQuantity);
            return new ProgressSummary(totalQuantity, BigDecimal.ZERO.setScale(6), totalQuantity,
                    BigDecimal.ZERO.setScale(6));
        }
        BigDecimal totalQuantity = BigDecimal.ZERO.setScale(6);
        BigDecimal completedQuantity = BigDecimal.ZERO.setScale(6);
        BigDecimal uncompletedQuantity = BigDecimal.ZERO.setScale(6);
        for (MesProScheduleOrderProcessDO process : enabledProcesses) {
            BigDecimal plannedQuantity = normalizeQuantity(process.getPlannedQuantity());
            BigDecimal reportedQuantity = normalizeQuantity(process.getReportedQuantity());
            totalQuantity = totalQuantity.add(plannedQuantity);
            completedQuantity = completedQuantity.add(reportedQuantity.min(plannedQuantity));
            uncompletedQuantity = uncompletedQuantity.add(plannedQuantity.subtract(reportedQuantity).max(BigDecimal.ZERO));
        }
        if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            totalQuantity = normalizeQuantity(scheduleOrderQuantity);
            uncompletedQuantity = totalQuantity;
        }
        return new ProgressSummary(totalQuantity, completedQuantity, uncompletedQuantity,
                calculateProgressPercent(completedQuantity, totalQuantity));
    }

    private BigDecimal resolveScheduleOrderTotalQuantity(MesProScheduleOrderDO scheduleOrder) {
        BigDecimal totalQuantity = normalizeQuantity(scheduleOrder.getTotalQuantity());
        if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
            return totalQuantity;
        }
        return normalizeQuantity(scheduleOrder.getQuantity());
    }

    private Integer resolveDirectProgressStatus(MesProScheduleOrderDO scheduleOrder, ProgressSummary summary) {
        if (Objects.equals(scheduleOrder.getStatus(), MesProScheduleOrderStatusEnum.CANCELED.getStatus())) {
            return MesProScheduleOrderStatusEnum.CANCELED.getStatus();
        }
        if (summary.totalQuantity().compareTo(BigDecimal.ZERO) > 0
                && summary.completedQuantity().compareTo(summary.totalQuantity()) >= 0) {
            return MesProScheduleOrderStatusEnum.FINISHED.getStatus();
        }
        if (summary.completedQuantity().compareTo(BigDecimal.ZERO) > 0) {
            return MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus();
        }
        return scheduleOrder.getStatus();
    }

    private record SimulatedFeedbackSource(MesProScheduleOrderDO scheduleOrder,
                                           MesProScheduleOrderProcessDO scheduleOrderProcess,
                                           MesProWorkOrderDO workOrder,
                                           MesMdItemDO item,
                                           MesProProcessDO process,
                                           MesProTaskDO task) {
    }

    private record UniqueWorkOrderLoadResult(Map<String, MesProWorkOrderDO> uniqueWorkOrders,
                                             Set<String> duplicatedCodes) {
    }

    private record UniqueScheduleOrderLoadResult(Map<Long, MesProScheduleOrderDO> uniqueScheduleOrders,
                                                 Set<Long> duplicatedWorkOrderIds) {
    }

    private record DirectProgressTarget(MesProWorkOrderDO workOrder,
                                        MesProScheduleOrderDO scheduleOrder,
                                        MesProScheduleOrderProcessDO scheduleOrderProcess,
                                        Long scheduleProcessIdentityProcessId) {
    }

    private record ProgressSummary(BigDecimal totalQuantity,
                                   BigDecimal completedQuantity,
                                   BigDecimal uncompletedQuantity,
                                   BigDecimal progressPercent) {
    }

    private record DirectProgressResolution(DirectProgressTarget target,
                                            ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning skipWarning) {

        private static DirectProgressResolution target(DirectProgressTarget target) {
            return new DirectProgressResolution(target, null);
        }

        private static DirectProgressResolution skip(ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning warning) {
            return new DirectProgressResolution(null, warning);
        }
    }

    private record DirectFeedbackContext(String feedbackCode,
                                         MesProFeedbackSaveReqVO request,
                                         ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning skipWarning) {

        private static DirectFeedbackContext success(String feedbackCode, MesProFeedbackSaveReqVO request) {
            return new DirectFeedbackContext(feedbackCode, request, null);
        }

        private static DirectFeedbackContext skip(ThirdPartyFeedbackImportResult.DirectWorkReportSkipWarning warning) {
            return new DirectFeedbackContext(null, null, warning);
        }
    }

    private record DirectUserResolution(AdminUserDO user,
                                        String reasonCode,
                                        String reason) {

        private static DirectUserResolution success(AdminUserDO user) {
            return new DirectUserResolution(user, null, null);
        }

        private static DirectUserResolution fail(String reasonCode, String reason) {
            return new DirectUserResolution(null, reasonCode, reason);
        }
    }

}
