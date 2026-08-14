package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import com.alibaba.fastjson.JSON;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class MesTeamLeaderActiveOrderReleaseApplicationPersistenceService {

    private static final String STATUS_BLOCKED = "BLOCKED";
    private static final String STATUS_PENDING_RELEASE_APPROVAL = "PENDING_RELEASE_APPROVAL";

    private final MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;

    public MesTeamLeaderActiveOrderReleaseApplicationPersistenceService(
            MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper) {
        this.applicationMapper = applicationMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public MesTeamLeaderActiveOrderReleaseApplicationResult persistPending(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        return toResult(insertOrReadStrict(application));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MesTeamLeaderActiveOrderReleaseApplicationResult persistBlocked(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (application == null || !STATUS_BLOCKED.equals(application.getApplicationStatus())) {
            throw new IllegalArgumentException("BLOCKED release application is required");
        }
        return toResult(insertOrReadStrict(application));
    }

    public MesTeamLeaderActiveOrderReleaseApplicationResult toResult(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        MesTeamLeaderActiveOrderReleaseDossierSummary summary = StrUtil.isBlank(application.getDossierSummaryJson())
                ? null : JSON.parseObject(application.getDossierSummaryJson(),
                MesTeamLeaderActiveOrderReleaseDossierSummary.class);
        List<MesTeamLeaderActiveOrderReleaseBlocker> blockers = StrUtil.isBlank(application.getBlockerSnapshotJson())
                ? List.of() : JSON.parseArray(application.getBlockerSnapshotJson(),
                MesTeamLeaderActiveOrderReleaseBlocker.class);
        return new MesTeamLeaderActiveOrderReleaseApplicationResult()
                .setApplicationId(application.getId())
                .setActiveOrderId(application.getActiveOrderId())
                .setWorkOrderId(application.getWorkOrderId())
                .setWorkOrderCode(application.getWorkOrderCode())
                .setBatchExecutionId(application.getBatchExecutionId())
                .setReleaseTransactionId(application.getReleaseTransactionId())
                .setReleaseApprovalWorkTaskId(application.getReleaseApprovalWorkTaskId())
                .setStatus(application.getApplicationStatus())
                .setStatusName(statusName(application.getApplicationStatus()))
                .setDossierSummary(summary)
                .setBlockers(blockers)
                .setAppliedAt(application.getAppliedAt());
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO insertOrReadStrict(
            MesProcessPoolActiveOrderReleaseApplicationDO application) {
        if (application == null || application.getActiveOrderId() == null
                || StrUtil.isBlank(application.getRequestIdempotencyKey())
                || StrUtil.isBlank(application.getBusinessIdempotencyKey())
                || StrUtil.isBlank(application.getSourceSnapshotHash())) {
            throw new IllegalArgumentException("Complete release application identity is required");
        }
        try {
            applicationMapper.insert(application);
            return application;
        } catch (DuplicateKeyException ex) {
            MesProcessPoolActiveOrderReleaseApplicationDO requestExisting =
                    applicationMapper.selectByRequestIdempotencyKey(application.getActiveOrderId(),
                            application.getRequestIdempotencyKey());
            if (sameRequestIdentity(requestExisting, application)) {
                return requestExisting;
            }
            MesProcessPoolActiveOrderReleaseApplicationDO businessExisting =
                    applicationMapper.selectByBusinessIdempotencyKey(application.getActiveOrderId(),
                            application.getBusinessIdempotencyKey());
            if (sameBusinessIdentity(businessExisting, application)) {
                return businessExisting;
            }
            throw ex;
        }
    }

    private boolean sameRequestIdentity(MesProcessPoolActiveOrderReleaseApplicationDO existing,
                                        MesProcessPoolActiveOrderReleaseApplicationDO incoming) {
        return existing != null
                && Objects.equals(existing.getActiveOrderId(), incoming.getActiveOrderId())
                && Objects.equals(existing.getRequestIdempotencyKey(), incoming.getRequestIdempotencyKey())
                && Objects.equals(existing.getBusinessIdempotencyKey(), incoming.getBusinessIdempotencyKey())
                && Objects.equals(existing.getSourceSnapshotHash(), incoming.getSourceSnapshotHash());
    }

    private boolean sameBusinessIdentity(MesProcessPoolActiveOrderReleaseApplicationDO existing,
                                         MesProcessPoolActiveOrderReleaseApplicationDO incoming) {
        return existing != null
                && Objects.equals(existing.getActiveOrderId(), incoming.getActiveOrderId())
                && Objects.equals(existing.getBusinessIdempotencyKey(), incoming.getBusinessIdempotencyKey())
                && Objects.equals(existing.getSourceSnapshotHash(), incoming.getSourceSnapshotHash());
    }

    private String statusName(String status) {
        if (STATUS_PENDING_RELEASE_APPROVAL.equals(status)) {
            return "待生产负责人放行";
        }
        if (STATUS_BLOCKED.equals(status)) {
            return "资料生成阻塞";
        }
        return status;
    }
}
