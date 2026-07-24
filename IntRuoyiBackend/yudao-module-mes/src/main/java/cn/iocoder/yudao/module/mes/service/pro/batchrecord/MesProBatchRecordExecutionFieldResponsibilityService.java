package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityExportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityExportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.projection.MesProBatchRecordExecutionFieldResponsibilityAuditProjection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_EXPORT_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.PRO_EDHR_OBJECT_PERMISSION_DENIED;

@Service
public class MesProBatchRecordExecutionFieldResponsibilityService {

    private static final String ABILITY_AUDIT_VIEW = "AUDIT_VIEW";
    private static final String ACTION_FIELD_CHANGE = "FIELD_CHANGE";
    private static final String RESPONSIBILITY_XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String[] RESPONSIBILITY_EXPORT_HEADERS = {
            "Generated At",
            "Execution ID",
            "Execution Code",
            "Batch Record Definition ID",
            "Batch Record Version ID",
            "Batch Record Report ID",
            "Field Audit Revision",
            "Field Audit Head Hash",
            "Cell Values Hash",
            "Field Path",
            "Field Key",
            "Field Label",
            "Row Index",
            "Column Index",
            "Component",
            "Value Type",
            "Current Value JSON",
            "Current Value Display",
            "Current Value Hash",
            "Value Origin",
            "First Human Actor ID",
            "First Human Actor Name",
            "First Human Changed At",
            "First Signature ID",
            "First Signature Username",
            "First Signature Nickname",
            "First Signature Display At",
            "Current Value Actor ID",
            "Current Value Actor Name",
            "Current Value Changed At",
            "Current Signature ID",
            "Current Signature Username",
            "Current Signature Nickname",
            "Current Signature Display At",
            "Evidence Status",
            "Reason Codes",
            "History Count",
            "Latest Audit Item ID",
            "Context Warnings"
    };
    private static final Set<MesProBatchRecordExecutionResponsibilityReasonCode> BLOCKING_REASONS =
            EnumSet.of(
                    MesProBatchRecordExecutionResponsibilityReasonCode.SIGNATURE_INVALID,
                    MesProBatchRecordExecutionResponsibilityReasonCode.CHAIN_INVALID,
                    MesProBatchRecordExecutionResponsibilityReasonCode.CURRENT_VALUE_MISMATCH,
                    MesProBatchRecordExecutionResponsibilityReasonCode.FIELD_IDENTITY_AMBIGUOUS,
                    MesProBatchRecordExecutionResponsibilityReasonCode.CROSS_TENANT_ASSOCIATION,
                    MesProBatchRecordExecutionResponsibilityReasonCode.CROSS_EXECUTION_ASSOCIATION);

    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordExecutionFieldAuditItemMapper itemMapper;
    @Resource
    private MesProBatchRecordExecutionFieldAuditBatchMapper batchMapper;
    @Resource
    private MesProBatchRecordExecutionSignatureMapper signatureMapper;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private MesProEdhrPermissionScopeService permissionScopeService;

    @Transactional(readOnly = true)
    public MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO getSummary(
            MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO reqVO) {
        if (reqVO == null || reqVO.getExecutionId() == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        validatePage(reqVO);
        SummaryComputation computation = computeSummary(reqVO.getExecutionId());
        List<MesProBatchRecordExecutionFieldResponsibilityItemRespVO> filtered = computation.items().stream()
                .filter(item -> matches(reqVO, item))
                .toList();
        int fromIndex = Math.min((reqVO.getPageNo() - 1) * reqVO.getPageSize(), filtered.size());
        int toIndex = Math.min(fromIndex + reqVO.getPageSize(), filtered.size());
        return toSummaryResponse(computation, new ArrayList<>(filtered.subList(fromIndex, toIndex)),
                (long) filtered.size());
    }

    private SummaryComputation computeSummary(Long executionId) {
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        requireAuditView(execution);

        SnapshotDirectory snapshot = parseSnapshot(execution.getExecutionSnapshotJson());
        CellDirectory cells = parseCells(execution.getCellValuesJson(), execution.getCellValuesHash());
        List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection> audits =
                nullToEmpty(itemMapper.selectResponsibilityProjectionList(execution.getId())).stream()
                        .sorted(Comparator
                                .comparing(MesProBatchRecordExecutionFieldResponsibilityAuditProjection::getFieldAuditRevision,
                                        Comparator.nullsFirst(Long::compareTo))
                                .thenComparing(
                                        MesProBatchRecordExecutionFieldResponsibilityAuditProjection::getAuditItemId,
                                        Comparator.nullsFirst(Long::compareTo)))
                        .toList();
        List<MesProBatchRecordExecutionFieldAuditBatchDO> batches =
                nullToEmpty(batchMapper.selectListByExecutionId(execution.getId()));
        Set<Long> signatureIds = new HashSet<>();
        for (MesProBatchRecordExecutionFieldResponsibilityAuditProjection audit : audits) {
            if (audit.getSignatureId() != null) {
                signatureIds.add(audit.getSignatureId());
            }
        }
        List<MesProBatchRecordExecutionSignatureDO> signatures =
                nullToEmpty(signatureMapper.selectResponsibilityListByIds(signatureIds));
        List<MesProEdhrWorkTaskDO> workTasks =
                nullToEmpty(workTaskMapper.selectTimelineListByExecutionId(execution.getId()));

        Map<Long, MesProBatchRecordExecutionFieldAuditBatchDO> batchById = indexBatches(batches);
        Map<Long, MesProBatchRecordExecutionSignatureDO> signatureById = indexSignatures(signatures);
        Map<String, List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection>> auditsByField =
                groupAuditsByField(audits);
        EnumSet<MesProBatchRecordExecutionResponsibilityReasonCode> globalReasons =
                EnumSet.noneOf(MesProBatchRecordExecutionResponsibilityReasonCode.class);
        globalReasons.addAll(snapshot.reasons());
        globalReasons.addAll(cells.reasons());
        addUnmatchedAuditReasons(snapshot, audits, globalReasons);
        addAssociationReasons(execution.getId(), TenantContextHolder.getTenantId(), audits, batches, workTasks,
                globalReasons);
        if (!validateChain(execution, snapshot, audits, batchById)) {
            globalReasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.CHAIN_INVALID);
        }

        List<MesProBatchRecordExecutionFieldResponsibilityItemRespVO> allItems = new ArrayList<>();
        for (SnapshotField field : snapshot.fields()) {
            allItems.add(calculateField(execution, field, cells, auditsByField.getOrDefault(field.identityKey(),
                    List.of()), batchById, signatureById, globalReasons));
        }

        EnumSet<MesProBatchRecordExecutionResponsibilityReasonCode> overallReasons =
                EnumSet.noneOf(MesProBatchRecordExecutionResponsibilityReasonCode.class);
        overallReasons.addAll(globalReasons);
        for (MesProBatchRecordExecutionFieldResponsibilityItemRespVO item : allItems) {
            overallReasons.addAll(nullToEmpty(item.getReasonCodes()));
        }
        MesProBatchRecordExecutionResponsibilityEvidenceStatus overallStatus = statusFor(overallReasons);

        List<MesProBatchRecordExecutionResponsibilityContextWarning> contextWarnings = new ArrayList<>();
        if (execution.getBatchRecordDefinitionId() == null || execution.getBatchRecordVersionId() == null
                || StrUtil.isBlank(execution.getBatchRecordReportId())) {
            contextWarnings.add(MesProBatchRecordExecutionResponsibilityContextWarning.VERSION_CONTEXT_MISSING);
        }
        return new SummaryComputation(execution, allItems, auditsByField, signatureById, overallStatus,
                orderedReasons(overallReasons), contextWarnings);
    }

