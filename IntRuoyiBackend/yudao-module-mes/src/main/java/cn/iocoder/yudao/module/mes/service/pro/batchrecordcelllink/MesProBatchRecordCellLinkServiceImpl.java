package cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkCellVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkFormCellsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkFormRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkPrefillItemVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkPrefillRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkRuleSaveItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkRuleVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkRulesSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkRulesSaveRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkWorkbenchContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRuleVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRulesRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordCellLinkRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
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
    private static final String OVERWRITE_POLICY_ONLY_WHEN_EMPTY = "ONLY_WHEN_EMPTY";
    private static final List<Integer> ACTIVE_EXECUTION_STATUSES = List.of(0, 1, 2, 3);

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

    @Override
    public BatchRecordCellLinkWorkbenchContextRespVO getWorkbenchContext(Long routeId, Long definitionId,
                                                                         Long versionId, String sourceReportId) {
        Scope scope = resolveQueryScope(definitionId, versionId, sourceReportId);
        List<MesProBatchRecordReportDO> reports = selectReportsInScope(scope);
        if (reports.isEmpty()) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_FORM_LIST_EMPTY);
        }
        List<BatchRecordCellLinkFormRespVO> forms = reports.stream().map(this::toFormVO).toList();
        Set<String> reportIds = forms.stream().map(BatchRecordCellLinkFormRespVO::getReportId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String defaultSourceReportId = StrUtil.isNotBlank(sourceReportId) ? sourceReportId : forms.get(0).getReportId();
        if (!reportIds.contains(defaultSourceReportId)) {
            throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_REPORT_NOT_EXISTS,
                    defaultSourceReportId);
        }
        String defaultTargetReportId = forms.stream()
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
                .setDefaultSourceReportId(defaultSourceReportId)
                .setDefaultTargetReportId(defaultTargetReportId)
                .setRules(toRuleVOList(ruleMapper.selectListByScope(scope.type(), scope.id())));
    }

    @Override
    public BatchRecordCellLinkFormCellsRespVO getFormCells(String reportId, Long versionId) {
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
        Map<String, BatchRecordCellLinkFormCellsRespVO> cellsCache = new LinkedHashMap<>();
        for (BatchRecordCellLinkRuleSaveItemReqVO item : reqVO.getRules()) {
            MesProBatchRecordReportDO sourceReport = reportCache.computeIfAbsent(item.getSourceReportId(),
                    this::requireReport);
            MesProBatchRecordReportDO targetReport = reportCache.computeIfAbsent(item.getTargetReportId(),
                    this::requireReport);
            requireReportInScope(scope, sourceReport);
            requireReportInScope(scope, targetReport);
            BatchRecordCellLinkFormCellsRespVO sourceCells = cellsCache.computeIfAbsent(item.getSourceReportId(),
                    reportId -> getFormCells(reportId, scope.versionId()));
            BatchRecordCellLinkFormCellsRespVO targetCells = cellsCache.computeIfAbsent(item.getTargetReportId(),
                    reportId -> getFormCells(reportId, scope.versionId()));
            BatchRecordCellLinkCellVO sourceCell = requireCell(sourceCells, item.getSourceRowIndex(),
                    item.getSourceColumnIndex());
            BatchRecordCellLinkCellVO targetCell = requireCell(targetCells, item.getTargetRowIndex(),
                    item.getTargetColumnIndex());
            if (!Boolean.TRUE.equals(targetCell.getLinkableAsTarget())) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_TARGET_NOT_WRITABLE,
                        targetReport.getReportName(), targetCell.getCellKey());
            }
            String targetKey = item.getTargetReportId() + ":" + targetCell.getCellKey();
            if (!targetKeys.add(targetKey)) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_TARGET_DUPLICATE,
                        targetReport.getReportName(), targetCell.getCellKey());
            }
            String pairKey = item.getSourceReportId() + ":" + sourceCell.getCellKey() + "->"
                    + item.getTargetReportId() + ":" + targetCell.getCellKey();
            if (!pairKeys.add(pairKey)) {
                throw exception(MesProBatchRecordCellLinkErrorCodeConstants.PRO_BATCH_RECORD_CELL_LINK_PAIR_DUPLICATE,
                        sourceCell.getCellKey(), targetCell.getCellKey());
            }
            ruleRows.add(toRuleDO(reqVO, scope, item, sourceReport, targetReport, sourceCell, targetCell,
                    sourceCells.getLayoutSnapshotHash(), targetCells.getLayoutSnapshotHash(), ruleVersion));
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

    private Scope resolveQueryScope(Long definitionId, Long versionId, String sourceReportId) {
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

    private MesProBatchRecordCellLinkRuleDO toRuleDO(BatchRecordCellLinkRulesSaveReqVO reqVO, Scope scope,
                                                     BatchRecordCellLinkRuleSaveItemReqVO item,
                                                     MesProBatchRecordReportDO sourceReport,
                                                     MesProBatchRecordReportDO targetReport,
                                                     BatchRecordCellLinkCellVO sourceCell,
                                                     BatchRecordCellLinkCellVO targetCell,
                                                     String sourceLayoutHash, String targetLayoutHash,
                                                     long ruleVersion) {
        MesProBatchRecordCellLinkRuleDO rule = new MesProBatchRecordCellLinkRuleDO();
        rule.setScopeType(scope.type());
        rule.setScopeId(scope.id());
        rule.setRouteId(reqVO.getRouteId());
        rule.setBatchRecordDefinitionId(scope.definitionId());
        rule.setBatchRecordVersionId(scope.versionId());
        rule.setSourceReportId(sourceReport.getReportId());
        rule.setSourceReportName(sourceReport.getReportName());
        rule.setSourceRowIndex(sourceCell.getRowIndex());
        rule.setSourceColumnIndex(sourceCell.getColumnIndex());
        rule.setSourceCellKey(sourceCell.getCellKey());
        rule.setSourceLabel(StrUtil.blankToDefault(StrUtil.trim(item.getSourceLabel()), sourceCell.getLabel()));
        rule.setSourceValueType(sourceCell.getValueType());
        rule.setTargetReportId(targetReport.getReportId());
        rule.setTargetReportName(targetReport.getReportName());
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
                .setSourceReportId(rule.getSourceReportId())
                .setSourceReportName(rule.getSourceReportName())
                .setSourceRowIndex(rule.getSourceRowIndex())
                .setSourceColumnIndex(rule.getSourceColumnIndex())
                .setSourceCellKey(rule.getSourceCellKey())
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
                .setSourceReportId(rule.getSourceReportId())
                .setSourceReportName(rule.getSourceReportName())
                .setSourceCellKey(rule.getSourceCellKey())
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

    private record Scope(String type, Long id, Long definitionId, Long versionId, String sourceFileSha256,
                         String routeKey) {

        static Scope routeVersion(Long definitionId, Long versionId) {
            return new Scope(SCOPE_TYPE_ROUTE_VERSION, versionId, definitionId, versionId, null, null);
        }

        static Scope reportSet(Long scopeId, String sourceFileSha256, String routeKey) {
            return new Scope(SCOPE_TYPE_REPORT_SET, scopeId, null, null, sourceFileSha256, routeKey);
        }
    }
}
