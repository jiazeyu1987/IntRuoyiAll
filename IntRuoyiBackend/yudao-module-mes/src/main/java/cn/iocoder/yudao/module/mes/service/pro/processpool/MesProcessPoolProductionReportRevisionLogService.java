package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProProductionReportRevisionLogPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDiffDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionDiffMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolTimelineReadMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.ProcessPoolTimelineEventReadDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderScopeService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS;

@Service
public class MesProcessPoolProductionReportRevisionLogService {

    private static final String LEGACY_LOSS_DETAILS_FIELD = "LOSS_DETAILS";

    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProProcessPoolEventRevisionMapper revisionMapper;
    private final MesProProcessPoolEventRevisionDiffMapper revisionDiffMapper;
    private final MesProProcessPoolTimelineReadMapper timelineReadMapper;
    private final MesTeamLeaderScopeService scopeService;

    public MesProcessPoolProductionReportRevisionLogService(
            MesProProcessPoolEventMapper eventMapper,
            MesProProcessPoolEventRevisionMapper revisionMapper,
            MesProProcessPoolEventRevisionDiffMapper revisionDiffMapper,
            MesProProcessPoolTimelineReadMapper timelineReadMapper,
            MesTeamLeaderScopeService scopeService) {
        this.eventMapper = eventMapper;
        this.revisionMapper = revisionMapper;
        this.revisionDiffMapper = revisionDiffMapper;
        this.timelineReadMapper = timelineReadMapper;
        this.scopeService = scopeService;
    }

    @Transactional(readOnly = true)
    public List<MesProcessPoolProductionReportRevisionLogBO> getLogs(Long eventId, Long actorUserId) {
        requirePositive(eventId, "eventId");
        requirePositive(actorUserId, "actorUserId");
        MesProProcessPoolEventDO event = loadProductionEvent(eventId);
        scopeService.assertCanAccessEmployee(actorUserId, "PRODUCTION", event.getActualEmployeeId());

        List<MesProProcessPoolEventRevisionDO> revisions = revisionMapper.selectListByEventId(eventId);
        if (revisions.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<MesProProcessPoolEventRevisionDiffDO>> diffsByRevision =
                loadDiffsByRevision(revisions);

        List<MesProcessPoolProductionReportRevisionLogBO> result = new ArrayList<>(revisions.size());
        for (MesProProcessPoolEventRevisionDO revision : revisions) {
            result.add(toLog(revision, diffsByRevision.get(revision.getId())));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public PageResult<MesProcessPoolProductionReportRevisionLogBO> getProductionReportRevisionPage(
            MesProProductionReportRevisionLogPageReqVO reqVO, Long actorUserId) {
        if (reqVO == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "revisionLog.pageQuery");
        }
        requirePositive(actorUserId, "actorUserId");
        Set<Long> responsibleEmployeeIds = scopeService.listResponsibleEmployeeIds(actorUserId, "PRODUCTION");
        if (responsibleEmployeeIds == null || responsibleEmployeeIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        List<Long> employeeUserIds = responsibleEmployeeIds.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.naturalOrder())
                .toList();
        if (employeeUserIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }

        long total = revisionMapper.selectProductionReportRevisionLogCount(reqVO, employeeUserIds);
        if (total <= 0) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        int pageNo = reqVO.getPageNo() == null ? 1 : reqVO.getPageNo();
        int pageSize = reqVO.getPageSize() == null ? 10 : reqVO.getPageSize();
        int offset = (pageNo - 1) * pageSize;
        List<MesProProcessPoolEventRevisionDO> revisions = revisionMapper
                .selectProductionReportRevisionLogPage(reqVO, employeeUserIds, offset, pageSize);
        return new PageResult<>(buildContextLogs(revisions, actorUserId), total);
    }

    @Transactional(readOnly = true)
    public MesProcessPoolProductionReportRevisionLogBO getProductionReportRevisionDetail(
            Long revisionId, Long actorUserId) {
        requirePositive(revisionId, "revisionId");
        requirePositive(actorUserId, "actorUserId");
        MesProProcessPoolEventRevisionDO revision = revisionMapper.selectById(revisionId);
        if (revision == null) {
            throw exception(PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS, revisionId);
        }
        return buildContextLogs(List.of(revision), actorUserId).get(0);
    }

    private List<MesProcessPoolProductionReportRevisionLogBO> buildContextLogs(
            List<MesProProcessPoolEventRevisionDO> revisions, Long actorUserId) {
        if (revisions == null || revisions.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<MesProProcessPoolEventRevisionDiffDO>> diffsByRevision =
                loadDiffsByRevision(revisions);
        List<MesProcessPoolProductionReportRevisionLogBO> result = new ArrayList<>(revisions.size());
        for (MesProProcessPoolEventRevisionDO revision : revisions) {
            requirePositive(revision.getEventId(), "revisionLog.eventId");
            MesProProcessPoolEventDO event = loadProductionEvent(revision.getEventId());
            scopeService.assertCanAccessEmployee(actorUserId, "PRODUCTION", event.getActualEmployeeId());
            MesProcessPoolProductionReportRevisionLogBO log =
                    toLog(revision, diffsByRevision.get(revision.getId()));
            attachTimelineContext(log, requireTimelineDetail(revision.getEventId()));
            result.add(log);
        }
        return result;
    }

    private MesProProcessPoolEventDO loadProductionEvent(Long eventId) {
        MesProProcessPoolEventDO event = eventMapper.selectById(eventId);
        if (event == null) {
            throw exception(PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS, eventId);
        }
        if (!MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "productionSubmitEvent");
        }
        requirePositive(event.getActualEmployeeId(), "actualEmployeeId");
        return event;
    }

    private ProcessPoolTimelineEventReadDO requireTimelineDetail(Long eventId) {
        ProcessPoolTimelineEventReadDO detail = timelineReadMapper.selectTimelineDetailById(eventId);
        if (detail == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "revisionLog.timelineContext");
        }
        return detail;
    }

    private Map<Long, List<MesProProcessPoolEventRevisionDiffDO>> loadDiffsByRevision(
            List<MesProProcessPoolEventRevisionDO> revisions) {
        List<Long> revisionIds = revisions.stream()
                .map(MesProProcessPoolEventRevisionDO::getId)
                .toList();
        Map<Long, Long> eventIdByRevisionId = new LinkedHashMap<>();
        for (MesProProcessPoolEventRevisionDO revision : revisions) {
            if (revision == null || revision.getId() == null) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "revisionLog.revisionId");
            }
            eventIdByRevisionId.put(revision.getId(), revision.getEventId());
        }

