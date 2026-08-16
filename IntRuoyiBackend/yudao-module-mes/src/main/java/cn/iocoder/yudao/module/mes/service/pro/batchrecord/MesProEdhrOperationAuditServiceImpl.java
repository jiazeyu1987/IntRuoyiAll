package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOperationAuditPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOperationAuditRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditErrorCodeConstants.PRO_EDHR_OPERATION_AUDIT_CONTEXT_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditErrorCodeConstants.PRO_EDHR_OPERATION_AUDIT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditErrorCodeConstants.PRO_EDHR_OPERATION_AUDIT_WRITE_FAILED;

@Service
@Validated
public class MesProEdhrOperationAuditServiceImpl implements MesProEdhrOperationAuditService {

    private static final String HASH_VERSION = "EDHR_OPERATION_AUDIT_V1";
    private static final String GENESIS_AUDIT_HASH =
            MesProBatchRecordExecutionFieldAuditHasher.sha256(HASH_VERSION + ":GENESIS");

    @Resource
    private MesProEdhrOperationAuditEventMapper auditEventMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public MesProEdhrOperationAuditRespVO record(MesProEdhrOperationAuditCommand command) {
        return doRecord(command);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrOperationAuditRespVO recordInCallerTransaction(MesProEdhrOperationAuditCommand command) {
        return doRecord(command);
    }

    private MesProEdhrOperationAuditRespVO doRecord(MesProEdhrOperationAuditCommand command) {
        validateCommand(command);
        LocalDateTime occurredAt = command.getOccurredAt() == null
                ? LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                : command.getOccurredAt().truncatedTo(ChronoUnit.SECONDS);
        List<MesProEdhrOperationAuditEventDO> previousEvents =
                auditEventMapper.selectListByObject(command.getObjectType(), command.getObjectId());
        String previousHash = previousEvents.isEmpty() ? GENESIS_AUDIT_HASH : previousEvents.get(0).getAuditHash();
        MesProEdhrOperationAuditEventDO event = toDO(command, occurredAt, previousHash);
        event.setAuditHash(hash(event));
        try {
            auditEventMapper.insert(event);
        } catch (RuntimeException ex) {
            throw exception(PRO_EDHR_OPERATION_AUDIT_WRITE_FAILED, ex.getMessage());
        }
        return toResp(event);
    }

    @Override
    public PageResult<MesProEdhrOperationAuditRespVO> getPage(MesProEdhrOperationAuditPageReqVO reqVO) {
        PageResult<MesProEdhrOperationAuditEventDO> page =
                auditEventMapper.selectPage(reqVO == null ? new MesProEdhrOperationAuditPageReqVO() : reqVO);
        return new PageResult<>(page.getList().stream().map(this::toResp).toList(), page.getTotal());
    }

    @Override
    public MesProEdhrOperationAuditRespVO get(Long id) {
        MesProEdhrOperationAuditEventDO event = id == null ? null : auditEventMapper.selectById(id);
        if (event == null) {
            throw exception(PRO_EDHR_OPERATION_AUDIT_NOT_EXISTS);
        }
        return toResp(event);
    }

    private void validateCommand(MesProEdhrOperationAuditCommand command) {
        if (command == null || StrUtil.isBlank(command.getRequestId())
                || StrUtil.isBlank(command.getObjectType()) || StrUtil.isBlank(command.getObjectId())
                || StrUtil.isBlank(command.getOperationType()) || command.getActorUserId() == null
                || StrUtil.isBlank(command.getPermissionDecision()) || StrUtil.isBlank(command.getResultStatus())) {
            throw exception(PRO_EDHR_OPERATION_AUDIT_CONTEXT_MISSING);
        }
    }

    private MesProEdhrOperationAuditEventDO toDO(MesProEdhrOperationAuditCommand command, LocalDateTime occurredAt,
                                                String previousHash) {
        return new MesProEdhrOperationAuditEventDO()
                .setRequestId(command.getRequestId())
                .setObjectType(command.getObjectType())
                .setObjectId(command.getObjectId())
                .setBatchExecutionId(command.getBatchExecutionId())
                .setExecutionId(command.getExecutionId())
                .setWorkTaskId(command.getWorkTaskId())
                .setRouteId(command.getRouteId())
                .setRouteProcessId(command.getRouteProcessId())
                .setReportId(command.getReportId())
                .setRecordCategory(command.getRecordCategory())
                .setOperationType(command.getOperationType())
                .setActionName(command.getActionName())
                .setActorUserId(command.getActorUserId())
                .setActorUsername(command.getActorUsername())
                .setPermissionCode(command.getPermissionCode())
                .setPermissionDecision(command.getPermissionDecision())
                .setMatchedRuleIds(command.getMatchedRuleIds())
                .setResultStatus(command.getResultStatus())
                .setFailureCode(command.getFailureCode())
                .setFailureMessage(command.getFailureMessage())
                .setBeforeSummaryHash(command.getBeforeSummaryHash())
                .setAfterSummaryHash(command.getAfterSummaryHash())
                .setMetadataJson(command.getMetadataJson())
                .setOccurredAt(occurredAt)
                .setPreviousAuditHash(previousHash);
    }

    private String hash(MesProEdhrOperationAuditEventDO event) {
        return MesProBatchRecordExecutionFieldAuditHasher.sha256(String.join("|",
                HASH_VERSION,
                value(event.getPreviousAuditHash()),
                value(event.getRequestId()),
                value(event.getObjectType()),
                value(event.getObjectId()),
                value(event.getBatchExecutionId()),
                value(event.getExecutionId()),
                value(event.getWorkTaskId()),
                value(event.getRouteId()),
                value(event.getRouteProcessId()),
                value(event.getReportId()),
                value(event.getRecordCategory()),
                value(event.getOperationType()),
                value(event.getActionName()),
                value(event.getActorUserId()),
                value(event.getActorUsername()),
                value(event.getPermissionCode()),
                value(event.getPermissionDecision()),
                value(event.getMatchedRuleIds()),
                value(event.getResultStatus()),
                value(event.getFailureCode()),
                value(event.getFailureMessage()),
                value(event.getBeforeSummaryHash()),
                value(event.getAfterSummaryHash()),
                value(event.getMetadataJson()),
                value(event.getOccurredAt())));
    }

    private MesProEdhrOperationAuditRespVO toResp(MesProEdhrOperationAuditEventDO event) {
        return new MesProEdhrOperationAuditRespVO()
                .setId(event.getId())
                .setRequestId(event.getRequestId())
                .setObjectType(event.getObjectType())
                .setObjectId(event.getObjectId())
                .setBatchExecutionId(event.getBatchExecutionId())
                .setExecutionId(event.getExecutionId())
                .setWorkTaskId(event.getWorkTaskId())
                .setRouteId(event.getRouteId())
                .setRouteProcessId(event.getRouteProcessId())
                .setReportId(event.getReportId())
                .setRecordCategory(event.getRecordCategory())
                .setOperationType(event.getOperationType())
                .setActionName(event.getActionName())
                .setActorUserId(event.getActorUserId())
                .setActorUsername(event.getActorUsername())
                .setPermissionCode(event.getPermissionCode())
                .setPermissionDecision(event.getPermissionDecision())
                .setMatchedRuleIds(event.getMatchedRuleIds())
                .setResultStatus(event.getResultStatus())
                .setFailureCode(event.getFailureCode())
                .setFailureMessage(event.getFailureMessage())
                .setBeforeSummaryHash(event.getBeforeSummaryHash())
                .setAfterSummaryHash(event.getAfterSummaryHash())
                .setMetadataJson(event.getMetadataJson())
                .setOccurredAt(event.getOccurredAt())
                .setPreviousAuditHash(event.getPreviousAuditHash())
                .setAuditHash(event.getAuditHash());
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
