package cn.iocoder.yudao.module.dcc.service.audit;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DccControlledFileAccessAuditService {

    @Resource
    private DccControlledFileAccessEventMapper accessEventMapper;
    @Resource
    private DccControlledFileWatermarkTraceMapper watermarkTraceMapper;
    @Resource
    private DccControlledFileAccessLogMapper accessLogMapper;

    @Transactional(rollbackFor = Exception.class)
    public DccControlledFileAccessEventDO createAccessEvent(DccAccessEventCreateCommand command) {
        requireAccessEvent(command);
        DccControlledFileAccessEventDO accessEvent = DccControlledFileAccessEventDO.builder()
                .accessEventCode(StrUtil.trim(command.accessEventCode()))
                .controlledFileId(command.fileId())
                .fileVersionNo(StrUtil.trim(command.versionId()))
                .userId(command.userId())
                .accessType(StrUtil.trim(command.accessType()))
                .purpose(StrUtil.trim(command.purpose()))
                .result(StrUtil.trim(command.result()))
                .failureCode(StrUtil.trim(command.failureCode()))
                .failureReason(StrUtil.trim(command.failureReason()))
                .sourceIp(StrUtil.trim(command.sourceIp()))
                .userAgent(StrUtil.trim(command.userAgent()))
                .requestId(StrUtil.trim(command.requestId()))
                .occurredAt(command.occurredAt())
                .build();
        accessEventMapper.insert(accessEvent);
        return accessEvent;
    }

    @Transactional(rollbackFor = Exception.class)
    public DccControlledFileWatermarkTraceDO recordWatermarkTrace(DccWatermarkTraceCreateCommand command) {
        requireWatermarkTrace(command);
        DccControlledFileWatermarkTraceDO watermarkTrace = DccControlledFileWatermarkTraceDO.builder()
                .traceCode(StrUtil.trim(command.traceCode()))
                .accessEventId(command.accessEventId())
                .accessEventCode(StrUtil.trim(command.accessEventCode()))
                .controlledFileId(command.fileId())
                .fileNumber(StrUtil.trim(command.fileNumber()))
                .fileVersionNo(StrUtil.trim(command.versionId()))
                .userId(command.userId())
                .userIdentifier(StrUtil.trim(command.userIdentifier()))
                .userDisplayName(StrUtil.trim(command.userDisplayName()))
                .deptId(command.deptId())
                .deptName(StrUtil.trim(command.deptName()))
                .tenantName(StrUtil.trim(command.tenantName()))
                .privacyMode(StrUtil.trim(command.privacyMode()))
                .watermarkPayloadJson(StrUtil.trim(command.watermarkPayloadJson()))
                .issuedAt(command.issuedAt())
                .expiresAt(command.expiresAt())
                .build();
        watermarkTraceMapper.insert(watermarkTrace);
        return watermarkTrace;
    }

    @Transactional(rollbackFor = Exception.class)
    public DccControlledFileAccessLogDO recordAccessLog(DccAccessLogCreateCommand command) {
        requireAccessLog(command);
        DccControlledFileAccessLogDO accessLog = DccControlledFileAccessLogDO.builder()
                .controlledFileId(command.fileId())
                .accessEventId(command.accessEventId())
                .accessEventCode(StrUtil.trim(command.accessEventCode()))
                .watermarkTraceCode(StrUtil.trim(command.watermarkTraceCode()))
                .fileVersionNo(StrUtil.trim(command.versionId()))
                .userId(command.userId())
                .actionType(StrUtil.trim(command.actionType()))
                .purpose(StrUtil.trim(command.purpose()))
                .result(StrUtil.trim(command.result()))
                .failureCode(StrUtil.trim(command.failureCode()))
                .reason(StrUtil.trim(command.reason()))
                .sourceIp(StrUtil.trim(command.sourceIp()))
                .requestId(StrUtil.trim(command.requestId()))
                .userAgent(StrUtil.trim(command.userAgent()))
                .build();
        accessLogMapper.insert(accessLog);
        return accessLog;
    }

    @Transactional(rollbackFor = Exception.class)
    public DccControlledFileAccessLogDO recordBoundaryLog(DccAccessBoundaryLogCreateCommand command) {
        requireBoundaryLog(command);
        DccControlledFileAccessLogDO accessLog = DccControlledFileAccessLogDO.builder()
                .tenantId(TenantContextHolder.getRequiredTenantId())
                .userId(command.userId())
                .actionType(StrUtil.trim(command.actionType()))
                .purpose(StrUtil.trim(command.purpose()))
                .result(StrUtil.trim(command.result()))
                .failureCode(StrUtil.trimToNull(command.failureCode()))
                .reason(StrUtil.trimToNull(command.reason()))
                .sourceIp(StrUtil.trim(command.sourceIp()))
                .requestId(StrUtil.trim(command.requestId()))
                .userAgent(StrUtil.trim(command.userAgent()))
                .build();
        accessLogMapper.insert(accessLog);
        return accessLog;
    }

    @Transactional(rollbackFor = Exception.class)
    public DccControlledFileAccessLogDO recordDirectLinkDeniedLog(DccDirectLinkDeniedLogCreateCommand command) {
        requireDirectLinkDeniedLog(command);
        return TenantUtils.execute(command.tenantId(), () -> {
            DccControlledFileAccessLogDO accessLog = DccControlledFileAccessLogDO.builder()
                    .tenantId(command.tenantId())
                    .controlledFileId(command.controlledFileId())
                    .userId(0L)
                    .actionType(StrUtil.trim(command.actionType()))
                    .purpose(StrUtil.trim(command.purpose()))
                    .result(StrUtil.trim(command.result()))
                    .failureCode(StrUtil.trim(command.failureCode()))
                    .reason(StrUtil.trim(command.reason()))
                    .sourceIp(StrUtil.trim(command.sourceIp()))
                    .requestId(StrUtil.trim(command.requestId()))
                    .userAgent(StrUtil.trim(command.userAgent()))
                    .build();
            accessLogMapper.insert(accessLog);
            return accessLog;
        });
    }

    private void requireAccessEvent(DccAccessEventCreateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("access event command is required");
        }
        requireNotBlank(command.accessEventCode(), "accessEventCode");
        requirePositive(command.fileId(), "fileId");
        requireNotBlank(command.versionId(), "versionId");
        requirePositive(command.userId(), "userId");
        requireNotBlank(command.accessType(), "accessType");
        requireNotBlank(command.purpose(), "purpose");
        requireNotBlank(command.result(), "result");
        if (command.occurredAt() == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
    }

    private void requireWatermarkTrace(DccWatermarkTraceCreateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("watermark trace command is required");
        }
        requireNotBlank(command.traceCode(), "traceCode");
        requirePositive(command.accessEventId(), "accessEventId");
        requireNotBlank(command.accessEventCode(), "accessEventCode");
        requirePositive(command.fileId(), "fileId");
        requireNotBlank(command.fileNumber(), "fileNumber");
        requireNotBlank(command.versionId(), "versionId");
        requirePositive(command.userId(), "userId");
        requireNotBlank(command.privacyMode(), "privacyMode");
        requireNotBlank(command.watermarkPayloadJson(), "watermarkPayloadJson");
        if (command.issuedAt() == null) {
            throw new IllegalArgumentException("issuedAt is required");
        }
    }

    private void requireAccessLog(DccAccessLogCreateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("access log command is required");
        }
        requirePositive(command.fileId(), "fileId");
        requirePositive(command.accessEventId(), "accessEventId");
        requireNotBlank(command.accessEventCode(), "accessEventCode");
        requireNotBlank(command.watermarkTraceCode(), "watermarkTraceCode");
        requireNotBlank(command.versionId(), "versionId");
        requirePositive(command.userId(), "userId");
        requireNotBlank(command.actionType(), "actionType");
        requireNotBlank(command.purpose(), "purpose");
        requireNotBlank(command.result(), "result");
    }

    private void requireBoundaryLog(DccAccessBoundaryLogCreateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("boundary log command is required");
        }
        requirePositive(command.userId(), "userId");
        requireNotBlank(command.actionType(), "actionType");
        requireNotBlank(command.purpose(), "purpose");
        requireNotBlank(command.result(), "result");
        requireNotBlank(command.sourceIp(), "sourceIp");
        requireNotBlank(command.requestId(), "requestId");
        requireNotBlank(command.userAgent(), "userAgent");
    }

    private void requireDirectLinkDeniedLog(DccDirectLinkDeniedLogCreateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("direct link denied log command is required");
        }
        requirePositive(command.tenantId(), "tenantId");
        requirePositive(command.controlledFileId(), "controlledFileId");
        requirePositive(command.infraFileId(), "infraFileId");
        requireNotBlank(command.artifactRole(), "artifactRole");
        requireNotBlank(command.actionType(), "actionType");
        requireNotBlank(command.purpose(), "purpose");
        requireNotBlank(command.result(), "result");
        requireNotBlank(command.failureCode(), "failureCode");
        requireNotBlank(command.reason(), "reason");
        requireNotBlank(command.sourceIp(), "sourceIp");
        requireNotBlank(command.requestId(), "requestId");
        requireNotBlank(command.userAgent(), "userAgent");
    }

    private void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private void requireNotBlank(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

}
