package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.Set;

@Service
public class DccControlledFileFinalizationFailureService {

    private static final Set<String> TERMINAL_SUCCESS_STATUSES = Set.of(
            DccControlledFileStatusEnum.ACTIVE.getStatus(),
            DccControlledFileStatusEnum.SUPERSEDED.getStatus(),
            DccControlledFileStatusEnum.OBSOLETE.getStatus());

    private final TransactionTemplate requiresNewTransaction;
    private final DccControlledFileMapper controlledFileMapper;
    private final DccControlledContentAdapter platformAdapter;

    public DccControlledFileFinalizationFailureService(PlatformTransactionManager transactionManager,
                                                       DccControlledFileMapper controlledFileMapper,
                                                       DccControlledContentAdapter platformAdapter) {
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.controlledFileMapper = controlledFileMapper;
        this.platformAdapter = platformAdapter;
    }

    public void recordFailure(Long tenantId, Long controlledFileId, String expectedStatus,
                              Long actorId, String reason, String eventKey) {
        requiresNewTransaction.executeWithoutResult(ignored -> {
            DccControlledFileDO current = controlledFileMapper.selectByIdAndTenantForUpdate(
                    tenantId, controlledFileId);
            if (current == null) {
                throw new IllegalStateException("Controlled file is missing while recording finalization failure");
            }
            if (DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(current.getStatus())
                    && !DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(expectedStatus)) {
                return;
            }
            if (TERMINAL_SUCCESS_STATUSES.contains(current.getStatus())) {
                return;
            }
            if (!Objects.equals(expectedStatus, current.getStatus())) {
                throw new IllegalStateException("Controlled file status changed before finalization failure recording: "
                        + current.getStatus());
            }
            recordFinalizationAttemptStarted(current, actorId, eventKey);
            int updated = controlledFileMapper.markFinalizationFailedWhenStatus(tenantId, controlledFileId,
                    expectedStatus, reason, actorId);
            if (updated != 1) {
                throw new IllegalStateException("Controlled file finalization failure status update lost its CAS");
            }
            current.setStatus(DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus());
            current.setFinalizationError(reason);
            platformAdapter.recordFinalizationFailed(current, actorId, reason, eventKey);
        });
    }

    private void recordFinalizationAttemptStarted(DccControlledFileDO file, Long actorId, String eventKey) {
        if (DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus().equals(file.getStatus())) {
            platformAdapter.recordPublishFinalizationStarted(file, actorId, eventKey);
            return;
        }
        if (DccControlledFileStatusEnum.FINALIZATION_FAILED.getStatus().equals(file.getStatus())) {
            platformAdapter.recordFinalizationRetried(file, actorId, eventKey);
            return;
        }
        platformAdapter.recordFinalizationStarted(file, actorId, eventKey);
    }
}
