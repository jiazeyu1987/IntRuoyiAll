package cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
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
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkSourceFieldVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkWorkbenchContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRuleVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRulesRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordCellLinkRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
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

import java.util.ArrayList;
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
    private static final String PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID = "PRODUCTION_WORK_ORDER";
    private static final String PRODUCTION_WORK_ORDER_SOURCE_REPORT_NAME = "生产工单";
    private static final String WORK_ORDER_SOURCE_FIELD_BATCH_CODE = "batchCode";
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

    @Resource
    private MesProBatchRecordCellLinkRuleMapper ruleMapper;
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

    @Override
    public BatchRecordCellLinkWorkbenchContextRespVO getWorkbenchContext(Long routeId, Long definitionId,
                                                                         Long versionId, String sourceReportId,
                                                                         Long templateId, String versionNo) {
        Scope scope = resolveQueryScope(definitionId, versionId, sourceReportId, templateId, versionNo);
        List<BatchRecordCellLinkFormRespVO> forms = selectFormsInScope(scope);
        if (forms.isEmpty()) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_FORM_LIST_EMPTY);
        }
        Set<String> reportIds = forms.stream().map(BatchRecordCellLinkFormRespVO::getReportId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String defaultSourceReportId = Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION)
                ? PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID
                : StrUtil.isNotBlank(sourceReportId) ? sourceReportId : forms.get(0).getReportId();
        if (Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION)
                && StrUtil.isNotBlank(sourceReportId)
                && !Objects.equals(sourceReportId, PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID)) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_REPORT_NOT_EXISTS,
                    sourceReportId);
        }
        if (!reportIds.contains(defaultSourceReportId)) {
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
        return new BatchRecordCellLinkWorkbenchContextRespVO()
                .setScopeType(scope.type())
                .setScopeId(scope.id())
                .setRouteId(routeId)
                .setBatchRecordDefinitionId(scope.definitionId())
                .setBatchRecordVersionId(scope.versionId())
                .setForms(forms)
                .setSourceFields(toWorkOrderSourceFieldVOList())
                .setDefaultSourceReportId(defaultSourceReportId)
                .setDefaultTargetReportId(defaultTargetReportId)
                .setRules(toRuleVOList(ruleMapper.selectListByScope(scope.type(), scope.id())));
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
        Map<String, BatchRecordReportCellRuleVO> ruleMap = new LinkedHashMap<>();
        putCellRules(ruleMap, parseTemplateCellRules(schema));
        Set<String> signatureMarkers = parseTemplateSignatureCellMarkers(schema);
        List<BatchRecordCellLinkCellVO> cells = new ArrayList<>();
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
                    reportId -> resolveTargetSpec(scope, reportId));
            BatchRecordCellLinkFormCellsRespVO targetCells = cellsCache.computeIfAbsent(item.getTargetReportId(),
                    reportId -> getFormCells(reportId, scope.versionId()));
            SourceSpec sourceSpec;
            if (SOURCE_TYPE_PRODUCTION_WORK_ORDER.equals(sourceType)) {
                WorkOrderSourceField sourceField = requireWorkOrderSourceField(
                        StrUtil.blankToDefault(item.getSourceFieldCode(), item.getSourceCellKey()));
                sourceSpec = SourceSpec.productionWorkOrder(sourceField);
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
        if (workOrderId == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_WORK_ORDER_MISSING,
                    templateVersion.getId());
        }
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_WORK_ORDER_MISSING,
                    templateVersion.getId());
        }
        for (MesProBatchRecordCellLinkRuleDO rule : rules) {
            if (!SOURCE_TYPE_PRODUCTION_WORK_ORDER.equals(normalizeSourceType(rule.getSourceType()))) {
                throw exception(
                        MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                        rule.getSourceType());
            }
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
            if (hasFormDataValue(result.get(targetCellKey))
                    && OVERWRITE_POLICY_ONLY_WHEN_EMPTY.equals(rule.getOverwritePolicy())) {
                continue;
            }
            result.put(targetCellKey, sourceValue);
        }
        return result;
    }

    private Object resolveFormTemplateWorkOrderFieldValue(MesProWorkOrderDO workOrder, WorkOrderSourceField field,
                                                          String executionBatchCode) {
        if (WORK_ORDER_SOURCE_FIELD_BATCH_CODE.equals(field.code())) {
            return StrUtil.trim(executionBatchCode);
        }
        return field.valueExtractor().apply(workOrder);
    }

    private Scope resolveQueryScope(Long definitionId, Long versionId, String sourceReportId,
                                    Long templateId, String versionNo) {
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
        if (StrUtil.isNotBlank(sourceReportId)) {
            MesProBatchRecordReportDO report = requireReport(sourceReportId);
            if (report.getBatchRecordDefinitionId() != null && report.getBatchRecordVersionId() != null) {
                return Scope.routeVersion(report.getBatchRecordDefinitionId(), report.getBatchRecordVersionId());
            }
            return resolveReportSetScope(report);
        }
        throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SCOPE_REQUIRED);
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

    private List<BatchRecordCellLinkFormRespVO> selectFormsInScope(Scope scope) {
        if (Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION)) {
            return List.of(toFormTemplateFormVO(scope.templateVersion() == null
                    ? requireFormTemplateVersion(scope.id())
                    : scope.templateVersion()));
        }
        return selectReportsInScope(scope).stream().map(this::toFormVO).toList();
    }

    private TargetSpec resolveTargetSpec(Scope scope, String targetReportId) {
        if (Objects.equals(scope.type(), SCOPE_TYPE_FORM_TEMPLATE_VERSION)) {
            String expectedReportId = formTemplateReportId(scope.id());
            if (!Objects.equals(expectedReportId, targetReportId)) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_REPORT_NOT_EXISTS,
                        targetReportId);
            }
            FormTemplateVersionDO templateVersion = scope.templateVersion() == null
                    ? requireFormTemplateVersion(scope.id())
                    : scope.templateVersion();
            return new TargetSpec(expectedReportId, formTemplateReportName(templateVersion), null, null);
        }
        MesProBatchRecordReportDO report = requireReport(targetReportId);
        requireReportInScope(scope, report);
        return new TargetSpec(report.getReportId(), report.getReportName(),
                report.getBatchRecordDefinitionId(), report.getBatchRecordVersionId());
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
                || SOURCE_TYPE_PRODUCTION_WORK_ORDER.equals(normalized)) {
            return normalized;
        }
        throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_SOURCE_FIELD_NOT_SUPPORTED,
                sourceType);
    }

    private List<BatchRecordCellLinkSourceFieldVO> toWorkOrderSourceFieldVOList() {
        return PRODUCTION_WORK_ORDER_SOURCE_FIELDS.stream()
                .map(field -> new BatchRecordCellLinkSourceFieldVO()
                        .setSourceType(SOURCE_TYPE_PRODUCTION_WORK_ORDER)
                        .setFieldCode(field.code())
                        .setFieldName(field.name())
                        .setValueType(field.valueType()))
                .toList();
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
        rule.setOverwritePolicy(StrUtil.blankToDefault(item.getOverwritePolicy(), OVERWRITE_POLICY_ONLY_WHEN_EMPTY));
        rule.setTemplateSnapshotHash(DigestUtil.sha256Hex(sourceLayoutHash + "|" + targetLayoutHash));
        rule.setRuleVersion(ruleVersion);
        rule.setEnabled(item.getEnabled() == null || Boolean.TRUE.equals(item.getEnabled()));
        rule.setRemark(StrUtil.trim(item.getRemark()));
        return rule;
    }

    private BatchRecordCellLinkFormRespVO toFormVO(MesProBatchRecordReportDO report) {
        return new BatchRecordCellLinkFormRespVO()
                .setId(report.getId())
                .setBatchRecordName(report.getBatchRecordName())
                .setFormSlotType(report.getFormSlotType())
                .setBatchRecordDefinitionId(report.getBatchRecordDefinitionId())
                .setBatchRecordVersionId(report.getBatchRecordVersionId())
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
            if (StrUtil.isNotBlank(sheetLayoutJson)) {
                return new TemplateLayout(schema, sheetLayoutJson);
            }
            if (schema.containsKey("cellRules") || schema.containsKey("signatureCellMarkers")) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID,
                        reportId);
            }
        }
        JSONObject recognizedSchema = buildTemplateRecognizedFieldsSchema(templateVersion, reportId);
        String recognizedSheetLayoutJson = resolveTemplateSheetLayoutJson(recognizedSchema, reportId);
        return new TemplateLayout(recognizedSchema, recognizedSheetLayoutJson);
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

    private record TargetSpec(String reportId, String reportName, Long batchRecordDefinitionId,
                              Long batchRecordVersionId) {
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

        String uniqueKey() {
            return sourceType + ":" + reportId + ":" + cellKey;
        }
    }

    private record TemplateLayout(JSONObject schema, String sheetLayoutJson) {
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
