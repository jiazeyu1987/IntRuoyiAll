package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogPageRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID;

@Service
@Validated
public class MesProEdhrFormFillLogServiceImpl implements MesProEdhrFormFillLogService {

    private static final String CONTEXT_COMPLETE = "COMPLETE";
    private static final String CONTEXT_BATCH_CONTEXT_MISSING = "BATCH_CONTEXT_MISSING";
    private static final String CONTEXT_EXECUTION_MISSING = "EXECUTION_MISSING";
    private static final int SUMMARY_LIMIT = 3;

    @Resource
    private MesProBatchRecordExecutionFieldAuditBatchMapper auditBatchMapper;
    @Resource
    private MesProBatchRecordExecutionFieldAuditItemMapper auditItemMapper;
    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchExecutionTaskMapper;

    @Override
    public PageResult<MesProEdhrFormFillLogPageRespVO> getPage(MesProEdhrFormFillLogPageReqVO pageReqVO) {
        MesProEdhrFormFillLogPageReqVO reqVO =
                pageReqVO == null ? new MesProEdhrFormFillLogPageReqVO() : pageReqVO;
        List<Long> executionIds = resolveExecutionIds(reqVO);
        if (executionIds != null && executionIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditBatchDO> query =
                new LambdaQueryWrapperX<MesProBatchRecordExecutionFieldAuditBatchDO>()
                        .inIfPresent(MesProBatchRecordExecutionFieldAuditBatchDO::getExecutionId, executionIds)
                        .eqIfPresent(MesProBatchRecordExecutionFieldAuditBatchDO::getActorId, reqVO.getActorId())
                        .likeIfPresent(MesProBatchRecordExecutionFieldAuditBatchDO::getActorName, reqVO.getActorName())
                        .betweenIfPresent(MesProBatchRecordExecutionFieldAuditBatchDO::getChangedAt,
                                reqVO.getChangedAtStart(), reqVO.getChangedAtEnd())
                        .orderByDesc(MesProBatchRecordExecutionFieldAuditBatchDO::getChangedAt)
                        .orderByDesc(MesProBatchRecordExecutionFieldAuditBatchDO::getId);
        PageResult<MesProBatchRecordExecutionFieldAuditBatchDO> page = auditBatchMapper.selectPage(reqVO, query);
        List<MesProEdhrFormFillLogPageRespVO> rows = toPageRespList(page.getList());
        return new PageResult<>(rows, page.getTotal());
    }

    @Override
    public MesProEdhrFormFillLogDetailRespVO getDetail(Long auditBatchId) {
        MesProBatchRecordExecutionFieldAuditBatchDO batch = auditBatchMapper.selectById(auditBatchId);
        if (batch == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID);
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(batch.getExecutionId());
        MesProEdhrBatchExecutionTaskDO task = execution == null
                ? null : batchExecutionTaskMapper.selectByExecutionId(execution.getId());
        List<MesProEdhrFormFillLogItemRespVO> items = auditItemMapper.selectListByBatchId(batch.getId()).stream()
                .map(this::toItemResp)
                .toList();
        return new MesProEdhrFormFillLogDetailRespVO()
                .setAuditBatchId(batch.getId())
                .setExecutionId(batch.getExecutionId())
                .setExecutionCode(execution == null ? null : execution.getExecutionCode())
                .setBatchRecordReportId(resolveBatchRecordReportId(execution, task))
                .setFormName(resolveFormName(execution, task))
                .setBatchExecutionId(task == null ? null : task.getBatchExecutionId())
                .setBatchCode(execution == null ? null : execution.getBatchCode())
                .setWorkOrderCode(execution == null ? null : execution.getWorkOrderCode())
                .setActorId(batch.getActorId())
                .setActorName(batch.getActorName())
                .setChangedAt(batch.getChangedAt())
                .setFieldCount(batch.getFieldCount())
                .setContextStatus(resolveContextStatus(execution, task))
                .setHashStatus(resolveHashStatus(batch))
                .setItems(items);
    }

    private List<MesProEdhrFormFillLogPageRespVO> toPageRespList(List<MesProBatchRecordExecutionFieldAuditBatchDO> batches) {
        if (batches == null || batches.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> executionIds = batches.stream()
                .map(MesProBatchRecordExecutionFieldAuditBatchDO::getExecutionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MesProBatchRecordExecutionDO> executionMap = executionMapper.selectListByIds(executionIds).stream()
                .collect(Collectors.toMap(MesProBatchRecordExecutionDO::getId, Function.identity(), (left, right) -> left));
        Map<Long, MesProEdhrBatchExecutionTaskDO> taskMap = batchExecutionTaskMapper.selectListByExecutionIds(executionIds).stream()
                .filter(task -> task.getExecutionId() != null)
                .collect(Collectors.toMap(MesProEdhrBatchExecutionTaskDO::getExecutionId, Function.identity(), (first, ignored) -> first));
        List<Long> auditBatchIds = batches.stream()
                .map(MesProBatchRecordExecutionFieldAuditBatchDO::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, List<MesProBatchRecordExecutionFieldAuditItemDO>> summaryItemMap =
                auditItemMapper.selectSummaryListByBatchIds(auditBatchIds, SUMMARY_LIMIT).stream()
                        .sorted(Comparator
                                .comparing(MesProBatchRecordExecutionFieldAuditItemDO::getAuditBatchId,
                                        Comparator.nullsLast(Long::compareTo))
                                .thenComparing(MesProBatchRecordExecutionFieldAuditItemDO::getBatchItemIndex,
                                        Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(MesProBatchRecordExecutionFieldAuditItemDO::getId,
                                        Comparator.nullsLast(Long::compareTo)))
                        .collect(Collectors.groupingBy(MesProBatchRecordExecutionFieldAuditItemDO::getAuditBatchId));
        return batches.stream()
                .map(batch -> toPageResp(batch, executionMap.get(batch.getExecutionId()),
                        taskMap.get(batch.getExecutionId()), summaryItemMap.get(batch.getId())))
                .toList();
    }

    private MesProEdhrFormFillLogPageRespVO toPageResp(MesProBatchRecordExecutionFieldAuditBatchDO batch,
                                                       MesProBatchRecordExecutionDO execution,
                                                       MesProEdhrBatchExecutionTaskDO task,
                                                       List<MesProBatchRecordExecutionFieldAuditItemDO> items) {
        return new MesProEdhrFormFillLogPageRespVO()
                .setAuditBatchId(batch.getId())
                .setExecutionId(batch.getExecutionId())
                .setExecutionCode(execution == null ? null : execution.getExecutionCode())
                .setBatchRecordReportId(resolveBatchRecordReportId(execution, task))
                .setFormName(resolveFormName(execution, task))
                .setBatchExecutionId(task == null ? null : task.getBatchExecutionId())
                .setBatchCode(execution == null ? null : execution.getBatchCode())
                .setWorkOrderCode(execution == null ? null : execution.getWorkOrderCode())
                .setActorId(batch.getActorId())
                .setActorName(batch.getActorName())
                .setChangedAt(batch.getChangedAt())
                .setFieldCount(batch.getFieldCount())
                .setCellSummary(resolveCellSummary(items))
                .setContextStatus(resolveContextStatus(execution, task))
                .setHashStatus(resolveHashStatus(batch));
    }

    private List<Long> resolveExecutionIds(MesProEdhrFormFillLogPageReqVO reqVO) {
        if (!hasExecutionFilter(reqVO)) {
            return null;
        }
        LambdaQueryWrapperX<MesProBatchRecordExecutionDO> query =
                new LambdaQueryWrapperX<MesProBatchRecordExecutionDO>()
                        .eqIfPresent(MesProBatchRecordExecutionDO::getBatchRecordReportId, reqVO.getBatchRecordReportId())
                        .likeIfPresent(MesProBatchRecordExecutionDO::getExecutionCode, reqVO.getExecutionCode())
                        .likeIfPresent(MesProBatchRecordExecutionDO::getBatchCode, reqVO.getBatchCode())
                        .likeIfPresent(MesProBatchRecordExecutionDO::getWorkOrderCode, reqVO.getWorkOrderCode());
        if (StrUtil.isNotBlank(reqVO.getFormKeyword())) {
            String formKeyword = StrUtil.trim(reqVO.getFormKeyword());
            query.and(wrapper -> wrapper
                    .like(MesProBatchRecordExecutionDO::getTemplateName, formKeyword)
                    .or()
                    .like(MesProBatchRecordExecutionDO::getBatchRecordReportId, formKeyword));
        }
        return executionMapper.selectList(query).stream()
                .map(MesProBatchRecordExecutionDO::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean hasExecutionFilter(MesProEdhrFormFillLogPageReqVO reqVO) {
        return StrUtil.isNotBlank(reqVO.getBatchRecordReportId())
                || StrUtil.isNotBlank(reqVO.getFormKeyword())
                || StrUtil.isNotBlank(reqVO.getExecutionCode())
                || StrUtil.isNotBlank(reqVO.getBatchCode())
                || StrUtil.isNotBlank(reqVO.getWorkOrderCode());
    }

    private String resolveBatchRecordReportId(MesProBatchRecordExecutionDO execution,
                                              MesProEdhrBatchExecutionTaskDO task) {
        if (StrUtil.isNotBlank(task == null ? null : task.getBatchRecordReportId())) {
            return task.getBatchRecordReportId();
        }
        return execution == null ? null : execution.getBatchRecordReportId();
    }

    private String resolveFormName(MesProBatchRecordExecutionDO execution, MesProEdhrBatchExecutionTaskDO task) {
        if (StrUtil.isNotBlank(task == null ? null : task.getBatchRecordReportName())) {
            return task.getBatchRecordReportName();
        }
        if (StrUtil.isNotBlank(execution == null ? null : execution.getTemplateName())) {
            return execution.getTemplateName();
        }
        return resolveBatchRecordReportId(execution, task);
    }

    private String resolveContextStatus(MesProBatchRecordExecutionDO execution, MesProEdhrBatchExecutionTaskDO task) {
        if (execution == null) {
            return CONTEXT_EXECUTION_MISSING;
        }
        if (task == null || task.getBatchExecutionId() == null) {
            return CONTEXT_BATCH_CONTEXT_MISSING;
        }
        return CONTEXT_COMPLETE;
    }

    private String resolveHashStatus(MesProBatchRecordExecutionFieldAuditBatchDO batch) {
        if (StrUtil.containsIgnoreCase(batch.getHashVerificationJson(), "\"status\":\"VALID\"")) {
            return "VALID";
        }
        if (StrUtil.isBlank(batch.getHashVerificationJson())) {
            return "UNKNOWN";
        }
        return "CHECK_REQUIRED";
    }

    private String resolveCellSummary(List<MesProBatchRecordExecutionFieldAuditItemDO> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        String summary = items.stream()
                .limit(SUMMARY_LIMIT)
                .map(item -> {
                    String label = StrUtil.blankToDefault(item.getFieldLabel(), item.getFieldKey());
                    if (item.getRecordbookValueDisplay() != null || item.getBatchRecordValueDisplay() != null) {
                        return label + "=记录本 " + StrUtil.nullToEmpty(item.getRecordbookValueDisplay())
                                + " / 批记录 " + StrUtil.nullToEmpty(item.getBatchRecordValueDisplay());
                    }
                    return label + "=" + StrUtil.blankToDefault(item.getNewValueDisplay(), "");
                })
                .collect(Collectors.joining("；"));
        return items.size() > SUMMARY_LIMIT ? summary + "；..." : summary;
    }

    private MesProEdhrFormFillLogItemRespVO toItemResp(MesProBatchRecordExecutionFieldAuditItemDO item) {
        return new MesProEdhrFormFillLogItemRespVO()
                .setAuditItemId(item.getId())
                .setFieldPath(item.getFieldPath())
                .setFieldKey(item.getFieldKey())
                .setFieldLabel(item.getFieldLabel())
                .setRowIndex(item.getRowIndex())
                .setColumnIndex(item.getColumnIndex())
                .setOldValueDisplay(item.getOldValueDisplay())
                .setNewValueDisplay(item.getNewValueDisplay())
                .setRecordbookValueDisplay(item.getRecordbookValueDisplay())
                .setBatchRecordValueDisplay(item.getBatchRecordValueDisplay())
                .setChangedAt(item.getChangedAt());
    }
}
