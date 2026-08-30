package cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormTemplateVersionDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormTemplateVersionMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkCellVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkFormCellsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkFormRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkPrefillItemVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkPrefillRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkRuleSaveItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkRuleVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkRulesSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkRulesSaveRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkRouteProcessVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkSourceFieldVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkPqcProcessVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkWorkbenchContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordRepeatRowGroupMappingSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordRepeatRowGroupMappingVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordRepeatRowGroupRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordRepeatRowGroupRecordVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordRepeatRowGroupSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordRepeatRowGroupSaveRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordRepeatRowGroupVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRuleVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRulesRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordRepeatRowGroupDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordCellLinkRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordRepeatRowGroupMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordCellRuleSupport;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
@Validated
public class MesProBatchRecordCellLinkServiceImpl implements MesProBatchRecordCellLinkService {

    private static final String SCOPE_TYPE_ROUTE_VERSION = "ROUTE_VERSION";
    private static final String SCOPE_TYPE_REPORT_SET = "REPORT_SET";
    private static final String SCOPE_TYPE_FORM_TEMPLATE_VERSION = "FORM_TEMPLATE_VERSION";
    private static final String FORM_TEMPLATE_REPORT_PREFIX = "FORMTPL:";
    private static final String SOURCE_TYPE_BATCH_RECORD_CELL = "BATCH_RECORD_CELL";
    private static final String SOURCE_TYPE_PRODUCTION_WORK_ORDER = "PRODUCTION_WORK_ORDER";
    private static final String SOURCE_TYPE_PROCESS_POOL_REPORT = "PROCESS_POOL_REPORT";
    private static final String SOURCE_TYPE_PRODUCTION_PICK_LIST = MesProductionPickListSourceService.SOURCE_TYPE;
    private static final String SOURCE_TYPE_PQC_AGGREGATE_DETAIL = "PQC_AGGREGATE_DETAIL";
    private static final String SOURCE_TYPE_PRODUCTION_LOSS = "PRODUCTION_LOSS";
    private static final String FORM_SLOT_TYPE_MAIN = "MAIN";
    private static final String FORM_SLOT_TYPE_PROCESS_INSPECTION = "PROCESS_INSPECTION";
    private static final String FORM_SLOT_TYPE_LOSS_REPORT = "LOSS_REPORT";
    private static final String PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID = "PRODUCTION_WORK_ORDER";
    private static final String PRODUCTION_WORK_ORDER_SOURCE_REPORT_NAME = "生产工单";
    private static final String PROCESS_POOL_REPORT_SOURCE_REPORT_ID = "PROCESS_POOL_REPORT";
    private static final String PROCESS_POOL_REPORT_SOURCE_REPORT_NAME = "报工数据";
    private static final String PQC_AGGREGATE_DETAIL_SOURCE_REPORT_ID = "PQC_AGGREGATE_DETAIL";
    private static final String PQC_AGGREGATE_DETAIL_SOURCE_REPORT_NAME = "一线PQC数据";
    private static final String WORK_ORDER_SOURCE_FIELD_BATCH_CODE = "batchCode";
    private static final String PROCESS_POOL_TEAM_DEVICE_STATUS_ENABLED = "ENABLED";
    private static final String PROCESS_POOL_DEVICE_SCOPE_SEPARATOR = "@device:";
    private static final String PROCESS_POOL_DEVICE_GROUP_SCOPE_SEPARATOR = "@deviceGroup:";
    private static final String OVERWRITE_POLICY_ONLY_WHEN_EMPTY = "ONLY_WHEN_EMPTY";
    private static final List<Integer> ACTIVE_EXECUTION_STATUSES = List.of(0, 1, 2, 3);
    private static final List<WorkOrderSourceField> PRODUCTION_WORK_ORDER_SOURCE_FIELDS = List.of(
            new WorkOrderSourceField("code", "生产工单编号", "STRING", MesProWorkOrderDO::getCode),
            new WorkOrderSourceField("name", "生产工单名称", "STRING", MesProWorkOrderDO::getName),
            new WorkOrderSourceField("batchCode", "生产批号", "STRING", MesProWorkOrderDO::getBatchCode),
            new WorkOrderSourceField("quantity", "生产数量", "NUMBER", MesProWorkOrderDO::getQuantity),
            new WorkOrderSourceField("orderSourceCode", "生产订单号", "STRING", MesProWorkOrderDO::getOrderSourceCode),
            new WorkOrderSourceField("workshopName", "生产车间", "STRING", MesProWorkOrderDO::getWorkshopName),
            new WorkOrderSourceField("bomVersion", "BOM 版本", "STRING", MesProWorkOrderDO::getBomVersion),
            new WorkOrderSourceField("drawingNumber", "图号", "STRING", MesProWorkOrderDO::getDrawingNumber),
            new WorkOrderSourceField("plannedStartTime", "计划开工时间", "DATETIME", MesProWorkOrderDO::getPlannedStartTime),
            new WorkOrderSourceField("plannedEndTime", "计划完工时间", "DATETIME", MesProWorkOrderDO::getPlannedEndTime),
            new WorkOrderSourceField("requestDate", "需求日期", "DATETIME", MesProWorkOrderDO::getRequestDate),
            new WorkOrderSourceField("remark", "备注", "STRING", MesProWorkOrderDO::getRemark));
    private static final List<ProcessPoolReportSourceField> PROCESS_POOL_REPORT_BASE_SOURCE_FIELDS = List.of(
            ProcessPoolReportSourceField.base("outputQuantity", "本次报工产出数量", "NUMBER"),
            ProcessPoolReportSourceField.base("lossQuantity", "本次报工损耗数量", "NUMBER"),
            ProcessPoolReportSourceField.base("serverSubmitTime", "提交时间", "STRING"),
            ProcessPoolReportSourceField.base("signatureUserId", "提交签名用户", "NUMBER"),
            ProcessPoolReportSourceField.base("reviewedAt", "审核时间", "STRING"),
            ProcessPoolReportSourceField.base("reviewSignatureUserId", "审核人签名用户", "NUMBER"),
            ProcessPoolReportSourceField.base("selectedDevice.deviceId", "选用设备编号", "NUMBER"),
            ProcessPoolReportSourceField.base("selectedDevice.deviceCode", "选用设备编码", "STRING"),
            ProcessPoolReportSourceField.base("selectedDevice.deviceName", "选用设备名称", "STRING"),
            ProcessPoolReportSourceField.base("deviceMeteringValidity.inMeteringValidityPeriod",
                    "选用设备计量有效期内", "BOOLEAN"),
            ProcessPoolReportSourceField.base("clearanceConfirmations.workplace.confirmed", "清场确认", "BOOLEAN"),
            ProcessPoolReportSourceField.base("clearanceConfirmations.material.confirmed", "物料确认", "BOOLEAN"),
            ProcessPoolReportSourceField.base("clearanceConfirmations.cleaning.confirmed", "清洁确认", "BOOLEAN"));
    private static final Set<String> PROCESS_POOL_REPORT_NUMBER_AGGREGATION_STRATEGIES =
            Set.of("SUM", "FIRST", "LAST", "MIN", "MAX");
    private static final Set<String> PROCESS_POOL_REPORT_TEXT_AGGREGATION_STRATEGIES =
            Set.of("LIST", "DISTINCT_LIST", "FIRST", "LAST");
    private static final Set<String> PROCESS_POOL_REPORT_DEVICE_SOURCE_FIELD_CODES = Set.of(
            "selectedDevice.deviceId", "selectedDevice.deviceCode", "selectedDevice.deviceName",
            "deviceMeteringValidity.inMeteringValidityPeriod");

    @Resource
    private MesProBatchRecordCellLinkRuleMapper ruleMapper;
    @Resource
    private MesProBatchRecordRepeatRowGroupMapper repeatRowGroupMapper;
    @Resource
    private MesProBatchRecordReportMapper reportMapper;
    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordReportService reportService;
    @Resource
    private MesProEdhrWorkTaskService workTaskService;
    @Resource
    private MesProWorkOrderMapper workOrderMapper;
    @Resource
    private FormTemplateVersionMapper templateVersionMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Resource
    private MesProRouteVersionMapper routeVersionMapper;
    @Resource
    private MesProRouteProcessMapper routeProcessMapper;
    @Resource
    private MesProProcessMapper processMapper;
    @Resource
    private MesRouteDccProjectBindingMapper routeDccProjectBindingMapper;
    @Resource
    private MesProcessPoolDeviceParameterRuleMapper deviceParameterRuleMapper;
    @Resource
    private MesProcessPoolTeamDeviceMapper teamDeviceMapper;
    @Resource
    private MesProcessPoolTeamProcessDeviceMapper processDeviceMapper;
    @Resource
    private MesProductionPickListSourceService productionPickListSourceService;
    @Resource
    private MesQaInspectionRegulationMapper qaRegulationMapper;
    @Resource
    private MesQaInspectionRegulationVersionMapper qaRegulationVersionMapper;
    @Resource
    private MesQaInspectionRegulationProcessMapper qaRegulationProcessMapper;
    @Resource
    private MesQaInspectionRegulationItemMapper qaRegulationItemMapper;

