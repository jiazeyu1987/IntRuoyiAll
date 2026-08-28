package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;

@Service
public class MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl
        implements MesTeamLeaderActiveOrderReleaseLossSourceReader {

    private static final String FEEDBACK_SOURCE_TYPE = "MES_PRO_FEEDBACK";

    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProFeedbackMapper feedbackMapper;
    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesProcessPoolSubmissionReviewMapper reviewMapper;

    public MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl(
            MesProProcessPoolEventMapper eventMapper,
            MesProFeedbackMapper feedbackMapper,
            MesProcessPoolReportAllocationMapper allocationMapper,
            MesProcessPoolSubmissionReviewMapper reviewMapper) {
        this.eventMapper = eventMapper;
        this.feedbackMapper = feedbackMapper;
        this.allocationMapper = allocationMapper;
        this.reviewMapper = reviewMapper;
    }

    @Override
    public MesTeamLeaderActiveOrderReleaseLossSourceReadResult read(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command) {
        validateCommand(command);
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots = command.getProcessSnapshots().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(MesProcessPoolActiveOrderProcessSnapshotDO::getRouteProcessId)
                        .thenComparing(MesProcessPoolActiveOrderProcessSnapshotDO::getProcessId))
                .toList();
        List<MesProProcessPoolEventDO> events = orderedEvents(
                eventMapper.selectProductionSubmitsByWorkOrderAndRouteForUpdate(command.getWorkOrderId(), command.getRouteId()));
        LinkedHashSet<Long> feedbackIds = new LinkedHashSet<>();
        for (MesProProcessPoolEventDO event : events) {
            if (event != null && FEEDBACK_SOURCE_TYPE.equals(event.getFeedbackSourceType())
                    && event.getFeedbackSourceId() != null) {
                feedbackIds.add(event.getFeedbackSourceId());
            }
        }
        Map<Long, MesProFeedbackDO> feedbackById = new LinkedHashMap<>();
        if (!feedbackIds.isEmpty()) {
            List<MesProFeedbackDO> feedbacks = feedbackMapper.selectListByIdsForUpdate(List.copyOf(feedbackIds));
            if (feedbacks != null) {
                feedbacks.stream().filter(Objects::nonNull)
                        .sorted(Comparator.comparing(MesProFeedbackDO::getId))
                        .forEach(feedback -> feedbackById.putIfAbsent(feedback.getId(), feedback));
            }
        }

        List<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource> sources = new ArrayList<>();
        List<MesTeamLeaderActiveOrderReleaseBlocker> blockers = new ArrayList<>();
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            List<MesProProcessPoolEventDO> matchingEvents = events.stream()
                    .filter(event -> matches(command, snapshot, event))
                    .toList();
            if (matchingEvents.size() > 1) {
                blockers.add(blocker("LOSS_SOURCE_REQUIRED", snapshot, "PRODUCTION_EVENT",
                        matchingEvents.get(0).getId(), null,
                        "当前活跃订单工序存在重复签名生产提交，无法形成唯一损耗来源闭环",
                        "请删除重复生产提交或重新生成正式反馈后再申请损耗"));
                continue;
            }
            int sourcesBefore = sources.size();
            for (MesProProcessPoolEventDO event : matchingEvents) {
                MesProFeedbackDO feedback = feedbackById.get(event.getFeedbackSourceId());
                MesProcessPoolReportAllocationDO allocation = uniqueAllocation(command, snapshot, event);
                MesProcessPoolSubmissionReviewDO review = uniqueReview(event, allocation);
                if (!validFormalJoin(command, snapshot, event, feedback, allocation, review)) {
                    blockers.add(blocker("LOSS_SOURCE_REQUIRED", snapshot, "PRODUCTION_FEEDBACK",
                            event.getFeedbackSourceId(), null,
                            "损耗来源未形成当前活跃订单的生产反馈、签名提交、分配和生产复核唯一闭环",
                            "请修复正式生产反馈追溯后重新申请"));
                    continue;
                }
                if (!validSignatures(feedback, event, allocation, review)) {
                    blockers.add(blocker("PRODUCTION_SIGNATURE_REQUIRED", snapshot, "PRODUCTION_REVIEW",
                            review.getId(), null,
                            "生产反馈提交或生产组长 APPROVED 复核缺少正式签名证据",
                            "请由正式填写人和生产组长完成电子签名"));
                    continue;
                }
                List<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.LossDetail> details =
                        exactLossDetails(event, feedback, snapshot, blockers);
                if (details == null) {
                    continue;
                }
                sources.add(new MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource()
                        .setSnapshot(snapshot)
                        .setFeedback(feedback)
                        .setEvent(event)
                        .setAllocation(allocation)
                        .setReview(review)
                        .setLossDetails(details));
            }
            if (sources.size() == sourcesBefore && matchingEvents.isEmpty()) {
                blockers.add(blocker("LOSS_SOURCE_REQUIRED", snapshot, "ROUTE_PROCESS",
                        snapshot.getRouteProcessId(), null,
                        "当前活跃订单工序缺少正式生产反馈提交",
                        "请完成当前批次生产反馈并形成签名事件"));
            }
        }
        sources.sort(Comparator
                .comparing((MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource source) ->
                        source.getSnapshot().getRouteProcessId())
                .thenComparing(source -> source.getEvent().getId()));
        return new MesTeamLeaderActiveOrderReleaseLossSourceReadResult()
                .setProcessSources(List.copyOf(sources))
                .setBlockers(List.copyOf(blockers));
    }

    private void validateCommand(MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command) {
        if (command == null || command.getTenantId() == null || command.getTenantId() <= 0
                || command.getActiveOrderId() == null || command.getActiveOrderId() <= 0
                || command.getWorkOrderId() == null || command.getWorkOrderId() <= 0
                || command.getRouteId() == null || command.getRouteId() <= 0
                || command.getRouteVersionId() == null || command.getRouteVersionId() <= 0
                || command.getProductId() == null || command.getProductId() <= 0
                || StrUtil.isBlank(command.getBatchCode()) || StrUtil.isBlank(command.getSourceSnapshotHash())
                || command.getProcessSnapshots() == null || command.getProcessSnapshots().isEmpty()
                || command.getProcessSnapshots().stream().anyMatch(snapshot -> snapshot == null
                || snapshot.getRouteProcessId() == null || snapshot.getProcessId() == null)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "activeOrderReleaseLossSourceRead");
        }
    }

    private boolean matches(MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command,
                            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
                            MesProProcessPoolEventDO event) {
        return event != null && event.getId() != null
                && MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())
                && FEEDBACK_SOURCE_TYPE.equals(event.getFeedbackSourceType())
                && event.getFeedbackSourceId() != null
                && Objects.equals(command.getWorkOrderId(), event.getWorkOrderId())
                && Objects.equals(command.getRouteId(), event.getRouteId())
                && Objects.equals(snapshot.getRouteProcessId(), event.getRouteProcessId())
                && Objects.equals(snapshot.getProcessId(), event.getProcessId());
    }

    private MesProcessPoolReportAllocationDO uniqueAllocation(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command,
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            MesProProcessPoolEventDO event) {
        List<MesProcessPoolReportAllocationDO> allocations = allocationMapper.selectListByEventIdForUpdate(event.getId());
        List<MesProcessPoolReportAllocationDO> formal = allocations == null ? List.of() : allocations.stream()
                .filter(Objects::nonNull)
                .filter(allocation -> Objects.equals(command.getActiveOrderId(), allocation.getActiveOrderId())
                        && Objects.equals(command.getWorkOrderId(), allocation.getWorkOrderId())
                        && Objects.equals(snapshot.getRouteProcessId(), allocation.getRouteProcessId())
                        && Objects.equals(snapshot.getProcessId(), allocation.getProcessId())
                        && Objects.equals(event.getId(), allocation.getEventId()))
                .toList();
        return formal.size() == 1 ? formal.get(0) : null;
    }

    private MesProcessPoolSubmissionReviewDO uniqueReview(
            MesProProcessPoolEventDO event, MesProcessPoolReportAllocationDO allocation) {
        List<MesProcessPoolSubmissionReviewDO> reviews = reviewMapper.selectListByEventIdForUpdate(event.getId());
        if (allocation == null) {
            return null;
        }
        List<MesProcessPoolSubmissionReviewDO> formal = reviews == null ? List.of() : reviews.stream()
                .filter(Objects::nonNull)
                .filter(review -> Objects.equals(allocation.getReviewId(), review.getId())
                        && Objects.equals(event.getId(), review.getEventId()))
                .toList();
        return formal.size() == 1 ? formal.get(0) : null;
    }

    private boolean validFormalJoin(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command,
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            MesProProcessPoolEventDO event,
            MesProFeedbackDO feedback,
            MesProcessPoolReportAllocationDO allocation,
            MesProcessPoolSubmissionReviewDO review) {
        return feedback != null && feedback.getId() != null
                && Objects.equals(event.getFeedbackSourceId(), feedback.getId())
                && Objects.equals(command.getWorkOrderId(), feedback.getWorkOrderId())
                && Objects.equals(command.getRouteId(), feedback.getRouteId())
                && Objects.equals(snapshot.getProcessId(), feedback.getProcessId())
                && allocation != null && allocation.getId() != null && allocation.getReviewId() != null
                && review != null && review.getId() != null;
    }

    private boolean validSignatures(MesProFeedbackDO feedback,
                                    MesProProcessPoolEventDO event,
                                    MesProcessPoolReportAllocationDO allocation,
                                    MesProcessPoolSubmissionReviewDO review) {
        return event.getActualEmployeeId() != null && event.getSignatureId() != null
                && event.getSignatureUserId() != null && event.getServerSubmitTime() != null
                && StrUtil.isNotBlank(event.getSignatureSnapshot())
                && Objects.equals(event.getActualEmployeeId(), event.getSignatureUserId())
                && Objects.equals(feedback.getFeedbackUserId(), event.getSignatureUserId())
                && "PRODUCTION".equals(review.getLeaderType())
                && MesProcessPoolSubmissionReviewDO.STATUS_APPROVED.equals(review.getReviewStatus())
                && review.getLeaderUserId() != null && review.getReviewSignatureId() != null
                && review.getReviewSignatureUserId() != null && review.getReviewedAt() != null
                && StrUtil.isNotBlank(review.getReviewSignatureSnapshotJson())
                && Objects.equals(review.getLeaderUserId(), review.getReviewSignatureUserId())
                && Objects.equals(feedback.getApproveUserId(), review.getReviewSignatureUserId())
                && Objects.equals(allocation.getLeaderUserId(), review.getLeaderUserId())
                && Objects.equals(allocation.getConfirmedAt(), review.getReviewedAt());
    }

    private List<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.LossDetail> exactLossDetails(
            MesProProcessPoolEventDO event,
            MesProFeedbackDO feedback,
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        JsonNode detailsNode;
        try {
            JsonNode payload = JsonUtils.getObjectMapper().readTree(event.getRawPayload());
            detailsNode = payload == null ? null : payload.get("lossDetails");
        } catch (Exception ex) {
            detailsNode = null;
        }
        if (detailsNode == null || !detailsNode.isArray()) {
            if (isZero(feedback.getUnqualifiedQuantity())) {
                return List.of();
            }
            blockers.add(blocker("LOSS_SOURCE_REQUIRED", snapshot, "PRODUCTION_EVENT", event.getId(),
                    "lossDetails", "签名生产提交缺少结构化 lossDetails",
                    "请由正式生产反馈链路保存结构化损耗明细"));
            return null;
        }
        List<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.LossDetail> details = new ArrayList<>();
        for (JsonNode detail : detailsNode) {
            JsonNode reasonId = detail.get("reasonId");
            JsonNode reasonCode = detail.get("reasonCode");
            JsonNode reasonName = detail.get("reasonName");
            JsonNode quantity = detail.get("quantity");
            if (reasonId == null || !reasonId.canConvertToLong()
                    || reasonCode == null || !reasonCode.isTextual() || StrUtil.isBlank(reasonCode.asText())
                    || reasonName == null || !reasonName.isTextual() || StrUtil.isBlank(reasonName.asText())
                    || quantity == null || !quantity.isNumber()) {
                blockers.add(blocker("LOSS_SOURCE_REQUIRED", snapshot, "PRODUCTION_EVENT", event.getId(),
                        "lossDetails", "签名生产提交的 lossDetails 结构不完整",
                        "请补齐损耗原因标识、编码、名称和数量"));
                return null;
            }
            BigDecimal detailQuantity = quantity.decimalValue();
            if (feedback.getUnqualifiedQuantity() != null
                    && detailQuantity.scale() < feedback.getUnqualifiedQuantity().scale()) {
                detailQuantity = detailQuantity.setScale(feedback.getUnqualifiedQuantity().scale());
            }
            details.add(new MesTeamLeaderActiveOrderReleaseLossSourceReadResult.LossDetail()
                    .setReasonId(reasonId.longValue())
                    .setReasonCode(reasonCode.asText())
                    .setReasonName(reasonName.asText())
                    .setQuantity(detailQuantity));
        }
        return List.copyOf(details);
    }

    private List<MesProProcessPoolEventDO> orderedEvents(List<MesProProcessPoolEventDO> events) {
        return events == null ? List.of() : events.stream().filter(Objects::nonNull)
                .sorted(Comparator.comparing(MesProProcessPoolEventDO::getId,
                        Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private boolean isZero(BigDecimal value) {
        return value != null && value.signum() == 0;
    }

    private MesTeamLeaderActiveOrderReleaseBlocker blocker(
            String type,
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            String objectType,
            Object objectId,
            String fieldCode,
            String reason,
            String suggestion) {
        return new MesTeamLeaderActiveOrderReleaseBlocker()
                .setBlockerType(type)
                .setObjectType(objectType)
                .setObjectId(objectId == null ? null : String.valueOf(objectId))
                .setRouteProcessId(snapshot == null ? null : snapshot.getRouteProcessId())
                .setProcessId(snapshot == null ? null : snapshot.getProcessId())
                .setFieldCode(fieldCode)
                .setReason(reason)
                .setSuggestion(suggestion);
    }
}
