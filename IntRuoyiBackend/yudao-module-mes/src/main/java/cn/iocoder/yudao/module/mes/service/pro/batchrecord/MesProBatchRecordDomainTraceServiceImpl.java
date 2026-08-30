package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTraceDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTracePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTracePageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTraceVerifyReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordDomainTraceItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordDomainTraceSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordDomainTraceItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordDomainTraceSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordDomainTraceErrorCodeConstants.PRO_BATCH_RECORD_DOMAIN_TRACE_BLOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordDomainTraceErrorCodeConstants.PRO_BATCH_RECORD_DOMAIN_TRACE_HASH_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordDomainTraceErrorCodeConstants.PRO_BATCH_RECORD_DOMAIN_TRACE_PERSIST_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS;

@Service
public class MesProBatchRecordDomainTraceServiceImpl implements MesProBatchRecordDomainTraceService {

    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_BLOCKED = "BLOCKED";

    private static final String SNAPSHOT_VERSION = "EDHR_DOMAIN_TRACE_V1";

    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordDomainTraceSnapshotMapper snapshotMapper;
    @Resource
    private MesProBatchRecordDomainTraceItemMapper itemMapper;
    @Resource
    private MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    @Resource
    private MesProBatchRecordExecutionAttachmentService attachmentService;
    @Resource
    private MesProEdhrNonconformanceReviewMapper nonconformanceReviewMapper;

    @Override
    public MesProBatchRecordDomainTraceDetailRespVO getTraceDetail(Long executionId) {
        MesProBatchRecordExecutionDO execution = requireExecution(executionId);
        MesProBatchRecordDomainTraceSnapshotDO snapshot = snapshotMapper.selectLatestByExecutionId(executionId);
        if (snapshot == null) {
            DomainTraceEvaluation evaluation = evaluate(execution, null);
            return toDetail(execution, evaluation.snapshot(), evaluation.items());
        }
        return toDetail(execution, snapshot, itemMapper.selectListBySnapshotId(snapshot.getId()));
    }

