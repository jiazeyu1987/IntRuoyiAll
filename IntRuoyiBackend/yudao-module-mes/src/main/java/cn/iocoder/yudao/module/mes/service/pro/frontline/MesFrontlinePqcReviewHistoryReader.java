package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcProcessRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesSignedReturnCorrectionResolver;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID;

/** Read-only history of the displayed task's own formal submission, including superseded decisions. */
@Component
public class MesFrontlinePqcReviewHistoryReader {
    private final MesProcessPoolSubmissionReviewMapper reviews;
    private final AdminUserApi users;
    private final MesSignedReturnCorrectionResolver corrections;

    public MesFrontlinePqcReviewHistoryReader(MesProcessPoolSubmissionReviewMapper reviews, AdminUserApi users,
                                             MesSignedReturnCorrectionResolver corrections) {
        this.reviews = reviews;
        this.users = users;
        this.corrections = corrections;
    }

    public Projection read(
            MesProcessPoolActiveOrderDO order, MesPqcInspectionTaskDO task, MesProProcessPoolEventDO event) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (!Objects.equals(tenantId, order.getTenantId())
                || !Objects.equals(tenantId, task.getTenantId())
                || !Objects.equals(tenantId, event.getTenantId())
                || !Objects.equals(order.getId(), task.getActiveOrderId())
                || !Objects.equals(order.getWorkOrderId(), task.getWorkOrderId())
                || !Objects.equals(order.getRouteId(), task.getRouteId())
                || !Objects.equals(order.getRouteVersionId(), task.getRouteVersionId())
                || task.getSubmittedEventId() == null
                || !Objects.equals(task.getSubmittedEventId(), event.getId())
                || !"PQC_INSPECTION".equals(event.getEventType())
                || !"MES_PQC_INSPECTION_TASK".equals(event.getFeedbackSourceType())
                || !Objects.equals(task.getId(), event.getFeedbackSourceId())
                || !Objects.equals(task.getWorkOrderId(), event.getWorkOrderId())
                || !Objects.equals(task.getRouteId(), event.getRouteId())
                || !Objects.equals(task.getQaProcessId(), event.getQaProcessId())) {
            throw invalid(task);
        }
        // Group review writes a separate formal review for every member event. Never borrow another member's decision.
        var history = reviews.selectFormalReviewsByEventIds(tenantId, List.of(event.getId()));
        for (var review : history) {
            if (!Objects.equals(tenantId, review.getTenantId())
                    || !Objects.equals(event.getId(), review.getEventId())
                    || !"PQC".equals(review.getLeaderType())
                    || review.getId() == null || review.getId() <= 0
                    || review.getLeaderUserId() == null || review.getLeaderUserId() <= 0
                    || review.getReviewedAt() == null || review.getReviewRound() == null
                    || Boolean.TRUE.equals(review.getSimulated())
                    || StrUtil.isNotBlank(review.getSimulationStage()) || StrUtil.isNotBlank(review.getSimulationRunId())
                    || review.getReviewStatus() == null
                    || !Set.of("APPROVED", "REJECTED").contains(review.getReviewStatus())) {
                throw invalid(task);
            }
        }
        var ordered = history.stream().sorted(Comparator.comparing(MesProcessPoolSubmissionReviewDO::getReviewedAt)
                .thenComparing(MesProcessPoolSubmissionReviewDO::getId)).toList();
        String currentStatus = "PENDING";
        if (!ordered.isEmpty()) {
            var latest = ordered.get(ordered.size() - 1);
            // Reuse the review command's signed-correction and pending-handoff evidence rules.
            currentStatus = "REJECTED".equals(latest.getReviewStatus()) && corrections.find(event, latest) != null
                    ? "PENDING" : latest.getReviewStatus();
        }
        if ("CONFIRMED".equals(task.getTaskStatus()) && !"APPROVED".equals(currentStatus)) {
            throw invalid(task);
        }
        var resultHistory = ordered.stream().map(review -> {
            var result = new MesFrontlinePqcProcessRespVO.PqcReviewHistory();
            result.setReviewId(review.getId());
            result.setEventId(review.getEventId());
            result.setReviewStatus(review.getReviewStatus());
            result.setReviewRemark(review.getReviewRemark());
            result.setReviewedAt(review.getReviewedAt());
            result.setReviewerUserId(review.getLeaderUserId());
            var user = users.getUser(review.getLeaderUserId());
            if (user != null && (!Objects.equals(tenantId, user.getTenantId())
                    || !Objects.equals(review.getLeaderUserId(), user.getId()))) {
                throw invalid(task);
            }
            result.setReviewerName(user == null ? null : user.getNickname());
            result.setReviewRound(review.getReviewRound());
            result.setRevisionId(review.getSourceRevisionId());
            return result;
        }).toList();
        return new Projection(currentStatus, resultHistory);
    }

    public record Projection(String currentReviewStatus,
                             List<MesFrontlinePqcProcessRespVO.PqcReviewHistory> reviewHistory) {}

    private static cn.iocoder.yudao.framework.common.exception.ServiceException invalid(MesPqcInspectionTaskDO task) {
        return exception(PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID,
                "PQC复核历史与当前检验任务不一致，检验任务编号：" + task.getId() + "，请刷新后联系组长核对");
    }
}