    @Override
    public BatchRecordCellLinkWorkbenchContextRespVO getWorkbenchContext(Long routeId, Long definitionId,
                                                                         Long versionId, String sourceReportId,
                                                                         Long templateId, String versionNo,
                                                                         Long routeProcessId, Long qaProcessId,
                                                                         Long dccProjectCodeId) {
        Long effectiveRouteId = resolveProcessPoolReportRouteId(routeId, sourceReportId, dccProjectCodeId);
        Scope scope = resolveQueryScope(effectiveRouteId, definitionId, versionId, sourceReportId, templateId, versionNo,
                routeProcessId);
        List<BatchRecordCellLinkFormRespVO> forms = selectFormsInScope(scope, effectiveRouteId);
        if (forms.isEmpty()) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_FORM_LIST_EMPTY);
        }
        Set<String> reportIds = forms.stream().map(BatchRecordCellLinkFormRespVO::getReportId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String defaultSourceReportId = Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION)
                ? isFormTemplateVirtualSourceReportId(sourceReportId)
                    ? StrUtil.trim(sourceReportId)
                    : PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID
                : StrUtil.isNotBlank(sourceReportId) ? sourceReportId : forms.get(0).getReportId();
        boolean routeVersionVirtualSource = Objects.equals(scope.type(), SCOPE_TYPE_ROUTE_VERSION)
                && isRouteVersionVirtualSourceReportId(defaultSourceReportId);
        boolean formTemplateVirtualSource = Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION)
                && isFormTemplateVirtualSourceReportId(defaultSourceReportId);
        if (Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION)
                && StrUtil.isNotBlank(sourceReportId)
                && !isFormTemplateVirtualSourceReportId(sourceReportId)) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_REPORT_NOT_EXISTS,
                    sourceReportId);
        }
        if (!reportIds.contains(defaultSourceReportId) && !routeVersionVirtualSource && !formTemplateVirtualSource) {
            if (!Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION)) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_REPORT_NOT_EXISTS,
                        defaultSourceReportId);
            }
        }
        String defaultTargetReportId = Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION)
                ? forms.get(0).getReportId()
                : forms.stream()
                        .map(BatchRecordCellLinkFormRespVO::getReportId)
                        .filter(reportId -> !Objects.equals(reportId, defaultSourceReportId))
                        .findFirst()
                        .orElse(defaultSourceReportId);
        List<BatchRecordCellLinkRouteProcessVO> routeProcesses = toRouteProcessVOList(effectiveRouteId);
        if (dccProjectCodeId != null
                && Objects.equals(defaultSourceReportId, PROCESS_POOL_REPORT_SOURCE_REPORT_ID)
                && routeProcessId != null
                && routeProcesses.stream().noneMatch(process -> Objects.equals(process.getId(), routeProcessId))) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_DCC_ROUTE_PROCESS_MISSING,
                    effectiveRouteId, routeProcessId);
        }
        return new BatchRecordCellLinkWorkbenchContextRespVO()
                .setScopeType(scope.type())
                .setScopeId(scope.id())
                .setRouteId(effectiveRouteId)
                .setDccProjectCodeId(dccProjectCodeId)
                .setBatchRecordDefinitionId(scope.definitionId())
                .setBatchRecordVersionId(scope.versionId())
                .setForms(forms)
                .setRouteProcesses(routeProcesses)
                .setPqcProcesses(isPqcAggregateSourceReport(defaultSourceReportId)
                        ? toPqcProcessVOList(requirePqcProcessCatalog(effectiveRouteId, scope), effectiveRouteId)
                        : List.of())
                .setSourceFields(toSourceFieldVOList(scope, effectiveRouteId, routeProcessId, qaProcessId,
                        defaultSourceReportId))
                .setDefaultSourceReportId(defaultSourceReportId)
                .setDefaultTargetReportId(defaultTargetReportId)
                .setRules(toRuleVOList(ruleMapper.selectListByScope(scope.type(), scope.id())))
                        .setRepeatRowGroups(toRepeatRowGroupVOList(repeatRowGroupMapper.selectListByScope(scope.type(), scope.id())));
    }

    private boolean isRouteVersionVirtualSourceReportId(String reportId) {
        return Objects.equals(reportId, PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID)
                || Objects.equals(reportId, PROCESS_POOL_REPORT_SOURCE_REPORT_ID)
                || Objects.equals(reportId, MesProductionPickListSourceService.SOURCE_REPORT_ID)
                || Objects.equals(reportId, PQC_AGGREGATE_DETAIL_SOURCE_REPORT_ID);
    }

    private boolean isFormTemplateVirtualSourceReportId(String reportId) {
        return Objects.equals(StrUtil.trim(reportId), PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID)
                || Objects.equals(StrUtil.trim(reportId), PQC_AGGREGATE_DETAIL_SOURCE_REPORT_ID);
    }

    private Long resolveProcessPoolReportRouteId(Long routeId, String sourceReportId, Long dccProjectCodeId) {
        if (dccProjectCodeId == null) {
            return routeId;
        }
        if (!Objects.equals(StrUtil.trim(sourceReportId), PROCESS_POOL_REPORT_SOURCE_REPORT_ID)) {
            return routeId;
        }
        if (routeId != null) {
            MesRouteDccProjectBindingDO binding = routeDccProjectBindingMapper.selectCurrentByRouteId(routeId);
            if (binding == null || !Objects.equals(binding.getDccProjectCodeId(), dccProjectCodeId)) {
                throw exception(
                        MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_DCC_ROUTE_BINDING_MISMATCH,
                        routeId, dccProjectCodeId);
            }
            return routeId;
        }
        List<MesRouteDccProjectBindingDO> bindings =
                routeDccProjectBindingMapper.selectCurrentListByDccProjectCodeId(dccProjectCodeId);
        List<MesRouteDccProjectBindingDO> validBindings = bindings == null
                ? List.of()
                : bindings.stream()
                        .filter(Objects::nonNull)
                        .filter(binding -> binding.getRouteId() != null)
                        .filter(binding -> Objects.equals(binding.getDccProjectCodeId(), dccProjectCodeId))
                        .toList();
        if (validBindings.size() != 1) {
            throw exception(
                    MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_DCC_ROUTE_BINDING_REQUIRED,
                    dccProjectCodeId);
        }
        return validBindings.get(0).getRouteId();
    }

    @Override
    public BatchRecordCellLinkFormCellsRespVO getFormCells(String reportId, Long versionId) {
        if (isFormTemplateReportId(reportId)) {
            return getFormTemplateCells(reportId);
        }
        MesProBatchRecordReportDO report = requireReport(reportId);
        if (versionId != null && report.getBatchRecordVersionId() != null
                && !Objects.equals(versionId, report.getBatchRecordVersionId())) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_REPORT_NOT_EXISTS,
                    reportId);
        }
        BatchRecordReportCellRulesRespVO cellRules = reportService.getCellRules(reportId);
        String sheetLayoutJson = cellRules.getSheetLayoutJson();
        JSONObject root = parseLayout(sheetLayoutJson, reportId);
        Map<String, BatchRecordReportCellRuleVO> ruleMap = new LinkedHashMap<>();
        putCellRules(ruleMap, cellRules.getSuggestions());
        putCellRules(ruleMap, cellRules.getRules());
        List<BatchRecordCellLinkCellVO> cells = new ArrayList<>();
        MesProBatchRecordCellRuleSupport.forEachCell(root, (rowIndex, columnIndex, cell) -> {
            String cellKey = cellKey(rowIndex, columnIndex);
            BatchRecordReportCellRuleVO rule = ruleMap.get(cellKey);
            boolean signatureCell = MesProBatchRecordCellRuleSupport.hasValidSignatureMarker(cell);
            boolean fillable = MesProBatchRecordCellRuleSupport.isFillableCell(cell);
            cells.add(new BatchRecordCellLinkCellVO()
                    .setRowIndex(rowIndex)
                    .setColumnIndex(columnIndex)
                    .setCellKey(cellKey)
                    .setSourceType(SOURCE_TYPE_BATCH_RECORD_CELL)
                    .setLabel(resolveLabel(rule, cell, rowIndex, columnIndex))
                    .setValueType(rule == null ? "STRING" : rule.getValueType())
                    .setComponentFlag(rule == null ? null : rule.getComponentFlag())
                    .setRequired(rule != null && Boolean.TRUE.equals(rule.getRequired()))
                    .setReadonly(!fillable)
                    .setSignatureCell(signatureCell)
                    .setLinkableAsSource(fillable && !signatureCell)
                    .setLinkableAsTarget(fillable && !signatureCell));
        });
        return new BatchRecordCellLinkFormCellsRespVO()
                .setReportId(report.getReportId())
                .setReportName(report.getReportName())
                .setBatchRecordDefinitionId(report.getBatchRecordDefinitionId())
                .setBatchRecordVersionId(report.getBatchRecordVersionId())
                .setLayoutSnapshotHash(DigestUtil.sha256Hex(sheetLayoutJson))
                .setSheetLayoutJson(sheetLayoutJson)
                .setCells(cells);
    }

    private BatchRecordCellLinkFormCellsRespVO getFormTemplateCells(String reportId) {
        Long templateVersionId = parseFormTemplateReportId(reportId);
        FormTemplateVersionDO templateVersion = requireFormTemplateVersion(templateVersionId);
        TemplateLayout templateLayout = resolveTemplateLayout(templateVersion, reportId);
        JSONObject schema = templateLayout.schema();
        String sheetLayoutJson = templateLayout.sheetLayoutJson();
        JSONObject root = parseLayout(sheetLayoutJson, reportId);
        List<BatchRecordReportCellRuleVO> templateRules = parseTemplateCellRules(schema);
        Map<String, BatchRecordReportCellRuleVO> ruleMap = new LinkedHashMap<>();
        putCellRules(ruleMap, templateRules);
        Set<String> signatureMarkers = parseTemplateSignatureCellMarkers(schema);
        List<BatchRecordCellLinkCellVO> cells = new ArrayList<>();
        if (templateLayout.recognizedProjection()) {
            putTemplateRecognizedFieldCells(cells, templateRules, signatureMarkers);
        } else {
            MesProBatchRecordCellRuleSupport.forEachCell(root, (rowIndex, columnIndex, cell) -> {
                String cellKey = cellKey(rowIndex, columnIndex);
                BatchRecordReportCellRuleVO rule = ruleMap.get(cellKey);
                boolean signatureCell = isTemplateSignatureCell(rule, cell, cellKey, signatureMarkers);
                boolean linkable = rule != null && !signatureCell;
                cells.add(new BatchRecordCellLinkCellVO()
                        .setRowIndex(rowIndex)
                        .setColumnIndex(columnIndex)
                        .setCellKey(cellKey)
                        .setSourceType(SOURCE_TYPE_BATCH_RECORD_CELL)
                        .setLabel(resolveLabel(rule, cell, rowIndex, columnIndex))
                        .setValueType(rule == null ? "STRING" : rule.getValueType())
                        .setComponentFlag(rule == null ? null : rule.getComponentFlag())
                        .setRequired(rule != null && Boolean.TRUE.equals(rule.getRequired()))
                        .setReadonly(!linkable)
                        .setSignatureCell(signatureCell)
                        .setLinkableAsSource(false)
                        .setLinkableAsTarget(linkable));
            });
        }
        if (cells.isEmpty() || cells.stream().noneMatch(cell -> Boolean.TRUE.equals(cell.getLinkableAsTarget()))) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                    reportId);
        }
        return new BatchRecordCellLinkFormCellsRespVO()
                .setReportId(reportId)
                .setReportName(formTemplateReportName(templateVersion))
                .setBatchRecordDefinitionId(null)
                .setBatchRecordVersionId(null)
                .setLayoutSnapshotHash(DigestUtil.sha256Hex(sheetLayoutJson))
                .setSheetLayoutJson(sheetLayoutJson)
                .setCells(cells);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchRecordCellLinkRulesSaveRespVO saveRules(BatchRecordCellLinkRulesSaveReqVO reqVO) {
        if (reqVO.getRules() == null || reqVO.getRules().isEmpty()) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_RULE_EMPTY);
        }
        Scope scope = resolveSaveScope(reqVO);
        long ruleVersion = System.currentTimeMillis();
        List<MesProBatchRecordCellLinkRuleDO> ruleRows = new ArrayList<>();
        Set<String> targetKeys = new LinkedHashSet<>();
        Set<String> pairKeys = new LinkedHashSet<>();
        Map<String, MesProBatchRecordReportDO> reportCache = new LinkedHashMap<>();
        Map<String, TargetSpec> targetCache = new LinkedHashMap<>();
        Map<String, BatchRecordCellLinkFormCellsRespVO> cellsCache = new LinkedHashMap<>();
        for (BatchRecordCellLinkRuleSaveItemReqVO item : reqVO.getRules()) {
            String sourceType = normalizeSourceType(item.getSourceType());
            TargetSpec targetReport = targetCache.computeIfAbsent(item.getTargetReportId(),
                    reportId -> resolveTargetSpec(scope, reportId, reqVO.getRouteId()));
            BatchRecordCellLinkFormCellsRespVO targetCells = cellsCache.computeIfAbsent(item.getTargetReportId(),
                    reportId -> getFormCells(reportId, scope.versionId()));
            SourceSpec sourceSpec;
            if (SOURCE_TYPE_PRODUCTION_WORK_ORDER.equals(sourceType)) {
                WorkOrderSourceField sourceField = requireWorkOrderSourceField(
                        StrUtil.blankToDefault(item.getSourceFieldCode(), item.getSourceCellKey()));
                sourceSpec = SourceSpec.productionWorkOrder(sourceField);
            } else if (SOURCE_TYPE_PROCESS_POOL_REPORT.equals(sourceType)) {
                if (!Objects.equals(scope.type(), SCOPE_TYPE_ROUTE_VERSION)) {
                    throw exception(
                            MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                            sourceType);
                }
                ProcessPoolReportSourceField sourceField = requireProcessPoolReportSourceField(scope,
                        StrUtil.blankToDefault(item.getSourceFieldCode(), item.getSourceCellKey()),
                        resolveTargetRouteProcessId(reqVO, targetReport));
                item.setAggregationStrategy(requireProcessPoolReportAggregationStrategy(
                        item.getAggregationStrategy(), sourceField.valueType()));
                sourceSpec = SourceSpec.processPoolReport(sourceField);
            } else if (SOURCE_TYPE_PRODUCTION_PICK_LIST.equals(sourceType)) {
                if (!Objects.equals(scope.type(), SCOPE_TYPE_ROUTE_VERSION) || reqVO.getRouteId() == null) {
                    throw exception(
                            MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                            sourceType);
                }
                MesProductionPickListSourceService.SourceField sourceField = requireProductionPickListSourceField(
                        reqVO.getRouteId(), resolveTargetRouteProcessId(reqVO, targetReport),
                        StrUtil.blankToDefault(item.getSourceFieldCode(), item.getSourceCellKey()));
                sourceSpec = SourceSpec.productionPickList(sourceField);
            } else if (SOURCE_TYPE_PQC_AGGREGATE_DETAIL.equals(sourceType)) {
                if (Objects.equals(scope.type(), SCOPE_TYPE_ROUTE_VERSION)
                        || Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION)) {
                    PqcAggregateSourceField sourceField = requirePqcAggregateSourceField(scope, reqVO.getRouteId(),
                            resolveTargetRouteProcessId(reqVO, targetReport),
                            item.getSourceFieldCode(), item.getSourceCellKey());
                    sourceSpec = SourceSpec.pqcAggregate(sourceField);
                } else {
                    throw exception(
                            MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                            sourceType);
                }
            } else if (SOURCE_TYPE_PRODUCTION_LOSS.equals(sourceType)) {
                if (!Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION)) {
                    throw exception(
                            MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                            sourceType);
                }
                sourceSpec = SourceSpec.formTemplateFormalSource(sourceType, item);
            } else {
                if (Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION)) {
                    throw exception(
                            MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                            sourceType);
                }
                MesProBatchRecordReportDO sourceReport = reportCache.computeIfAbsent(item.getSourceReportId(),
                        this::requireReport);
                requireReportInScope(scope, sourceReport);
                BatchRecordCellLinkFormCellsRespVO sourceCells = cellsCache.computeIfAbsent(item.getSourceReportId(),
                        reportId -> getFormCells(reportId, scope.versionId()));
                BatchRecordCellLinkCellVO sourceCell = requireCell(sourceCells, item.getSourceRowIndex(),
                        item.getSourceColumnIndex());
                sourceSpec = SourceSpec.batchRecordCell(sourceReport, sourceCell, sourceCells.getLayoutSnapshotHash());
            }
            BatchRecordCellLinkCellVO targetCell = requireCell(targetCells, item.getTargetRowIndex(),
                    item.getTargetColumnIndex());
            if (!Boolean.TRUE.equals(targetCell.getLinkableAsTarget())) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_TARGET_NOT_WRITABLE,
                        targetReport.reportName(), targetCell.getCellKey());
            }
            String targetKey = item.getTargetReportId() + ":" + targetCell.getCellKey();
            if (!targetKeys.add(targetKey)) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_TARGET_DUPLICATE,
                        targetReport.reportName(), targetCell.getCellKey());
            }
            String pairKey = sourceSpec.uniqueKey() + "->"
                    + item.getTargetReportId() + ":" + targetCell.getCellKey();
            if (!pairKeys.add(pairKey)) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_PAIR_DUPLICATE,
                        sourceSpec.cellKey(), targetCell.getCellKey());
            }
            ruleRows.add(toRuleDO(reqVO, scope, item, sourceSpec, targetReport, targetCell,
                    sourceSpec.snapshotHashPart(), targetCells.getLayoutSnapshotHash(), ruleVersion));
        }
        ruleMapper.deleteByScope(scope.type(), scope.id());
        ruleMapper.insertBatch(ruleRows);
        return new BatchRecordCellLinkRulesSaveRespVO()
                .setSavedCount(ruleRows.size())
                .setRuleVersion(ruleVersion)
                .setRules(toRuleVOList(ruleMapper.selectListByScope(scope.type(), scope.id())));
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchRecordRepeatRowGroupSaveRespVO saveRepeatRowGroup(BatchRecordRepeatRowGroupSaveReqVO reqVO) {
        if (reqVO.getRecords() == null || reqVO.getRecords().isEmpty()
                || reqVO.getMappings() == null || reqVO.getMappings().isEmpty()) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_RULE_EMPTY);
        }
        Scope scope = resolveRepeatRowGroupScope(reqVO);
        TargetSpec targetReport = resolveTargetSpec(scope, reqVO.getTargetReportId(), reqVO.getRouteId());
        BatchRecordCellLinkFormCellsRespVO targetCells = getFormCells(reqVO.getTargetReportId(), scope.versionId());
        validateRepeatRowGroupRequest(reqVO, scope, targetReport, targetCells);
        List<BatchRecordRepeatRowGroupRecordVO> records = toRepeatRowRecords(reqVO.getRecords());
        List<BatchRecordRepeatRowGroupMappingVO> mappings = toRepeatRowMappings(reqVO, scope, targetCells, records);
        long configVersion = System.currentTimeMillis();
        String recordsJson = JSON.toJSONString(records);
        String mappingsJson = JSON.toJSONString(mappings);
        MesProBatchRecordRepeatRowGroupDO row = new MesProBatchRecordRepeatRowGroupDO()
                .setScopeType(scope.type())
                .setScopeId(scope.id())
                .setRouteId(reqVO.getRouteId())
                .setBatchRecordDefinitionId(scope.definitionId())
                .setBatchRecordVersionId(scope.versionId())
                .setRouteProcessId(reqVO.getRouteProcessId())
                .setTargetReportId(targetReport.reportId())
                .setTargetReportName(targetReport.reportName())
                .setTemplateStartRowIndex(reqVO.getTemplateStartRowIndex())
                .setTemplateEndRowIndex(reqVO.getTemplateEndRowIndex())
                .setRepeatAreaStartRowIndex(reqVO.getRepeatAreaStartRowIndex())
                .setRepeatAreaEndRowIndex(reqVO.getRepeatAreaEndRowIndex())
                .setSourceType(normalizeSourceType(reqVO.getMappings().get(0).getSourceType()))
                .setRecordsJson(recordsJson)
                .setMappingsJson(mappingsJson)
                .setConfigVersion(configVersion)
                .setTemplateSnapshotHash(DigestUtil.sha256Hex(targetCells.getLayoutSnapshotHash()
                        + "|" + recordsJson + "|" + mappingsJson))
                .setEnabled(reqVO.getEnabled() == null || Boolean.TRUE.equals(reqVO.getEnabled()))
                .setRemark(StrUtil.trim(reqVO.getRemark()));
        repeatRowGroupMapper.deleteEnabledByScopeAndTargetReport(scope.type(), scope.id(), targetReport.reportId());
        repeatRowGroupMapper.insert(row);
        return toRepeatRowGroupSaveResp(row, records, mappings);
    }
    @Override
    public BatchRecordCellLinkPrefillRespVO getPrefill(Long targetExecutionId, Long workTaskId) {
        MesProBatchRecordExecutionDO targetExecution = executionMapper.selectById(targetExecutionId);
        if (targetExecution == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_EXECUTION_NOT_EXISTS);
        }
        if (workTaskId != null) {
            workTaskService.validateWritableFillTaskForExecution(workTaskId, targetExecution.getId());
        }
        MesProBatchRecordReportDO targetReport = requireReport(targetExecution.getBatchRecordReportId());
        Scope scope = resolveExecutionScope(targetExecution, targetReport);
        List<MesProBatchRecordCellLinkRuleDO> rules = ruleMapper.selectEnabledListByScopeAndTargetReport(
                scope.type(), scope.id(), targetExecution.getBatchRecordReportId());
        Map<String, JSONObject> targetValues = parseCellValues(targetExecution.getCellValuesJson(), targetExecutionId);
        List<BatchRecordCellLinkPrefillItemVO> prefills = new ArrayList<>();
        List<BatchRecordCellLinkPrefillItemVO> conflicts = new ArrayList<>();
        Map<String, MesProBatchRecordExecutionDO> sourceExecutionCache = new LinkedHashMap<>();
        Map<Long, Map<String, JSONObject>> sourceValueCache = new LinkedHashMap<>();
        for (MesProBatchRecordCellLinkRuleDO rule : rules) {
            if (SOURCE_TYPE_PROCESS_POOL_REPORT.equals(StrUtil.trim(rule.getSourceType()))) {
                continue;
            }
            BatchRecordCellLinkPrefillItemVO item = basePrefillItem(rule);
            if (SOURCE_TYPE_PRODUCTION_WORK_ORDER.equals(normalizeSourceType(rule.getSourceType()))) {
                Object sourceValue = resolveProductionWorkOrderFieldValue(targetExecution, rule);
                if (!hasPlainValue(sourceValue)) {
                    conflicts.add(item.setStatus("SOURCE_VALUE_MISSING"));
                    continue;
                }
                JSONObject targetValue = targetValues.get(rule.getTargetCellKey());
                if (hasValue(targetValue) && OVERWRITE_POLICY_ONLY_WHEN_EMPTY.equals(rule.getOverwritePolicy())) {
                    conflicts.add(item.setValue(sourceValue).setStatus("TARGET_ALREADY_MANUAL"));
                    continue;
                }
                prefills.add(item.setValue(sourceValue).setStatus("APPLICABLE"));
                continue;
            }
            MesProBatchRecordExecutionDO sourceExecution = sourceExecutionCache.computeIfAbsent(rule.getSourceReportId(),
                    reportId -> executionMapper.selectLatestByWorkOrderVersionBatchAndReport(
                            targetExecution.getWorkOrderId(), targetExecution.getBatchRecordVersionId(),
                            targetExecution.getBatchCode(), reportId, ACTIVE_EXECUTION_STATUSES));
            if (sourceExecution == null) {
                conflicts.add(item.setStatus("SOURCE_EXECUTION_MISSING"));
                continue;
            }
            Map<String, JSONObject> sourceValues = sourceValueCache.computeIfAbsent(sourceExecution.getId(),
                    id -> parseCellValues(sourceExecution.getCellValuesJson(), id));
            JSONObject sourceValue = sourceValues.get(rule.getSourceCellKey());
            if (!hasValue(sourceValue)) {
                conflicts.add(item.setSourceExecutionId(sourceExecution.getId()).setStatus("SOURCE_VALUE_MISSING"));
                continue;
            }
            JSONObject targetValue = targetValues.get(rule.getTargetCellKey());
            if (hasValue(targetValue) && OVERWRITE_POLICY_ONLY_WHEN_EMPTY.equals(rule.getOverwritePolicy())) {
                conflicts.add(item.setSourceExecutionId(sourceExecution.getId())
                        .setValue(sourceValue.get("value"))
                        .setStatus("TARGET_ALREADY_MANUAL"));
                continue;
            }
            prefills.add(item.setSourceExecutionId(sourceExecution.getId())
                    .setValue(sourceValue.get("value"))
                    .setStatus("APPLICABLE"));
        }
        return new BatchRecordCellLinkPrefillRespVO()
                .setTargetExecutionId(targetExecutionId)
                .setPrefills(prefills)
                .setConflicts(conflicts);
    }

    @Override
    public Map<String, Object> buildFormTemplateVersionPrefillData(Long templateVersionId, Long workOrderId,
                                                                   String executionBatchCode,
                                                                   Map<String, Object> formData) {
        FormTemplateVersionDO templateVersion = requireFormTemplateVersion(templateVersionId);
        String targetReportId = formTemplateReportId(templateVersion.getId());
        List<MesProBatchRecordCellLinkRuleDO> rules = ruleMapper.selectEnabledListByScopeAndTargetReport(
                SCOPE_TYPE_FORM_TEMPLATE_VERSION, templateVersion.getId(), targetReportId);
        Map<String, Object> result = new LinkedHashMap<>();
        if (formData != null) {
            result.putAll(formData);
        }
        if (rules.isEmpty()) {
            return result;
        }
        List<MesProBatchRecordCellLinkRuleDO> workOrderRules = new ArrayList<>();
        for (MesProBatchRecordCellLinkRuleDO rule : rules) {
            String sourceType = normalizeSourceType(rule.getSourceType());
            if (SOURCE_TYPE_PRODUCTION_WORK_ORDER.equals(sourceType)) {
                workOrderRules.add(rule);
                continue;
            }
            if (isFormTemplateFormalSource(sourceType)) {
                continue;
            }
            throw exception(
                    MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                    rule.getSourceType());
        }
        if (workOrderRules.isEmpty()) {
            return result;
        }
        if (workOrderId == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_WORK_ORDER_MISSING,
                    templateVersion.getId());
        }
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_WORK_ORDER_MISSING,
                    templateVersion.getId());
        }
        for (MesProBatchRecordCellLinkRuleDO rule : workOrderRules) {
            WorkOrderSourceField field = requireWorkOrderSourceField(
                    StrUtil.blankToDefault(rule.getSourceFieldCode(), rule.getSourceCellKey()));
            Object sourceValue = resolveFormTemplateWorkOrderFieldValue(workOrder, field, executionBatchCode);
            if (!hasPlainValue(sourceValue)) {
                throw exception(
                        MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_AUTO_PERSIST_SOURCE_VALUE_MISSING,
                        templateVersion.getId(), rule.getId(), field.code(), rule.getTargetCellKey());
            }
            String targetCellKey = StrUtil.blankToDefault(rule.getTargetCellKey(),
                    rule.getTargetRowIndex() == null || rule.getTargetColumnIndex() == null
                            ? null : cellKey(rule.getTargetRowIndex(), rule.getTargetColumnIndex()));
            if (StrUtil.isBlank(targetCellKey)) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_CELL_MISSING,
                        targetReportId, rule.getTargetRowIndex(), rule.getTargetColumnIndex());
            }
            String targetFormDataKey = resolveFormTemplateTargetFormDataKey(templateVersion, targetCellKey,
                    rule.getTargetRowIndex(), rule.getTargetColumnIndex(), targetReportId);
            if (hasFormDataValue(result.get(targetFormDataKey))
                    && OVERWRITE_POLICY_ONLY_WHEN_EMPTY.equals(rule.getOverwritePolicy())) {
                continue;
            }
            result.put(targetFormDataKey, sourceValue);
        }
        return result;
    }

    private String resolveFormTemplateTargetFormDataKey(FormTemplateVersionDO templateVersion,
                                                        String targetCellKey,
                                                        Integer targetRowIndex,
                                                        Integer targetColumnIndex,
                                                        String targetReportId) {
        String fieldCode = resolveRecognizedFieldCode(templateVersion, targetRowIndex, targetColumnIndex,
                targetReportId);
        if (StrUtil.isNotBlank(fieldCode)) {
            return fieldCode;
        }
        throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_CELL_MISSING,
                targetReportId, targetRowIndex, targetColumnIndex == null ? targetCellKey : targetColumnIndex);
    }

    private String resolveRecognizedFieldCode(FormTemplateVersionDO templateVersion,
                                              Integer targetRowIndex,
                                              Integer targetColumnIndex,
                                              String targetReportId) {
        if (templateVersion == null || StrUtil.isBlank(templateVersion.getRecognizedSchemaJson())
                || targetRowIndex == null || targetColumnIndex == null) {
            return null;
        }
        JSONArray recognizedFields;
        try {
            recognizedFields = JSON.parseArray(templateVersion.getRecognizedSchemaJson());
        } catch (Exception ex) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                    targetReportId);
        }
        if (recognizedFields == null || recognizedFields.isEmpty()) {
            return null;
        }
        Integer fieldIndex = recognizedFieldIndex(targetRowIndex, targetColumnIndex);
        if (fieldIndex == null || fieldIndex < 0 || fieldIndex >= recognizedFields.size()) {
            return null;
        }
        JSONObject field = toTemplateJsonObject(recognizedFields.get(fieldIndex), targetReportId);
        return StrUtil.trim(field.getString("fieldCode"));
    }

    private Integer recognizedFieldIndex(Integer rowIndex, Integer columnIndex) {
        if (rowIndex == null || columnIndex == null || rowIndex < 3) {
            return null;
        }
        if (columnIndex == 1) {
            return (rowIndex - 3) * 2;
        }
        if (columnIndex == 3) {
            return (rowIndex - 3) * 2 + 1;
        }
        return null;
    }

    private Object resolveFormTemplateWorkOrderFieldValue(MesProWorkOrderDO workOrder, WorkOrderSourceField field,
                                                          String executionBatchCode) {
        if (WORK_ORDER_SOURCE_FIELD_BATCH_CODE.equals(field.code())) {
            return StrUtil.trim(executionBatchCode);
        }
        return field.valueExtractor().apply(workOrder);
    }

    private Scope resolveQueryScope(Long routeId, Long definitionId, Long versionId, String sourceReportId,
                                    Long templateId, String versionNo, Long routeProcessId) {
        if (templateId != null || StrUtil.isNotBlank(versionNo)) {
            if (templateId == null || StrUtil.isBlank(versionNo)) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SCOPE_REQUIRED);
            }
            FormTemplateVersionDO templateVersion = templateVersionMapper.selectByTemplateIdAndVersionNo(
                    templateId, StrUtil.trim(versionNo));
            if (templateVersion == null) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_REPORT_NOT_EXISTS,
                        templateId + "/" + versionNo);
            }
            return Scope.formTemplateVersion(templateVersion);
        }
        if (definitionId != null && versionId != null) {
            return Scope.routeVersion(definitionId, versionId);
        }
        if (routeId != null && routeProcessId != null
                && Objects.equals(sourceReportId, PQC_AGGREGATE_DETAIL_SOURCE_REPORT_ID)) {
            Scope processInspectionTemplateScope = resolveProcessInspectionTemplateScopeFromRouteBinding(
                    routeId, routeProcessId);
            if (processInspectionTemplateScope != null) {
                return processInspectionTemplateScope;
            }
        }
        if (routeId != null && isRouteVersionVirtualSourceReportId(sourceReportId)) {
            return resolveRouteVersionScopeFromRouteBindings(routeId);
        }
        if (StrUtil.isNotBlank(sourceReportId)) {
            MesProBatchRecordReportDO report = requireReport(sourceReportId);
            if (report.getBatchRecordDefinitionId() != null && report.getBatchRecordVersionId() != null) {
                return Scope.routeVersion(report.getBatchRecordDefinitionId(), report.getBatchRecordVersionId());
            }
            return resolveReportSetScope(report);
        }
        throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SCOPE_REQUIRED);
    }

    private Scope resolveProcessInspectionTemplateScopeFromRouteBinding(Long routeId, Long routeProcessId) {
        List<MesProRouteFlowProcessBatchRecordDO> bindings =
                routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(routeId, "BATCH");
        List<MesProRouteFlowProcessBatchRecordDO> candidates = bindings.stream()
                .filter(binding -> binding != null
                        && Objects.equals(routeProcessId, binding.getRouteProcessId())
                        && Objects.equals(FORM_SLOT_TYPE_PROCESS_INSPECTION,
                        StrUtil.trim(binding.getFormSlotType()))
                        && binding.getFormTemplateId() != null)
                .toList();
        Set<Long> templateIds = candidates.stream()
                .map(MesProRouteFlowProcessBatchRecordDO::getFormTemplateId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (templateIds.isEmpty()) {
            return null;
        }
        if (templateIds.size() != 1) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SCOPE_REQUIRED);
        }
        MesProRouteFlowProcessBatchRecordDO binding = candidates.get(0);
        FormTemplateVersionDO templateVersion = binding.getLastPublishedTemplateVersionId() == null
                ? null
                : templateVersionMapper.selectById(binding.getLastPublishedTemplateVersionId());
        if (templateVersion == null && StrUtil.isNotBlank(binding.getLastPublishedTemplateVersionNo())) {
            templateVersion = templateVersionMapper.selectByTemplateIdAndVersionNo(
                    binding.getFormTemplateId(), StrUtil.trim(binding.getLastPublishedTemplateVersionNo()));
        }
        if (templateVersion == null
                || !Objects.equals(binding.getFormTemplateId(), templateVersion.getTemplateId())
                || !Objects.equals("PUBLISHED", StrUtil.trim(templateVersion.getStatus()))) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_REPORT_NOT_EXISTS,
                    binding.getFormTemplateId() + "/"
                            + StrUtil.blankToDefault(binding.getLastPublishedTemplateVersionNo(), "PUBLISHED"));
        }
        return Scope.formTemplateVersion(templateVersion);
    }

    private Scope resolveRouteVersionScopeFromRouteBindings(Long routeId) {
        List<MesProRouteFlowProcessBatchRecordDO> bindings =
                routeFlowProcessBatchRecordMapper.selectListByRouteIdAndUseType(routeId, "BATCH");
        Map<Long, LinkedHashSet<Long>> definitionIdsByVersionId = new LinkedHashMap<>();
        Set<String> reportIds = new LinkedHashSet<>();
        for (MesProRouteFlowProcessBatchRecordDO binding : bindings) {
            if (binding == null || !isFormalBatchRecordBinding(binding)) {
                continue;
            }
            if (binding.getBatchRecordDefinitionId() != null && binding.getBatchRecordVersionId() != null) {
                definitionIdsByVersionId.computeIfAbsent(binding.getBatchRecordVersionId(), ignored -> new LinkedHashSet<>())
                        .add(binding.getBatchRecordDefinitionId());
            } else if (StrUtil.isNotBlank(binding.getBatchRecordReportId())) {
                reportIds.add(binding.getBatchRecordReportId());
            }
        }
        if (!reportIds.isEmpty()) {
            for (MesProBatchRecordReportDO report : reportMapper.selectListByReportIds(reportIds)) {
                if (report.getBatchRecordDefinitionId() != null && report.getBatchRecordVersionId() != null) {
                    definitionIdsByVersionId.computeIfAbsent(report.getBatchRecordVersionId(), ignored -> new LinkedHashSet<>())
                            .add(report.getBatchRecordDefinitionId());
                }
            }
        }
        if (definitionIdsByVersionId.size() != 1) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SCOPE_REQUIRED);
        }
        Map.Entry<Long, LinkedHashSet<Long>> entry = definitionIdsByVersionId.entrySet().iterator().next();
        if (entry.getValue().size() != 1) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SCOPE_REQUIRED);
        }
        return Scope.routeVersion(entry.getValue().iterator().next(), entry.getKey());
    }

    private Scope resolveSaveScope(BatchRecordCellLinkRulesSaveReqVO reqVO) {
        if (Objects.equals(reqVO.getScopeType(), SCOPE_TYPE_FORM_TEMPLATE_VERSION)) {
            Long templateVersionId = reqVO.getScopeId();
            if (templateVersionId == null && reqVO.getRules() != null && !reqVO.getRules().isEmpty()) {
                String targetReportId = reqVO.getRules().get(0).getTargetReportId();
                if (isFormTemplateReportId(targetReportId)) {
                    templateVersionId = parseFormTemplateReportId(targetReportId);
                }
            }
            requireFormTemplateVersion(templateVersionId);
            return Scope.formTemplateVersion(templateVersionId);
        }
        if (Objects.equals(reqVO.getScopeType(), SCOPE_TYPE_REPORT_SET)) {
            return resolveReportSetScope(requireReport(reqVO.getRules().get(0).getSourceReportId()));
        }
        Long versionId = reqVO.getScopeId() == null ? reqVO.getBatchRecordVersionId() : reqVO.getScopeId();
        if (reqVO.getBatchRecordDefinitionId() == null || versionId == null) {
            if (reqVO.getRules() != null && !reqVO.getRules().isEmpty()) {
                MesProBatchRecordReportDO sourceReport = requireReport(reqVO.getRules().get(0).getSourceReportId());
                if (sourceReport.getBatchRecordDefinitionId() == null
                        || sourceReport.getBatchRecordVersionId() == null) {
                    return resolveReportSetScope(sourceReport);
                }
            }
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SCOPE_REQUIRED);
        }
        return Scope.routeVersion(reqVO.getBatchRecordDefinitionId(), versionId);
    }


    private Scope resolveRepeatRowGroupScope(BatchRecordRepeatRowGroupSaveReqVO reqVO) {
        Long versionId = reqVO.getScopeId() == null ? reqVO.getBatchRecordVersionId() : reqVO.getScopeId();
        if (reqVO.getBatchRecordDefinitionId() == null || versionId == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SCOPE_REQUIRED);
        }
        return Scope.routeVersion(reqVO.getBatchRecordDefinitionId(), versionId);
    }

    private void validateRepeatRowGroupRequest(BatchRecordRepeatRowGroupSaveReqVO reqVO, Scope scope,
                                               TargetSpec targetReport,
                                               BatchRecordCellLinkFormCellsRespVO targetCells) {
        if (reqVO.getRouteProcessId() == null || reqVO.getTemplateStartRowIndex() == null
                || reqVO.getTemplateEndRowIndex() == null || reqVO.getRepeatAreaStartRowIndex() == null
                || reqVO.getRepeatAreaEndRowIndex() == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SCOPE_REQUIRED);
        }
        if (targetReport.routeProcessId() != null && !Objects.equals(targetReport.routeProcessId(), reqVO.getRouteProcessId())) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_REPORT_NOT_EXISTS,
                    reqVO.getTargetReportId());
        }
        if (reqVO.getTemplateStartRowIndex() > reqVO.getTemplateEndRowIndex()
                || reqVO.getRepeatAreaStartRowIndex() > reqVO.getRepeatAreaEndRowIndex()
                || reqVO.getTemplateStartRowIndex() < reqVO.getRepeatAreaStartRowIndex()
                || reqVO.getTemplateEndRowIndex() > reqVO.getRepeatAreaEndRowIndex()) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                    targetReport.reportId());
        }
        Set<Integer> sequences = new LinkedHashSet<>();
        Set<String> recordRows = new LinkedHashSet<>();
        for (BatchRecordRepeatRowGroupRecordSaveReqVO record : reqVO.getRecords()) {
            if (record.getRecordSequence() == null || record.getStartRowIndex() == null || record.getEndRowIndex() == null
                    || record.getRecordSequence() <= 0 || record.getStartRowIndex() > record.getEndRowIndex()
                    || record.getStartRowIndex() < reqVO.getRepeatAreaStartRowIndex()
                    || record.getEndRowIndex() > reqVO.getRepeatAreaEndRowIndex()) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                        targetReport.reportId());
            }
            if (!sequences.add(record.getRecordSequence())
                    || !recordRows.add(record.getStartRowIndex() + ":" + record.getEndRowIndex())) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_PAIR_DUPLICATE,
                        "REPEAT_ROW_GROUP", record.getRecordSequence());
            }
        }
        for (int sequence = 1; sequence <= sequences.size(); sequence++) {
            if (!sequences.contains(sequence)) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                        targetReport.reportId());
            }
        }
        Set<String> templateTargets = new LinkedHashSet<>();
        for (BatchRecordRepeatRowGroupMappingSaveReqVO mapping : reqVO.getMappings()) {
            String sourceType = normalizeSourceType(mapping.getSourceType());
            if (!SOURCE_TYPE_PROCESS_POOL_REPORT.equals(sourceType)) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                        mapping.getSourceType());
            }
            ProcessPoolReportSourceField sourceField = requireProcessPoolReportSourceField(scope,
                    mapping.getSourceFieldCode(), reqVO.getRouteProcessId());
            BatchRecordCellLinkCellVO templateCell = requireCell(targetCells,
                    mapping.getTemplateTargetRowIndex(), mapping.getTemplateTargetColumnIndex());
            if (!Boolean.TRUE.equals(templateCell.getLinkableAsTarget())) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_TARGET_NOT_WRITABLE,
                        targetReport.reportName(), templateCell.getCellKey());
            }
            String templateKey = templateCell.getCellKey();
            if (!templateTargets.add(templateKey)) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_TARGET_DUPLICATE,
                        targetReport.reportName(), templateKey);
            }
            if (StrUtil.isBlank(sourceField.code())) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                        mapping.getSourceFieldCode());
            }
        }
    }

    private List<BatchRecordRepeatRowGroupRecordVO> toRepeatRowRecords(
            List<BatchRecordRepeatRowGroupRecordSaveReqVO> records) {
        return records.stream()
                .sorted((left, right) -> Integer.compare(left.getRecordSequence(), right.getRecordSequence()))
                .map(record -> new BatchRecordRepeatRowGroupRecordVO()
                        .setRecordSequence(record.getRecordSequence())
                        .setStartRowIndex(record.getStartRowIndex())
                        .setEndRowIndex(record.getEndRowIndex())
                        .setRecordKey("R" + record.getRecordSequence() + ":" + record.getStartRowIndex()
                                + "-" + record.getEndRowIndex()))
                .toList();
    }

    private List<BatchRecordRepeatRowGroupMappingVO> toRepeatRowMappings(
            BatchRecordRepeatRowGroupSaveReqVO reqVO,
            Scope scope,
            BatchRecordCellLinkFormCellsRespVO targetCells,
            List<BatchRecordRepeatRowGroupRecordVO> records) {
        Set<String> projectedTargets = new LinkedHashSet<>();
        List<BatchRecordRepeatRowGroupMappingVO> result = new ArrayList<>();
        for (BatchRecordRepeatRowGroupMappingSaveReqVO mapping : reqVO.getMappings()) {
            ProcessPoolReportSourceField sourceField = requireProcessPoolReportSourceField(scope,
                    mapping.getSourceFieldCode(), reqVO.getRouteProcessId());
            BatchRecordCellLinkCellVO templateCell = requireCell(targetCells,
                    mapping.getTemplateTargetRowIndex(), mapping.getTemplateTargetColumnIndex());
            String firstProjection = null;
            int rowOffset = mapping.getTemplateTargetRowIndex() - reqVO.getTemplateStartRowIndex();
            for (BatchRecordRepeatRowGroupRecordVO record : records) {
                int projectedRow = record.getStartRowIndex() + rowOffset;
                if (projectedRow > record.getEndRowIndex()) {
                    throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                            targetCells.getReportName());
                }
                BatchRecordCellLinkCellVO projectedCell = requireCell(targetCells, projectedRow,
                        mapping.getTemplateTargetColumnIndex());
                if (!Boolean.TRUE.equals(projectedCell.getLinkableAsTarget())) {
                    throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_TARGET_NOT_WRITABLE,
                            targetCells.getReportName(), projectedCell.getCellKey());
                }
                if (!projectedTargets.add(record.getRecordSequence() + ":" + projectedCell.getCellKey())) {
                    throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_TARGET_DUPLICATE,
                            targetCells.getReportName(), projectedCell.getCellKey());
                }
                if (firstProjection == null) {
                    firstProjection = projectedCell.getCellKey();
                }
            }
            result.add(new BatchRecordRepeatRowGroupMappingVO()
                    .setSourceType(SOURCE_TYPE_PROCESS_POOL_REPORT)
                    .setSourceFieldCode(sourceField.code())
                    .setSourceFieldName(StrUtil.blankToDefault(mapping.getSourceFieldName(), sourceField.name()))
                    .setSourceValueType(sourceField.valueType())
                    .setTemplateTargetRowIndex(templateCell.getRowIndex())
                    .setTemplateTargetColumnIndex(templateCell.getColumnIndex())
                    .setTemplateTargetCellKey(templateCell.getCellKey())
                    .setTargetValueType(templateCell.getValueType())
                    .setProjectionTargetCellKey(firstProjection));
        }
        return List.copyOf(result);
    }

    private List<BatchRecordRepeatRowGroupVO> toRepeatRowGroupVOList(List<MesProBatchRecordRepeatRowGroupDO> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(row -> toRepeatRowGroupVO(row,
                        parseRepeatRowRecords(row.getRecordsJson()),
                        parseRepeatRowMappings(row.getMappingsJson())))
                .toList();
    }

    private BatchRecordRepeatRowGroupSaveRespVO toRepeatRowGroupSaveResp(MesProBatchRecordRepeatRowGroupDO row,
                                                                         List<BatchRecordRepeatRowGroupRecordVO> records,
                                                                         List<BatchRecordRepeatRowGroupMappingVO> mappings) {
        BatchRecordRepeatRowGroupSaveRespVO resp = new BatchRecordRepeatRowGroupSaveRespVO();
        copyRepeatRowGroupFields(resp, row, records, mappings);
        return resp;
    }

    private BatchRecordRepeatRowGroupVO toRepeatRowGroupVO(MesProBatchRecordRepeatRowGroupDO row,
                                                           List<BatchRecordRepeatRowGroupRecordVO> records,
                                                           List<BatchRecordRepeatRowGroupMappingVO> mappings) {
        BatchRecordRepeatRowGroupVO vo = new BatchRecordRepeatRowGroupVO();
        copyRepeatRowGroupFields(vo, row, records, mappings);
        return vo;
    }

    private void copyRepeatRowGroupFields(BatchRecordRepeatRowGroupVO vo, MesProBatchRecordRepeatRowGroupDO row,
                                          List<BatchRecordRepeatRowGroupRecordVO> records,
                                          List<BatchRecordRepeatRowGroupMappingVO> mappings) {
        vo.setId(row.getId())
                .setScopeType(row.getScopeType())
                .setScopeId(row.getScopeId())
                .setRouteId(row.getRouteId())
                .setBatchRecordDefinitionId(row.getBatchRecordDefinitionId())
                .setBatchRecordVersionId(row.getBatchRecordVersionId())
                .setRouteProcessId(row.getRouteProcessId())
                .setTargetReportId(row.getTargetReportId())
                .setTargetReportName(row.getTargetReportName())
                .setTemplateStartRowIndex(row.getTemplateStartRowIndex())
                .setTemplateEndRowIndex(row.getTemplateEndRowIndex())
                .setRepeatAreaStartRowIndex(row.getRepeatAreaStartRowIndex())
                .setRepeatAreaEndRowIndex(row.getRepeatAreaEndRowIndex())
                .setSourceType(row.getSourceType())
                .setRecords(records)
                .setMappings(mappings)
                .setConfigVersion(row.getConfigVersion())
                .setTemplateSnapshotHash(row.getTemplateSnapshotHash())
                .setEnabled(row.getEnabled())
                .setRemark(row.getRemark());
    }

    private List<BatchRecordRepeatRowGroupRecordVO> parseRepeatRowRecords(String recordsJson) {
        if (StrUtil.isBlank(recordsJson)) {
            return List.of();
        }
        List<BatchRecordRepeatRowGroupRecordVO> records =
                JSON.parseArray(recordsJson, BatchRecordRepeatRowGroupRecordVO.class);
        return records == null ? List.of() : records;
    }

    private List<BatchRecordRepeatRowGroupMappingVO> parseRepeatRowMappings(String mappingsJson) {
        if (StrUtil.isBlank(mappingsJson)) {
            return List.of();
        }
        List<BatchRecordRepeatRowGroupMappingVO> mappings =
                JSON.parseArray(mappingsJson, BatchRecordRepeatRowGroupMappingVO.class);
        return mappings == null ? List.of() : mappings;
    }
    private Scope resolveExecutionScope(MesProBatchRecordExecutionDO execution, MesProBatchRecordReportDO report) {
        if (execution.getBatchRecordDefinitionId() != null && execution.getBatchRecordVersionId() != null) {
            return Scope.routeVersion(execution.getBatchRecordDefinitionId(), execution.getBatchRecordVersionId());
        }
        if (report.getBatchRecordDefinitionId() != null && report.getBatchRecordVersionId() != null) {
            return Scope.routeVersion(report.getBatchRecordDefinitionId(), report.getBatchRecordVersionId());
        }
        return resolveReportSetScope(report);
    }

    private Scope resolveReportSetScope(MesProBatchRecordReportDO report) {
        if (StrUtil.isBlank(report.getSourceFileSha256()) || StrUtil.isBlank(report.getRouteKey())) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SCOPE_REQUIRED);
        }
        List<MesProBatchRecordReportDO> reports = reportMapper.selectListBySourceFileSha256AndRouteKey(
                report.getSourceFileSha256(), report.getRouteKey());
        Long scopeId = reports.stream().map(MesProBatchRecordReportDO::getId)
                .filter(Objects::nonNull)
                .min(Long::compareTo)
                .orElseThrow(() -> exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_FORM_LIST_EMPTY));
        return Scope.reportSet(scopeId, report.getSourceFileSha256(), report.getRouteKey());
    }

    private List<MesProBatchRecordReportDO> selectReportsInScope(Scope scope) {
        if (Objects.equals(scope.type(), SCOPE_TYPE_REPORT_SET)) {
            return reportMapper.selectListBySourceFileSha256AndRouteKey(scope.sourceFileSha256(), scope.routeKey());
        }
        return reportMapper.selectListByDefinitionIdAndVersionId(scope.definitionId(), scope.versionId());
    }

    private List<BatchRecordCellLinkFormRespVO> selectFormsInScope(Scope scope, Long routeId) {
        if (Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION)) {
            return List.of(toFormTemplateFormVO(scope.templateVersion() == null
                    ? requireFormTemplateVersion(scope.id())
                    : scope.templateVersion()));
        }
        Map<String, Long> routeProcessIdByReportId = routeProcessIdByReportId(scope, routeId);
        return selectReportsInScope(scope).stream()
                .map(report -> toFormVO(report, routeProcessIdByReportId.get(report.getReportId())))
                .toList();
    }

    private Map<String, Long> routeProcessIdByReportId(Scope scope) {
        return routeProcessIdByReportId(scope, null);
    }

    private Map<String, Long> routeProcessIdByReportId(Scope scope, Long routeId) {
        if (!Objects.equals(scope.type(), SCOPE_TYPE_ROUTE_VERSION) || scope.versionId() == null) {
            return Map.of();
        }
        Map<String, LinkedHashSet<Long>> idsByReportId = new LinkedHashMap<>();
        for (MesProRouteFlowProcessBatchRecordDO binding : selectBindingsInScope(scope, routeId)) {
            if (StrUtil.isBlank(binding.getBatchRecordReportId())
                    || !isFormalBatchRecordBinding(binding)
                    || binding.getRouteProcessId() == null) {
                continue;
            }
            idsByReportId.computeIfAbsent(binding.getBatchRecordReportId(), ignored -> new LinkedHashSet<>())
                    .add(binding.getRouteProcessId());
        }
        Map<String, Long> result = new LinkedHashMap<>();
        idsByReportId.forEach((reportId, routeProcessIds) -> {
            if (routeProcessIds.size() == 1) {
                result.put(reportId, routeProcessIds.iterator().next());
            }
        });
        return result;
    }

    private List<MesProRouteFlowProcessBatchRecordDO> selectBindingsInScope(Scope scope, Long routeId) {
        Set<String> reportIds = selectReportsInScope(scope).stream()
                .map(MesProBatchRecordReportDO::getReportId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (reportIds.isEmpty()) {
            return List.of();
        }
        return routeFlowProcessBatchRecordMapper.selectListByBatchRecordReportIds(reportIds).stream()
                .filter(binding -> routeId == null || Objects.equals(routeId, binding.getRouteId()))
                .toList();
    }

    private TargetSpec resolveTargetSpec(Scope scope, String targetReportId, Long routeId) {
        if (Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION)) {
            String expectedReportId = formTemplateReportId(scope.id());
            if (!Objects.equals(expectedReportId, targetReportId)) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_REPORT_NOT_EXISTS,
                        targetReportId);
            }
            FormTemplateVersionDO templateVersion = scope.templateVersion() == null
                    ? requireFormTemplateVersion(scope.id())
                    : scope.templateVersion();
            return new TargetSpec(expectedReportId, formTemplateReportName(templateVersion), null, null, null);
        }
        MesProBatchRecordReportDO report = requireReport(targetReportId);
        requireReportInScope(scope, report);
        return new TargetSpec(report.getReportId(), report.getReportName(),
                report.getBatchRecordDefinitionId(), report.getBatchRecordVersionId(),
                routeProcessIdByReportId(scope, routeId).get(report.getReportId()));
    }

    private MesProBatchRecordReportDO requireReport(String reportId) {
        MesProBatchRecordReportDO report = reportMapper.selectByReportId(reportId);
        if (report == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_REPORT_NOT_EXISTS,
                    reportId);
        }
        return report;
    }

    private void requireReportInScope(Scope scope, MesProBatchRecordReportDO report) {
        if (Objects.equals(scope.type(), SCOPE_TYPE_REPORT_SET)) {
            if (!Objects.equals(scope.sourceFileSha256(), report.getSourceFileSha256())
                    || !Objects.equals(scope.routeKey(), report.getRouteKey())) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_REPORT_NOT_EXISTS,
                        report.getReportId());
            }
            return;
        }
        if (!Objects.equals(scope.definitionId(), report.getBatchRecordDefinitionId())
                || !Objects.equals(scope.versionId(), report.getBatchRecordVersionId())) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_REPORT_NOT_EXISTS,
                    report.getReportId());
        }
    }

    private BatchRecordCellLinkCellVO requireCell(BatchRecordCellLinkFormCellsRespVO formCells,
                                                  Integer rowIndex, Integer columnIndex) {
        String key = cellKey(rowIndex, columnIndex);
        return formCells.getCells().stream()
                .filter(cell -> Objects.equals(cell.getCellKey(), key))
                .findFirst()
                .orElseThrow(() -> exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_CELL_MISSING,
                        formCells.getReportName(), rowIndex, columnIndex));
    }

    private String normalizeSourceType(String sourceType) {
        String normalized = StrUtil.blankToDefault(StrUtil.trim(sourceType), SOURCE_TYPE_BATCH_RECORD_CELL);
        if (SOURCE_TYPE_BATCH_RECORD_CELL.equals(normalized)
                || SOURCE_TYPE_PRODUCTION_WORK_ORDER.equals(normalized)
                || SOURCE_TYPE_PROCESS_POOL_REPORT.equals(normalized)
                || SOURCE_TYPE_PRODUCTION_PICK_LIST.equals(normalized)
                || isFormTemplateFormalSource(normalized)) {
            return normalized;
        }
        throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                sourceType);
    }

    private boolean isFormTemplateFormalSource(String sourceType) {
        return SOURCE_TYPE_PQC_AGGREGATE_DETAIL.equals(sourceType)
                || SOURCE_TYPE_PRODUCTION_LOSS.equals(sourceType);
    }

    private boolean isPqcAggregateSourceReport(String sourceReportId) {
        return SOURCE_TYPE_PQC_AGGREGATE_DETAIL.equals(StrUtil.trim(sourceReportId));
    }

    private PqcProcessCatalog requirePqcProcessCatalog(Long routeId, Scope scope) {
        if (routeId == null || scope == null
                || (!Objects.equals(scope.type(), SCOPE_TYPE_ROUTE_VERSION)
                && !Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION))) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_PQC_QA_CATALOG_MISSING,
                    routeId);
        }
        MesRouteDccProjectBindingDO binding = routeDccProjectBindingMapper.selectCurrentByRouteId(routeId);
        if (binding == null || binding.getDccProjectCodeId() == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_PQC_QA_CATALOG_MISSING,
                    routeId);
        }
        MesQaInspectionRegulationDO regulation = qaRegulationMapper.selectByDccProjectCodeId(
                binding.getDccProjectCodeId());
        if (regulation == null || regulation.getId() == null || regulation.getCurrentVersionId() == null
                || !"PUBLISHED".equals(StrUtil.trim(regulation.getLifecycleStatus()))) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_PQC_QA_CATALOG_MISSING,
                    routeId);
        }
        MesQaInspectionRegulationVersionDO version = qaRegulationVersionMapper.selectById(
                regulation.getCurrentVersionId());
        if (version == null || version.getId() == null || !"PUBLISHED".equals(StrUtil.trim(version.getLifecycleStatus()))) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_PQC_QA_CATALOG_MISSING,
                    routeId);
        }
        List<MesQaInspectionRegulationProcessDO> processes = qaRegulationProcessMapper.selectListByVersionId(version.getId());
        if (processes == null || processes.isEmpty()) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_PQC_QA_CATALOG_MISSING,
                    routeId);
        }
        return new PqcProcessCatalog(regulation, version, List.copyOf(processes));
    }

    private List<BatchRecordCellLinkPqcProcessVO> toPqcProcessVOList(PqcProcessCatalog catalog, Long routeId) {
        return catalog.processes().stream()
                .map(process -> new BatchRecordCellLinkPqcProcessVO()
                        .setId(process.getId())
                        .setProcessCode(process.getProcessCode())
                        .setProcessName(process.getProcessName())
                        .setSort(process.getSort()))
                .toList();
    }

    private List<BatchRecordCellLinkRouteProcessVO> toRouteProcessVOList(Long routeId) {
        if (routeId == null) {
            return List.of();
        }
        List<MesProRouteProcessDO> routeProcesses = routeProcessMapper.selectListByRouteId(routeId);
        if (routeProcesses == null || routeProcesses.isEmpty()) {
            return List.of();
        }
        Set<Long> processIds = routeProcesses.stream()
                .map(MesProRouteProcessDO::getProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesProProcessDO> processById = processIds.isEmpty()
                ? Map.of()
                : processMapper.selectListByIdsIgnoreDeleted(processIds).stream()
                        .collect(Collectors.toMap(MesProProcessDO::getId, Function.identity(),
                                (left, right) -> left, LinkedHashMap::new));
        return routeProcesses.stream()
                .map(routeProcess -> {
                    MesProProcessDO process = processById.get(routeProcess.getProcessId());
                    return new BatchRecordCellLinkRouteProcessVO()
                            .setId(routeProcess.getId())
                            .setProcessId(routeProcess.getProcessId())
                            .setProcessCode(process == null ? null : process.getCode())
                            .setProcessName(process == null ? null : process.getName())
                            .setSort(routeProcess.getSort())
                            .setBatchRecordReportId(routeProcess.getBatchRecordReportId());
                })
                .toList();
    }

    private List<BatchRecordCellLinkSourceFieldVO> toSourceFieldVOList(Scope scope, Long routeId,
                                                                       Long requestedRouteProcessId,
                                                                       Long requestedQaProcessId,
                                                                       String sourceReportId) {
        List<BatchRecordCellLinkSourceFieldVO> result = new ArrayList<>();
        PRODUCTION_WORK_ORDER_SOURCE_FIELDS.stream()
                .map(field -> new BatchRecordCellLinkSourceFieldVO()
                        .setSourceType(SOURCE_TYPE_PRODUCTION_WORK_ORDER)
                        .setFieldCode(field.code())
                        .setFieldName(field.name())
                        .setValueType(field.valueType()))
                .forEach(result::add);
        if (Objects.equals(scope.type(), SCOPE_TYPE_ROUTE_VERSION)) {
            Long processPoolTargetRouteProcessId = Objects.equals(StrUtil.trim(sourceReportId),
                    PROCESS_POOL_REPORT_SOURCE_REPORT_ID) ? requestedRouteProcessId : null;
            processPoolReportSourceFields(scope, routeId, processPoolTargetRouteProcessId,
                    processPoolTargetRouteProcessId != null).stream()
                    .map(field -> new BatchRecordCellLinkSourceFieldVO()
                            .setSourceType(SOURCE_TYPE_PROCESS_POOL_REPORT)
                            .setFieldCode(field.code())
                            .setFieldName(field.name())
                            .setValueType(field.valueType())
                            .setRouteProcessId(field.routeProcessId())
                            .setDeviceId(field.deviceId())
                            .setDeviceCode(field.deviceCode())
                            .setDeviceName(field.deviceName()))
                    .forEach(result::add);
            if (routeId != null && shouldIncludeProductionPickListSourceFields(sourceReportId)) {
                productionPickListSourceService.listSourceFields(routeId).stream()
                        .map(field -> new BatchRecordCellLinkSourceFieldVO()
                                .setSourceType(SOURCE_TYPE_PRODUCTION_PICK_LIST)
                                .setFieldCode(field.fieldCode())
                                .setFieldName(field.fieldName())
                                .setValueType(field.valueType())
                                .setRouteProcessId(field.routeProcessId()))
                        .forEach(result::add);
            }
            if (isPqcAggregateSourceReport(sourceReportId)) {
                pqcAggregateSourceFields(scope, routeId, requestedRouteProcessId, requestedQaProcessId).stream()
                    .map(field -> new BatchRecordCellLinkSourceFieldVO()
                            .setSourceType(SOURCE_TYPE_PQC_AGGREGATE_DETAIL)
                            .setFieldCode(field.code())
                            .setSourceCellKey(field.sourceCellKey())
                            .setFieldName(field.name())
                            .setValueType(field.valueType())
                            .setRouteProcessId(field.routeProcessId())
                            .setQaProcessId(field.qaProcessId()))
                    .forEach(result::add);
            }
        }
        if (Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION)
                && routeId != null && requestedRouteProcessId != null
                && isPqcAggregateSourceReport(sourceReportId)) {
            pqcAggregateSourceFields(scope, routeId, requestedRouteProcessId, requestedQaProcessId).stream()
                    .map(field -> new BatchRecordCellLinkSourceFieldVO()
                            .setSourceType(SOURCE_TYPE_PQC_AGGREGATE_DETAIL)
                            .setFieldCode(field.code())
                            .setSourceCellKey(field.sourceCellKey())
                            .setFieldName(field.name())
                            .setValueType(field.valueType())
                            .setRouteProcessId(field.routeProcessId())
                            .setQaProcessId(field.qaProcessId()))
                    .forEach(result::add);
        }
        return List.copyOf(result);
    }

    private boolean shouldIncludeProductionPickListSourceFields(String sourceReportId) {
        String normalized = StrUtil.trim(sourceReportId);
        return !Objects.equals(normalized, PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID)
                && !Objects.equals(normalized, PROCESS_POOL_REPORT_SOURCE_REPORT_ID)
                && !Objects.equals(normalized, PQC_AGGREGATE_DETAIL_SOURCE_REPORT_ID);
    }

    private Long resolveTargetRouteProcessId(BatchRecordCellLinkRulesSaveReqVO reqVO, TargetSpec targetReport) {
        Long routeProcessId = targetReport.routeProcessId() == null
                ? reqVO.getRouteProcessId()
                : targetReport.routeProcessId();
        if (routeProcessId == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SCOPE_REQUIRED);
        }
        return routeProcessId;
    }

    private PqcAggregateSourceField requirePqcAggregateSourceField(Scope scope, Long routeId,
                                                                  Long targetRouteProcessId, String fieldCode,
                                                                  String sourceCellKey) {
        String normalized = StrUtil.trim(fieldCode);
        String normalizedSourceCellKey = StrUtil.trim(sourceCellKey);
        Long qaProcessId = parseQaProcessId(normalizedSourceCellKey);
        return pqcAggregateSourceFields(scope, routeId, targetRouteProcessId, qaProcessId).stream()
                .filter(field -> Objects.equals(targetRouteProcessId, field.routeProcessId()))
                .filter(field -> Objects.equals(qaProcessId, field.qaProcessId()))
                .filter(field -> field.code().equals(normalized))
                .filter(field -> field.sourceCellKey().equals(normalizedSourceCellKey))
                .findFirst()
                .orElseThrow(() -> exception(
                        MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                        fieldCode));
    }

    private Long parseQaProcessId(String sourceCellKey) {
        String prefix = "QA_PROCESS:";
        if (StrUtil.isBlank(sourceCellKey) || !sourceCellKey.startsWith(prefix)) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_PQC_QA_PROCESS_MISSING,
                    sourceCellKey);
        }
        int separator = sourceCellKey.indexOf('|', prefix.length());
        if (separator <= prefix.length()) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_PQC_QA_PROCESS_MISSING,
                    sourceCellKey);
        }
        try {
            return Long.valueOf(sourceCellKey.substring(prefix.length(), separator));
        } catch (NumberFormatException ex) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_PQC_QA_PROCESS_MISSING,
                    sourceCellKey);
        }
    }

    private List<PqcAggregateSourceField> pqcAggregateSourceFields(Scope scope, Long routeId,
                                                                   Long targetRouteProcessId,
                                                                   Long qaProcessId) {
        boolean routeVersionScope = Objects.equals(scope.type(), SCOPE_TYPE_ROUTE_VERSION);
        boolean formTemplateScope = Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION);
        if ((!routeVersionScope && !formTemplateScope) || targetRouteProcessId == null
                || (routeVersionScope && scope.versionId() == null)
                || (formTemplateScope && routeId == null)) {
            return List.of();
        }
        PqcProcessCatalog catalog = requirePqcProcessCatalog(routeId, scope);
        if (qaProcessId == null) {
            return List.of();
        }
        MesQaInspectionRegulationProcessDO qaProcess = catalog.processes().stream()
                .filter(process -> Objects.equals(process.getId(), qaProcessId))
                .findFirst()
                .orElseThrow(() -> exception(
                        MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_PQC_QA_PROCESS_MISSING,
                        qaProcessId));
        List<MesQaInspectionRegulationItemDO> items = qaRegulationItemMapper.selectListByVersionId(
                        catalog.version().getId()).stream()
                .filter(item -> Objects.equals(qaProcess.getId(), item.getQaProcessId()))
                .toList();
        if (items.isEmpty()) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_PQC_QA_ITEMS_MISSING,
                    qaProcessId);
        }
        List<PqcAggregateSourceField> fields = new ArrayList<>();
        Set<String> fieldCodes = new LinkedHashSet<>();
        addPqcHeaderFields(fields, fieldCodes, targetRouteProcessId, qaProcessId, items);
        addPqcDccFields(fields, fieldCodes, targetRouteProcessId, qaProcessId, items);
        for (MesQaInspectionRegulationItemDO item : items) {
            String inspectionType = StrUtil.trim(item.getInspectionType());
            String itemCode = StrUtil.trim(item.getItemCode());
            String itemName = StrUtil.trim(item.getItemName());
            if (StrUtil.isBlank(inspectionType) || StrUtil.isBlank(itemCode) || StrUtil.isBlank(itemName)) {
                throw exception(
                        MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                        "PQC_AGGREGATE_DETAIL 项目定义不完整");
            }
            int sampleCount = Math.max(1, item.getFirstInspectionQuantity() == null
                    ? 1 : item.getFirstInspectionQuantity());
            for (int sampleNo = 1; sampleNo <= sampleCount; sampleNo += 1) {
                addPqcItemFields(fields, fieldCodes, targetRouteProcessId, qaProcessId, item, inspectionType,
                        itemCode, itemName, sampleNo);
            }
        }
        return List.copyOf(fields);
    }

    private void addPqcHeaderFields(List<PqcAggregateSourceField> fields, Set<String> fieldCodes,
                                    Long routeProcessId, Long qaProcessId, List<MesQaInspectionRegulationItemDO> items) {
        items.stream().map(MesQaInspectionRegulationItemDO::getInspectionType)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .forEach(inspectionType -> {
                    addPqcField(fields, fieldCodes, routeProcessId, qaProcessId,
                            inspectionType + "|inspectorUserId", inspectionTypeLabel(inspectionType)
                                    + " / 检验员", "NUMBER");
                    addPqcField(fields, fieldCodes, routeProcessId, qaProcessId,
                            inspectionType + "|inspectedAt", inspectionTypeLabel(inspectionType)
                                    + " / 检验时间", "DATETIME");
                    addPqcField(fields, fieldCodes, routeProcessId, qaProcessId,
                            inspectionType + "|reviewerUserId", inspectionTypeLabel(inspectionType)
                                    + " / 复核人", "NUMBER");
                    addPqcField(fields, fieldCodes, routeProcessId, qaProcessId,
                            inspectionType + "|reviewedAt", inspectionTypeLabel(inspectionType)
                                    + " / 复核时间", "DATETIME");
                });
    }

    private void addPqcDccFields(List<PqcAggregateSourceField> fields, Set<String> fieldCodes,
                                 Long routeProcessId, Long qaProcessId, List<MesQaInspectionRegulationItemDO> items) {
        items.stream().map(MesQaInspectionRegulationItemDO::getInspectionType)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .forEach(inspectionType -> {
                    addPqcField(fields, fieldCodes, routeProcessId, qaProcessId,
                            inspectionType + "|DCC|dccProjectId", inspectionTypeLabel(inspectionType)
                                    + " / DCC项目ID", "NUMBER");
                    addPqcField(fields, fieldCodes, routeProcessId, qaProcessId,
                            inspectionType + "|DCC|dccProjectCode", inspectionTypeLabel(inspectionType)
                                    + " / DCC项目代码", "STRING");
                    addPqcField(fields, fieldCodes, routeProcessId, qaProcessId,
                            inspectionType + "|DCC|dccProjectName", inspectionTypeLabel(inspectionType)
                                    + " / DCC项目名称", "STRING");
                });
    }

    private void addPqcItemFields(List<PqcAggregateSourceField> fields, Set<String> fieldCodes,
                                  Long routeProcessId, Long qaProcessId, MesQaInspectionRegulationItemDO item,
                                  String inspectionType, String itemCode, String itemName, int sampleNo) {
        String prefix = inspectionType + "|" + itemCode + "|" + sampleNo + "|";
        String labelPrefix = inspectionTypeLabel(inspectionType) + " / " + itemName + " / 第" + sampleNo + "件 / ";
        addPqcField(fields, fieldCodes, routeProcessId, qaProcessId, prefix + "itemCode", labelPrefix + "检验项目编号", "STRING");
        addPqcField(fields, fieldCodes, routeProcessId, qaProcessId, prefix + "itemName", labelPrefix + "检验项目名称", "STRING");
        addPqcField(fields, fieldCodes, routeProcessId, qaProcessId, prefix + "inspectionMethod", labelPrefix + "检验方法", "STRING");
        addPqcField(fields, fieldCodes, routeProcessId, qaProcessId, prefix + "standardText", labelPrefix + "标准要求", "STRING");
        if (item.getStandardLowerLimit() != null) {
            addPqcField(fields, fieldCodes, routeProcessId, qaProcessId, prefix + "standardLowerLimit", labelPrefix + "下限", "NUMBER");
        }
        if (item.getStandardUpperLimit() != null) {
            addPqcField(fields, fieldCodes, routeProcessId, qaProcessId, prefix + "standardUpperLimit", labelPrefix + "上限", "NUMBER");
        }
        if (StrUtil.isNotBlank(item.getStandardUnit())) {
            addPqcField(fields, fieldCodes, routeProcessId, qaProcessId, prefix + "standardUnit", labelPrefix + "单位", "STRING");
        }
        if (item.getStandardPrecision() != null) {
            addPqcField(fields, fieldCodes, routeProcessId, qaProcessId, prefix + "standardPrecision", labelPrefix + "精度", "NUMBER");
        }
        addPqcField(fields, fieldCodes, routeProcessId, qaProcessId, prefix + "resultType", labelPrefix + "结果类型", "STRING");
        addPqcField(fields, fieldCodes, routeProcessId, qaProcessId, prefix + "measuredValue", labelPrefix + "实测值",
                pqcMeasuredValueType(item.getResultType()));
        addPqcField(fields, fieldCodes, routeProcessId, qaProcessId, prefix + "judgement", labelPrefix + "判定", "STRING");
        addPqcField(fields, fieldCodes, routeProcessId, qaProcessId, prefix + "selectedEquipmentId", labelPrefix + "检验设备ID", "NUMBER");
        addPqcField(fields, fieldCodes, routeProcessId, qaProcessId, prefix + "selectedEquipmentCode", labelPrefix + "检验设备编码", "STRING");
        addPqcField(fields, fieldCodes, routeProcessId, qaProcessId, prefix + "selectedEquipmentName", labelPrefix + "检验设备名称", "STRING");
        addPqcField(fields, fieldCodes, routeProcessId, qaProcessId, prefix + "selectedEquipmentNumber", labelPrefix + "设备编号", "STRING");
    }

    private void addPqcField(List<PqcAggregateSourceField> fields, Set<String> fieldCodes,
                             Long routeProcessId, Long qaProcessId, String code, String name, String valueType) {
        String sourceCellKey = "QA_PROCESS:" + qaProcessId + "|" + code;
        if (fieldCodes.add(sourceCellKey)) {
            fields.add(new PqcAggregateSourceField(code, sourceCellKey, name, valueType, routeProcessId, qaProcessId));
        }
    }

    private String pqcMeasuredValueType(String resultType) {
        return "NUMERIC".equals(StrUtil.trim(resultType)) ? "NUMBER" : "STRING";
    }

    private String inspectionTypeLabel(String inspectionType) {
        return switch (StrUtil.trim(inspectionType)) {
            case "FIRST" -> "首检";
            case "PATROL" -> "巡检";
            case "FINAL" -> "末检";
            default -> StrUtil.trim(inspectionType);
        };
    }

    private MesProductionPickListSourceService.SourceField requireProductionPickListSourceField(
            Long routeId, Long targetRouteProcessId, String fieldCode) {
        String normalized = StrUtil.trim(fieldCode);
        return productionPickListSourceService.listSourceFields(routeId).stream()
                .filter(field -> Objects.equals(targetRouteProcessId, field.routeProcessId()))
                .filter(field -> field.fieldCode().equals(normalized))
                .findFirst()
                .orElseThrow(() -> exception(
                        MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                        fieldCode));
    }

    private WorkOrderSourceField requireWorkOrderSourceField(String fieldCode) {
        String normalized = StrUtil.trim(fieldCode);
        return PRODUCTION_WORK_ORDER_SOURCE_FIELDS.stream()
                .filter(field -> field.code().equals(normalized))
                .findFirst()
                .orElseThrow(() -> exception(
                        MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                        fieldCode));
    }

    private ProcessPoolReportSourceField requireProcessPoolReportSourceField(Scope scope, String fieldCode,
                                                                            Long targetRouteProcessId) {
        String normalized = StrUtil.trim(fieldCode);
        List<ProcessPoolReportSourceField> supportedFields = targetRouteProcessId == null
                ? PROCESS_POOL_REPORT_BASE_SOURCE_FIELDS
                : processPoolReportSourceFields(scope, targetRouteProcessId);
        return supportedFields.stream()
                .filter(field -> field.code().equals(normalized))
                .findFirst()
                .orElseThrow(() -> exception(
                        MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                        fieldCode));
    }

    private List<ProcessPoolReportSourceField> processPoolReportSourceFields(Scope scope) {
        return processPoolReportSourceFields(scope, null, null, false, currentProcessPoolLeaderUserId());
    }

    private List<ProcessPoolReportSourceField> processPoolReportSourceFields(Scope scope, Long targetRouteProcessId) {
        return processPoolReportSourceFields(scope, null, targetRouteProcessId, false,
                currentProcessPoolLeaderUserId());
    }

    private List<ProcessPoolReportSourceField> processPoolReportSourceFields(Scope scope, Long routeId,
                                                                             Long targetRouteProcessId) {
        return processPoolReportSourceFields(scope, routeId, targetRouteProcessId, false,
                currentProcessPoolLeaderUserId());
    }

    private List<ProcessPoolReportSourceField> processPoolReportSourceFields(Scope scope, Long routeId,
                                                                             Long targetRouteProcessId,
                                                                             boolean requireFormalDeviceBinding) {
        return processPoolReportSourceFields(scope, routeId, targetRouteProcessId, requireFormalDeviceBinding,
                currentProcessPoolLeaderUserId());
    }

    private List<ProcessPoolReportSourceField> processPoolReportSourceFields(Scope scope, Long routeId,
                                                                             Long targetRouteProcessId,
                                                                             boolean requireFormalDeviceBinding,
                                                                             Long processPoolLeaderUserId) {
        Map<String, ProcessPoolReportSourceField> fields = new LinkedHashMap<>();
        PROCESS_POOL_REPORT_BASE_SOURCE_FIELDS.stream()
                .filter(field -> targetRouteProcessId == null
                        || !PROCESS_POOL_REPORT_DEVICE_SOURCE_FIELD_CODES.contains(field.code()))
                .forEach(field -> fields.put(processPoolReportFieldMapKey(field), field));
        if (scope.versionId() == null) {
            return List.copyOf(fields.values());
        }
        List<Long> routeProcessIds = targetRouteProcessId == null
                ? selectBindingsInScope(scope, routeId).stream()
                        .filter(this::isFormalBatchRecordBinding)
                        .map(MesProRouteFlowProcessBatchRecordDO::getRouteProcessId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList()
                : List.of(targetRouteProcessId);
        if (routeProcessIds.isEmpty()) {
            return List.copyOf(fields.values());
        }
        Map<Long, List<ProcessPoolReportDevice>> devicesByRouteProcess =
                listProcessPoolReportDevicesByRouteProcess(routeProcessIds, processPoolLeaderUserId);
        List<MesProcessPoolDeviceParameterRuleDO> parameterRules = deviceParameterRuleMapper.selectList(
                new LambdaQueryWrapperX<MesProcessPoolDeviceParameterRuleDO>()
                        .in(MesProcessPoolDeviceParameterRuleDO::getRouteProcessId, routeProcessIds)
                        .eqIfPresent(MesProcessPoolDeviceParameterRuleDO::getLeaderUserId, processPoolLeaderUserId)
                        .eq(MesProcessPoolDeviceParameterRuleDO::getEnabled, Boolean.TRUE)
                        .orderByAsc(MesProcessPoolDeviceParameterRuleDO::getParameterCode)
                        .orderByAsc(MesProcessPoolDeviceParameterRuleDO::getId));
        parameterRules.forEach(rule -> validateProcessPoolReportParameterRule(rule, routeProcessIds,
                processPoolLeaderUserId));
        if (requireFormalDeviceBinding
                && targetRouteProcessId != null
                && devicesByRouteProcess.getOrDefault(targetRouteProcessId, List.of()).isEmpty()) {
            throw exception(
                    MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_PROCESS_POOL_DEVICE_MISSING,
                    targetRouteProcessId);
        }
        Map<Long, List<ProcessPoolReportDeviceGroup>> deviceGroupsByRouteProcess =
                listProcessPoolReportDeviceGroupsByRouteProcess(devicesByRouteProcess);
        deviceGroupsByRouteProcess.forEach((routeProcessId, deviceGroups) ->
                deviceGroups.forEach(deviceGroup -> PROCESS_POOL_REPORT_BASE_SOURCE_FIELDS.stream()
                        .filter(field -> PROCESS_POOL_REPORT_DEVICE_SOURCE_FIELD_CODES.contains(field.code()))
                        .map(field -> field.forDeviceGroup(routeProcessId, deviceGroup, true))
                        .forEach(field -> addProcessPoolReportField(fields, field))));
        for (MesProcessPoolDeviceParameterRuleDO rule : parameterRules) {
            String code = StrUtil.trim(rule.getParameterCode());
            String name = StrUtil.trim(rule.getParameterName());
            ProcessPoolReportDevice device = requireProcessPoolReportDevice(
                    devicesByRouteProcess, rule.getRouteProcessId(), rule.getDeviceId(), code);
            ProcessPoolReportDeviceGroup deviceGroup = new ProcessPoolReportDeviceGroup(device.deviceName());
            addProcessPoolReportParameterFields(fields, rule, deviceGroup, code, name,
                    processPoolReportValueType(rule.getValueType()));
        }
        return List.copyOf(fields.values());
    }

    private Map<Long, List<ProcessPoolReportDeviceGroup>> listProcessPoolReportDeviceGroupsByRouteProcess(
            Map<Long, List<ProcessPoolReportDevice>> devicesByRouteProcess) {
        Map<Long, List<ProcessPoolReportDeviceGroup>> result = new LinkedHashMap<>();
        devicesByRouteProcess.forEach((routeProcessId, devices) -> {
            Set<String> emittedNames = new LinkedHashSet<>();
            List<ProcessPoolReportDeviceGroup> groups = new ArrayList<>();
            for (ProcessPoolReportDevice device : devices) {
                if (emittedNames.add(device.deviceName())) {
                    groups.add(new ProcessPoolReportDeviceGroup(device.deviceName()));
                }
            }
            result.put(routeProcessId, groups);
        });
        return result;
    }

    private ProcessPoolReportDevice requireProcessPoolReportDevice(
            Map<Long, List<ProcessPoolReportDevice>> devicesByRouteProcess,
            Long routeProcessId,
            Long deviceId,
            String parameterCode) {
        return devicesByRouteProcess.getOrDefault(routeProcessId, List.of()).stream()
                .filter(device -> Objects.equals(device.deviceId(), deviceId))
                .findFirst()
                .orElseThrow(() -> exception(
                        MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                        "PROCESS_POOL_REPORT 参数规则未绑定有效设备：routeProcessId=" + routeProcessId
                                + "，deviceId=" + deviceId + "，parameterCode=" + parameterCode));
    }

    private void validateProcessPoolReportParameterRule(MesProcessPoolDeviceParameterRuleDO rule,
                                                        List<Long> routeProcessIds,
                                                        Long processPoolLeaderUserId) {
        String code = StrUtil.trim(rule.getParameterCode());
        String name = StrUtil.trim(rule.getParameterName());
        if (rule.getRouteProcessId() == null || !routeProcessIds.contains(rule.getRouteProcessId())
                || rule.getDeviceId() == null || rule.getDeviceId() <= 0
                || StrUtil.isBlank(code) || StrUtil.isBlank(name) || StrUtil.isBlank(rule.getValueType())) {
            throw exception(
                    MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                    "PROCESS_POOL_REPORT 参数定义不完整");
        }
        if (processPoolLeaderUserId != null && !Objects.equals(rule.getLeaderUserId(), processPoolLeaderUserId)) {
            throw exception(
                    MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                    "PROCESS_POOL_REPORT 参数规则生产组长不匹配：routeProcessId=" + rule.getRouteProcessId()
                            + "，deviceId=" + rule.getDeviceId());
        }
    }

    private Map<Long, List<ProcessPoolReportDevice>> listProcessPoolReportDevicesByRouteProcess(
            List<Long> routeProcessIds, Long processPoolLeaderUserId) {
        Map<Long, Long> processIdByRouteProcessId = new LinkedHashMap<>();
        Map<Long, List<Long>> routeProcessIdsByProcessId = new LinkedHashMap<>();
        for (Long routeProcessId : routeProcessIds) {
            MesProRouteProcessDO routeProcess = routeProcessMapper.selectByIdIgnoreDeleted(routeProcessId);
            if (routeProcess == null || routeProcess.getProcessId() == null) {
                continue;
            }
            processIdByRouteProcessId.put(routeProcessId, routeProcess.getProcessId());
            routeProcessIdsByProcessId.computeIfAbsent(routeProcess.getProcessId(), ignored -> new ArrayList<>())
                    .add(routeProcessId);
        }
        if (processIdByRouteProcessId.isEmpty()) {
            return Map.of();
        }
        List<MesProcessPoolTeamProcessDeviceDO> bindings = processDeviceMapper.selectList(
                new LambdaQueryWrapperX<MesProcessPoolTeamProcessDeviceDO>()
                        .in(MesProcessPoolTeamProcessDeviceDO::getProcessId, processIdByRouteProcessId.values())
                        .eqIfPresent(MesProcessPoolTeamProcessDeviceDO::getLeaderUserId, processPoolLeaderUserId)
                        .eq(MesProcessPoolTeamProcessDeviceDO::getEnabled, Boolean.TRUE)
                        .orderByAsc(MesProcessPoolTeamProcessDeviceDO::getProcessId)
                        .orderByAsc(MesProcessPoolTeamProcessDeviceDO::getId));
        Set<Long> deviceIds = bindings.stream()
                .map(MesProcessPoolTeamProcessDeviceDO::getDeviceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (deviceIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, MesProcessPoolTeamDeviceDO> deviceById = teamDeviceMapper.selectBatchIds(deviceIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(MesProcessPoolTeamDeviceDO::getId, Function.identity(),
                        (left, ignored) -> left, LinkedHashMap::new));
        Map<Long, List<ProcessPoolReportDevice>> result = new LinkedHashMap<>();
        Set<String> emittedDeviceKeys = new LinkedHashSet<>();
        for (MesProcessPoolTeamProcessDeviceDO binding : bindings) {
            if (processPoolLeaderUserId != null && !Objects.equals(binding.getLeaderUserId(), processPoolLeaderUserId)) {
                continue;
            }
            MesProcessPoolTeamDeviceDO device = deviceById.get(binding.getDeviceId());
            if (device == null || !Objects.equals(device.getLeaderUserId(), binding.getLeaderUserId())) {
                throw exception(
                        MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                        "PROCESS_POOL_REPORT 设备主数据缺失或班组归属不一致：deviceId=" + binding.getDeviceId());
            }
            if (!Boolean.TRUE.equals(device.getEnabled())
                    || !Objects.equals(PROCESS_POOL_TEAM_DEVICE_STATUS_ENABLED, device.getDeviceStatus())) {
                continue;
            }
            String deviceCode = StrUtil.trim(device.getDeviceCode());
            String deviceName = StrUtil.trim(device.getDeviceName());
            if (StrUtil.isBlank(deviceCode) || StrUtil.isBlank(deviceName)) {
                throw exception(
                        MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                        "PROCESS_POOL_REPORT 设备编码或名称缺失：deviceId=" + device.getId());
            }
            List<Long> routeProcessIdsForProcess = routeProcessIdsByProcessId.get(binding.getProcessId());
            if (routeProcessIdsForProcess == null) {
                continue;
            }
            ProcessPoolReportDevice processPoolDevice =
                    new ProcessPoolReportDevice(device.getId(), deviceCode, deviceName);
            for (Long routeProcessId : routeProcessIdsForProcess) {
                String emittedKey = routeProcessId + "|" + device.getId();
                if (emittedDeviceKeys.add(emittedKey)) {
                    result.computeIfAbsent(routeProcessId, ignored -> new ArrayList<>()).add(processPoolDevice);
                }
            }
        }
        return result;
    }

    private Long currentProcessPoolLeaderUserId() {
        return SecurityFrameworkUtils.getLoginUserId();
    }

    private boolean isFormalBatchRecordBinding(MesProRouteFlowProcessBatchRecordDO binding) {
        if (binding == null || StrUtil.isBlank(binding.getBatchRecordReportId())) {
            return false;
        }
        String formSlotType = StrUtil.blankToDefault(StrUtil.trim(binding.getFormSlotType()), FORM_SLOT_TYPE_MAIN);
        if (Objects.equals(FORM_SLOT_TYPE_PROCESS_INSPECTION, formSlotType)
                || Objects.equals(FORM_SLOT_TYPE_LOSS_REPORT, formSlotType)) {
            return false;
        }
        String recordCategory = StrUtil.blankToDefault(StrUtil.trim(binding.getRecordCategory()),
                Objects.equals(FORM_SLOT_TYPE_MAIN, formSlotType) ? "BATCH_RECORD" : null);
        return Objects.equals("BATCH_RECORD", recordCategory);
    }

    private void addProcessPoolReportParameterFields(Map<String, ProcessPoolReportSourceField> fields,
                                                     MesProcessPoolDeviceParameterRuleDO rule,
                                                     ProcessPoolReportDeviceGroup deviceGroup,
                                                     String code,
                                                     String name,
                                                     String valueType) {
        addProcessPoolReportField(fields, ProcessPoolReportSourceField.ofRouteProcess(
                "deviceParameterReadings." + code + ".value", name, valueType, rule.getRouteProcessId())
                .forDeviceGroup(rule.getRouteProcessId(), deviceGroup, false));
        addProcessPoolReportField(fields, ProcessPoolReportSourceField.ofRouteProcess(
                "deviceParameterReadings." + code + ".unit", name + "单位", "STRING", rule.getRouteProcessId())
                .forDeviceGroup(rule.getRouteProcessId(), deviceGroup, false));
        addProcessPoolReportField(fields, ProcessPoolReportSourceField.ofRouteProcess(
                "deviceParameterReadings." + code + ".lowerLimit", name + "下限", "NUMBER", rule.getRouteProcessId())
                .forDeviceGroup(rule.getRouteProcessId(), deviceGroup, false));
        addProcessPoolReportField(fields, ProcessPoolReportSourceField.ofRouteProcess(
                "deviceParameterReadings." + code + ".upperLimit", name + "上限", "NUMBER", rule.getRouteProcessId())
                .forDeviceGroup(rule.getRouteProcessId(), deviceGroup, false));
        addProcessPoolReportField(fields, ProcessPoolReportSourceField.ofRouteProcess(
                "deviceParameterReadings." + code + ".parameterStatus", name + "状态", "STRING", rule.getRouteProcessId())
                .forDeviceGroup(rule.getRouteProcessId(), deviceGroup, false));
        addProcessPoolReportField(fields, ProcessPoolReportSourceField.ofRouteProcess(
                "equipmentParameterRules." + code + ".standardText", name + "参考标准", "STRING", rule.getRouteProcessId())
                .forDeviceGroup(rule.getRouteProcessId(), deviceGroup, false));
        addProcessPoolReportField(fields, ProcessPoolReportSourceField.ofRouteProcess(
                "equipmentParameterRules." + code + ".defaultText", name + "默认文本", "STRING", rule.getRouteProcessId())
                .forDeviceGroup(rule.getRouteProcessId(), deviceGroup, false));
        addProcessPoolReportField(fields, ProcessPoolReportSourceField.ofRouteProcess(
                "equipmentParameterRules." + code + ".defaultValue", name + "默认值", "NUMBER", rule.getRouteProcessId())
                .forDeviceGroup(rule.getRouteProcessId(), deviceGroup, false));
    }

    private void addProcessPoolReportField(Map<String, ProcessPoolReportSourceField> fields,
                                           ProcessPoolReportSourceField field) {
        ProcessPoolReportSourceField existing = fields.putIfAbsent(processPoolReportFieldMapKey(field), field);
        if (existing != null && !existing.equals(field)) {
            throw exception(
                    MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                    "PROCESS_POOL_REPORT 字段冲突：" + field.code());
        }
    }

    private String processPoolReportFieldMapKey(ProcessPoolReportSourceField field) {
        return (field.routeProcessId() == null ? "*" : String.valueOf(field.routeProcessId())) + "|" + field.code();
    }

    private String processPoolReportValueType(String parameterValueType) {
        return switch (StrUtil.trim(parameterValueType)) {
            case MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_INTEGER,
                 MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_DECIMAL -> "NUMBER";
            case MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_BOOLEAN -> "BOOLEAN";
            case MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_TEXT_STANDARD,
                 MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_SELECT -> "STRING";
            default -> throw exception(
                    MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                    "PROCESS_POOL_REPORT 参数值类型：" + parameterValueType);
        };
    }

    private static String encodeProcessPoolDeviceGroupScope(String deviceName) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(StrUtil.trim(deviceName).getBytes(StandardCharsets.UTF_8));
    }

    private String requireProcessPoolReportAggregationStrategy(String strategy, String valueType) {
        String normalized = StrUtil.trim(strategy);
        if (StrUtil.isBlank(normalized)) {
            throw exception(
                    MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                    "PROCESS_POOL_REPORT 聚合策略：" + strategy);
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        Set<String> allowed = "NUMBER".equals(valueType)
                ? PROCESS_POOL_REPORT_NUMBER_AGGREGATION_STRATEGIES
                : PROCESS_POOL_REPORT_TEXT_AGGREGATION_STRATEGIES;
        if (!allowed.contains(normalized)) {
            throw exception(
                    MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                    "PROCESS_POOL_REPORT 聚合策略：" + strategy);
        }
        return normalized;
    }

    private Object resolveProductionWorkOrderFieldValue(MesProBatchRecordExecutionDO targetExecution,
                                                        MesProBatchRecordCellLinkRuleDO rule) {
        if (targetExecution.getWorkOrderId() == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_WORK_ORDER_MISSING,
                    targetExecution.getId());
        }
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(targetExecution.getWorkOrderId());
        if (workOrder == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_WORK_ORDER_MISSING,
                    targetExecution.getId());
        }
        WorkOrderSourceField field = requireWorkOrderSourceField(
                StrUtil.blankToDefault(rule.getSourceFieldCode(), rule.getSourceCellKey()));
        if (WORK_ORDER_SOURCE_FIELD_BATCH_CODE.equals(field.code())) {
            return StrUtil.trim(targetExecution.getBatchCode());
        }
        return field.valueExtractor().apply(workOrder);
    }

    private boolean hasPlainValue(Object value) {
        if (value == null) {
            return false;
        }
        return !(value instanceof String text) || StrUtil.isNotBlank(text);
    }

    private MesProBatchRecordCellLinkRuleDO toRuleDO(BatchRecordCellLinkRulesSaveReqVO reqVO, Scope scope,
                                                     BatchRecordCellLinkRuleSaveItemReqVO item,
                                                     SourceSpec source,
                                                     TargetSpec targetReport,
                                                     BatchRecordCellLinkCellVO targetCell,
                                                     String sourceLayoutHash, String targetLayoutHash,
                                                     long ruleVersion) {
        MesProBatchRecordCellLinkRuleDO rule = new MesProBatchRecordCellLinkRuleDO();
        rule.setScopeType(scope.type());
        rule.setScopeId(scope.id());
        rule.setRouteId(reqVO.getRouteId());
        rule.setRouteProcessId(Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION)
                ? reqVO.getRouteProcessId()
                : resolveTargetRouteProcessId(reqVO, targetReport));
        rule.setBatchRecordDefinitionId(scope.definitionId());
        rule.setBatchRecordVersionId(scope.versionId());
        rule.setSourceType(source.sourceType());
        rule.setSourceReportId(source.reportId());
        rule.setSourceReportName(source.reportName());
        rule.setSourceRowIndex(source.rowIndex());
        rule.setSourceColumnIndex(source.columnIndex());
        rule.setSourceCellKey(source.cellKey());
        rule.setSourceFieldCode(source.fieldCode());
        rule.setSourceFieldName(source.fieldName());
        rule.setSourceLabel(StrUtil.blankToDefault(StrUtil.trim(item.getSourceLabel()), source.label()));
        rule.setSourceValueType(source.valueType());
        rule.setTargetReportId(targetReport.reportId());
        rule.setTargetReportName(targetReport.reportName());
        rule.setTargetRowIndex(targetCell.getRowIndex());
        rule.setTargetColumnIndex(targetCell.getColumnIndex());
        rule.setTargetCellKey(targetCell.getCellKey());
        rule.setTargetLabel(StrUtil.blankToDefault(StrUtil.trim(item.getTargetLabel()), targetCell.getLabel()));
        rule.setTargetValueType(targetCell.getValueType());
        rule.setAggregationStrategy(StrUtil.trim(item.getAggregationStrategy()));
        rule.setOverwritePolicy(StrUtil.blankToDefault(item.getOverwritePolicy(), OVERWRITE_POLICY_ONLY_WHEN_EMPTY));
        rule.setTemplateSnapshotHash(DigestUtil.sha256Hex(sourceLayoutHash + "|" + targetLayoutHash));
        rule.setRuleVersion(ruleVersion);
        rule.setEnabled(item.getEnabled() == null || Boolean.TRUE.equals(item.getEnabled()));
        rule.setRemark(StrUtil.trim(item.getRemark()));
        return rule;
    }

    private BatchRecordCellLinkFormRespVO toFormVO(MesProBatchRecordReportDO report, Long routeProcessId) {
        return new BatchRecordCellLinkFormRespVO()
                .setId(report.getId())
                .setBatchRecordName(report.getBatchRecordName())
                .setFormSlotType(report.getFormSlotType())
                .setBatchRecordDefinitionId(report.getBatchRecordDefinitionId())
                .setBatchRecordVersionId(report.getBatchRecordVersionId())
                .setRouteProcessId(routeProcessId)
                .setSourceTableIndex(report.getSourceTableIndex())
                .setTableTitle(report.getTableTitle())
                .setReportId(report.getReportId())
                .setReportCode(report.getReportCode())
                .setReportName(report.getReportName());
    }

    private BatchRecordCellLinkFormRespVO toFormTemplateFormVO(FormTemplateVersionDO templateVersion) {
        return new BatchRecordCellLinkFormRespVO()
                .setId(templateVersion.getId())
                .setBatchRecordName(null)
                .setFormSlotType(null)
                .setBatchRecordDefinitionId(null)
                .setBatchRecordVersionId(null)
                .setSourceTableIndex(null)
                .setTableTitle(templateVersion.getTemplateName())
                .setReportId(formTemplateReportId(templateVersion.getId()))
                .setReportCode(formTemplateReportId(templateVersion.getId()))
                .setReportName(formTemplateReportName(templateVersion));
    }

    private FormTemplateVersionDO requireFormTemplateVersion(Long templateVersionId) {
        if (templateVersionId == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SCOPE_REQUIRED);
        }
        FormTemplateVersionDO templateVersion = templateVersionMapper.selectById(templateVersionId);
        if (templateVersion == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_REPORT_NOT_EXISTS,
                    formTemplateReportId(templateVersionId));
        }
        return templateVersion;
    }

    private TemplateLayout resolveTemplateLayout(FormTemplateVersionDO templateVersion, String reportId) {
        JSONObject schema = parseTemplateJimuSchema(templateVersion, reportId);
        if (schema != null) {
            String sheetLayoutJson = resolveTemplateSheetLayoutJson(schema, reportId);
            if (StrUtil.isNotBlank(sheetLayoutJson) && hasExplicitTemplateCellRules(schema)) {
                return new TemplateLayout(schema, sheetLayoutJson, false);
            }
            if ((schema.containsKey("cellRules") || schema.containsKey("signatureCellMarkers"))
                    && StrUtil.isBlank(templateVersion.getRecognizedSchemaJson())) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                        reportId);
            }
        }
        JSONObject recognizedSchema = buildTemplateRecognizedFieldsSchema(templateVersion, reportId);
        String recognizedSheetLayoutJson = resolveTemplateSheetLayoutJson(recognizedSchema, reportId);
        return new TemplateLayout(recognizedSchema, recognizedSheetLayoutJson, true);
    }

    private boolean hasExplicitTemplateCellRules(JSONObject schema) {
        return !parseTemplateCellRules(schema).isEmpty();
    }

    private JSONObject parseTemplateJimuSchema(FormTemplateVersionDO templateVersion, String reportId) {
        if (templateVersion == null || StrUtil.isBlank(templateVersion.getJimuSchemaJson())) {
            return null;
        }
        try {
            JSONObject schema = JSON.parseObject(templateVersion.getJimuSchemaJson());
            if (schema == null) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                        reportId);
            }
            return schema;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                    reportId);
        }
    }

    private JSONObject buildTemplateRecognizedFieldsSchema(FormTemplateVersionDO templateVersion, String reportId) {
        if (templateVersion == null || StrUtil.isBlank(templateVersion.getRecognizedSchemaJson())) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                    reportId);
        }
        try {
            JSONArray recognizedFields = JSON.parseArray(templateVersion.getRecognizedSchemaJson());
            if (recognizedFields == null || recognizedFields.isEmpty()) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                        reportId);
            }
            JSONArray rules = buildTemplateRecognizedFieldRules(recognizedFields, reportId);
            JSONObject schema = buildTemplateRecognizedFieldsBaseLayout(templateVersion, rules, reportId);
            schema.put("cellRules", rules);
            JSONArray signatureMarkers = buildTemplateSignatureMarkers(rules, reportId);
            if (!signatureMarkers.isEmpty()) {
                schema.put("signatureCellMarkers", signatureMarkers);
            }
            return schema;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                    reportId);
        }
    }

    private String resolveTemplateSheetLayoutJson(JSONObject schema, String reportId) {
        Object sheetLayout = schema == null ? null : schema.get("sheetLayoutJson");
        if (sheetLayout instanceof String text) {
            return text;
        }
        if (sheetLayout instanceof JSONObject object) {
            return JSON.toJSONString(object);
        }
        if (sheetLayout != null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                    reportId);
        }
        Object layout = schema == null ? null : schema.get("layout");
        if (layout instanceof String text) {
            return text;
        }
        if (layout instanceof JSONObject object) {
            return JSON.toJSONString(object);
        }
        if (layout != null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                    reportId);
        }
        return schema != null && schema.getJSONObject("rows") != null ? JSON.toJSONString(schema) : null;
    }

    private JSONArray buildTemplateRecognizedFieldRules(JSONArray recognizedFields, String reportId) {
        JSONArray rules = new JSONArray();
        for (int index = 0; index < recognizedFields.size(); index++) {
            JSONObject field = toTemplateJsonObject(recognizedFields.get(index), reportId);
            String label = StrUtil.blankToDefault(StrUtil.trim(field.getString("label")),
                    StrUtil.trim(field.getString("fieldCode")));
            if (StrUtil.isBlank(label)) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                        reportId);
            }
            int labelColumnIndex = index % 2 == 0 ? 0 : 2;
            int inputColumnIndex = labelColumnIndex + 1;
            JSONObject rule = new JSONObject();
            rule.put("rowIndex", index / 2 + 3);
            rule.put("columnIndex", inputColumnIndex);
            rule.put("valueType", templateRecognizedFieldValueType(field.getString("fieldType")));
            rule.put("componentFlag", templateRecognizedFieldComponentFlag(field.getString("fieldType")));
            rule.put("required", Boolean.TRUE.equals(field.getBoolean("required")));
            rule.put("label", label);
            rule.put("placeholder", "checkbox".equals(StrUtil.trimToEmpty(field.getString("fieldType"))
                    .toLowerCase(Locale.ROOT)) ? "□" : "");
            rule.put("source", "AUTO");
            rule.put("reviewed", false);
            rules.add(rule);
        }
        return rules;
    }

    private JSONObject buildTemplateRecognizedFieldsBaseLayout(
            FormTemplateVersionDO templateVersion, JSONArray rules, String reportId) {
        JSONObject schema = new JSONObject();
        JSONObject cols = new JSONObject();
        cols.put("0", JSON.parseObject("{\"width\":140}"));
        cols.put("1", JSON.parseObject("{\"width\":220}"));
        cols.put("2", JSON.parseObject("{\"width\":140}"));
        cols.put("3", JSON.parseObject("{\"width\":220}"));
        schema.put("cols", cols);
        JSONObject rows = new JSONObject();
        rows.put("0", templateRecognizedRow(28, Map.of(
                "0", templateRecognizedTextCell(templateVersion.getTemplateName(), List.of(0, 1)),
                "2", templateRecognizedTextCell("记录编号", null),
                "3", templateRecognizedTextCell("TPL-" + templateVersion.getTemplateId(), null))));
        rows.put("1", templateRecognizedRow(28, Map.of(
                "0", templateRecognizedTextCell("版本", null),
                "1", templateRecognizedTextCell(templateVersion.getVersionNo(), null),
                "2", templateRecognizedTextCell("版本状态", null),
                "3", templateRecognizedTextCell(
                        templateStatusLabel(templateVersion.getStatus()), null))));
        rows.put("2", templateRecognizedRow(26, Map.of(
                "0", templateRecognizedTextCell("识别字段", List.of(0, 3)))));
        for (Object rawRule : rules) {
            JSONObject rule = toTemplateJsonObject(rawRule, reportId);
            String rowKey = String.valueOf(rule.getInteger("rowIndex"));
            JSONObject row = rows.getJSONObject(rowKey);
            if (row == null) {
                row = templateRecognizedRow(36, Map.of());
                rows.put(rowKey, row);
            }
            JSONObject cells = row.getJSONObject("cells");
            int labelColumnIndex = Math.max(0, rule.getInteger("columnIndex") - 1);
            String labelText = rule.getString("label") + (Boolean.TRUE.equals(rule.getBoolean("required")) ? " *" : "");
            cells.put(String.valueOf(labelColumnIndex), templateRecognizedTextCell(labelText, null));
            cells.put(String.valueOf(rule.getInteger("columnIndex")), templateRecognizedTextCell("", null));
        }
        schema.put("rows", rows);
        return schema;
    }

    private JSONObject templateRecognizedRow(int height, Map<String, JSONObject> cells) {
        JSONObject row = new JSONObject();
        row.put("height", height);
        JSONObject cellMap = new JSONObject();
        cellMap.putAll(cells);
        row.put("cells", cellMap);
        return row;
    }

    private JSONObject templateRecognizedTextCell(String text, List<Integer> merge) {
        JSONObject cell = new JSONObject();
        cell.put("text", StrUtil.blankToDefault(text, ""));
        if (merge != null) {
            cell.put("merge", merge);
        }
        return cell;
    }

    private JSONArray buildTemplateSignatureMarkers(JSONArray rules, String reportId) {
        JSONArray markers = new JSONArray();
        for (Object rawRule : rules) {
            JSONObject rule = toTemplateJsonObject(rawRule, reportId);
            String valueType = StrUtil.trimToEmpty(rule.getString("valueType"));
            String componentFlag = StrUtil.trimToEmpty(rule.getString("componentFlag")).toLowerCase(Locale.ROOT);
            if (!"SIGNATURE".equals(valueType) && !componentFlag.contains("signature")) {
                continue;
            }
            JSONObject marker = new JSONObject();
            marker.put("rowIndex", rule.getInteger("rowIndex"));
            marker.put("columnIndex", rule.getInteger("columnIndex"));
            marker.put("enabled", true);
            marker.put("actionType", "FORM_REVIEW");
            marker.put("label", StrUtil.blankToDefault(rule.getString("label"), "签名"));
            marker.put("signatureCellKey", rule.getInteger("rowIndex") + ":" + rule.getInteger("columnIndex"));
            markers.add(marker);
        }
        return markers;
    }

    private String templateRecognizedFieldValueType(String fieldType) {
        return switch (StrUtil.trimToEmpty(fieldType).toLowerCase(Locale.ROOT)) {
            case "number" -> "NUMBER";
            case "date" -> "DATE";
            case "datetime" -> "DATETIME";
            case "checkbox" -> "BOOLEAN";
            case "signature" -> "SIGNATURE";
            default -> "STRING";
        };
    }

    private String templateRecognizedFieldComponentFlag(String fieldType) {
        return switch (StrUtil.trimToEmpty(fieldType).toLowerCase(Locale.ROOT)) {
            case "number" -> "input-number";
            case "date" -> "date";
            case "datetime" -> "datetime";
            case "checkbox" -> "checkbox";
            case "signature" -> "signature";
            case "textarea" -> "textarea";
            default -> "input-text";
        };
    }

    private String templateStatusLabel(String status) {
        return switch (StrUtil.trimToEmpty(status)) {
            case "DRAFT" -> "草稿";
            case "PUBLISHED" -> "已发布";
            case "DISABLED" -> "已停用";
            case "OBSOLETE" -> "已作废";
            default -> StrUtil.blankToDefault(status, "");
        };
    }

    private JSONObject toTemplateJsonObject(Object rawValue, String reportId) {
        if (rawValue instanceof JSONObject object) {
            return object;
        }
        if (rawValue == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                    reportId);
        }
        JSONObject object = JSON.parseObject(JSON.toJSONString(rawValue));
        if (object == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                    reportId);
        }
        return object;
    }

    private List<BatchRecordReportCellRuleVO> parseTemplateCellRules(JSONObject schema) {
        Object rawRules = schema == null ? null : schema.get("cellRules");
        if (rawRules == null) {
            return List.of();
        }
        try {
            if (rawRules instanceof JSONArray array) {
                return JSON.parseArray(array.toJSONString(), BatchRecordReportCellRuleVO.class);
            }
            if (rawRules instanceof String text && StrUtil.isNotBlank(text)) {
                return JSON.parseArray(text, BatchRecordReportCellRuleVO.class);
            }
            return List.of();
        } catch (Exception ex) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                    "cellRules");
        }
    }

    private Set<String> parseTemplateSignatureCellMarkers(JSONObject schema) {
        JSONArray markers = schema == null ? null : schema.getJSONArray("signatureCellMarkers");
        if (markers == null || markers.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (int index = 0; index < markers.size(); index++) {
            JSONObject marker = markers.getJSONObject(index);
            Integer rowIndex = marker == null ? null : marker.getInteger("rowIndex");
            Integer columnIndex = marker == null ? null : marker.getInteger("columnIndex");
            if (rowIndex != null && columnIndex != null) {
                result.add(cellKey(rowIndex, columnIndex));
            }
        }
        return result;
    }

    private void putTemplateRecognizedFieldCells(List<BatchRecordCellLinkCellVO> cells,
                                                 List<BatchRecordReportCellRuleVO> rules,
                                                 Set<String> signatureMarkers) {
        if (rules == null) {
            return;
        }
        for (BatchRecordReportCellRuleVO rule : rules) {
            if (rule.getRowIndex() == null || rule.getColumnIndex() == null) {
                continue;
            }
            String cellKey = cellKey(rule.getRowIndex(), rule.getColumnIndex());
            boolean signatureCell = isTemplateSignatureCell(rule, null, cellKey, signatureMarkers);
            boolean linkable = !signatureCell;
            cells.add(new BatchRecordCellLinkCellVO()
                    .setRowIndex(rule.getRowIndex())
                    .setColumnIndex(rule.getColumnIndex())
                    .setCellKey(cellKey)
                    .setSourceType(SOURCE_TYPE_BATCH_RECORD_CELL)
                    .setLabel(StrUtil.blankToDefault(rule.getLabel(), cellKey))
                    .setValueType(StrUtil.blankToDefault(rule.getValueType(), "STRING"))
                    .setComponentFlag(rule.getComponentFlag())
                    .setRequired(Boolean.TRUE.equals(rule.getRequired()))
                    .setReadonly(!linkable)
                    .setSignatureCell(signatureCell)
                    .setLinkableAsSource(false)
                    .setLinkableAsTarget(linkable));
        }
    }

    private boolean isTemplateSignatureCell(BatchRecordReportCellRuleVO rule, JSONObject cell, String cellKey,
                                            Set<String> signatureMarkers) {
        if (signatureMarkers.contains(cellKey) || MesProBatchRecordCellRuleSupport.hasValidSignatureMarker(cell)) {
            return true;
        }
        if (rule == null) {
            return false;
        }
        return "SIGNATURE".equals(MesProBatchRecordCellRuleSupport.normalizeValueType(rule.getValueType()))
                || StrUtil.containsIgnoreCase(rule.getComponentFlag(), "signature");
    }

    private List<BatchRecordCellLinkRuleVO> toRuleVOList(List<MesProBatchRecordCellLinkRuleDO> rules) {
        return rules.stream().map(this::toRuleVO).toList();
    }

    private BatchRecordCellLinkRuleVO toRuleVO(MesProBatchRecordCellLinkRuleDO rule) {
        return new BatchRecordCellLinkRuleVO()
                .setId(rule.getId())
                .setScopeType(rule.getScopeType())
                .setScopeId(rule.getScopeId())
                .setRouteId(rule.getRouteId())
                .setRouteProcessId(rule.getRouteProcessId())
                .setBatchRecordDefinitionId(rule.getBatchRecordDefinitionId())
                .setBatchRecordVersionId(rule.getBatchRecordVersionId())
                .setSourceType(normalizeSourceType(rule.getSourceType()))
                .setSourceReportId(rule.getSourceReportId())
                .setSourceReportName(rule.getSourceReportName())
                .setSourceRowIndex(rule.getSourceRowIndex())
                .setSourceColumnIndex(rule.getSourceColumnIndex())
                .setSourceCellKey(rule.getSourceCellKey())
                .setSourceFieldCode(rule.getSourceFieldCode())
                .setSourceFieldName(rule.getSourceFieldName())
                .setSourceLabel(rule.getSourceLabel())
                .setSourceValueType(rule.getSourceValueType())
                .setTargetReportId(rule.getTargetReportId())
                .setTargetReportName(rule.getTargetReportName())
                .setTargetRowIndex(rule.getTargetRowIndex())
                .setTargetColumnIndex(rule.getTargetColumnIndex())
                .setTargetCellKey(rule.getTargetCellKey())
                .setTargetLabel(rule.getTargetLabel())
                .setTargetValueType(rule.getTargetValueType())
                .setAggregationStrategy(rule.getAggregationStrategy())
                .setOverwritePolicy(rule.getOverwritePolicy())
                .setTemplateSnapshotHash(rule.getTemplateSnapshotHash())
                .setRuleVersion(rule.getRuleVersion())
                .setEnabled(rule.getEnabled())
                .setRemark(rule.getRemark());
    }

    private BatchRecordCellLinkPrefillItemVO basePrefillItem(MesProBatchRecordCellLinkRuleDO rule) {
        return new BatchRecordCellLinkPrefillItemVO()
                .setTargetCellKey(rule.getTargetCellKey())
                .setTargetRowIndex(rule.getTargetRowIndex())
                .setTargetColumnIndex(rule.getTargetColumnIndex())
                .setSourceType(normalizeSourceType(rule.getSourceType()))
                .setSourceReportId(rule.getSourceReportId())
                .setSourceReportName(rule.getSourceReportName())
                .setSourceCellKey(rule.getSourceCellKey())
                .setSourceFieldCode(rule.getSourceFieldCode())
                .setSourceFieldName(rule.getSourceFieldName())
                .setSourceLabel(rule.getSourceLabel())
                .setRuleId(rule.getId())
                .setRuleVersion(rule.getRuleVersion())
                .setOverwritePolicy(rule.getOverwritePolicy());
    }

    private JSONObject parseLayout(String sheetLayoutJson, String reportId) {
        if (StrUtil.isBlank(sheetLayoutJson)) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                    reportId);
        }
        try {
            return JSON.parseObject(sheetLayoutJson);
        } catch (Exception ex) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                    reportId);
        }
    }

    private Map<String, JSONObject> parseCellValues(String cellValuesJson, Long executionId) {
        if (StrUtil.isBlank(cellValuesJson)) {
            return Map.of();
        }
        try {
            JSONArray array = JSON.parseArray(cellValuesJson);
            Map<String, JSONObject> result = new LinkedHashMap<>();
            for (int i = 0; i < array.size(); i++) {
                JSONObject item = array.getJSONObject(i);
                Integer rowIndex = item.getInteger("rowIndex");
                Integer columnIndex = item.getInteger("columnIndex");
                if (rowIndex != null && columnIndex != null) {
                    result.put(cellKey(rowIndex, columnIndex), item);
                }
            }
            return result;
        } catch (Exception ex) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_CELL_VALUES_INVALID,
                    executionId);
        }
    }

    private void putCellRules(Map<String, BatchRecordReportCellRuleVO> ruleMap, List<BatchRecordReportCellRuleVO> rules) {
        if (rules == null) {
            return;
        }
        rules.stream()
                .filter(rule -> rule.getRowIndex() != null && rule.getColumnIndex() != null)
                .collect(Collectors.toMap(rule -> cellKey(rule.getRowIndex(), rule.getColumnIndex()),
                        Function.identity(), (left, right) -> right, LinkedHashMap::new))
                .forEach(ruleMap::put);
    }

    private String resolveLabel(BatchRecordReportCellRuleVO rule, JSONObject cell, Integer rowIndex, Integer columnIndex) {
        if (rule != null && StrUtil.isNotBlank(rule.getLabel())) {
            return rule.getLabel();
        }
        String text = cell == null ? null : StrUtil.blankToDefault(cell.getString("text"), cell.getString("value"));
        return StrUtil.blankToDefault(StrUtil.trim(text), "第 " + (rowIndex + 1) + " 行第 " + (columnIndex + 1) + " 列");
    }

    private boolean hasValue(JSONObject cellValue) {
        if (cellValue == null || !cellValue.containsKey("value")) {
            return false;
        }
        Object value = cellValue.get("value");
        if (value == null) {
            return false;
        }
        return !(value instanceof String text) || StrUtil.isNotBlank(text);
    }

    private String cellKey(Integer rowIndex, Integer columnIndex) {
        return rowIndex + ":" + columnIndex;
    }

    private boolean hasFormDataValue(Object value) {
        if (value == null) {
            return false;
        }
        return !(value instanceof String text) || StrUtil.isNotBlank(text);
    }

    private String formTemplateReportId(Long templateVersionId) {
        return FORM_TEMPLATE_REPORT_PREFIX + templateVersionId;
    }

    private boolean isFormTemplateReportId(String reportId) {
        return StrUtil.startWith(reportId, FORM_TEMPLATE_REPORT_PREFIX);
    }

    private Long parseFormTemplateReportId(String reportId) {
        if (!isFormTemplateReportId(reportId)) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_REPORT_NOT_EXISTS,
                    reportId);
        }
        try {
            return Long.valueOf(reportId.substring(FORM_TEMPLATE_REPORT_PREFIX.length()));
        } catch (Exception ex) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_REPORT_NOT_EXISTS,
                    reportId);
        }
    }

    private String formTemplateReportName(FormTemplateVersionDO templateVersion) {
        String templateName = StrUtil.blankToDefault(templateVersion.getTemplateName(),
                formTemplateReportId(templateVersion.getId()));
        return StrUtil.isBlank(templateVersion.getVersionNo())
                ? templateName
                : templateName + " " + StrUtil.trim(templateVersion.getVersionNo());
    }

    private record WorkOrderSourceField(String code, String name, String valueType,
                                        Function<MesProWorkOrderDO, Object> valueExtractor) {
    }

    private record ProcessPoolReportSourceField(String code, String name, String valueType, Long routeProcessId,
                                                Long deviceId, String deviceCode, String deviceName) {
        static ProcessPoolReportSourceField base(String code, String name, String valueType) {
            return new ProcessPoolReportSourceField(code, name, valueType, null, null, null, null);
        }

        static ProcessPoolReportSourceField ofRouteProcess(String code, String name, String valueType,
                                                           Long routeProcessId) {
            return new ProcessPoolReportSourceField(code, name, valueType, routeProcessId, null, null, null);
        }

        ProcessPoolReportSourceField forDevice(Long routeProcessId, ProcessPoolReportDevice device) {
            return new ProcessPoolReportSourceField(
                    code + PROCESS_POOL_DEVICE_SCOPE_SEPARATOR + device.deviceId(),
                    name + "（" + device.deviceName() + " / " + device.deviceCode() + "）",
                    valueType, routeProcessId, device.deviceId(), device.deviceCode(), device.deviceName());
        }

        ProcessPoolReportSourceField forDeviceGroup(Long routeProcessId, ProcessPoolReportDeviceGroup deviceGroup,
                                                    boolean includeDeviceGroupInName) {
            String scopedName = includeDeviceGroupInName ? name + "（" + deviceGroup.deviceName() + "）" : name;
            return new ProcessPoolReportSourceField(
                    code + PROCESS_POOL_DEVICE_GROUP_SCOPE_SEPARATOR
                            + encodeProcessPoolDeviceGroupScope(deviceGroup.deviceName()),
                    scopedName, valueType, routeProcessId, null, null, deviceGroup.deviceName());
        }
    }

    private record ProcessPoolReportDevice(Long deviceId, String deviceCode, String deviceName) {
    }

    private record ProcessPoolReportDeviceGroup(String deviceName) {
    }

    private record PqcAggregateSourceField(String code, String sourceCellKey, String name, String valueType,
                                           Long routeProcessId, Long qaProcessId) {
    }

    private record PqcProcessCatalog(MesQaInspectionRegulationDO regulation,
                                     MesQaInspectionRegulationVersionDO version,
                                     List<MesQaInspectionRegulationProcessDO> processes) {
    }

    private record TargetSpec(String reportId, String reportName, Long batchRecordDefinitionId,
                              Long batchRecordVersionId, Long routeProcessId) {
    }

    private record SourceSpec(String sourceType, String reportId, String reportName, Integer rowIndex,
                              Integer columnIndex, String cellKey, String fieldCode, String fieldName,
                              String label, String valueType, String snapshotHashPart) {

        static SourceSpec batchRecordCell(MesProBatchRecordReportDO report,
                                          BatchRecordCellLinkCellVO cell,
                                          String layoutSnapshotHash) {
            return new SourceSpec(SOURCE_TYPE_BATCH_RECORD_CELL, report.getReportId(), report.getReportName(),
                    cell.getRowIndex(), cell.getColumnIndex(), cell.getCellKey(), null, null,
                    cell.getLabel(), cell.getValueType(), layoutSnapshotHash);
        }

        static SourceSpec productionWorkOrder(WorkOrderSourceField field) {
            return new SourceSpec(SOURCE_TYPE_PRODUCTION_WORK_ORDER, PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID,
                    PRODUCTION_WORK_ORDER_SOURCE_REPORT_NAME, -1, -1, field.code(), field.code(), field.name(),
                    field.name(), field.valueType(), SOURCE_TYPE_PRODUCTION_WORK_ORDER + ":" + field.code());
        }

        static SourceSpec processPoolReport(ProcessPoolReportSourceField field) {
            return new SourceSpec(SOURCE_TYPE_PROCESS_POOL_REPORT, PROCESS_POOL_REPORT_SOURCE_REPORT_ID,
                    PROCESS_POOL_REPORT_SOURCE_REPORT_NAME, -1, -1, field.code(), field.code(), field.name(),
                    field.name(), field.valueType(), SOURCE_TYPE_PROCESS_POOL_REPORT + ":" + field.code());
        }

        static SourceSpec productionPickList(MesProductionPickListSourceService.SourceField field) {
            return new SourceSpec(SOURCE_TYPE_PRODUCTION_PICK_LIST,
                    MesProductionPickListSourceService.SOURCE_REPORT_ID,
                    MesProductionPickListSourceService.SOURCE_REPORT_NAME,
                    -1, -1, field.fieldCode(), field.fieldCode(), field.fieldName(),
                    field.fieldName(), field.valueType(), SOURCE_TYPE_PRODUCTION_PICK_LIST + ":" + field.fieldCode());
        }

        static SourceSpec pqcAggregate(PqcAggregateSourceField field) {
            return new SourceSpec(SOURCE_TYPE_PQC_AGGREGATE_DETAIL,
                    PQC_AGGREGATE_DETAIL_SOURCE_REPORT_ID,
                    PQC_AGGREGATE_DETAIL_SOURCE_REPORT_NAME,
                    -1, -1, field.sourceCellKey(), field.code(), field.name(),
                    field.name(), field.valueType(), SOURCE_TYPE_PQC_AGGREGATE_DETAIL + ":" + field.sourceCellKey());
        }

        static SourceSpec formTemplateFormalSource(String sourceType, BatchRecordCellLinkRuleSaveItemReqVO item) {
            String fieldCode = StrUtil.trim(item.getSourceFieldCode());
            if (StrUtil.isBlank(fieldCode)) {
                throw exception(
                        MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                        sourceType);
            }
            String fieldName = StrUtil.blankToDefault(StrUtil.trim(item.getSourceFieldName()), fieldCode);
            String label = StrUtil.blankToDefault(StrUtil.trim(item.getSourceLabel()), fieldName);
            String cellKey = "SUMMARY|" + fieldCode;
            return new SourceSpec(sourceType, sourceType, sourceType, -1, -1, cellKey, fieldCode, fieldName,
                    label, "STRING", cellKey);
        }

        String uniqueKey() {
            return sourceType + ":" + reportId + ":" + cellKey;
        }
    }

    private record TemplateLayout(JSONObject schema, String sheetLayoutJson, boolean recognizedProjection) {
    }

    private record Scope(String type, Long id, Long definitionId, Long versionId, String sourceFileSha256,
                         String routeKey, FormTemplateVersionDO templateVersion) {

        static Scope routeVersion(Long definitionId, Long versionId) {
            return new Scope(SCOPE_TYPE_ROUTE_VERSION, versionId, definitionId, versionId, null, null, null);
        }

        static Scope reportSet(Long scopeId, String sourceFileSha256, String routeKey) {
            return new Scope(SCOPE_TYPE_REPORT_SET, scopeId, null, null, sourceFileSha256, routeKey, null);
        }

        static Scope formTemplateVersion(Long templateVersionId) {
            return new Scope(SCOPE_TYPE_FORM_TEMPLATE_VERSION, templateVersionId, null, null, null, null, null);
        }

        static Scope formTemplateVersion(FormTemplateVersionDO templateVersion) {
            return new Scope(SCOPE_TYPE_FORM_TEMPLATE_VERSION, templateVersion.getId(),
                    null, null, null, null, templateVersion);
        }
    }
}