        Map<Long, List<MesProProcessPoolEventRevisionDiffDO>> diffsByRevision = new LinkedHashMap<>();
        for (MesProProcessPoolEventRevisionDiffDO diff : revisionDiffMapper.selectListByRevisionIds(revisionIds)) {
            Long expectedEventId = diff == null ? null : eventIdByRevisionId.get(diff.getRevisionId());
            if (diff == null || expectedEventId == null || !Objects.equals(expectedEventId, diff.getEventId())) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "revisionDiff.eventContext");
            }
            diffsByRevision.computeIfAbsent(diff.getRevisionId(), ignored -> new ArrayList<>()).add(diff);
        }
        return diffsByRevision;
    }

    private MesProcessPoolProductionReportRevisionLogBO toLog(
            MesProProcessPoolEventRevisionDO revision,
            List<MesProProcessPoolEventRevisionDiffDO> diffs) {
        if (revision == null || revision.getId() == null
                || !MesProProcessPoolEventRevisionDO.STATUS_EFFECTIVE.equals(revision.getRevisionStatus())
                || StrUtil.isBlank(revision.getChangeReason()) || revision.getServerRevisionTime() == null
                || revision.getRevisionSignatureId() == null || revision.getRevisionSignatureId() <= 0
                || revision.getRevisionSignatureUserId() == null || revision.getRevisionSignatureUserId() <= 0
                || !Objects.equals(revision.getRevisionSignatureUserId(), revision.getModifiedByUserId())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "revisionLog.auditEvidence");
        }
        if (diffs == null || diffs.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "revisionLog.changes");
        }
        String actorName = requireSnapshotActorName(revision);
        List<MesProcessPoolProductionReportRevisionLogBO.FieldChange> changes = diffs.stream()
                .map(this::toReadableChange)
                .toList();
        return MesProcessPoolProductionReportRevisionLogBO.builder()
                .revisionId(revision.getId())
                .eventId(revision.getEventId())
                .modifiedByName(actorName)
                .modifiedAt(revision.getServerRevisionTime())
                .changeReason(revision.getChangeReason().trim())
                .signatureConfirmed(Boolean.TRUE)
                .fieldCount(changes.size())
                .changeSummary(buildChangeSummary(changes))
                .changes(changes)
                .build();
    }

    private void attachTimelineContext(
            MesProcessPoolProductionReportRevisionLogBO log,
            ProcessPoolTimelineEventReadDO timeline) {
        if (!Objects.equals(log.getEventId(), timeline.getId())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "revisionLog.timelineEvent");
        }
        log.setWorkOrderCode(timeline.getWorkOrderCode());
        log.setWorkOrderName(timeline.getWorkOrderName());
        log.setProcessCode(timeline.getProcessCode());
        log.setProcessName(timeline.getProcessName());
        log.setActualEmployeeName(timeline.getActualEmployeeUserName());
        log.setSubmittedAt(timeline.getSubmittedAt());
    }

    private String buildChangeSummary(List<MesProcessPoolProductionReportRevisionLogBO.FieldChange> changes) {
        List<String> fieldNames = changes.stream()
                .map(MesProcessPoolProductionReportRevisionLogBO.FieldChange::getFieldName)
                .filter(StrUtil::isNotBlank)
                .limit(3)
                .toList();
        String summary = String.join("、", fieldNames);
        return changes.size() > 3 ? summary + " 等" + changes.size() + "项" : summary;
    }

    private String requireSnapshotActorName(MesProProcessPoolEventRevisionDO revision) {
        try {
            JsonNode snapshot = JsonUtils.parseTree(revision.getRevisionSignatureSnapshot());
            JsonNode actorId = snapshot == null ? null : snapshot.get("actorId");
            JsonNode actorName = snapshot == null ? null : snapshot.get("actorName");
            if (actorId == null || !actorId.canConvertToLong()
                    || !Objects.equals(actorId.longValue(), revision.getRevisionSignatureUserId())
                    || actorName == null || StrUtil.isBlank(actorName.asText())) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "revisionSignatureSnapshot.actor");
            }
            return actorName.asText().trim();
        } catch (RuntimeException ex) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "revisionSignatureSnapshot.actor");
        }
    }

    private MesProcessPoolProductionReportRevisionLogBO.FieldChange toReadableChange(
            MesProProcessPoolEventRevisionDiffDO diff) {
        if (diff == null || StrUtil.isBlank(diff.getFieldName())
                || Objects.equals(diff.getBeforeValue(), diff.getAfterValue())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "revisionLog.fieldChange");
        }
        String beforeValue = diff.getBeforeValue();
        String afterValue = diff.getAfterValue();
        if (LEGACY_LOSS_DETAILS_FIELD.equals(diff.getFieldCode())) {
            beforeValue = formatLegacyLossDetails(beforeValue);
            afterValue = formatLegacyLossDetails(afterValue);
        } else if (beforeValue == null || afterValue == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "revisionLog.fieldValue");
        }
        return MesProcessPoolProductionReportRevisionLogBO.FieldChange.builder()
                .fieldName(diff.getFieldName().trim())
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .build();
    }

    private String formatLegacyLossDetails(String json) {
        try {
            JsonNode details = JsonUtils.parseTree(json);
            if (details == null || !details.isArray()) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "revisionLog.lossDetails");
            }
            List<String> items = new ArrayList<>();
            for (JsonNode detail : details) {
                String reasonName = text(detail, "reasonName");
                if (StrUtil.isBlank(reasonName)) {
                    reasonName = text(detail, "reasonCode");
                }
                JsonNode quantityNode = detail.get("quantity");
                if (StrUtil.isBlank(reasonName) || quantityNode == null || !quantityNode.isNumber()) {
                    throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "revisionLog.lossDetails.item");
                }
                items.add(reasonName.trim() + " " + formatDecimal(quantityNode.decimalValue()));
            }
            return items.isEmpty() ? "无" : String.join("；", items);
        } catch (RuntimeException ex) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "revisionLog.lossDetails");
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String formatDecimal(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.compareTo(BigDecimal.ZERO) == 0 ? "0" : normalized.toPlainString();
    }

    private void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, fieldName);
        }
    }
}
