package cn.iocoder.yudao.module.signature.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureComplianceReviewDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureComplianceReviewMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.signature.enums.SignatureErrorCodeConstants.ESIGN_COMMAND_INVALID;

@Service
public class ElectronicSignatureComplianceReviewService {

    public static final String REVIEW_TYPE_QUARTERLY = "QUARTERLY";
    public static final String REVIEW_TYPE_SPECIAL = "SPECIAL";
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_ESCALATED = "ESCALATED";

    @Resource
    private ElectronicSignatureComplianceReviewMapper reviewMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long createQuarterlyReview(String reviewScope, Long ownerUserId, Long backupOwnerUserId,
                                      String sopVersion, String trainingEvidenceId, LocalDateTime plannedAt,
                                      LocalDateTime dueAt) {
        return createReview(REVIEW_TYPE_QUARTERLY, reviewScope, "QUARTERLY_SCHEDULE", ownerUserId,
                backupOwnerUserId, sopVersion, trainingEvidenceId, plannedAt, dueAt);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createSpecialReview(String reviewScope, String triggerReason, Long ownerUserId,
                                    Long backupOwnerUserId, String sopVersion, String trainingEvidenceId,
                                    LocalDateTime plannedAt, LocalDateTime dueAt) {
        return createReview(REVIEW_TYPE_SPECIAL, reviewScope, triggerReason, ownerUserId, backupOwnerUserId,
                sopVersion, trainingEvidenceId, plannedAt, dueAt);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markOverdueEscalated(Long reviewId, Long escalatedToUserId, LocalDateTime escalatedAt) {
        if (reviewId == null || escalatedToUserId == null || escalatedAt == null) {
            throw exception(ESIGN_COMMAND_INVALID, "复核编号、升级接收人和升级时间不能为空");
        }
        ElectronicSignatureComplianceReviewDO review = reviewMapper.selectById(reviewId);
        if (review == null || review.getTenantId() == null
                || !review.getTenantId().equals(TenantContextHolder.getRequiredTenantId())) {
            throw exception(ESIGN_COMMAND_INVALID, "复核批次不存在");
        }
        reviewMapper.updateById(new ElectronicSignatureComplianceReviewDO()
                .setId(reviewId)
                .setStatus(STATUS_ESCALATED)
                .setEscalatedToUserId(escalatedToUserId)
                .setEscalatedAt(escalatedAt));
    }

    private Long createReview(String reviewType, String reviewScope, String triggerReason, Long ownerUserId,
                              Long backupOwnerUserId, String sopVersion, String trainingEvidenceId,
                              LocalDateTime plannedAt, LocalDateTime dueAt) {
        if (StrUtil.hasBlank(reviewScope, triggerReason, sopVersion, trainingEvidenceId)
                || ownerUserId == null || backupOwnerUserId == null || plannedAt == null || dueAt == null) {
            throw exception(ESIGN_COMMAND_INVALID, "复核范围、原因、责任人、SOP、培训证据、计划和截止时间不能为空");
        }
        ElectronicSignatureComplianceReviewDO review = ElectronicSignatureComplianceReviewDO.builder()
                .reviewType(reviewType)
                .reviewScope(reviewScope)
                .triggerReason(triggerReason)
                .status(STATUS_OPEN)
                .ownerUserId(ownerUserId)
                .backupOwnerUserId(backupOwnerUserId)
                .sopVersion(sopVersion)
                .trainingEvidenceId(trainingEvidenceId)
                .plannedAt(plannedAt)
                .dueAt(dueAt)
                .sampleRuleJson("{\"source\":\"SERVER_GENERATED\"}")
                .build();
        review.setTenantId(TenantContextHolder.getRequiredTenantId());
        reviewMapper.insert(review);
        return review.getId();
    }

}
