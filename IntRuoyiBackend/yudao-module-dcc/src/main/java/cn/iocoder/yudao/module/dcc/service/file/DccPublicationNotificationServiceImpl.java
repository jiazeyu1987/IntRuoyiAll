package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationFollowupBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationCandidateDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationDeliveryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationFollowupBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationCandidateMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationDeliveryMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_NOTIFICATION_CANDIDATE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_NOTIFICATION_MANAGE_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_NOTIFICATION_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_NOTIFICATION_REASON_REQUIRED;

@Service
public class DccPublicationNotificationServiceImpl implements DccPublicationNotificationService {
    private static final String TEMPLATE_CODE = "dcc_publication_released";
    @Resource private DccPublicationFollowupBatchMapper batchMapper;
    @Resource private DccPublicationNotificationCandidateMapper candidateMapper;
    @Resource private DccPublicationNotificationDeliveryMapper deliveryMapper;
    @Resource private DccPublicationNotificationAuditMapper auditMapper;
    @Resource private DccPublicationNotificationPostCommitScheduler scheduler;
    @Resource private DccPublicationNotificationDispatchOrchestrator orchestrator;
    @Resource private PermissionApi permissionApi;
    @Resource private DccPublicationFollowupStatusService statusService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void materializeForPublicationBatch(Long batchId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        DccPublicationFollowupBatchDO batch = batchMapper.selectById(batchId);
        if (batch == null || !tenantId.equals(batch.getTenantId())) throw exception(PUBLICATION_NOTIFICATION_NOT_EXISTS);
        java.util.List<DccPublicationNotificationCandidateDO> candidates =
                candidateMapper.selectListByBatchId(tenantId, batchId);
        if (candidates == null) {
            throw new IllegalStateException("publication notification candidates must not be null");
        }
        for (DccPublicationNotificationCandidateDO candidate : candidates) {
            if (!"ACTIVE".equals(candidate.getResolutionStatus())) continue;
            DccPublicationNotificationDeliveryDO row = DccPublicationNotificationDeliveryDO.builder()
                    .batchId(batchId).candidateId(candidate.getId()).userId(candidate.getUserId())
                    .businessKey("DCC_PUBLICATION:" + batchId + ":USER:" + candidate.getUserId())
                    .templateCode(TEMPLATE_CODE).status("PENDING").creationToken(UUID.randomUUID().toString()).build();
            row.setTenantId(tenantId);
            deliveryMapper.insertOrKeepExisting(row);
            DccPublicationNotificationDeliveryDO existing = deliveryMapper.selectByCandidateId(tenantId, candidate.getId());
            if (existing == null) throw new IllegalStateException("notification delivery missing after insert");
            requireMatchingIdentity(existing, row);
            if (row.getCreationToken().equals(existing.getCreationToken())) {
                DccPublicationNotificationAuditDO audit = DccPublicationNotificationAuditDO.builder()
                        .deliveryId(existing.getId()).batchId(batchId).actionType("MATERIALIZE")
                        .statusAfter(existing.getStatus()).attemptCount(0).rowVersionBefore(0).rowVersionAfter(0)
                        .occurredAt(LocalDateTime.now()).build();
                audit.setTenantId(tenantId);
                auditMapper.insert(audit);
            }
        }
        statusService.refreshBatchStatus(tenantId, batchId);
        scheduler.scheduleAfterCommit(tenantId, batchId);
    }

    @Override
    public void retryDelivery(Long actorId, Long deliveryId, Integer expectedVersion, String reason) {
        if (!permissionApi.hasAnyRoles(actorId, "doc_control")
                || !permissionApi.hasAnyPermissions(actorId, "dcc:controlled-file:approve")
                || !permissionApi.hasAnyPermissions(actorId,
                "dcc:controlled-file:publication-followup:manage")) {
            throw exception(PUBLICATION_NOTIFICATION_MANAGE_DENIED);
        }
        if (StrUtil.isBlank(reason)) throw exception(PUBLICATION_NOTIFICATION_REASON_REQUIRED);
        orchestrator.retryDelivery(TenantContextHolder.getRequiredTenantId(), actorId, deliveryId, expectedVersion, reason);
    }

    private void requireMatchingIdentity(DccPublicationNotificationDeliveryDO existing,
                                         DccPublicationNotificationDeliveryDO proposed) {
        if (!java.util.Objects.equals(existing.getTenantId(), proposed.getTenantId())
                || !java.util.Objects.equals(existing.getBatchId(), proposed.getBatchId())
                || !java.util.Objects.equals(existing.getCandidateId(), proposed.getCandidateId())
                || !java.util.Objects.equals(existing.getUserId(), proposed.getUserId())
                || !java.util.Objects.equals(existing.getBusinessKey(), proposed.getBusinessKey())
                || !java.util.Objects.equals(existing.getTemplateCode(), proposed.getTemplateCode())) {
            throw new IllegalStateException("Publication notification delivery identity conflict");
        }
    }
}