    private MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO toSummaryResponse(
            SummaryComputation computation,
            List<MesProBatchRecordExecutionFieldResponsibilityItemRespVO> items,
            Long total) {
        MesProBatchRecordExecutionDO execution = computation.execution();
        return new MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO()
                .setExecutionId(execution.getId())
                .setExecutionCode(execution.getExecutionCode())
                .setBatchRecordDefinitionId(execution.getBatchRecordDefinitionId())
                .setBatchRecordVersionId(execution.getBatchRecordVersionId())
                .setBatchRecordReportId(execution.getBatchRecordReportId())
                .setFieldAuditRevision(execution.getFieldAuditRevision())
                .setFieldAuditHeadHash(execution.getFieldAuditHeadHash())
                .setCellValuesHash(execution.getCellValuesHash())
                .setOverallEvidenceStatus(computation.overallStatus())
                .setOverallReasonCodes(computation.overallReasons())
                .setContextWarnings(computation.contextWarnings())
                .setTotal(total)
                .setList(items);
    }

    @Transactional(readOnly = true)
    public MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO getHistory(
            MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO reqVO) {
        if (reqVO == null || reqVO.getExecutionId() == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(reqVO.getExecutionId());
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        requireAuditView(execution);

        int pageSize = reqVO.getPageSize();
        List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection> fetched = nullToEmpty(
                itemMapper.selectResponsibilityHistoryProjectionPage(
                        execution.getId(),
                        reqVO.getFieldPath(),
                        reqVO.getFieldKey(),
                        reqVO.getRowIndex(),
                        reqVO.getColumnIndex(),
                        reqVO.getCursorFieldAuditRevision(),
                        reqVO.getCursorAuditItemId(),
                        pageSize + 1));
        boolean hasMore = fetched.size() > pageSize;
        List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection> page =
                new ArrayList<>(fetched.subList(0, Math.min(pageSize, fetched.size())));

        Map<Long, MesProBatchRecordExecutionFieldAuditBatchDO> batchById =
                indexBatches(nullToEmpty(batchMapper.selectListByExecutionId(execution.getId())));
        Set<Long> signatureIds = page.stream()
                .map(MesProBatchRecordExecutionFieldResponsibilityAuditProjection::getSignatureId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        List<MesProBatchRecordExecutionSignatureDO> signatures = signatureIds.isEmpty()
                ? List.of()
                : nullToEmpty(signatureMapper.selectResponsibilityListByIds(signatureIds));
        Map<Long, MesProBatchRecordExecutionSignatureDO> signatureById = indexSignatures(signatures);

        List<MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO> items = page.stream()
                .map(audit -> toHistoryItem(execution, audit, batchById.get(audit.getAuditBatchId()),
                        signatureById.get(audit.getSignatureId())))
                .toList();
        MesProBatchRecordExecutionFieldResponsibilityAuditProjection last =
                hasMore && !page.isEmpty() ? page.get(page.size() - 1) : null;
        return new MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO()
                .setExecutionId(execution.getId())
                .setFieldPath(reqVO.getFieldPath())
                .setFieldKey(reqVO.getFieldKey())
                .setRowIndex(reqVO.getRowIndex())
                .setColumnIndex(reqVO.getColumnIndex())
                .setList(items)
                .setHasMore(hasMore)
                .setNextCursorFieldAuditRevision(last == null ? null : last.getFieldAuditRevision())
                .setNextCursorAuditItemId(last == null ? null : last.getAuditItemId());
    }

    @Transactional(readOnly = true)
    public MesProBatchRecordExecutionFieldResponsibilityExportRespVO export(
            MesProBatchRecordExecutionFieldResponsibilityExportReqVO reqVO) {
        if (reqVO == null || reqVO.getExecutionId() == null || !"XLSX".equals(reqVO.getFormat())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_EXPORT_FAILED);
        }
        SummaryComputation computation = computeSummary(reqVO.getExecutionId());
        LocalDateTime generatedAt = LocalDateTime.now();
        byte[] content = renderResponsibilityWorkbook(computation, generatedAt);
        MesProBatchRecordExecutionDO execution = computation.execution();
        return new MesProBatchRecordExecutionFieldResponsibilityExportRespVO()
                .setFileName("field-responsibility-" + execution.getId() + ".xlsx")
                .setFormat("XLSX")
                .setContentType(RESPONSIBILITY_XLSX_CONTENT_TYPE)
                .setContentBase64(Base64.getEncoder().encodeToString(content))
                .setSha256(DigestUtil.sha256Hex(content))
                .setRecordCount((long) computation.items().size())
                .setFieldAuditRevision(execution.getFieldAuditRevision())
                .setFieldAuditHeadHash(execution.getFieldAuditHeadHash())
                .setCellValuesHash(execution.getCellValuesHash())
                .setEvidenceStatus(computation.overallStatus())
                .setReasonCodes(computation.overallReasons())
                .setContextWarnings(computation.contextWarnings())
                .setGeneratedAt(generatedAt);
    }

    private byte[] renderResponsibilityWorkbook(SummaryComputation computation, LocalDateTime generatedAt) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Responsibility");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            Row header = sheet.createRow(0);
            for (int columnIndex = 0; columnIndex < RESPONSIBILITY_EXPORT_HEADERS.length; columnIndex++) {
                header.createCell(columnIndex).setCellValue(RESPONSIBILITY_EXPORT_HEADERS[columnIndex]);
                header.getCell(columnIndex).setCellStyle(headerStyle);
                sheet.setColumnWidth(columnIndex,
                        Math.min(48, Math.max(14, RESPONSIBILITY_EXPORT_HEADERS[columnIndex].length() + 2)) * 256);
            }
            sheet.createFreezePane(0, 1);

            for (int itemIndex = 0; itemIndex < computation.items().size(); itemIndex++) {
                MesProBatchRecordExecutionFieldResponsibilityItemRespVO item = computation.items().get(itemIndex);
                List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection> fieldAudits =
                        computation.auditsByField().getOrDefault(
                                identityKey(item.getFieldPath(), item.getFieldKey(), item.getRowIndex(),
                                        item.getColumnIndex()),
                                List.of());
                MesProBatchRecordExecutionFieldResponsibilityAuditProjection firstAudit =
                        fieldAudits.isEmpty() ? null : fieldAudits.get(0);
                MesProBatchRecordExecutionFieldResponsibilityAuditProjection currentAudit =
                        fieldAudits.isEmpty() ? null : fieldAudits.get(fieldAudits.size() - 1);
                MesProBatchRecordExecutionSignatureDO firstSignature = firstAudit == null ? null
                        : computation.signatureById().get(firstAudit.getSignatureId());
                MesProBatchRecordExecutionSignatureDO currentSignature = currentAudit == null ? null
                        : computation.signatureById().get(currentAudit.getSignatureId());
                writeResponsibilityRow(sheet.createRow(itemIndex + 1), computation, item, generatedAt,
                        firstAudit, firstSignature, currentAudit, currentSignature);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_EXPORT_FAILED);
        }
    }

    private void writeResponsibilityRow(
            Row row,
            SummaryComputation computation,
            MesProBatchRecordExecutionFieldResponsibilityItemRespVO item,
            LocalDateTime generatedAt,
            MesProBatchRecordExecutionFieldResponsibilityAuditProjection firstAudit,
            MesProBatchRecordExecutionSignatureDO firstSignature,
            MesProBatchRecordExecutionFieldResponsibilityAuditProjection currentAudit,
            MesProBatchRecordExecutionSignatureDO currentSignature) {
        MesProBatchRecordExecutionDO execution = computation.execution();
        Object[] values = {
                generatedAt,
                execution.getId(),
                execution.getExecutionCode(),
                execution.getBatchRecordDefinitionId(),
                execution.getBatchRecordVersionId(),
                execution.getBatchRecordReportId(),
                execution.getFieldAuditRevision(),
                execution.getFieldAuditHeadHash(),
                execution.getCellValuesHash(),
                item.getFieldPath(),
                item.getFieldKey(),
                item.getFieldLabel(),
                item.getRowIndex(),
                item.getColumnIndex(),
                item.getComponent(),
                item.getValueType(),
                item.getCurrentValueJson(),
                item.getCurrentValueDisplay(),
                item.getCurrentValueHash(),
                item.getValueOrigin(),
                item.getFirstHumanActorId(),
                item.getFirstHumanActorName(),
                item.getFirstHumanChangedAt(),
                firstAudit == null ? null : firstAudit.getSignatureId(),
                firstSignature == null ? null : firstSignature.getActorUsernameSnapshot(),
                firstSignature == null ? null : firstSignature.getActorNicknameSnapshot(),
                firstSignature == null ? null : firstSignature.getSignatureDisplayAt(),
                item.getCurrentValueActorId(),
                item.getCurrentValueActorName(),
                item.getCurrentValueChangedAt(),
                currentAudit == null ? null : currentAudit.getSignatureId(),
                currentSignature == null ? null : currentSignature.getActorUsernameSnapshot(),
                currentSignature == null ? null : currentSignature.getActorNicknameSnapshot(),
                currentSignature == null ? null : currentSignature.getSignatureDisplayAt(),
                item.getEvidenceStatus(),
                joinedNames(item.getReasonCodes()),
                item.getHistoryCount(),
                item.getLatestAuditItemId(),
                joinedNames(computation.contextWarnings())
        };
        for (int columnIndex = 0; columnIndex < values.length; columnIndex++) {
            row.createCell(columnIndex).setCellValue(stringValue(values[columnIndex]));
        }
    }

    private String joinedNames(Collection<?> values) {
        return values == null ? "" : values.stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private void requireAuditView(MesProBatchRecordExecutionDO execution) {
        MesProEdhrPermissionEvaluateResult result = permissionScopeService.evaluate(
                new MesProEdhrPermissionEvaluateCommand()
                        .setScopeId(execution.getPermissionScopeId())
                        .setObjectType("BATCH_RECORD_EXECUTION")
                        .setObjectId(String.valueOf(execution.getId()))
                        .setExecutionId(execution.getId())
                        .setRouteId(execution.getRouteId())
                        .setRouteProcessId(execution.getRouteProcessId())
                        .setReportId(execution.getBatchRecordReportId())
                        .setRecordCategory(execution.getRecordCategory())
                        .setAbilities(List.of(ABILITY_AUDIT_VIEW))
                        .setPermissionCode("mes:pro-batch-record-execution:field-audit-query")
                        .setActionName("查看字段责任汇总"));
        if (result == null || result.getDecisions() == null
                || !"ALLOW".equals(result.getDecisions().get(ABILITY_AUDIT_VIEW))) {
            throw exception(PRO_EDHR_OBJECT_PERMISSION_DENIED,
                    "BATCH_RECORD_EXECUTION:" + execution.getId() + ":" + ABILITY_AUDIT_VIEW);
        }
    }

    private MesProBatchRecordExecutionFieldResponsibilityItemRespVO calculateField(
            MesProBatchRecordExecutionDO execution,
            SnapshotField field,
            CellDirectory cells,
            List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection> fieldAudits,
            Map<Long, MesProBatchRecordExecutionFieldAuditBatchDO> batchById,
            Map<Long, MesProBatchRecordExecutionSignatureDO> signatureById,
            Set<MesProBatchRecordExecutionResponsibilityReasonCode> globalReasons) {
        EnumSet<MesProBatchRecordExecutionResponsibilityReasonCode> reasons =
                EnumSet.noneOf(MesProBatchRecordExecutionResponsibilityReasonCode.class);
        if (field.definitionMissing()) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.FIELD_DEFINITION_MISSING);
        }
        if (field.ambiguous() || cells.ambiguousIdentities().contains(field.identityKey())) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.FIELD_IDENTITY_AMBIGUOUS);
        }
        if (globalReasons.contains(MesProBatchRecordExecutionResponsibilityReasonCode.BASELINE_MISSING)) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.BASELINE_MISSING);
        }
        if (globalReasons.contains(MesProBatchRecordExecutionResponsibilityReasonCode.CHAIN_INVALID)) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.CHAIN_INVALID);
        }
        if (globalReasons.contains(MesProBatchRecordExecutionResponsibilityReasonCode.CROSS_EXECUTION_ASSOCIATION)) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.CROSS_EXECUTION_ASSOCIATION);
        }
        if (globalReasons.contains(MesProBatchRecordExecutionResponsibilityReasonCode.CROSS_TENANT_ASSOCIATION)) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.CROSS_TENANT_ASSOCIATION);
        }

        CellState current = currentState(field, cells.cells().get(field.identityKey()));
        if (current.hash() == null && !field.definitionMissing()) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.FIELD_DEFINITION_MISSING);
        }

        MesProBatchRecordExecutionFieldResponsibilityItemRespVO item =
                new MesProBatchRecordExecutionFieldResponsibilityItemRespVO()
                        .setFieldPath(field.fieldPath())
                        .setFieldKey(field.fieldKey())
                        .setFieldLabel(field.label())
                        .setRowIndex(field.rowIndex())
                        .setColumnIndex(field.columnIndex())
                        .setComponent(field.component())
                        .setValueType(field.valueType() == null ? null : field.valueType().name())
                        .setCurrentValueJson(current.valueJson())
                        .setCurrentValueDisplay(current.display())
                        .setCurrentValueHash(current.hash())
                        .setHistoryCount((long) fieldAudits.size())
                        .setLatestAuditItemId(fieldAudits.isEmpty() ? null
                                : fieldAudits.get(fieldAudits.size() - 1).getAuditItemId());

        MesProBatchRecordExecutionResponsibilityValueOrigin origin;
        if (fieldAudits.isEmpty()) {
            origin = calculateUntouchedOrigin(field, current, reasons);
        } else {
            for (MesProBatchRecordExecutionFieldResponsibilityAuditProjection audit : fieldAudits) {
                addAuditAssociationReasons(execution.getId(), TenantContextHolder.getTenantId(), audit,
                        batchById.get(audit.getAuditBatchId()), reasons);
                addSignatureReasons(execution, audit, batchById.get(audit.getAuditBatchId()),
                        signatureById.get(audit.getSignatureId()), reasons);
            }
            MesProBatchRecordExecutionFieldResponsibilityAuditProjection first = fieldAudits.get(0);
            MesProBatchRecordExecutionFieldResponsibilityAuditProjection latest =
                    fieldAudits.get(fieldAudits.size() - 1);
            String latestHash = resolveAuditValueHash(latest.getNewValueHash(), latest.getNewValueJson(),
                    field.valueType());
            if (current.hash() == null || latestHash == null || !Objects.equals(current.hash(), latestHash)) {
                reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.CURRENT_VALUE_MISMATCH);
            }
            if (reasons.isEmpty()) {
                origin = MesProBatchRecordExecutionResponsibilityValueOrigin.HUMAN;
                item.setFirstHumanActorId(first.getActorId())
                        .setFirstHumanActorName(first.getActorName())
                        .setFirstHumanChangedAt(first.getChangedAt())
                        .setCurrentValueActorId(latest.getActorId())
                        .setCurrentValueActorName(latest.getActorName())
                        .setCurrentValueChangedAt(latest.getChangedAt());
            } else {
                origin = MesProBatchRecordExecutionResponsibilityValueOrigin.UNKNOWN;
            }
        }
        MesProBatchRecordExecutionResponsibilityEvidenceStatus status = statusFor(reasons);
        if (status != MesProBatchRecordExecutionResponsibilityEvidenceStatus.COMPLETE) {
            origin = MesProBatchRecordExecutionResponsibilityValueOrigin.UNKNOWN;
            item.setFirstHumanActorId(null)
                    .setFirstHumanActorName(null)
                    .setFirstHumanChangedAt(null)
                    .setCurrentValueActorId(null)
                    .setCurrentValueActorName(null)
                    .setCurrentValueChangedAt(null);
        }
        return item.setValueOrigin(origin)
                .setEvidenceStatus(status)
                .setReasonCodes(orderedReasons(reasons));
    }

    private MesProBatchRecordExecutionResponsibilityValueOrigin calculateUntouchedOrigin(
            SnapshotField field,
            CellState current,
            Set<MesProBatchRecordExecutionResponsibilityReasonCode> reasons) {
        if (!reasons.isEmpty()) {
            return MesProBatchRecordExecutionResponsibilityValueOrigin.UNKNOWN;
        }
        String defaultJson = MesProBatchRecordExecutionFieldAuditHasher.canonicalize(field.defaultValue());
        if (!Objects.equals(defaultJson, current.valueJson())) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.FIELD_AUDIT_MISSING);
            return MesProBatchRecordExecutionResponsibilityValueOrigin.UNKNOWN;
        }
        return isEmptyValue(current.value())
                ? MesProBatchRecordExecutionResponsibilityValueOrigin.EMPTY_UNTOUCHED
                : MesProBatchRecordExecutionResponsibilityValueOrigin.SYSTEM_BASELINE;
    }

    private void addAuditAssociationReasons(
            Long executionId,
            Long tenantId,
            MesProBatchRecordExecutionFieldResponsibilityAuditProjection audit,
            MesProBatchRecordExecutionFieldAuditBatchDO batch,
            Set<MesProBatchRecordExecutionResponsibilityReasonCode> reasons) {
        if (!Objects.equals(executionId, audit.getExecutionId())
                || batch != null && !Objects.equals(executionId, batch.getExecutionId())) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.CROSS_EXECUTION_ASSOCIATION);
        }
        if (tenantId != null && (audit.getTenantId() != null && !Objects.equals(tenantId, audit.getTenantId())
                || batch != null && batch.getTenantId() != null && !Objects.equals(tenantId, batch.getTenantId()))) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.CROSS_TENANT_ASSOCIATION);
        }
        if (batch == null) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.CHAIN_INVALID);
        }
    }

    private MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO toHistoryItem(
            MesProBatchRecordExecutionDO execution,
            MesProBatchRecordExecutionFieldResponsibilityAuditProjection audit,
            MesProBatchRecordExecutionFieldAuditBatchDO batch,
            MesProBatchRecordExecutionSignatureDO signature) {
        EnumSet<MesProBatchRecordExecutionResponsibilityReasonCode> reasons =
                EnumSet.noneOf(MesProBatchRecordExecutionResponsibilityReasonCode.class);
        addAuditAssociationReasons(execution.getId(), TenantContextHolder.getTenantId(), audit, batch, reasons);
        addSignatureReasons(execution, audit, batch, signature, reasons);
        if (!validHistoryAuditBoundary(audit, batch)) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.CHAIN_INVALID);
        }
        MesProBatchRecordExecutionResponsibilityEvidenceStatus evidenceStatus = statusFor(reasons);
        boolean complete = evidenceStatus == MesProBatchRecordExecutionResponsibilityEvidenceStatus.COMPLETE;
        return new MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO()
                .setAuditItemId(audit.getAuditItemId())
                .setAuditBatchId(audit.getAuditBatchId())
                .setFieldAuditRevision(audit.getFieldAuditRevision())
                .setOldValueJson(audit.getOldValueJson())
                .setOldValueDisplay(audit.getOldValueDisplay())
                .setOldValueHash(audit.getOldValueHash())
                .setNewValueJson(audit.getNewValueJson())
                .setNewValueDisplay(audit.getNewValueDisplay())
                .setNewValueHash(audit.getNewValueHash())
                .setReasonCategory(audit.getReasonCategory())
                .setReasonText(audit.getReasonText())
                .setActorId(complete ? audit.getActorId() : null)
                .setActorName(complete ? audit.getActorName() : null)
                .setChangedAt(audit.getChangedAt())
                .setSignatureId(audit.getSignatureId())
                .setSignatureActorUsernameSnapshot(
                        complete && signature != null ? signature.getActorUsernameSnapshot() : null)
                .setSignatureActorNicknameSnapshot(
                        complete && signature != null ? signature.getActorNicknameSnapshot() : null)
                .setSignatureDisplayAt(complete && signature != null ? signature.getSignatureDisplayAt() : null)
                .setSignatureProjectionHash(audit.getSignatureProjectionHash())
                .setPreviousHash(audit.getPreviousHash())
                .setAuditHash(audit.getAuditHash())
                .setEvidenceStatus(evidenceStatus)
                .setReasonCodes(orderedReasons(reasons));
    }

    private boolean validHistoryAuditBoundary(
            MesProBatchRecordExecutionFieldResponsibilityAuditProjection audit,
            MesProBatchRecordExecutionFieldAuditBatchDO batch) {
        String calculated = calculateAuditHash(audit);
        if (batch == null || calculated == null || !Objects.equals(calculated, audit.getAuditHash())
                || !Objects.equals(audit.getFieldAuditRevision(), batch.getAfterFieldAuditRevision())) {
            return false;
        }
        if (Objects.equals(0, audit.getBatchItemIndex())
                && !Objects.equals(audit.getPreviousHash(), batch.getPreviousHeadHash())) {
            return false;
        }
        return batch.getFieldCount() == null
                || audit.getBatchItemIndex() == null
                || audit.getBatchItemIndex() != batch.getFieldCount() - 1
                || Objects.equals(audit.getAuditHash(), batch.getNewHeadHash());
    }

    private void addSignatureReasons(
            MesProBatchRecordExecutionDO execution,
            MesProBatchRecordExecutionFieldResponsibilityAuditProjection audit,
            MesProBatchRecordExecutionFieldAuditBatchDO batch,
            MesProBatchRecordExecutionSignatureDO signature,
            Set<MesProBatchRecordExecutionResponsibilityReasonCode> reasons) {
        if (audit.getSignatureId() == null || signature == null) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.SIGNATURE_MISSING);
            return;
        }
        if (!Objects.equals(execution.getId(), signature.getExecutionId())
                || !Objects.equals(audit.getExecutionId(), signature.getExecutionId())
                || batch != null && !Objects.equals(batch.getExecutionId(), signature.getExecutionId())) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.CROSS_EXECUTION_ASSOCIATION);
        }
        if (!validSignature(execution, audit, batch, signature)) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.SIGNATURE_INVALID);
        }
    }

    private boolean validSignature(
            MesProBatchRecordExecutionDO execution,
            MesProBatchRecordExecutionFieldResponsibilityAuditProjection audit,
            MesProBatchRecordExecutionFieldAuditBatchDO batch,
            MesProBatchRecordExecutionSignatureDO signature) {
        if (batch == null
                || !Objects.equals(signature.getId(), audit.getSignatureId())
                || !Objects.equals(signature.getId(), batch.getSignatureId())
                || !Objects.equals(signature.getExecutionId(), execution.getId())
                || !Objects.equals(signature.getExecutionId(), audit.getExecutionId())
                || !Objects.equals(signature.getAuditBatchId(), audit.getAuditBatchId())
                || !Objects.equals(signature.getAuditBatchId(), batch.getId())
                || !ACTION_FIELD_CHANGE.equals(signature.getActionType())
                || !ACTION_FIELD_CHANGE.equals(batch.getActionType())
                || !Boolean.TRUE.equals(signature.getPasswordVerified())
                || !MesProBatchRecordExecutionSignatureService.SIGNATURE_MODE_PASSWORD.equals(
                        signature.getSignatureMode())
                || !Objects.equals(signature.getActorId(), audit.getActorId())
                || !Objects.equals(signature.getActorName(), audit.getActorName())
                || !Objects.equals(signature.getActorId(), batch.getActorId())
                || !Objects.equals(signature.getActorName(), batch.getActorName())
                || !Objects.equals(signature.getReasonCategory(), audit.getReasonCategory())
                || !Objects.equals(signature.getReason(), audit.getReasonText())
                || !Objects.equals(signature.getSignatureChallengeHash(), batch.getSignatureChallengeHash())
                || !Objects.equals(signature.getFieldAuditRevision(), batch.getAfterFieldAuditRevision())
                || !Objects.equals(signature.getFieldAuditHeadHash(), batch.getNewHeadHash())
                || !Objects.equals(signature.getCellValuesHash(), batch.getAfterCellValuesHash())) {
            return false;
        }
        try {
            String calculated = MesProBatchRecordExecutionFieldAuditHasher.hashSignatureProjection(
                    toSignatureProjection(signature));
            return Objects.equals(calculated, audit.getSignatureProjectionHash())
                    && Objects.equals(calculated, batch.getSignatureProjectionHash());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private MesProBatchRecordExecutionFieldAuditSignatureProjection toSignatureProjection(
            MesProBatchRecordExecutionSignatureDO signature) {
        return new MesProBatchRecordExecutionFieldAuditSignatureProjection()
                .setId(signature.getId())
                .setExecutionId(signature.getExecutionId())
                .setActionType(signature.getActionType())
                .setActorId(signature.getActorId())
                .setActorName(signature.getActorName())
                .setSignatureMode(signature.getSignatureMode())
                .setPasswordVerified(signature.getPasswordVerified())
                .setSignedAt(signature.getSignedAt())
                .setSelectedSignedAt(signature.getSelectedSignedAt())
                .setSignatureDisplayAt(signature.getSignatureDisplayAt())
                .setSignatureTimeMode(signature.getSignatureTimeMode())
                .setSelectedTimeZone(signature.getSelectedTimeZone())
                .setSelectedTimeReason(signature.getSelectedTimeReason())
                .setSelectedTimePolicyVersion(signature.getSelectedTimePolicyVersion())
                .setSelectedTimeAuditHash(signature.getSelectedTimeAuditHash())
                .setReasonCategory(signature.getReasonCategory())
                .setReasonText(signature.getReason())
                .setAuditBatchId(signature.getAuditBatchId())
                .setSignatureChallengeHash(signature.getSignatureChallengeHash())
                .setFieldAuditRevision(signature.getFieldAuditRevision())
                .setFieldAuditHeadHash(signature.getFieldAuditHeadHash())
                .setCellValuesHash(signature.getCellValuesHash());
    }

    private boolean validateChain(
            MesProBatchRecordExecutionDO execution,
            SnapshotDirectory snapshot,
            List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection> audits,
            Map<Long, MesProBatchRecordExecutionFieldAuditBatchDO> batchById) {
        if (audits.isEmpty()) {
            return Objects.equals(0L, execution.getFieldAuditRevision())
                    && Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH,
                    execution.getFieldAuditHeadHash());
        }
        if (execution.getFieldAuditRevision() == null || StrUtil.isBlank(execution.getFieldAuditHeadHash())
                || snapshot.snapshotHash() == null) {
            return false;
        }
        String previousHash = MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH;
        Map<Long, String> firstPreviousHashByBatch = new HashMap<>();
        Map<Long, String> lastHashByBatch = new HashMap<>();
        long maxRevision = 0L;
        for (MesProBatchRecordExecutionFieldResponsibilityAuditProjection audit : audits) {
            MesProBatchRecordExecutionFieldAuditBatchDO batch = batchById.get(audit.getAuditBatchId());
            if (batch == null || !Objects.equals(previousHash, audit.getPreviousHash())
                    || StrUtil.isBlank(audit.getExecutionSnapshotHash())
                    || !Objects.equals(snapshot.snapshotHash(), audit.getExecutionSnapshotHash())) {
                return false;
            }
            String calculated = calculateAuditHash(audit);
            if (calculated == null || !Objects.equals(calculated, audit.getAuditHash())) {
                return false;
            }
            firstPreviousHashByBatch.putIfAbsent(batch.getId(), audit.getPreviousHash());
            lastHashByBatch.put(batch.getId(), calculated);
            previousHash = calculated;
            if (audit.getFieldAuditRevision() != null) {
                maxRevision = Math.max(maxRevision, audit.getFieldAuditRevision());
            }
        }
        for (Map.Entry<Long, String> entry : firstPreviousHashByBatch.entrySet()) {
            MesProBatchRecordExecutionFieldAuditBatchDO batch = batchById.get(entry.getKey());
            if (!Objects.equals(entry.getValue(), batch.getPreviousHeadHash())
                    || !Objects.equals(lastHashByBatch.get(entry.getKey()), batch.getNewHeadHash())) {
                return false;
            }
        }
        return Objects.equals(previousHash, execution.getFieldAuditHeadHash())
                && Objects.equals(maxRevision, execution.getFieldAuditRevision());
    }

    private String calculateAuditHash(MesProBatchRecordExecutionFieldResponsibilityAuditProjection audit) {
        try {
            MesProBatchRecordExecutionFieldAuditValueType valueType =
                    MesProBatchRecordExecutionFieldAuditValueType.valueOf(audit.getValueType());
            String oldValueHash = resolveAuditValueHash(audit.getOldValueHash(), audit.getOldValueJson(), valueType);
            String newValueHash = resolveAuditValueHash(audit.getNewValueHash(), audit.getNewValueJson(), valueType);
            if (oldValueHash == null || newValueHash == null) {
                return null;
            }
            return MesProBatchRecordExecutionFieldAuditHasher.hashItem(
                    MesProBatchRecordExecutionFieldAuditItemHashInput.builder()
                            .fieldPath(audit.getFieldPath())
                            .fieldKey(audit.getFieldKey())
                            .rowIndex(audit.getRowIndex())
                            .columnIndex(audit.getColumnIndex())
                            .valueType(valueType)
                            .oldValueJson(audit.getOldValueJson())
                            .oldValueDisplay(audit.getOldValueDisplay())
                            .oldValueHash(oldValueHash)
                            .newValueJson(audit.getNewValueJson())
                            .newValueDisplay(audit.getNewValueDisplay())
                            .newValueHash(newValueHash)
                            .reasonCategory(audit.getReasonCategory())
                            .reasonText(audit.getReasonText())
                            .actorId(audit.getActorId())
                            .actorName(audit.getActorName())
                            .signatureProjectionHash(audit.getSignatureProjectionHash())
                            .previousHash(audit.getPreviousHash())
                            .changedAt(audit.getChangedAt())
                            .build());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String resolveAuditValueHash(
            String storedHash,
            String valueJson,
            MesProBatchRecordExecutionFieldAuditValueType valueType) {
        if (StrUtil.isNotBlank(storedHash)) {
            return storedHash;
        }
        if (valueType == null || StrUtil.isBlank(valueJson)) {
            return null;
        }
        try {
            JsonNode value = JsonUtils.getObjectMapper().readTree(valueJson);
            return MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(valueType, value);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            return null;
        }
    }

    private CellState currentState(SnapshotField field, JsonNode cell) {
        JsonNode value = field.defaultValue();
        String display = field.defaultDisplay();
        String valueHash = null;
        if (cell != null) {
            JsonNode cellValue = cell.get("value");
            value = cellValue == null || cellValue.isMissingNode() ? NullNode.instance : cellValue;
            JsonNode displayNode = cell.get("valueDisplay");
            display = displayNode != null && displayNode.isTextual() ? displayNode.textValue() : displayValue(value);
            JsonNode hashNode = cell.get("valueHash");
            valueHash = hashNode != null && hashNode.isTextual() && StrUtil.isNotBlank(hashNode.textValue())
                    ? hashNode.textValue() : null;
        }
        if (value == null || value.isMissingNode()) {
            value = NullNode.instance;
        }
        String valueJson = MesProBatchRecordExecutionFieldAuditHasher.canonicalize(value);
        if (valueHash == null && field.valueType() != null) {
            try {
                valueHash = MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(field.valueType(), value);
            } catch (IllegalArgumentException ignored) {
                valueHash = null;
            }
        }
        return new CellState(value, valueJson, display == null ? displayValue(value) : display, valueHash);
    }

    private SnapshotDirectory parseSnapshot(String executionSnapshotJson) {
        EnumSet<MesProBatchRecordExecutionResponsibilityReasonCode> reasons =
                EnumSet.noneOf(MesProBatchRecordExecutionResponsibilityReasonCode.class);
        if (StrUtil.isBlank(executionSnapshotJson)) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.EXECUTION_SNAPSHOT_MISSING);
            return new SnapshotDirectory(List.of(), reasons, null, Set.of());
        }
        try {
            JsonNode root = JsonUtils.getObjectMapper().readTree(executionSnapshotJson);
            JsonNode fieldsNode = root == null ? null : root.get("fields");
            if (fieldsNode == null || !fieldsNode.isArray() || fieldsNode.isEmpty()) {
                reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.EXECUTION_SNAPSHOT_MISSING);
                return new SnapshotDirectory(List.of(), reasons,
                        MesProBatchRecordExecutionFieldAuditHasher.hashExecutionSnapshot(executionSnapshotJson),
                        Set.of());
            }
            List<SnapshotFieldDraft> drafts = new ArrayList<>();
            Map<String, Integer> identityCounts = new HashMap<>();
            for (JsonNode node : fieldsNode) {
                if (!node.isObject()) {
                    drafts.add(new SnapshotFieldDraft(null, null, null, null, null, null, null,
                            NullNode.instance, "", null, true));
                    continue;
                }
                String fieldPath = text(node, "fieldPath");
                String fieldKey = text(node, "fieldKey");
                Integer rowIndex = integer(node, "rowIndex");
                Integer columnIndex = integer(node, "columnIndex");
                MesProBatchRecordExecutionFieldAuditValueType valueType = parseValueType(text(node, "valueType"));
                boolean definitionMissing = StrUtil.isBlank(fieldPath) || StrUtil.isBlank(fieldKey)
                        || rowIndex == null || columnIndex == null || valueType == null;
                String identityKey = definitionMissing ? null : identityKey(fieldPath, fieldKey, rowIndex, columnIndex);
                if (identityKey != null) {
                    identityCounts.merge(identityKey, 1, Integer::sum);
                }
                JsonNode defaultValue = node.has("defaultValue") ? node.get("defaultValue")
                        : node.has("value") ? node.get("value") : NullNode.instance;
                if (defaultValue == null || defaultValue.isMissingNode()) {
                    defaultValue = NullNode.instance;
                }
                String defaultDisplay = text(node, "valueDisplay");
                drafts.add(new SnapshotFieldDraft(fieldPath, fieldKey,
                        StrUtil.blankToDefault(text(node, "label"), fieldKey), rowIndex, columnIndex,
                        text(node, "component"), valueType, defaultValue.deepCopy(),
                        defaultDisplay == null ? displayValue(defaultValue) : defaultDisplay,
                        identityKey, definitionMissing));
            }
            Set<String> ambiguous = new HashSet<>();
            identityCounts.forEach((key, count) -> {
                if (count > 1) {
                    ambiguous.add(key);
                }
            });
            List<SnapshotField> fields = drafts.stream()
                    .map(draft -> new SnapshotField(draft.fieldPath(), draft.fieldKey(), draft.label(),
                            draft.rowIndex(), draft.columnIndex(), draft.component(), draft.valueType(),
                            draft.defaultValue(), draft.defaultDisplay(), draft.identityKey(),
                            draft.definitionMissing(), draft.identityKey() != null
                            && ambiguous.contains(draft.identityKey())))
                    .toList();
            return new SnapshotDirectory(fields, reasons,
                    MesProBatchRecordExecutionFieldAuditHasher.hashExecutionSnapshot(executionSnapshotJson),
                    ambiguous);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.EXECUTION_SNAPSHOT_MISSING);
            return new SnapshotDirectory(List.of(), reasons, null, Set.of());
        }
    }

    private CellDirectory parseCells(String cellValuesJson, String storedCellValuesHash) {
        EnumSet<MesProBatchRecordExecutionResponsibilityReasonCode> reasons =
                EnumSet.noneOf(MesProBatchRecordExecutionResponsibilityReasonCode.class);
        Map<String, JsonNode> cells = new LinkedHashMap<>();
        Set<String> ambiguous = new HashSet<>();
        if (StrUtil.isBlank(cellValuesJson) || StrUtil.isBlank(storedCellValuesHash)) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.BASELINE_MISSING);
            return new CellDirectory(cells, ambiguous, reasons);
        }
        try {
            JsonNode root = JsonUtils.getObjectMapper().readTree(cellValuesJson);
            if (root == null || !root.isArray()) {
                reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.BASELINE_MISSING);
                return new CellDirectory(cells, ambiguous, reasons);
            }
            for (JsonNode node : root) {
                String fieldPath = text(node, "fieldPath");
                String fieldKey = text(node, "fieldKey");
                Integer rowIndex = integer(node, "rowIndex");
                Integer columnIndex = integer(node, "columnIndex");
                if (!node.isObject() || StrUtil.isBlank(fieldPath) || StrUtil.isBlank(fieldKey)
                        || rowIndex == null || columnIndex == null) {
                    reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.BASELINE_MISSING);
                    continue;
                }
                String identityKey = identityKey(fieldPath, fieldKey, rowIndex, columnIndex);
                if (cells.putIfAbsent(identityKey, node.deepCopy()) != null) {
                    ambiguous.add(identityKey);
                }
            }
            String calculatedHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson);
            if (!Objects.equals(calculatedHash, storedCellValuesHash)) {
                reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.CHAIN_INVALID);
            }
            return new CellDirectory(cells, ambiguous, reasons);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.BASELINE_MISSING);
            return new CellDirectory(cells, ambiguous, reasons);
        }
    }

    private Map<String, List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection>> groupAuditsByField(
            List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection> audits) {
        Map<String, List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection>> result =
                new LinkedHashMap<>();
        for (MesProBatchRecordExecutionFieldResponsibilityAuditProjection audit : audits) {
            String identityKey = identityKey(audit.getFieldPath(), audit.getFieldKey(), audit.getRowIndex(),
                    audit.getColumnIndex());
            if (identityKey != null) {
                result.computeIfAbsent(identityKey, ignored -> new ArrayList<>()).add(audit);
            }
        }
        return result;
    }

    private void addUnmatchedAuditReasons(
            SnapshotDirectory snapshot,
            List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection> audits,
            Set<MesProBatchRecordExecutionResponsibilityReasonCode> reasons) {
        Set<String> snapshotIdentities = new HashSet<>();
        for (SnapshotField field : snapshot.fields()) {
            if (field.identityKey() != null) {
                snapshotIdentities.add(field.identityKey());
            }
        }
        for (MesProBatchRecordExecutionFieldResponsibilityAuditProjection audit : audits) {
            String identity = identityKey(audit.getFieldPath(), audit.getFieldKey(), audit.getRowIndex(),
                    audit.getColumnIndex());
            if (identity == null || !snapshotIdentities.contains(identity)) {
                reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.FIELD_DEFINITION_MISSING);
            }
        }
    }

    private void addAssociationReasons(
            Long executionId,
            Long tenantId,
            List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection> audits,
            List<MesProBatchRecordExecutionFieldAuditBatchDO> batches,
            List<MesProEdhrWorkTaskDO> workTasks,
            Set<MesProBatchRecordExecutionResponsibilityReasonCode> reasons) {
        if (audits.stream().anyMatch(audit -> !Objects.equals(executionId, audit.getExecutionId()))
                || batches.stream().anyMatch(batch -> !Objects.equals(executionId, batch.getExecutionId()))
                || workTasks.stream().anyMatch(task -> !Objects.equals(executionId, task.getExecutionId()))) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.CROSS_EXECUTION_ASSOCIATION);
        }
        if (tenantId != null && (audits.stream().anyMatch(audit -> audit.getTenantId() != null
                && !Objects.equals(tenantId, audit.getTenantId()))
                || batches.stream().anyMatch(batch -> batch.getTenantId() != null
                && !Objects.equals(tenantId, batch.getTenantId())))) {
            reasons.add(MesProBatchRecordExecutionResponsibilityReasonCode.CROSS_TENANT_ASSOCIATION);
        }
    }

    private Map<Long, MesProBatchRecordExecutionFieldAuditBatchDO> indexBatches(
            List<MesProBatchRecordExecutionFieldAuditBatchDO> batches) {
        Map<Long, MesProBatchRecordExecutionFieldAuditBatchDO> result = new HashMap<>();
        for (MesProBatchRecordExecutionFieldAuditBatchDO batch : batches) {
            if (batch.getId() != null) {
                result.put(batch.getId(), batch);
            }
        }
        return result;
    }

    private Map<Long, MesProBatchRecordExecutionSignatureDO> indexSignatures(
            List<MesProBatchRecordExecutionSignatureDO> signatures) {
        Map<Long, MesProBatchRecordExecutionSignatureDO> result = new HashMap<>();
        for (MesProBatchRecordExecutionSignatureDO signature : signatures) {
            if (signature.getId() != null) {
                result.put(signature.getId(), signature);
            }
        }
        return result;
    }

    private boolean matches(
            MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO reqVO,
            MesProBatchRecordExecutionFieldResponsibilityItemRespVO item) {
        if (StrUtil.isNotBlank(reqVO.getFieldKeyword())) {
            String keyword = reqVO.getFieldKeyword().trim().toLowerCase(Locale.ROOT);
            if (!containsIgnoreCase(item.getFieldPath(), keyword)
                    && !containsIgnoreCase(item.getFieldKey(), keyword)
                    && !containsIgnoreCase(item.getFieldLabel(), keyword)) {
                return false;
            }
        }
        if (reqVO.getEvidenceStatus() != null && reqVO.getEvidenceStatus() != item.getEvidenceStatus()) {
            return false;
        }
        if (reqVO.getValueOrigin() != null && reqVO.getValueOrigin() != item.getValueOrigin()) {
            return false;
        }
        return reqVO.getActorId() == null
                || Objects.equals(reqVO.getActorId(), item.getFirstHumanActorId())
                || Objects.equals(reqVO.getActorId(), item.getCurrentValueActorId());
    }

    private boolean containsIgnoreCase(String value, String lowerCaseKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerCaseKeyword);
    }

    private MesProBatchRecordExecutionResponsibilityEvidenceStatus statusFor(
            Collection<MesProBatchRecordExecutionResponsibilityReasonCode> reasons) {
        if (reasons.stream().anyMatch(BLOCKING_REASONS::contains)) {
            return MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED;
        }
        return reasons.isEmpty()
                ? MesProBatchRecordExecutionResponsibilityEvidenceStatus.COMPLETE
                : MesProBatchRecordExecutionResponsibilityEvidenceStatus.EVIDENCE_MISSING;
    }

    private List<MesProBatchRecordExecutionResponsibilityReasonCode> orderedReasons(
            Collection<MesProBatchRecordExecutionResponsibilityReasonCode> reasons) {
        return java.util.Arrays.stream(MesProBatchRecordExecutionResponsibilityReasonCode.values())
                .filter(reasons::contains)
                .toList();
    }

    private void validatePage(MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO reqVO) {
        if (reqVO.getPageNo() == null) {
            reqVO.setPageNo(1);
        }
        if (reqVO.getPageSize() == null) {
            reqVO.setPageSize(50);
        }
        if (reqVO.getPageNo() < 1 || reqVO.getPageSize() < 1 || reqVO.getPageSize() > 200) {
            throw new IllegalArgumentException("responsibility summary page parameters are invalid");
        }
    }

    private MesProBatchRecordExecutionFieldAuditValueType parseValueType(String valueType) {
        if (StrUtil.isBlank(valueType)) {
            return null;
        }
        try {
            return MesProBatchRecordExecutionFieldAuditValueType.valueOf(valueType);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String identityKey(String fieldPath, String fieldKey, Integer rowIndex, Integer columnIndex) {
        if (StrUtil.isBlank(fieldPath) || StrUtil.isBlank(fieldKey) || rowIndex == null || columnIndex == null) {
            return null;
        }
        return fieldPath + '\u001f' + fieldKey + '\u001f' + rowIndex + '\u001f' + columnIndex;
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() || !value.isTextual() ? null : value.textValue();
    }

    private Integer integer(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(fieldName);
        return value == null || !value.canConvertToInt() ? null : value.intValue();
    }

    private String displayValue(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return "";
        }
        return value.isTextual() ? value.textValue()
                : MesProBatchRecordExecutionFieldAuditHasher.canonicalize(value);
    }

    private boolean isEmptyValue(JsonNode value) {
        return value == null || value.isNull() || value.isMissingNode()
                || value.isTextual() && value.textValue().isBlank();
    }

    private <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record SnapshotFieldDraft(
            String fieldPath,
            String fieldKey,
            String label,
            Integer rowIndex,
            Integer columnIndex,
            String component,
            MesProBatchRecordExecutionFieldAuditValueType valueType,
            JsonNode defaultValue,
            String defaultDisplay,
            String identityKey,
            boolean definitionMissing) {
    }

    private record SnapshotField(
            String fieldPath,
            String fieldKey,
            String label,
            Integer rowIndex,
            Integer columnIndex,
            String component,
            MesProBatchRecordExecutionFieldAuditValueType valueType,
            JsonNode defaultValue,
            String defaultDisplay,
            String identityKey,
            boolean definitionMissing,
            boolean ambiguous) {
    }

    private record SnapshotDirectory(
            List<SnapshotField> fields,
            Set<MesProBatchRecordExecutionResponsibilityReasonCode> reasons,
            String snapshotHash,
            Set<String> ambiguousIdentities) {
    }

    private record CellDirectory(
            Map<String, JsonNode> cells,
            Set<String> ambiguousIdentities,
            Set<MesProBatchRecordExecutionResponsibilityReasonCode> reasons) {
    }

    private record CellState(JsonNode value, String valueJson, String display, String hash) {
    }

    private record SummaryComputation(
            MesProBatchRecordExecutionDO execution,
            List<MesProBatchRecordExecutionFieldResponsibilityItemRespVO> items,
            Map<String, List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection>> auditsByField,
            Map<Long, MesProBatchRecordExecutionSignatureDO> signatureById,
            MesProBatchRecordExecutionResponsibilityEvidenceStatus overallStatus,
            List<MesProBatchRecordExecutionResponsibilityReasonCode> overallReasons,
            List<MesProBatchRecordExecutionResponsibilityContextWarning> contextWarnings) {
    }
}
