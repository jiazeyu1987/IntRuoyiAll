package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrContractErrorCodeConstants.PRO_EDHR_CONTRACT_AUDIT_EVENT_FIELD_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrContractErrorCodeConstants.PRO_EDHR_CONTRACT_EVIDENCE_HASH_INPUT_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrContractErrorCodeConstants.PRO_EDHR_CONTRACT_IDEMPOTENCY_KEY_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrContractErrorCodeConstants.PRO_EDHR_CONTRACT_IDEMPOTENCY_KEY_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrUnifiedContractTest {

    @Test
    void commonStatus_containsRequiredLifecycleStates() {
        assertEquals(
                Set.of("DRAFT", "PRECHECK_FAILED", "BLOCKED", "PENDING_APPROVAL", "COMPLETED", "VOIDED"),
                MesProEdhrCommonStatus.names());
        assertFalse(MesProEdhrCommonStatus.DRAFT.isTerminal());
        assertTrue(MesProEdhrCommonStatus.PRECHECK_FAILED.isBlocking());
        assertTrue(MesProEdhrCommonStatus.BLOCKED.isBlocking());
        assertTrue(MesProEdhrCommonStatus.COMPLETED.isTerminal());
        assertTrue(MesProEdhrCommonStatus.VOIDED.isTerminal());
    }

    @Test
    void idempotencyKey_requiredAndFormatChecked() {
        assertEquals(
                "init-import-20260618",
                MesProEdhrIdempotencySupport.requireIdempotencyKey(
                        " init-import-20260618 ",
                        MesProEdhrAuditAction.IMPORT.name()));

        ServiceException missing = assertThrows(ServiceException.class,
                () -> MesProEdhrIdempotencySupport.requireIdempotencyKey(" ", MesProEdhrAuditAction.IMPORT.name()));
        assertEquals(PRO_EDHR_CONTRACT_IDEMPOTENCY_KEY_REQUIRED.getCode(), missing.getCode());

        ServiceException invalid = assertThrows(ServiceException.class,
                () -> MesProEdhrIdempotencySupport.requireIdempotencyKey("bad key with space", MesProEdhrAuditAction.IMPORT.name()));
        assertEquals(PRO_EDHR_CONTRACT_IDEMPOTENCY_KEY_INVALID.getCode(), invalid.getCode());
    }

    @Test
    void evidenceHash_isStableSha256AndFailsOnMissingPayload() {
        String first = MesProEdhrEvidenceHashSupport.sha256(
                "INIT_BATCH",
                "batch-1001",
                "MANIFEST",
                "{\"file\":\"manifest.xlsx\",\"rows\":3}");
        String second = MesProEdhrEvidenceHashSupport.sha256(
                "INIT_BATCH",
                "batch-1001",
                "MANIFEST",
                "{\"file\":\"manifest.xlsx\",\"rows\":3}");

        assertEquals(first, second);
        assertEquals(64, first.length());
        assertTrue(first.matches("[0-9a-f]{64}"));

        ServiceException missing = assertThrows(ServiceException.class,
                () -> MesProEdhrEvidenceHashSupport.sha256("INIT_BATCH", "batch-1001", "MANIFEST", ""));
        assertEquals(PRO_EDHR_CONTRACT_EVIDENCE_HASH_INPUT_REQUIRED.getCode(), missing.getCode());
    }

    @Test
    void auditEvent_requiresTraceFieldsEvidenceHashAndIdempotencyKey() {
        String hash = MesProEdhrEvidenceHashSupport.sha256(
                "RELEASE",
                "release-1001",
                "PRECHECK",
                "{\"blocked\":true}");
        MesProEdhrAuditEventContract event = new MesProEdhrAuditEventContract()
                .setSourceModule("RELEASE")
                .setSourceObjectId("release-1001")
                .setAction(MesProEdhrAuditAction.GATE_CHECK.name())
                .setPermissionCode("mes:pro-edhr-release:precheck")
                .setResult(MesProEdhrAuditResult.BLOCKED.name())
                .setReason("缺偏差关闭证据")
                .setEvidenceHash(hash)
                .setIdempotencyKey("release-precheck-1001");

        assertSame(event, event.validateRequiredFields());

        ServiceException missingField = assertThrows(ServiceException.class,
                () -> new MesProEdhrAuditEventContract()
                        .setSourceModule("RELEASE")
                        .setSourceObjectId("release-1001")
                        .setAction(MesProEdhrAuditAction.GATE_CHECK.name())
                        .setPermissionCode("mes:pro-edhr-release:precheck")
                        .setResult(MesProEdhrAuditResult.BLOCKED.name())
                        .setReason("缺偏差关闭证据")
                        .setIdempotencyKey("release-precheck-1001")
                        .validateRequiredFields());
        assertEquals(PRO_EDHR_CONTRACT_AUDIT_EVENT_FIELD_REQUIRED.getCode(), missingField.getCode());
    }
}
