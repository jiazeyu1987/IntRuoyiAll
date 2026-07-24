package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsBusinessHealthCheckResult;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsBusinessHealthCollector;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsInspectionStatus;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionArchiveEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionArchiveEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionArchiveMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class MesProBatchRecordExecutionArchiveBusinessHealthCollector implements RuntimeOpsBusinessHealthCollector {

    private static final String HEALTH_CODE = "edhr-archive-integrity";
    private static final String HEALTH_NAME = "eDHR 归档完整性";
    private static final String STATUS_SEALED = "SEALED";
    private static final String STATUS_FAILED = "FAILED";
    private static final Set<String> CONTROLLED_INVALID_STATUSES = Set.of("VOIDED", "SUPERSEDED");
    private static final String EVENT_ARCHIVE_SEAL = "ARCHIVE_SEAL";
    private static final String EVENT_GENERATE_SUCCESS = "GENERATE_SUCCESS";
    private static final String METADATA_STORAGE_RETENTION = "storageRetention";

    @Resource
    private MesProBatchRecordExecutionArchiveMapper archiveMapper;
    @Resource
    private MesProBatchRecordExecutionArchiveEventMapper archiveEventMapper;

    @Override
    @Transactional(readOnly = true)
    public RuntimeOpsBusinessHealthCheckResult collect() {
        LocalDateTime sampledAt = LocalDateTime.now();
        try {
            requireMapper(archiveMapper, "archiveMapper");
            requireMapper(archiveEventMapper, "archiveEventMapper");

            List<MesProBatchRecordExecutionArchiveDO> allSealedArchives = selectArchivesByStatus(STATUS_SEALED);
            List<MesProBatchRecordExecutionArchiveDO> controlledInvalidArchives = allSealedArchives.stream()
                    .filter(this::isControlledInvalidArchive)
                    .toList();
            List<MesProBatchRecordExecutionArchiveDO> sealedArchives = allSealedArchives.stream()
                    .filter(archive -> !isControlledInvalidArchive(archive))
                    .toList();
            List<MesProBatchRecordExecutionArchiveDO> failedArchives = selectArchivesByStatus(STATUS_FAILED);
            String countEvidence = countEvidence(sealedArchives.size(), failedArchives.size(), controlledInvalidArchives);

            List<String> retentionSignals = new ArrayList<>();
            for (MesProBatchRecordExecutionArchiveDO archive : sealedArchives) {
                ArchiveIntegrityIssue issue = inspectSealedArchive(archive, retentionSignals);
                if (issue != null) {
                    return blocked(countEvidence + ", archiveId=" + archive.getId() + ", " + issue.reason(),
                            sampledAt);
                }
            }

            if (CollUtil.isNotEmpty(failedArchives)) {
                String failedArchiveIds = archiveIds(failedArchives);
                String evidence = countEvidence + retentionEvidence(retentionSignals)
                        + ", failedArchiveIds=" + failedArchiveIds;
                return new RuntimeOpsBusinessHealthCheckResult(HEALTH_CODE, HEALTH_NAME, RuntimeOpsInspectionStatus.WARN,
                        evidence, "存在生成失败归档，sealed=" + sealedArchives.size() + ", failed=" + failedArchives.size()
                        + ", failedArchiveIds=" + failedArchiveIds, sampledAt);
            }

            return pass(countEvidence + retentionEvidence(retentionSignals), sampledAt);
        } catch (ArchiveBusinessHealthCollectionException ex) {
            return blocked("collector query failed, context=" + ex.context() + ", error=" + failureDetail(ex),
                    sampledAt);
        }
    }

    private RuntimeOpsBusinessHealthCheckResult pass(String evidence, LocalDateTime sampledAt) {
        return new RuntimeOpsBusinessHealthCheckResult(HEALTH_CODE, HEALTH_NAME, RuntimeOpsInspectionStatus.PASS,
                evidence, null, sampledAt);
    }

    private RuntimeOpsBusinessHealthCheckResult blocked(String reason, LocalDateTime sampledAt) {
        return new RuntimeOpsBusinessHealthCheckResult(HEALTH_CODE, HEALTH_NAME, RuntimeOpsInspectionStatus.BLOCKED,
                null, reason, sampledAt);
    }

    private List<MesProBatchRecordExecutionArchiveDO> selectArchivesByStatus(String status) {
        String context = "archiveMapper.selectArchivesByStatus(" + status + ")";
        try {
            return archiveMapper.selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionArchiveDO>()
                    .eq(MesProBatchRecordExecutionArchiveDO::getArchiveStatus, status)
                    .orderByDesc(MesProBatchRecordExecutionArchiveDO::getId));
        } catch (RuntimeException ex) {
            throw collectionFailure(context, ex);
        }
    }

    private ArchiveIntegrityIssue inspectSealedArchive(MesProBatchRecordExecutionArchiveDO archive,
                                                       List<String> retentionSignals) {
        List<MesProBatchRecordExecutionArchiveEventDO> events = selectArchiveEvents(archive.getId());
        boolean hasSealEvent = events.stream().anyMatch(event -> EVENT_ARCHIVE_SEAL.equals(event.getEventType()));
        if (!hasSealEvent) {
            return new ArchiveIntegrityIssue(
                    "missing ARCHIVE_SEAL event, missingCount=1, missingSummary=ARCHIVE_SEAL");
        }

        RetentionMetadataLookup lookup = findRetentionMetadataSource(events);
        if (lookup.issue() != null) {
            return lookup.issue();
        }
        RetentionMetadataSource source = lookup.source();
        if (source == null) {
            return new ArchiveIntegrityIssue("missing storageRetention source event");
        }

        RetentionMetadata metadata = source.metadata();
        ArchiveIntegrityIssue metadataIssue = metadata.completeIssue(source);
        if (metadataIssue != null) {
            return metadataIssue;
        }
        String retentionSignal = metadata.retentionSignal(source);
        if (!Objects.equals(archive.getFileId(), metadata.fileId())) {
            return new ArchiveIntegrityIssue("storageRetention fileId mismatch, expected=" + archive.getFileId()
                    + ", actual=" + metadata.fileId() + ", " + retentionSignal);
        }
        if (!Objects.equals(archive.getSha256(), metadata.sha256())) {
            return new ArchiveIntegrityIssue("storageRetention sha256 mismatch, expected=" + archive.getSha256()
                    + ", actual=" + metadata.sha256() + ", " + retentionSignal);
        }
        retentionSignals.add(retentionSignal);
        return null;
    }

    private List<MesProBatchRecordExecutionArchiveEventDO> selectArchiveEvents(Long archiveId) {
        String context = "archiveEventMapper.selectListByArchiveId(archiveId=" + archiveId + ")";
        try {
            return archiveEventMapper.selectListByArchiveId(archiveId);
        } catch (RuntimeException ex) {
            throw collectionFailure(context, ex);
        }
    }

    private void requireMapper(Object mapper, String mapperName) {
        if (mapper == null) {
            throw new ArchiveBusinessHealthCollectionException(mapperName + " missing",
                    new IllegalStateException(mapperName + " missing"));
        }
    }

    private ArchiveBusinessHealthCollectionException collectionFailure(String context, RuntimeException ex) {
        if (ex instanceof ArchiveBusinessHealthCollectionException failure) {
            return failure;
        }
        return new ArchiveBusinessHealthCollectionException(context, ex);
    }

    private String failureDetail(ArchiveBusinessHealthCollectionException ex) {
        Throwable cause = ex.getCause();
        if (cause == null) {
            return ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
        String message = StrUtil.blankToDefault(cause.getMessage(), "no message");
        return cause.getClass().getSimpleName() + ": " + message;
    }

    private RetentionMetadataLookup findRetentionMetadataSource(List<MesProBatchRecordExecutionArchiveEventDO> events) {
        for (MesProBatchRecordExecutionArchiveEventDO event : events) {
            if (StrUtil.isBlank(event.getMetadataJson())) {
                continue;
            }
            JSONObject metadataJson;
            try {
                metadataJson = JSON.parseObject(event.getMetadataJson());
            } catch (RuntimeException ex) {
                return RetentionMetadataLookup.issue(invalidMetadataIssue(event, "metadataJson is not valid JSON",
                        RetentionMetadata.empty()));
            }
            if (metadataJson == null || !metadataJson.containsKey(METADATA_STORAGE_RETENTION)) {
                continue;
            }
            JSONObject value;
            try {
                value = metadataJson.getJSONObject(METADATA_STORAGE_RETENTION);
            } catch (RuntimeException ex) {
                return RetentionMetadataLookup.issue(invalidMetadataIssue(event,
                        "storageRetention is not a JSON object", RetentionMetadata.empty()));
            }
            RetentionMetadata metadata = toRetentionMetadata(value);
            if (metadata.fileIdInvalid()) {
                return RetentionMetadataLookup.issue(invalidMetadataIssue(event,
                        "storageRetention fileId invalid", metadata));
            }
            return RetentionMetadataLookup.source(new RetentionMetadataSource(event.getEventType(), event.getId(),
                    metadata));
        }
        return RetentionMetadataLookup.empty();
    }

    private RetentionMetadata toRetentionMetadata(JSONObject storageRetention) {
        if (storageRetention == null) {
            return RetentionMetadata.empty();
        }
        return new RetentionMetadata(
                fileIdValue(storageRetention.get("fileId")),
                fileIdInvalid(storageRetention.get("fileId")),
                storageRetention.getString("sha256"),
                storageRetention.getString("objectVersionId"),
                storageRetention.getString("retentionMode"),
                storageRetention.getString("retainUntil"),
                storageRetention.getString("legalHoldStatus"),
                storageRetention.getString("verifiedAt"));
    }

    private Long fileIdValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StrUtil.isNotBlank(text)) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean fileIdInvalid(Object value) {
        return value != null && fileIdValue(value) == null;
    }

    private ArchiveIntegrityIssue invalidMetadataIssue(MesProBatchRecordExecutionArchiveEventDO event, String detail,
                                                       RetentionMetadata metadata) {
        return new ArchiveIntegrityIssue("invalid metadata, sourceEvent=" + event.getEventType()
                + ", sourceEventId=" + event.getId() + ", detail=" + detail + ", "
                + metadata.retentionSignal(new RetentionMetadataSource(event.getEventType(), event.getId(), metadata)));
    }

    private boolean isControlledInvalidArchive(MesProBatchRecordExecutionArchiveDO archive) {
        return Boolean.FALSE.equals(archive.getArchiveValidFlag())
                && CONTROLLED_INVALID_STATUSES.contains(archive.getArchiveValidStatus())
                && archive.getInvalidatedByChangeEventId() != null;
    }

    private String countEvidence(int sealedCount, int failedCount,
                                 List<MesProBatchRecordExecutionArchiveDO> controlledInvalidArchives) {
        String evidence = "sealed=" + sealedCount + ", failed=" + failedCount
                + ", controlledInvalid=" + controlledInvalidArchives.size();
        if (controlledInvalidArchives.isEmpty()) {
            return evidence;
        }
        String statusCounts = controlledInvalidArchives.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        MesProBatchRecordExecutionArchiveDO::getArchiveValidStatus,
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(java.util.stream.Collectors.joining("|"));
        String changeEventIds = controlledInvalidArchives.stream()
                .map(MesProBatchRecordExecutionArchiveDO::getInvalidatedByChangeEventId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining("|"));
        return evidence + ", controlledInvalidStatusCounts=" + statusCounts
                + ", changeEventIds=" + changeEventIds;
    }

    private String retentionEvidence(List<String> retentionSignals) {
        if (retentionSignals.isEmpty()) {
            return "";
        }
        Set<String> uniqueSignals = new LinkedHashSet<>(retentionSignals);
        return ", storageRetentionEvidence=[" + String.join("; ", uniqueSignals) + "]"
                + ", currentKnownSource=" + EVENT_GENERATE_SUCCESS;
    }

    private String archiveIds(List<MesProBatchRecordExecutionArchiveDO> archives) {
        return String.join("|", archives.stream()
                .map(MesProBatchRecordExecutionArchiveDO::getId)
                .map(String::valueOf)
                .toList());
    }

    private record ArchiveIntegrityIssue(String reason) {
    }

    private static final class ArchiveBusinessHealthCollectionException extends RuntimeException {

        private final String context;

        private ArchiveBusinessHealthCollectionException(String context, RuntimeException cause) {
            super(context, cause);
            this.context = context;
        }

        private String context() {
            return context;
        }
    }

    private record RetentionMetadataLookup(RetentionMetadataSource source, ArchiveIntegrityIssue issue) {

        static RetentionMetadataLookup source(RetentionMetadataSource source) {
            return new RetentionMetadataLookup(source, null);
        }

        static RetentionMetadataLookup issue(ArchiveIntegrityIssue issue) {
            return new RetentionMetadataLookup(null, issue);
        }

        static RetentionMetadataLookup empty() {
            return new RetentionMetadataLookup(null, null);
        }
    }

    private record RetentionMetadataSource(String eventType, Long eventId, RetentionMetadata metadata) {
    }

    private record RetentionMetadata(Long fileId, boolean fileIdInvalid, String sha256, String objectVersionId,
                                     String retentionMode, String retainUntil, String legalHoldStatus,
                                     String verifiedAt) {

        static RetentionMetadata empty() {
            return new RetentionMetadata(null, false, null, null, null, null, null, null);
        }

        ArchiveIntegrityIssue completeIssue(RetentionMetadataSource source) {
            if (fileId == null) {
                return invalid("fileId", source);
            }
            if (StrUtil.isBlank(sha256)) {
                return invalid("sha256", source);
            }
            if (StrUtil.isBlank(objectVersionId)) {
                return invalid("objectVersionId", source);
            }
            if (StrUtil.isBlank(retentionMode)) {
                return invalid("retentionMode", source);
            }
            if (!validInstant(retainUntil)) {
                return invalid("retainUntil", source);
            }
            if (StrUtil.isBlank(legalHoldStatus)) {
                return invalid("legalHoldStatus", source);
            }
            if (!validInstant(verifiedAt)) {
                return invalid("verifiedAt", source);
            }
            return null;
        }

        private ArchiveIntegrityIssue invalid(String fieldName, RetentionMetadataSource source) {
            return new ArchiveIntegrityIssue("invalid metadata, storageRetention " + fieldName
                    + " missing or invalid, " + retentionSignal(source));
        }

        private String retentionSignal(RetentionMetadataSource source) {
            return "sourceEvent=" + source.eventType() + ", sourceEventId=" + source.eventId()
                    + ", fileId=" + fileId + ", sha256=" + sha256 + ", objectVersionId=" + objectVersionId
                    + ", retainUntil=" + retainUntil + ", verifiedAt=" + verifiedAt;
        }

        private boolean validInstant(String value) {
            if (StrUtil.isBlank(value)) {
                return false;
            }
            try {
                Instant.parse(value);
                return true;
            } catch (RuntimeException ex) {
                return false;
            }
        }
    }
}