    @Override
    public PageResult<MesProBatchRecordDomainTracePageRespVO> getTracePage(MesProBatchRecordDomainTracePageReqVO pageReqVO) {
        PageResult<MesProBatchRecordExecutionDO> pageResult = executionMapper.selectDomainTracePage(pageReqVO);
        List<MesProBatchRecordExecutionDO> executions = pageResult.getList();
        Map<Long, MesProBatchRecordDomainTraceSnapshotDO> snapshotMap = selectLatestSnapshotsByExecutionId(executions);
        Map<Long, Integer> itemCountMap = selectItemCountsBySnapshotId(snapshotMap.values());
        return new PageResult<>(executions.stream()
                .map(execution -> toPageRow(execution, snapshotMap.get(execution.getId()), itemCountMap))
                .toList(), pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordDomainTraceDetailRespVO verify(MesProBatchRecordDomainTraceVerifyReqVO reqVO) {
        MesProBatchRecordExecutionDO execution = requireExecution(reqVO.getExecutionId());
        LocalDateTime verifiedAt = LocalDateTime.now();
        DomainTraceEvaluation evaluation = evaluate(execution, verifiedAt);
        if (StrUtil.isNotBlank(reqVO.getExpectedDomainTraceHash())
                && !StrUtil.equals(reqVO.getExpectedDomainTraceHash(), evaluation.snapshot().getSnapshotHash())) {
            throw exception(PRO_BATCH_RECORD_DOMAIN_TRACE_HASH_MISMATCH);
        }
        DomainTraceEvaluation persistedEvaluation = persistEvaluation(execution, evaluation);
        return toDetail(execution, persistedEvaluation.snapshot(), persistedEvaluation.items());
    }

    @Override
    public MesProBatchRecordDomainTraceDetailRespVO verifyForSubmit(Long executionId) {
        MesProBatchRecordDomainTraceDetailRespVO detail = verify(new MesProBatchRecordDomainTraceVerifyReqVO()
                .setExecutionId(executionId));
        if (!STATUS_VERIFIED.equals(detail.getStatus())) {
            throw exception(PRO_BATCH_RECORD_DOMAIN_TRACE_BLOCKED);
        }
        return detail;
    }

    @Override
    public MesProBatchRecordDomainTraceDetailRespVO verifyForApproval(Long executionId, String expectedDomainTraceHash) {
        return verifyExpectedAndRequireVerified(executionId, expectedDomainTraceHash);
    }

    @Override
    public MesProBatchRecordDomainTraceDetailRespVO verifyForArchive(Long executionId, String expectedDomainTraceHash) {
        return verifyExpectedAndRequireVerified(executionId, expectedDomainTraceHash);
    }

    private MesProBatchRecordDomainTraceDetailRespVO verifyExpectedAndRequireVerified(Long executionId,
                                                                                      String expectedDomainTraceHash) {
        MesProBatchRecordDomainTraceDetailRespVO detail = verify(new MesProBatchRecordDomainTraceVerifyReqVO()
                .setExecutionId(executionId)
                .setExpectedDomainTraceHash(expectedDomainTraceHash));
        if (!STATUS_VERIFIED.equals(detail.getStatus())) {
            throw exception(PRO_BATCH_RECORD_DOMAIN_TRACE_BLOCKED);
        }
        return detail;
    }

    private DomainTraceEvaluation persistEvaluation(MesProBatchRecordExecutionDO execution,
                                                    DomainTraceEvaluation evaluation) {
        DomainTraceEvaluation existingEvaluation =
                loadPersistedEvaluation(execution.getId(), evaluation.snapshot().getSnapshotHash());
        if (existingEvaluation != null) {
            updateExecutionDomainTrace(execution, existingEvaluation.snapshot());
            return existingEvaluation;
        }

        try {
            if (snapshotMapper.insert(evaluation.snapshot()) != 1 || evaluation.snapshot().getId() == null) {
                throw exception(PRO_BATCH_RECORD_DOMAIN_TRACE_PERSIST_FAILED);
            }
        } catch (DuplicateKeyException duplicateKeyException) {
            DomainTraceEvaluation concurrentEvaluation =
                    loadPersistedEvaluation(execution.getId(), evaluation.snapshot().getSnapshotHash());
            if (concurrentEvaluation == null) {
                throw exception(PRO_BATCH_RECORD_DOMAIN_TRACE_PERSIST_FAILED);
            }
            updateExecutionDomainTrace(execution, concurrentEvaluation.snapshot());
            return concurrentEvaluation;
        }
        for (MesProBatchRecordDomainTraceItemDO item : evaluation.items()) {
            item.setSnapshotId(evaluation.snapshot().getId());
            if (itemMapper.insert(item) != 1) {
                throw exception(PRO_BATCH_RECORD_DOMAIN_TRACE_PERSIST_FAILED);
            }
        }
        updateExecutionDomainTrace(execution, evaluation.snapshot());
        return evaluation;
    }

    private DomainTraceEvaluation loadPersistedEvaluation(Long executionId, String snapshotHash) {
        MesProBatchRecordDomainTraceSnapshotDO snapshot =
                snapshotMapper.selectByExecutionIdAndSnapshotHash(executionId, snapshotHash);
        if (snapshot == null) {
            return null;
        }
        List<MesProBatchRecordDomainTraceItemDO> items = itemMapper.selectListBySnapshotId(snapshot.getId());
        if (items == null || items.isEmpty()) {
            throw exception(PRO_BATCH_RECORD_DOMAIN_TRACE_PERSIST_FAILED);
        }
        return new DomainTraceEvaluation(snapshot, items);
    }

    private void updateExecutionDomainTrace(MesProBatchRecordExecutionDO execution,
                                            MesProBatchRecordDomainTraceSnapshotDO snapshot) {
        if (executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setDomainTraceSnapshotId(snapshot.getId())
                .setDomainTraceHash(snapshot.getSnapshotHash())
                .setDomainTraceStatus(snapshot.getCompletenessStatus())
                .setDomainTraceVerifiedAt(snapshot.getVerifiedAt())) != 1) {
            throw exception(PRO_BATCH_RECORD_DOMAIN_TRACE_PERSIST_FAILED);
        }
        execution.setDomainTraceSnapshotId(snapshot.getId());
        execution.setDomainTraceHash(snapshot.getSnapshotHash());
        execution.setDomainTraceStatus(snapshot.getCompletenessStatus());
        execution.setDomainTraceVerifiedAt(snapshot.getVerifiedAt());
    }

    private DomainTraceEvaluation evaluate(MesProBatchRecordExecutionDO execution, LocalDateTime verifiedAt) {
        List<MesProBatchRecordDomainTraceItemDO> items = new ArrayList<>();
        addRequiredItem(items, execution, "WORK_ORDER", "workOrderId", "Work order",
                "mes_pro_work_order", execution.getWorkOrderId(), execution.getWorkOrderCode(), "v1",
                execution.getWorkOrderId() != null && StrUtil.isNotBlank(execution.getWorkOrderCode()),
                "EDHR_DOMAIN_TRACE_WORK_ORDER_REQUIRED", "Work order link and code are required");
        addRequiredItem(items, execution, "ROUTE_PROCESS", "routeProcessId", "Route process",
                "mes_pro_route_process", execution.getRouteProcessId(),
                execution.getRouteProcessId() == null ? null : String.valueOf(execution.getRouteProcessId()), "v1",
                execution.getRouteProcessId() != null,
                "EDHR_DOMAIN_TRACE_ROUTE_PROCESS_REQUIRED", "Route process link is required");
        addRequiredItem(items, execution, "BATCH_RECORD_REPORT", "batchRecordReportId", "Batch record report",
                "jimu_report", null, execution.getBatchRecordReportId(), "v1",
                StrUtil.isNotBlank(execution.getBatchRecordReportId()),
                "EDHR_DOMAIN_TRACE_BATCH_RECORD_REPORT_REQUIRED", "Batch record report link is required");
        addRequiredItem(items, execution, "BATCH", "batchCode", "Production batch",
                "mes_wm_batch", null, execution.getBatchCode(), "v1",
                StrUtil.isNotBlank(execution.getBatchCode()),
                "EDHR_DOMAIN_TRACE_BATCH_CODE_REQUIRED", "Production batch code is required");
        addSnapshotItem(items, execution, "EXECUTION_SNAPSHOT", "executionSnapshotJson", "Execution snapshot",
                StrUtil.isNotBlank(execution.getExecutionSnapshotJson()), execution.getExecutionSnapshotJson(),
                "EDHR_DOMAIN_TRACE_EXECUTION_SNAPSHOT_REQUIRED", "Execution snapshot JSON is required");
        addSnapshotItem(items, execution, "FIELD_AUDIT_BASELINE", "fieldAuditBaseline", "Field audit baseline",
                StrUtil.isNotBlank(execution.getCellValuesHash())
                        && execution.getFieldAuditRevision() != null
                        && StrUtil.isNotBlank(execution.getFieldAuditHeadHash()),
                buildFieldAuditBaselineJson(execution),
                "EDHR_DOMAIN_TRACE_FIELD_AUDIT_BASELINE_REQUIRED", "Field audit baseline hash and revision are required");
        addNonconformanceReviewItems(items, execution);

        int blockerCount = (int) items.stream().filter(item -> STATUS_BLOCKED.equals(item.getStatus())).count();
        String status = blockerCount == 0 ? STATUS_VERIFIED : STATUS_BLOCKED;
        String snapshotJson = buildSnapshotJson(execution, status, items);
        MesProBatchRecordDomainTraceSnapshotDO snapshot = MesProBatchRecordDomainTraceSnapshotDO.builder()
                .executionId(execution.getId())
                .snapshotVersion(SNAPSHOT_VERSION)
                .snapshotJson(snapshotJson)
                .snapshotHash(DigestUtil.sha256Hex(snapshotJson))
                .completenessStatus(status)
                .blockerCount(blockerCount)
                .verifiedAt(verifiedAt)
                .build();
        return new DomainTraceEvaluation(snapshot, items);
    }

    private void addRequiredItem(List<MesProBatchRecordDomainTraceItemDO> items,
                                 MesProBatchRecordExecutionDO execution,
                                 String itemType,
                                 String itemKey,
                                 String itemName,
                                 String sourceTable,
                                 Long sourceId,
                                 String sourceCode,
                                 String sourceVersion,
                                 boolean complete,
                                 String blockerCode,
                                 String blockerMessage) {
        JSONObject payload = new JSONObject(true);
        payload.put("itemType", itemType);
        payload.put("itemKey", itemKey);
        payload.put("sourceTable", sourceTable);
        payload.put("sourceId", sourceId);
        payload.put("sourceCode", sourceCode);
        payload.put("sourceVersion", sourceVersion);
        items.add(baseItem(execution, itemType, itemKey, itemName, sourceTable, sourceId, sourceCode, sourceVersion,
                complete, blockerCode, blockerMessage)
                .setSnapshotJson(complete ? payload.toJSONString() : null)
                .setSnapshotHash(complete ? DigestUtil.sha256Hex(payload.toJSONString()) : null));
    }

    private void addSnapshotItem(List<MesProBatchRecordDomainTraceItemDO> items,
                                 MesProBatchRecordExecutionDO execution,
                                 String itemType,
                                 String itemKey,
                                 String itemName,
                                 boolean complete,
                                 String snapshotJson,
                                 String blockerCode,
                                 String blockerMessage) {
        String normalizedSnapshot = StrUtil.trim(snapshotJson);
        items.add(baseItem(execution, itemType, itemKey, itemName, "mes_pro_batch_record_execution", execution.getId(),
                execution.getExecutionCode(), SNAPSHOT_VERSION, complete, blockerCode, blockerMessage)
                .setSnapshotJson(complete ? normalizedSnapshot : null)
                .setSnapshotHash(complete ? DigestUtil.sha256Hex(normalizedSnapshot) : null));
    }

    private void addNonconformanceReviewItems(List<MesProBatchRecordDomainTraceItemDO> items,
                                              MesProBatchRecordExecutionDO execution) {
        for (MesProEdhrNonconformanceReviewDO review : selectNonconformanceReviews(execution)) {
            String snapshotJson = StrUtil.blankToDefault(review.getTraceSnapshotJson(),
                    buildNonconformanceReviewTraceSnapshotJson(review));
            items.add(baseItem(execution, "NONCONFORMANCE_REVIEW", review.getReviewCode(), "不合格评审",
                    "mes_pro_edhr_nonconformance_review", review.getId(), review.getReviewCode(),
                    review.getReviewStatus(), true, null, null)
                    .setSnapshotJson(snapshotJson)
                    .setSnapshotHash(DigestUtil.sha256Hex(snapshotJson)));
        }
    }

    private MesProBatchRecordDomainTraceItemDO baseItem(MesProBatchRecordExecutionDO execution,
                                                        String itemType,
                                                        String itemKey,
                                                        String itemName,
                                                        String sourceTable,
                                                        Long sourceId,
                                                        String sourceCode,
                                                        String sourceVersion,
                                                        boolean complete,
                                                        String blockerCode,
                                                        String blockerMessage) {
        return MesProBatchRecordDomainTraceItemDO.builder()
                .executionId(execution.getId())
                .itemType(itemType)
                .itemKey(itemKey)
                .itemName(itemName)
                .sourceTable(sourceTable)
                .sourceId(sourceId)
                .sourceCode(sourceCode)
                .sourceVersion(sourceVersion)
                .requiredFlag(Boolean.TRUE)
                .status(complete ? STATUS_VERIFIED : STATUS_BLOCKED)
                .blockerCode(complete ? null : blockerCode)
                .blockerMessage(complete ? null : blockerMessage)
                .blockerReason(complete ? null : blockerMessage)
                .build();
    }

    private String buildFieldAuditBaselineJson(MesProBatchRecordExecutionDO execution) {
        JSONObject payload = new JSONObject(true);
        payload.put("cellValuesHash", execution.getCellValuesHash());
        payload.put("fieldAuditRevision", execution.getFieldAuditRevision());
        payload.put("fieldAuditHeadHash", execution.getFieldAuditHeadHash());
        return payload.toJSONString();
    }

    private String buildSnapshotJson(MesProBatchRecordExecutionDO execution,
                                     String status,
                                     List<MesProBatchRecordDomainTraceItemDO> items) {
        JSONObject snapshot = new JSONObject(true);
        snapshot.put("snapshotVersion", SNAPSHOT_VERSION);
        snapshot.put("executionId", execution.getId());
        snapshot.put("executionCode", execution.getExecutionCode());
        snapshot.put("workOrderCode", execution.getWorkOrderCode());
        snapshot.put("batchCode", execution.getBatchCode());
        snapshot.put("status", status);
        JSONArray itemArray = new JSONArray();
        JSONArray blockerArray = new JSONArray();
        for (MesProBatchRecordDomainTraceItemDO item : items) {
            JSONObject itemJson = new JSONObject(true);
            itemJson.put("itemType", item.getItemType());
            itemJson.put("itemKey", item.getItemKey());
            itemJson.put("itemName", item.getItemName());
            itemJson.put("sourceTable", item.getSourceTable());
            itemJson.put("sourceId", item.getSourceId());
            itemJson.put("sourceCode", item.getSourceCode());
            itemJson.put("sourceVersion", item.getSourceVersion());
            itemJson.put("snapshotHash", item.getSnapshotHash());
            itemJson.put("status", item.getStatus());
            itemJson.put("blockerCode", item.getBlockerCode());
            itemJson.put("blockerMessage", item.getBlockerMessage());
            itemArray.add(itemJson);
            if (STATUS_BLOCKED.equals(item.getStatus())) {
                JSONObject blocker = new JSONObject(true);
                blocker.put("itemType", item.getItemType());
                blocker.put("itemKey", item.getItemKey());
                blocker.put("blockerCode", item.getBlockerCode());
                blocker.put("blockerMessage", item.getBlockerMessage());
                blockerArray.add(blocker);
            }
        }
        snapshot.put("items", itemArray);
        snapshot.put("blockers", blockerArray);
        return snapshot.toJSONString();
    }

    private MesProBatchRecordDomainTraceDetailRespVO toDetail(MesProBatchRecordExecutionDO execution,
                                                              MesProBatchRecordDomainTraceSnapshotDO snapshot,
                                                              List<MesProBatchRecordDomainTraceItemDO> items) {
        MesProBatchRecordDomainTraceDetailRespVO detail = new MesProBatchRecordDomainTraceDetailRespVO()
                .setExecutionId(execution.getId())
                .setExecutionCode(execution.getExecutionCode())
                .setStatus(snapshot.getCompletenessStatus())
                .setDomainTraceSnapshotId(snapshot.getId())
                .setDomainTraceHash(snapshot.getSnapshotHash())
                .setVerifiedAt(snapshot.getVerifiedAt());
        detail.setNonconformanceReviews(selectNonconformanceReviews(execution).stream()
                .map(this::toNonconformanceReviewTrace)
                .toList());
        detail.setItems(items.stream().map(this::toItem).toList());
        detail.setBlockers(items.stream()
                .filter(item -> STATUS_BLOCKED.equals(item.getStatus()))
                .map(this::toBlocker)
                .toList());
        populateAttachmentSummary(detail, execution.getId());
        return detail;
    }

    private void populateAttachmentSummary(MesProBatchRecordDomainTraceDetailRespVO detail, Long executionId) {
        MesProBatchRecordExecutionAttachmentChainVerifyResult verifyResult =
                attachmentService.verifyAttachmentChain(executionId);
        List<MesProBatchRecordExecutionAttachmentDO> attachments =
                attachmentMapper.selectListByExecutionId(executionId);
        detail.setAttachmentCount(attachments.size());
        detail.setAttachmentChainStatus(verifyResult != null && verifyResult.isValid() ? "VALID" : "INVALID");
        detail.setAttachmentChainHeadHash(verifyResult == null ? null : verifyResult.getHeadHash());
        detail.setAttachmentSummaries(attachments.stream()
                .map(this::toAttachmentSummary)
                .toList());
    }

    private MesProBatchRecordDomainTraceDetailRespVO.AttachmentSummary toAttachmentSummary(
            MesProBatchRecordExecutionAttachmentDO attachment) {
        return new MesProBatchRecordDomainTraceDetailRespVO.AttachmentSummary()
                .setId(attachment.getId())
                .setAuditBatchId(attachment.getAuditBatchId())
                .setSignatureId(attachment.getSignatureId())
                .setWorkTaskId(attachment.getWorkTaskId())
                .setRowIndex(attachment.getRowIndex())
                .setColumnIndex(attachment.getColumnIndex())
                .setFieldKey(attachment.getFieldKey())
                .setFieldPath(attachment.getFieldPath())
                .setFieldLabel(attachment.getFieldLabel())
                .setAttachmentType(attachment.getAttachmentType())
                .setAttachmentGroupKey(attachment.getAttachmentGroupKey())
                .setAttachmentAction(attachment.getAttachmentAction())
                .setVersionNo(attachment.getVersionNo())
                .setFileId(attachment.getFileId())
                .setFileName(attachment.getFileName())
                .setContentType(attachment.getContentType())
                .setFileSize(attachment.getFileSize())
                .setSha256(attachment.getSha256())
                .setStorageRetentionHash(attachment.getStorageRetentionHash())
                .setPreviousAttachmentHash(attachment.getPreviousAttachmentHash())
                .setAttachmentHash(attachment.getAttachmentHash())
                .setOperatorId(attachment.getOperatorId())
                .setOperatorName(attachment.getOperatorName())
                .setOperatedAt(attachment.getOperatedAt())
                .setReasonCategory(attachment.getReasonCategory())
                .setReasonText(attachment.getReasonText());
    }

    private Map<Long, MesProBatchRecordDomainTraceSnapshotDO> selectLatestSnapshotsByExecutionId(
            List<MesProBatchRecordExecutionDO> executions) {
        List<Long> executionIds = executions.stream()
                .filter(execution -> execution.getDomainTraceSnapshotId() != null)
                .map(MesProBatchRecordExecutionDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (executionIds.isEmpty()) {
            return Map.of();
        }
        return snapshotMapper.selectListByExecutionIds(executionIds).stream()
                .collect(Collectors.toMap(MesProBatchRecordDomainTraceSnapshotDO::getExecutionId,
                        snapshot -> snapshot,
                        (current, ignoredOlderSnapshot) -> current));
    }

    private Map<Long, Integer> selectItemCountsBySnapshotId(
            Collection<MesProBatchRecordDomainTraceSnapshotDO> snapshots) {
        List<Long> snapshotIds = snapshots.stream()
                .map(MesProBatchRecordDomainTraceSnapshotDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (snapshotIds.isEmpty()) {
            return Map.of();
        }
        return itemMapper.selectListBySnapshotIds(snapshotIds).stream()
                .collect(Collectors.groupingBy(MesProBatchRecordDomainTraceItemDO::getSnapshotId,
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));
    }

    private MesProBatchRecordDomainTracePageRespVO toPageRow(MesProBatchRecordExecutionDO execution,
                                                             MesProBatchRecordDomainTraceSnapshotDO snapshot,
                                                             Map<Long, Integer> itemCountMap) {
        Integer itemCount = snapshot == null ? null : itemCountMap.getOrDefault(snapshot.getId(), 0);
        return new MesProBatchRecordDomainTracePageRespVO()
                .setExecutionId(execution.getId())
                .setExecutionCode(execution.getExecutionCode())
                .setWorkOrderCode(execution.getWorkOrderCode())
                .setBatchCode(execution.getBatchCode())
                .setStatus(StrUtil.blankToDefault(execution.getDomainTraceStatus(), STATUS_BLOCKED))
                .setDomainTraceHash(execution.getDomainTraceHash())
                .setVerifiedAt(execution.getDomainTraceVerifiedAt())
                .setBlockerCount(snapshot == null ? null : snapshot.getBlockerCount())
                .setItemCount(itemCount);
    }

    private MesProBatchRecordDomainTraceDetailRespVO.Item toItem(MesProBatchRecordDomainTraceItemDO item) {
        return new MesProBatchRecordDomainTraceDetailRespVO.Item()
                .setItemType(item.getItemType())
                .setItemKey(item.getItemKey())
                .setItemName(item.getItemName())
                .setSourceId(item.getSourceId())
                .setSourceCode(item.getSourceCode())
                .setSourceVersion(item.getSourceVersion())
                .setSnapshotJson(item.getSnapshotJson())
                .setSnapshotHash(item.getSnapshotHash())
                .setStatus(item.getStatus())
                .setBlockerReason(item.getBlockerReason());
    }

    private MesProBatchRecordDomainTraceDetailRespVO.Blocker toBlocker(MesProBatchRecordDomainTraceItemDO item) {
        return new MesProBatchRecordDomainTraceDetailRespVO.Blocker()
                .setItemType(item.getItemType())
                .setItemKey(item.getItemKey())
                .setBlockerCode(item.getBlockerCode())
                .setBlockerMessage(item.getBlockerMessage());
    }

    private List<MesProEdhrNonconformanceReviewDO> selectNonconformanceReviews(
            MesProBatchRecordExecutionDO execution) {
        if (execution.getBatchExecutionId() == null) {
            return List.of();
        }
        return nonconformanceReviewMapper.selectListByBatchExecutionId(execution.getBatchExecutionId());
    }

    private String buildNonconformanceReviewTraceSnapshotJson(MesProEdhrNonconformanceReviewDO review) {
        JSONObject payload = new JSONObject(true);
        payload.put("reviewId", review.getId());
        payload.put("reviewCode", review.getReviewCode());
        payload.put("sourceType", review.getSourceType());
        payload.put("sourceId", review.getSourceId());
        payload.put("batchExecutionId", review.getBatchExecutionId());
        payload.put("batchExecutionCode", review.getBatchExecutionCode());
        payload.put("workOrderCode", review.getWorkOrderCode());
        payload.put("batchCode", review.getBatchCode());
        payload.put("reviewStatus", review.getReviewStatus());
        payload.put("nonconformanceReason", review.getNonconformanceReason());
        payload.put("reviewMaterialUrl", review.getReviewMaterialUrl());
        payload.put("reviewOpinion", review.getReviewOpinion());
        payload.put("qaSignature", review.getQaSignature());
        payload.put("qaUserId", review.getQaUserId());
        payload.put("disposition", review.getDisposition());
        payload.put("frozenAt", review.getFrozenAt());
        payload.put("unfrozenAt", review.getUnfrozenAt());
        payload.put("voidedAt", review.getVoidedAt());
        payload.put("closedAt", review.getClosedAt());
        return payload.toJSONString();
    }

    private MesProBatchRecordDomainTraceDetailRespVO.NonconformanceReviewTrace toNonconformanceReviewTrace(
            MesProEdhrNonconformanceReviewDO review) {
        return new MesProBatchRecordDomainTraceDetailRespVO.NonconformanceReviewTrace()
                .setId(review.getId())
                .setReviewCode(review.getReviewCode())
                .setSourceType(review.getSourceType())
                .setSourceId(review.getSourceId())
                .setBatchExecutionId(review.getBatchExecutionId())
                .setBatchExecutionCode(review.getBatchExecutionCode())
                .setWorkOrderCode(review.getWorkOrderCode())
                .setBatchCode(review.getBatchCode())
                .setReviewStatus(review.getReviewStatus())
                .setNonconformanceReason(review.getNonconformanceReason())
                .setReviewMaterialUrl(review.getReviewMaterialUrl())
                .setReviewOpinion(review.getReviewOpinion())
                .setQaSignature(review.getQaSignature())
                .setQaUserId(review.getQaUserId())
                .setDisposition(review.getDisposition())
                .setFrozenAt(review.getFrozenAt())
                .setClosedAt(review.getClosedAt())
                .setUnfrozenAt(review.getUnfrozenAt())
                .setVoidedAt(review.getVoidedAt())
                .setTraceSnapshotJson(review.getTraceSnapshotJson());
    }

    private MesProBatchRecordExecutionDO requireExecution(Long executionId) {
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        return execution;
    }

    private record DomainTraceEvaluation(MesProBatchRecordDomainTraceSnapshotDO snapshot,
                                         List<MesProBatchRecordDomainTraceItemDO> items) {
    }
}
