package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterial;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterialService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
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
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesFrontlineProcessMaterialService materialService;
    private final ErpKingdeeProductionReplenishmentListItemMapper replenishmentItemMapper;
    private final ErpKingdeeProductionReplenishmentListMapper replenishmentMapper;

    public MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl(
            MesProProcessPoolEventMapper eventMapper,
            MesProFeedbackMapper feedbackMapper,
            MesProcessPoolReportAllocationMapper allocationMapper,
            MesProcessPoolSubmissionReviewMapper reviewMapper,
            MesProWorkOrderMapper workOrderMapper,
            MesFrontlineProcessMaterialService materialService,
            ErpKingdeeProductionReplenishmentListItemMapper replenishmentItemMapper,
            ErpKingdeeProductionReplenishmentListMapper replenishmentMapper) {
        this.eventMapper = eventMapper;
        this.feedbackMapper = feedbackMapper;
        this.allocationMapper = allocationMapper;
        this.reviewMapper = reviewMapper;
        this.workOrderMapper = workOrderMapper;
        this.materialService = materialService;
        this.replenishmentItemMapper = replenishmentItemMapper;
        this.replenishmentMapper = replenishmentMapper;
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
        List<MesProcessPoolReportAllocationDO> targetAllocations = allocationMapper
                .selectListByActiveOrderIdForUpdate(command.getActiveOrderId());
        if (targetAllocations == null || targetAllocations.stream().anyMatch(allocation -> allocation == null
                || !Objects.equals(command.getActiveOrderId(), allocation.getActiveOrderId())
                || !Objects.equals(command.getWorkOrderId(), allocation.getWorkOrderId())
                || allocation.getEventId() == null || allocation.getAllocatedQuantity() == null
                || allocation.getAllocatedQuantity().signum() < 0)) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "lossSourceTargetAllocations");
        }
        targetAllocations = targetAllocations.stream().filter(allocation -> allocation.getAllocatedQuantity().signum() > 0).toList();
        List<Long> sourceEventIds = targetAllocations.stream().map(MesProcessPoolReportAllocationDO::getEventId).distinct().toList();
        List<MesProProcessPoolEventDO> events = sourceEventIds.isEmpty() ? List.of()
                : orderedEvents(eventMapper.selectProductionSubmitsByIdsForUpdate(sourceEventIds));
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
        if (!events.stream().map(MesProProcessPoolEventDO::getId).toList().containsAll(sourceEventIds)) {
            blockers.add(blocker("LOSS_SOURCE_REQUIRED", null, "PRODUCTION_EVENT", null, null,
                    "分配指向的正式生产来源事件缺失", "请恢复分配对应的生产来源后重新申请"));
        }
        var replenishments = readReplenishments(command, snapshots, blockers);
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            List<MesProProcessPoolEventDO> matchingEvents = events.stream()
                    .filter(event -> matches(command, snapshot, event))
                    .toList();
            int sourcesBefore = sources.size();
            for (MesProProcessPoolEventDO event : matchingEvents) {
                MesProFeedbackDO feedback = feedbackById.get(event.getFeedbackSourceId());
                MesProcessPoolReportAllocationDO allocation = uniqueAllocation(command, snapshot, event, targetAllocations);
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
                // A replenishment line belongs to the process, not to each production submission.
                var formalSources = sources.size() == sourcesBefore
                        ? replenishments.getOrDefault(snapshot.getRouteProcessId(), List.of())
                        : List.<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ReplenishmentSource>of();
                BigDecimal formalQuantity = formalSources.stream()
                        .map(MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ReplenishmentSource::getActualQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                var details = formalSources.stream().filter(item -> item.getActualQuantity().signum() > 0)
                        .map(item -> new MesTeamLeaderActiveOrderReleaseLossSourceReadResult.LossDetail()
                                .setReasonId(item.getItemId()).setReasonCode("REPLENISHMENT:" + item.getMaterialCode())
                                .setReasonName("生产补料：" + item.getMaterialName()).setQuantity(item.getActualQuantity()))
                        .toList();
                sources.add(new MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource()
                        .setSnapshot(snapshot)
                        .setFeedback(feedback)
                        .setEvent(event)
                        .setAllocation(allocation)
                        .setReview(review)
                        .setReplenishmentSources(formalSources).setFormalLossQuantity(formalQuantity)
                        .setLossDetails(details)
                        .setHasActualLoss(formalQuantity.signum() > 0)
                        .setZeroLossConfirmed(formalQuantity.signum() == 0)
                        .setLossDecision(formalQuantity.signum() > 0 ? "REQUIRED" : "NO_LOSS"));
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
                && event.getWorkOrderId() != null
                && Objects.equals(command.getRouteId(), event.getRouteId())
                && Objects.equals(snapshot.getRouteProcessId(), event.getRouteProcessId())
                && Objects.equals(snapshot.getProcessId(), event.getProcessId());
    }

    private MesProcessPoolReportAllocationDO uniqueAllocation(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command,
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            MesProProcessPoolEventDO event, List<MesProcessPoolReportAllocationDO> allocations) {
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
                && Objects.equals(event.getWorkOrderId(), feedback.getWorkOrderId())
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

    private Map<Long, List<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ReplenishmentSource>> readReplenishments(
            MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command,
            List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots,
            List<MesTeamLeaderActiveOrderReleaseBlocker> blockers) {
        Map<Long, List<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ReplenishmentSource>> result = new LinkedHashMap<>();
        var workOrder = workOrderMapper.selectByIdForUpdate(command.getWorkOrderId());
        if (workOrder == null || StrUtil.isBlank(workOrder.getCode())) {
            blockers.add(blocker("LOSS_WORK_ORDER_REQUIRED", null, "WORK_ORDER", command.getWorkOrderId(),
                    null, "生产订单编号缺失，无法查询正式补料单", "请补齐正式生产订单"));
            return result;
        }
        var items = replenishmentItemMapper.selectListByProductionOrderNo(workOrder.getCode());
        if (items == null) {
            throw new IllegalStateException("生产补料单查询未返回结果集合");
        }
        if (items.isEmpty()) {
            if (Boolean.TRUE.equals(command.getRequireNoReplenishmentConfirmation())
                    && !Boolean.TRUE.equals(command.getConfirmNoReplenishmentInfo())) {
                blockers.add(blocker("NO_REPLENISHMENT_CONFIRMATION_REQUIRED", null, "WORK_ORDER", workOrder.getId(),
                        null, "完成时未查到生产补料单，需要确认无补料信息", "确认后按无正式损耗完成；取消则不完成"));
            }
            return result;
        }
        var headerIds = items.stream().map(item -> item.getProductionReplenishmentListId())
                .filter(Objects::nonNull).distinct().sorted().toList();
        Map<Long, ErpKingdeeProductionReplenishmentListDO> headers = new LinkedHashMap<>();
        if (!headerIds.isEmpty()) {
            replenishmentMapper.selectBatchIds(headerIds).forEach(header -> headers.put(header.getId(), header));
        }
        Map<Long, List<String>> inputCodes = new LinkedHashMap<>();
        for (var snapshot : snapshots) {
            var materials = materialService.listFrozenMaterials(command.getActiveOrderId(), command.getRouteId(),
                    snapshot.getRouteProcessId(), snapshot.getProcessId());
            inputCodes.put(snapshot.getRouteProcessId(), materials.stream()
                    .filter(material -> MesFrontlineProcessMaterial.ROLE_INPUT.equals(material.materialRole()))
                    .map(MesFrontlineProcessMaterial::materialCode).filter(StrUtil::isNotBlank)
                    .map(String::trim).distinct().toList());
        }
        for (var item : items) {
            var header = headers.get(item.getProductionReplenishmentListId());
            if (item.getId() == null || header == null || StrUtil.isBlank(header.getSourceBillNo())
                    || !"C".equals(header.getDocumentStatus()) || StrUtil.isBlank(item.getMaterialNumber())
                    || StrUtil.isBlank(item.getMaterialName()) || !Objects.equals(workOrder.getCode(), item.getProductionOrderNo())
                    || item.getActualQuantity() == null || item.getActualQuantity().signum() < 0) {
                blockers.add(blocker("LOSS_REPLENISHMENT_SOURCE_INVALID", null, "REPLENISHMENT_ITEM", item.getId(),
                        null, "生产补料单未审核或正式单号、物料、实补数量不完整", "请同步并核对正式生产补料单"));
                continue;
            }
            var owners = snapshots.stream().filter(snapshot -> inputCodes.get(snapshot.getRouteProcessId())
                    .contains(item.getMaterialNumber().trim())).toList();
            if (owners.size() != 1) {
                blockers.add(blocker(owners.isEmpty() ? "LOSS_REPLENISHMENT_MATERIAL_UNBOUND"
                                : "LOSS_REPLENISHMENT_MATERIAL_AMBIGUOUS", null, "REPLENISHMENT_ITEM", item.getId(),
                        null, "补料物料未能唯一匹配订单工序输入物料：" + item.getMaterialNumber(),
                        "请核对补料物料与工序输入物料配置"));
                continue;
            }
            result.computeIfAbsent(owners.get(0).getRouteProcessId(), ignored -> new ArrayList<>())
                    .add(new MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ReplenishmentSource()
                            .setHeaderId(header.getId()).setItemId(item.getId()).setSourceBillNo(header.getSourceBillNo())
                            .setMaterialCode(item.getMaterialNumber()).setMaterialName(item.getMaterialName())
                            .setLotNumber(item.getLotNumber()).setActualQuantity(item.getActualQuantity()));
        }
        result.values().forEach(lines -> lines.sort(Comparator.comparing(
                MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ReplenishmentSource::getItemId)));
        return result;
    }

    private List<MesProProcessPoolEventDO> orderedEvents(List<MesProProcessPoolEventDO> events) {
        return events == null ? List.of() : events.stream().filter(Objects::nonNull)
                .sorted(Comparator.comparing(MesProProcessPoolEventDO::getId,
                        Comparator.nullsLast(Long::compareTo)))
                .toList();
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
