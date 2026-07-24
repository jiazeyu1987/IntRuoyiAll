package cn.iocoder.yudao.module.dcc.service.audit;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DccControlledFileAuditQueryServiceImpl implements DccControlledFileAuditQueryService {

    @Resource
    private DccControlledFileAccessLogMapper accessLogMapper;
    @Resource
    private DccControlledFileAccessEventMapper accessEventMapper;
    @Resource
    private DccControlledFileWatermarkTraceMapper watermarkTraceMapper;

    @Override
    public PageResult<DccControlledFileAuditRecord> getAuditPage(DccControlledFileAuditQuery query) {
        requireQuery(query);

        LambdaQueryWrapperX<DccControlledFileAccessLogDO> wrapper =
                new LambdaQueryWrapperX<DccControlledFileAccessLogDO>()
                        .eqIfPresent(DccControlledFileAccessLogDO::getAccessEventCode,
                                StrUtil.trimToNull(query.getAccessEventCode()))
                        .eqIfPresent(DccControlledFileAccessLogDO::getWatermarkTraceCode,
                                StrUtil.trimToNull(query.getWatermarkTraceCode()))
                        .eqIfPresent(DccControlledFileAccessLogDO::getControlledFileId, query.getControlledFileId())
                        .eqIfPresent(DccControlledFileAccessLogDO::getUserId, query.getUserId())
                        .eqIfPresent(DccControlledFileAccessLogDO::getActionType,
                                StrUtil.trimToNull(query.getActionType()))
                        .eqIfPresent(DccControlledFileAccessLogDO::getResult, StrUtil.trimToNull(query.getResult()))
                        .eqIfPresent(DccControlledFileAccessLogDO::getFailureCode,
                                StrUtil.trimToNull(query.getFailureCode()))
                        .eqIfPresent(DccControlledFileAccessLogDO::getRequestId,
                                StrUtil.trimToNull(query.getRequestId()));
        if (query.getOccurredAt() != null) {
            applyOccurredAtFilter(wrapper, query.getOccurredAt());
        }
        wrapper.orderByDesc(DccControlledFileAccessLogDO::getCreateTime)
                .orderByDesc(DccControlledFileAccessLogDO::getId);

        PageResult<DccControlledFileAccessLogDO> pageResult = accessLogMapper.selectPage(query, wrapper);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        return new PageResult<>(toRecords(pageResult.getList()), pageResult.getTotal());
    }

    private List<DccControlledFileAuditRecord> toRecords(List<DccControlledFileAccessLogDO> accessLogs) {
        Map<Long, DccControlledFileAccessEventDO> eventMap = selectEventMap(accessLogs);
        Map<String, DccControlledFileWatermarkTraceDO> traceMap = selectTraceMap(accessLogs);
        return accessLogs.stream()
                .map(accessLog -> toRecord(accessLog, selectEvent(eventMap, accessLog),
                        selectTrace(traceMap, accessLog)))
                .toList();
    }

    private DccControlledFileAccessEventDO selectEvent(Map<Long, DccControlledFileAccessEventDO> eventMap,
                                                       DccControlledFileAccessLogDO accessLog) {
        return accessLog.getAccessEventId() == null ? null : eventMap.get(accessLog.getAccessEventId());
    }

    private DccControlledFileWatermarkTraceDO selectTrace(Map<String, DccControlledFileWatermarkTraceDO> traceMap,
                                                          DccControlledFileAccessLogDO accessLog) {
        return StrUtil.isBlank(accessLog.getWatermarkTraceCode())
                ? null : traceMap.get(accessLog.getWatermarkTraceCode());
    }

    private DccControlledFileAuditRecord toRecord(DccControlledFileAccessLogDO accessLog,
                                                  DccControlledFileAccessEventDO accessEvent,
                                                  DccControlledFileWatermarkTraceDO watermarkTrace) {
        DccControlledFileAuditRecord record = new DccControlledFileAuditRecord();
        record.setId(accessLog.getId());
        record.setAccessEventId(accessLog.getAccessEventId());
        record.setAccessEventCode(firstNotBlank(accessLog.getAccessEventCode(),
                accessEvent != null ? accessEvent.getAccessEventCode() : null));
        record.setWatermarkTraceCode(firstNotBlank(accessLog.getWatermarkTraceCode(),
                watermarkTrace != null ? watermarkTrace.getTraceCode() : null));
        record.setControlledFileId(firstNonNull(accessLog.getControlledFileId(),
                accessEvent != null ? accessEvent.getControlledFileId() : null,
                watermarkTrace != null ? watermarkTrace.getControlledFileId() : null));
        record.setFileNumber(watermarkTrace != null ? watermarkTrace.getFileNumber() : null);
        record.setFileVersionNo(firstNotBlank(accessLog.getFileVersionNo(),
                accessEvent != null ? accessEvent.getFileVersionNo() : null,
                watermarkTrace != null ? watermarkTrace.getFileVersionNo() : null));
        record.setUserId(firstNonNull(accessLog.getUserId(),
                accessEvent != null ? accessEvent.getUserId() : null,
                watermarkTrace != null ? watermarkTrace.getUserId() : null));
        record.setUserIdentifier(watermarkTrace != null ? watermarkTrace.getUserIdentifier() : null);
        record.setUserDisplayName(watermarkTrace != null ? watermarkTrace.getUserDisplayName() : null);
        record.setDeptId(watermarkTrace != null ? watermarkTrace.getDeptId() : null);
        record.setDeptName(watermarkTrace != null ? watermarkTrace.getDeptName() : null);
        record.setTenantName(watermarkTrace != null ? watermarkTrace.getTenantName() : null);
        record.setActionType(accessLog.getActionType());
        record.setPurpose(firstNotBlank(accessLog.getPurpose(),
                accessEvent != null ? accessEvent.getPurpose() : null));
        record.setResult(firstNotBlank(accessLog.getResult(),
                accessEvent != null ? accessEvent.getResult() : null));
        record.setFailureCode(firstNotBlank(accessLog.getFailureCode(),
                accessEvent != null ? accessEvent.getFailureCode() : null));
        record.setReason(firstNotBlank(accessLog.getReason(),
                accessEvent != null ? accessEvent.getFailureReason() : null));
        record.setSourceIp(firstNotBlank(accessLog.getSourceIp(),
                accessEvent != null ? accessEvent.getSourceIp() : null));
        record.setRequestId(firstNotBlank(accessLog.getRequestId(),
                accessEvent != null ? accessEvent.getRequestId() : null));
        record.setUserAgent(firstNotBlank(accessLog.getUserAgent(),
                accessEvent != null ? accessEvent.getUserAgent() : null));
        record.setPrivacyMode(watermarkTrace != null ? watermarkTrace.getPrivacyMode() : null);
        record.setWatermarkPayloadJson(watermarkTrace != null ? watermarkTrace.getWatermarkPayloadJson() : null);
        record.setOccurredAt(accessEvent != null ? accessEvent.getOccurredAt() : accessLog.getCreateTime());
        record.setIssuedAt(watermarkTrace != null ? watermarkTrace.getIssuedAt() : null);
        record.setExpiresAt(watermarkTrace != null ? watermarkTrace.getExpiresAt() : null);
        record.setCreateTime(accessLog.getCreateTime());
        return record;
    }

    private Map<Long, DccControlledFileAccessEventDO> selectEventMap(List<DccControlledFileAccessLogDO> accessLogs) {
        Set<Long> eventIds = accessLogs.stream()
                .map(DccControlledFileAccessLogDO::getAccessEventId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(eventIds)) {
            return Map.of();
        }
        List<DccControlledFileAccessEventDO> events = accessEventMapper.selectList(
                new LambdaQueryWrapperX<DccControlledFileAccessEventDO>()
                        .in(DccControlledFileAccessEventDO::getId, eventIds));
        return events.stream().collect(Collectors.toMap(DccControlledFileAccessEventDO::getId, event -> event,
                (left, right) -> left, LinkedHashMap::new));
    }

    private Map<String, DccControlledFileWatermarkTraceDO> selectTraceMap(List<DccControlledFileAccessLogDO> accessLogs) {
        Set<String> traceCodes = accessLogs.stream()
                .map(DccControlledFileAccessLogDO::getWatermarkTraceCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(traceCodes)) {
            return Map.of();
        }
        List<DccControlledFileWatermarkTraceDO> traces = watermarkTraceMapper.selectList(
                new LambdaQueryWrapperX<DccControlledFileWatermarkTraceDO>()
                        .in(DccControlledFileWatermarkTraceDO::getTraceCode, traceCodes));
        return traces.stream().collect(Collectors.toMap(DccControlledFileWatermarkTraceDO::getTraceCode,
                trace -> trace, (left, right) -> left, LinkedHashMap::new));
    }

    private void applyOccurredAtFilter(LambdaQueryWrapperX<DccControlledFileAccessLogDO> wrapper,
                                       LocalDateTime[] occurredAt) {
        Set<Long> accessEventIds = selectAccessEventIdsInRange(occurredAt);
        wrapper.and(nested -> {
            if (CollUtil.isNotEmpty(accessEventIds)) {
                nested.isNotNull(DccControlledFileAccessLogDO::getAccessEventId)
                        .in(DccControlledFileAccessLogDO::getAccessEventId, accessEventIds)
                        .or();
            }
            nested.isNull(DccControlledFileAccessLogDO::getAccessEventId);
            applyCreateTimeRange(nested, occurredAt);
        });
    }

    private void applyCreateTimeRange(LambdaQueryWrapper<DccControlledFileAccessLogDO> wrapper,
                                      LocalDateTime[] occurredAt) {
        if (occurredAt[0] != null && occurredAt[1] != null) {
            wrapper.between(DccControlledFileAccessLogDO::getCreateTime, occurredAt[0], occurredAt[1]);
            return;
        }
        if (occurredAt[0] != null) {
            wrapper.ge(DccControlledFileAccessLogDO::getCreateTime, occurredAt[0]);
            return;
        }
        wrapper.le(DccControlledFileAccessLogDO::getCreateTime, occurredAt[1]);
    }

    private Set<Long> selectAccessEventIdsInRange(LocalDateTime[] occurredAt) {
        requireOccurredAtRange(occurredAt);
        List<DccControlledFileAccessEventDO> events = accessEventMapper.selectList(
                new LambdaQueryWrapperX<DccControlledFileAccessEventDO>()
                        .betweenIfPresent(DccControlledFileAccessEventDO::getOccurredAt, occurredAt));
        return events.stream()
                .map(DccControlledFileAccessEventDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void requireQuery(DccControlledFileAuditQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("audit query is required");
        }
        if (query.getOccurredAt() != null) {
            requireOccurredAtRange(query.getOccurredAt());
        }
    }

    private void requireOccurredAtRange(LocalDateTime[] occurredAt) {
        if (occurredAt.length != 2) {
            throw new IllegalArgumentException("occurredAt requires start and end");
        }
        if (occurredAt[0] == null && occurredAt[1] == null) {
            throw new IllegalArgumentException("occurredAt requires at least one boundary");
        }
        if (occurredAt[0] != null && occurredAt[1] != null && occurredAt[0].isAfter(occurredAt[1])) {
            throw new IllegalArgumentException("occurredAt start must be before or equal to end");
        }
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

}
