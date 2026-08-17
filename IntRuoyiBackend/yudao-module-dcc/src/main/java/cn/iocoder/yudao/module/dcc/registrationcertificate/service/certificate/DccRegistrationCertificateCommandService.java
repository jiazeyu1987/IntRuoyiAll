package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAuditDO;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.function.Supplier;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_KEY_REQUIRED;

@Service
public class DccRegistrationCertificateCommandService {

    private final DccRegistrationCertificateCommandTransactionService transactionService;
    private final DccRegistrationCertificateFailureAuditService failureAuditService;
    private final DccRegistrationCertificateTerminalAuditService terminalAuditService;
    private final DccRegistrationCertificateCommandMutex commandMutex;

    public DccRegistrationCertificateCommandService(
            DccRegistrationCertificateCommandTransactionService transactionService,
            DccRegistrationCertificateFailureAuditService failureAuditService,
            DccRegistrationCertificateTerminalAuditService terminalAuditService,
            DccRegistrationCertificateCommandMutex commandMutex) {
        this.transactionService = require(transactionService, "transactionService");
        this.failureAuditService = require(failureAuditService, "failureAuditService");
        this.terminalAuditService = require(terminalAuditService, "terminalAuditService");
        this.commandMutex = require(commandMutex, "commandMutex");
    }

    public Long createDraft(Long tenantId, Long actorId, String idempotencyKey, String requestTraceId,
                            DccRegistrationCertificateDraftData draft) {
        String kind = "DRAFT_CREATE";
        DccRegistrationCertificateCommandMetadata metadata = metadata(
                tenantId, actorId, idempotencyKey, requestTraceId, kind,
                DccRegistrationCertificateCommandFingerprint.draft(kind, null, null, null, draft));
        DccRegistrationCertificateCommandContext context =
                new DccRegistrationCertificateCommandContext(draft.ownerCompanyId(), null);
        return execute(metadata, context, () -> transactionService.createDraft(metadata, context, draft));
    }

    public Long updateDraft(Long tenantId, Long actorId, String idempotencyKey, String requestTraceId,
                            Long certificateId, Integer expectedRowVersion, Integer expectedSnapshotRevision,
                            DccRegistrationCertificateDraftData draft) {
        String kind = "DRAFT_UPDATE";
        DccRegistrationCertificateCommandMetadata metadata = metadata(
                tenantId, actorId, idempotencyKey, requestTraceId, kind,
                DccRegistrationCertificateCommandFingerprint.draft(
                        kind, certificateId, expectedRowVersion, expectedSnapshotRevision, draft));
        DccRegistrationCertificateCommandContext context =
                new DccRegistrationCertificateCommandContext(draft.ownerCompanyId(), certificateId);
        return execute(metadata, context, () -> transactionService.updateDraft(
                metadata, context, certificateId, expectedRowVersion, expectedSnapshotRevision, draft));
    }

    public Long deleteDraft(Long tenantId, Long actorId, String idempotencyKey, String requestTraceId,
                            Long certificateId, Integer expectedRowVersion, Integer expectedSnapshotRevision) {
        String kind = "DRAFT_DELETE";
        DccRegistrationCertificateCommandMetadata metadata = metadata(
                tenantId, actorId, idempotencyKey, requestTraceId, kind,
                DccRegistrationCertificateCommandFingerprint.delete(
                        certificateId, expectedRowVersion, expectedSnapshotRevision));
        DccRegistrationCertificateCommandContext context =
                new DccRegistrationCertificateCommandContext(null, certificateId);
        return execute(metadata, context, () -> transactionService.deleteDraft(
                metadata, context, certificateId, expectedRowVersion, expectedSnapshotRevision));
    }

    public Long formalize(Long tenantId, Long actorId, String idempotencyKey, String requestTraceId,
                          Long certificateId, Integer expectedRowVersion, Integer expectedSnapshotRevision,
                          Long businessFileId) {
        String kind = "FORMALIZE";
        DccRegistrationCertificateCommandMetadata metadata = metadata(
                tenantId, actorId, idempotencyKey, requestTraceId, kind,
                DccRegistrationCertificateCommandFingerprint.formalize(
                        certificateId, expectedRowVersion, expectedSnapshotRevision, businessFileId));
        DccRegistrationCertificateCommandContext context =
                new DccRegistrationCertificateCommandContext(null, certificateId);
        return execute(metadata, context, () -> transactionService.formalize(
                metadata, context, certificateId, expectedRowVersion, expectedSnapshotRevision, businessFileId));
    }

    private Long execute(DccRegistrationCertificateCommandMetadata metadata,
                         DccRegistrationCertificateCommandContext context,
                         Supplier<Long> operation) {
        return commandMutex.execute(metadata.tenantId() + ":" + metadata.idempotencyKey(), () -> {
            DccRegistrationCertificateAuditDO existing = terminalAuditService.find(
                    metadata.tenantId(), metadata.idempotencyKey());
            if (existing != null) {
                return replay(metadata, existing);
            }
            try {
                return operation.get();
            } catch (RuntimeException original) {
                ServiceException stable = stabilize(original);
                try {
                    failureAuditService.recordFailure(
                            metadata, context, stable.getCode(), stable.getMessage());
                } catch (RuntimeException auditFailure) {
                    auditFailure.addSuppressed(stable);
                    throw auditFailure;
                }
                throw stable;
            }
        });
    }

    private Long replay(DccRegistrationCertificateCommandMetadata metadata,
                        DccRegistrationCertificateAuditDO existing) {
        DccRegistrationCertificateAuditDetail detail;
        try {
            detail = JsonUtils.parseObject(existing.getDetailJson(), DccRegistrationCertificateAuditDetail.class);
        } catch (RuntimeException exception) {
            ServiceException conflict = new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
            conflict.initCause(exception);
            throw conflict;
        }
        if (detail == null || !Objects.equals(existing.getActorId(), metadata.actorId())
                || !Objects.equals(detail.getActorId(), metadata.actorId())
                || !Objects.equals(detail.getCommandKind(), metadata.commandKind())
                || !Objects.equals(detail.getPayloadHash(), metadata.payloadHash())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
        }
        if ("SUCCESS".equals(existing.getResult()) && detail.getOutcomeCertificateId() != null) {
            return detail.getOutcomeCertificateId();
        }
        if ("FAILURE".equals(existing.getResult()) && detail.getFailureCode() != null
                && detail.getFailureMessage() != null) {
            throw new ServiceException(detail.getFailureCode(), detail.getFailureMessage());
        }
        throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
    }

    private DccRegistrationCertificateCommandMetadata metadata(
            Long tenantId, Long actorId, String idempotencyKey, String requestTraceId,
            String kind, String payloadHash) {
        if (tenantId == null || tenantId <= 0 || actorId == null || actorId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        }
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()
                || idempotencyKey.trim().length() > 256) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_KEY_REQUIRED);
        }
        if (requestTraceId == null || requestTraceId.trim().isEmpty()
                || requestTraceId.trim().length() > 128) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        }
        return new DccRegistrationCertificateCommandMetadata(
                tenantId, actorId, idempotencyKey.trim(), requestTraceId.trim(), kind, payloadHash);
    }

    private static ServiceException stabilize(RuntimeException exception) {
        if (exception instanceof ServiceException serviceException) {
            return serviceException;
        }
        ServiceException stable = new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        stable.initCause(exception);
        return stable;
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }
}
