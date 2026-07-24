package cn.iocoder.yudao.module.dcc.signature;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceBlocker;
import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceBlockerCode;
import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernancePermissionCode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_ACTION_UNDEFINED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_AUDIT_PERSIST_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_BLOCKED_PRECONDITION;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_CSV_APPROVAL_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_MODULE_ADAPTER_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_OPEN_REMEDIATION_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_OWNER_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_POLICY_SOURCE_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_RECOVERY_HASH_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.SIGNATURE_GOVERNANCE_RETENTION_PRECHECK_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureGovernanceSharedContractTest {

    @Test
    void errorCodes_areDccScopedAndDistinct() {
        List<ErrorCode> codes = List.of(
                SIGNATURE_GOVERNANCE_OWNER_MISSING,
                SIGNATURE_GOVERNANCE_POLICY_SOURCE_MISSING,
                SIGNATURE_GOVERNANCE_RETENTION_PRECHECK_FAILED,
                SIGNATURE_GOVERNANCE_RECOVERY_HASH_MISMATCH,
                SIGNATURE_GOVERNANCE_OPEN_REMEDIATION_EXISTS,
                SIGNATURE_GOVERNANCE_CSV_APPROVAL_MISSING,
                SIGNATURE_GOVERNANCE_MODULE_ADAPTER_MISSING,
                SIGNATURE_GOVERNANCE_AUDIT_PERSIST_FAILED,
                SIGNATURE_GOVERNANCE_BLOCKED_PRECONDITION,
                SIGNATURE_GOVERNANCE_ACTION_UNDEFINED);

        assertEquals(codes.size(), codes.stream().map(ErrorCode::getCode).collect(Collectors.toSet()).size());
        assertTrue(codes.stream().allMatch(code -> code.getCode() >= 1_080_000_094
                && code.getCode() <= 1_080_000_103));
        assertTrue(codes.stream().allMatch(code -> code.getMsg().contains("Signature governance")));
    }

    @Test
    void moduleCodes_coverAllSignatureSources() {
        Set<String> moduleCodes = Arrays.stream(SignatureGovernanceModuleCode.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(Set.of("DCC", "EDHR", "SHOWROOM", "INTAUTH"), moduleCodes);
    }

    @Test
    void blocker_requiresCodeMessageAndImpact() {
        SignatureGovernanceBlocker blocker = SignatureGovernanceBlocker.of(
                SignatureGovernanceBlockerCode.OBJECT_LOCK_MISSING,
                "Object Lock is required",
                "Long-term retention cannot be enabled");

        assertEquals(SignatureGovernanceBlockerCode.OBJECT_LOCK_MISSING, blocker.getCode());
        assertEquals("Object Lock is required", blocker.getMessage());
        assertEquals("Long-term retention cannot be enabled", blocker.getImpact());
        assertThrows(IllegalArgumentException.class,
                () -> SignatureGovernanceBlocker.of(null, "missing", "impact"));
        assertThrows(IllegalArgumentException.class,
                () -> SignatureGovernanceBlocker.of(SignatureGovernanceBlockerCode.OWNER_MISSING, "", "impact"));
        assertThrows(IllegalArgumentException.class,
                () -> SignatureGovernanceBlocker.of(SignatureGovernanceBlockerCode.OWNER_MISSING, "message", ""));
    }

    @Test
    void permissionCodes_areNamespacedForGovernance() {
        List<String> permissions = List.of(
                SignatureGovernancePermissionCode.RETENTION_QUERY,
                SignatureGovernancePermissionCode.RETENTION_MANAGE,
                SignatureGovernancePermissionCode.PERIODIC_REVIEW_QUERY,
                SignatureGovernancePermissionCode.PERIODIC_REVIEW_MANAGE,
                SignatureGovernancePermissionCode.CSV_PACKAGE_QUERY,
                SignatureGovernancePermissionCode.CSV_PACKAGE_MANAGE,
                SignatureGovernancePermissionCode.POLICY_QUERY,
                SignatureGovernancePermissionCode.POLICY_MANAGE);

        assertTrue(permissions.stream().allMatch(permission -> permission.startsWith("signature-governance:")));
        assertEquals(permissions.size(), permissions.stream().collect(Collectors.toSet()).size());
    }
}
