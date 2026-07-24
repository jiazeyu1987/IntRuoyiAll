package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.dept.PostService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_LOCKED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_NOT_AUTHORIZED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TASK_PASSWORD_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileSignatureServiceTest extends BaseMockitoUnitTest {

    @Mock
    private AdminUserService adminUserService;
    @Mock
    private DccControlledFileSignatureMapper signatureMapper;
    @Mock
    private DccElectronicSignatureAuthorizationService electronicSignatureAuthorizationService;
    @Mock
    private DccElectronicSignatureFailureAuditService electronicSignatureFailureAuditService;
    @Mock
    private DccControlledFileSignatureEvidenceService signatureEvidenceService;
    @Mock
    private DccElectronicSignatureImageService signatureImageService;
    @Mock
    private DeptService deptService;
    @Mock
    private PostService postService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private RoleService roleService;

    @InjectMocks
    private DccSignatureVerificationServiceImpl signatureVerificationService;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void verifyPasswordAndCreateSignature_success() {
        TenantContextHolder.setTenantId(1L);
        when(adminUserService.getUser(99L)).thenReturn(snapshotUser(20L, "审核员"));
        when(adminUserService.isPasswordMatch("secret", "encoded-password")).thenReturn(true);
        stubActorSnapshot(true);
        when(signatureEvidenceService.createEvidence(org.mockito.ArgumentMatchers.any(DccControlledFileSignatureEvidenceCreateReq.class)))
                .thenReturn(signatureEvidence());
        when(signatureMapper.insert(org.mockito.ArgumentMatchers.any(DccControlledFileSignatureDO.class))).thenReturn(1);

        signatureVerificationService.verifyPasswordAndCreateSignature(99L, 900L, "task-1",
                "MATRIX_REVIEW", "APPROVE", "secret", "looks good");

        ArgumentCaptor<DccControlledFileSignatureEvidenceCreateReq> evidenceCaptor =
                ArgumentCaptor.forClass(DccControlledFileSignatureEvidenceCreateReq.class);
        verify(signatureEvidenceService).createEvidence(evidenceCaptor.capture());
        assertEquals(1L, evidenceCaptor.getValue().getTenantId());
        assertEquals(900L, evidenceCaptor.getValue().getControlledFileId());
        assertEquals("task-1", evidenceCaptor.getValue().getTaskId());
        assertEquals("APPROVED", evidenceCaptor.getValue().getTaskActionResult());
        assertEquals("MATRIX_REVIEW_APPROVE", evidenceCaptor.getValue().getMeaningCode());
        assertEquals(99L, evidenceCaptor.getValue().getSignerUserId());
        assertEquals("auditor", evidenceCaptor.getValue().getSignerUsername());
        assertEquals("审核员", evidenceCaptor.getValue().getSignerNickname());
        assertEquals(20L, evidenceCaptor.getValue().getSignerDeptId());
        assertEquals("质量部", evidenceCaptor.getValue().getSignerDeptName());
        assertEquals("QA岗位", evidenceCaptor.getValue().getSignerPostNames());
        assertEquals("质量审核员", evidenceCaptor.getValue().getSignerRoleNames());
        assertEquals("MATRIX_REVIEW_APPROVE", evidenceCaptor.getValue().getSignaturePurpose());
        assertEquals("DCC电子签名授权启用；系统角色/岗位快照已记录", evidenceCaptor.getValue().getAuthorizationBasis());
        assertEquals("PASSWORD", evidenceCaptor.getValue().getAuthenticationMethod());
        assertEquals("looks good", evidenceCaptor.getValue().getReasonText());
        assertEquals(501L, evidenceCaptor.getValue().getSignatureImageId());
        assertEquals(3, evidenceCaptor.getValue().getSignatureImageVersionNo());
        assertEquals(1501L, evidenceCaptor.getValue().getSignatureImageFileId());
        assertEquals("image-sha256", evidenceCaptor.getValue().getSignatureImageSha256());

        ArgumentCaptor<DccControlledFileSignatureDO> signatureCaptor = ArgumentCaptor.forClass(DccControlledFileSignatureDO.class);
        verify(signatureMapper).insert(signatureCaptor.capture());
        assertEquals(900L, signatureCaptor.getValue().getControlledFileId());
        assertEquals(901L, signatureCaptor.getValue().getRevisionId());
        assertEquals("A.1", signatureCaptor.getValue().getVersionNo());
        assertEquals("task-1", signatureCaptor.getValue().getTaskId());
        assertEquals(99L, signatureCaptor.getValue().getActorId());
        assertEquals("auditor", signatureCaptor.getValue().getActorUsernameSnapshot());
        assertEquals("审核员", signatureCaptor.getValue().getActorNicknameSnapshot());
        assertEquals(20L, signatureCaptor.getValue().getActorDeptIdSnapshot());
        assertEquals("质量部", signatureCaptor.getValue().getActorDeptNameSnapshot());
        assertEquals("QA岗位", signatureCaptor.getValue().getActorPostNamesSnapshot());
        assertEquals("质量审核员", signatureCaptor.getValue().getActorRoleNamesSnapshot());
        assertEquals("MATRIX_REVIEW_APPROVE", signatureCaptor.getValue().getSignaturePurpose());
        assertEquals("DCC电子签名授权启用；系统角色/岗位快照已记录", signatureCaptor.getValue().getAuthorizationBasis());
        assertEquals("PASSWORD", signatureCaptor.getValue().getAuthenticationMethod());
        assertEquals("A.1", signatureCaptor.getValue().getRecordVersionSnapshot());
        assertEquals("0e7b12ca44fe", signatureCaptor.getValue().getRecordHashSnapshot());
        assertEquals("CAPTURED", signatureCaptor.getValue().getSnapshotStatus());
        assertEquals("APPROVE", signatureCaptor.getValue().getActionType());
        assertEquals("MATRIX_REVIEW_APPROVE", signatureCaptor.getValue().getMeaningCode());
        assertEquals("looks good", signatureCaptor.getValue().getComment());
        assertTrue(Boolean.TRUE.equals(signatureCaptor.getValue().getPasswordVerified()));
        assertEquals("0e7b12ca44fe", signatureCaptor.getValue().getSourceFileHash());
        assertEquals(501L, signatureCaptor.getValue().getSignatureImageId());
        assertEquals(3, signatureCaptor.getValue().getSignatureImageVersionNo());
        assertEquals(1501L, signatureCaptor.getValue().getSignatureImageFileId());
        assertEquals("image-sha256", signatureCaptor.getValue().getSignatureImageSha256());
        assertEquals("VALID", signatureCaptor.getValue().getSignatureImageVerifiedStatus());
        assertEquals("VALID", signatureCaptor.getValue().getEvidenceStatus());
        verify(signatureImageService).markReferenced(501L);
    }

    @Test
    void verifyPasswordAndCreateSignature_derivesDistinctMeaningCodeFromStageAndAction() {
        TenantContextHolder.setTenantId(1L);
        when(adminUserService.getUser(99L)).thenReturn(snapshotUser(20L, "审核员"));
        when(adminUserService.isPasswordMatch("secret", "encoded-password")).thenReturn(true);
        stubActorSnapshot(true);
        when(signatureEvidenceService.createEvidence(org.mockito.ArgumentMatchers.any(DccControlledFileSignatureEvidenceCreateReq.class)))
                .thenReturn(signatureEvidence());
        when(signatureMapper.insert(org.mockito.ArgumentMatchers.any(DccControlledFileSignatureDO.class))).thenReturn(1);

        signatureVerificationService.verifyPasswordAndCreateSignature(99L, 900L, "task-review",
                "MATRIX_REVIEW", "APPROVE", "secret", "review approved");
        signatureVerificationService.verifyPasswordAndCreateSignature(99L, 900L, "task-approve",
                "MATRIX_APPROVAL", "APPROVE", "secret", "approval approved");

        ArgumentCaptor<DccControlledFileSignatureEvidenceCreateReq> evidenceCaptor =
                ArgumentCaptor.forClass(DccControlledFileSignatureEvidenceCreateReq.class);
        verify(signatureEvidenceService, org.mockito.Mockito.times(2)).createEvidence(evidenceCaptor.capture());
        assertEquals("MATRIX_REVIEW_APPROVE", evidenceCaptor.getAllValues().get(0).getMeaningCode());
        assertEquals("MATRIX_APPROVAL_APPROVE", evidenceCaptor.getAllValues().get(1).getMeaningCode());
        assertNotEquals(evidenceCaptor.getAllValues().get(0).getMeaningCode(),
                evidenceCaptor.getAllValues().get(1).getMeaningCode());
    }

    @Test
    void verifyPasswordAndCreateSignature_applicantReworkApproveUsesApplicantReworkMeaningCode() {
        TenantContextHolder.setTenantId(1L);
        when(adminUserService.getUser(99L)).thenReturn(snapshotUser(20L, "申请人"));
        when(adminUserService.isPasswordMatch("secret", "encoded-password")).thenReturn(true);
        stubActorSnapshot(true);
        when(signatureEvidenceService.createEvidence(org.mockito.ArgumentMatchers.any(DccControlledFileSignatureEvidenceCreateReq.class)))
                .thenReturn(signatureEvidence());
        when(signatureMapper.insert(org.mockito.ArgumentMatchers.any(DccControlledFileSignatureDO.class))).thenReturn(1);

        signatureVerificationService.verifyPasswordAndCreateSignature(99L, 900L, "task-applicant",
                "APPLICANT_REWORK", "APPROVE", "secret", "已修改，继续原流程");

        ArgumentCaptor<DccControlledFileSignatureEvidenceCreateReq> evidenceCaptor =
                ArgumentCaptor.forClass(DccControlledFileSignatureEvidenceCreateReq.class);
        verify(signatureEvidenceService).createEvidence(evidenceCaptor.capture());
        assertEquals("APPROVED", evidenceCaptor.getValue().getTaskActionResult());
        assertEquals("APPLICANT_REWORK_APPROVE", evidenceCaptor.getValue().getMeaningCode());

        ArgumentCaptor<DccControlledFileSignatureDO> signatureCaptor =
                ArgumentCaptor.forClass(DccControlledFileSignatureDO.class);
        verify(signatureMapper).insert(signatureCaptor.capture());
        assertEquals("APPROVE", signatureCaptor.getValue().getActionType());
        assertEquals("APPLICANT_REWORK_APPROVE", signatureCaptor.getValue().getMeaningCode());
    }

    @Test
    void verifyPasswordAndCreateSignature_derivesDistributionReceiptMeaningCodeStrictly() {
        TenantContextHolder.setTenantId(1L);
        when(adminUserService.getUser(99L)).thenReturn(snapshotUser(20L, "审核员"));
        when(adminUserService.isPasswordMatch("secret", "encoded-password")).thenReturn(true);
        stubActorSnapshot(true);
        when(signatureEvidenceService.createEvidence(org.mockito.ArgumentMatchers.any(DccControlledFileSignatureEvidenceCreateReq.class)))
                .thenReturn(signatureEvidence());
        when(signatureMapper.insert(org.mockito.ArgumentMatchers.any(DccControlledFileSignatureDO.class))).thenReturn(1);

        signatureVerificationService.verifyPasswordAndCreateSignature(99L, 900L, "DISTRIBUTION:301:501",
                "DISTRIBUTION", "DISTRIBUTION_ACK", "secret", "ack");
        signatureVerificationService.verifyPasswordAndCreateSignature(99L, 900L, "DISTRIBUTION_SIGN:301:501",
                "DISTRIBUTION", "DISTRIBUTION_SIGN", "secret", "sign");

        ArgumentCaptor<DccControlledFileSignatureEvidenceCreateReq> evidenceCaptor =
                ArgumentCaptor.forClass(DccControlledFileSignatureEvidenceCreateReq.class);
        verify(signatureEvidenceService, org.mockito.Mockito.times(2)).createEvidence(evidenceCaptor.capture());
        assertEquals("DISTRIBUTION_ACK", evidenceCaptor.getAllValues().get(0).getTaskActionResult());
        assertEquals("DISTRIBUTION_ACK", evidenceCaptor.getAllValues().get(0).getMeaningCode());
        assertEquals("DISTRIBUTION_SIGN", evidenceCaptor.getAllValues().get(1).getTaskActionResult());
        assertEquals("DISTRIBUTION_SIGN", evidenceCaptor.getAllValues().get(1).getMeaningCode());
    }

    @Test
    void verifyPasswordAndCreateSignature_allowsNullDeptSnapshot() {
        TenantContextHolder.setTenantId(1L);
        when(adminUserService.getUser(99L)).thenReturn(AdminUserDO.builder()
                .id(99L)
                .username("auditor")
                .nickname("Auditor")
                .deptId(null)
                .postIds(Set.of(30L))
                .password("encoded-password")
                .build());
        when(adminUserService.isPasswordMatch("secret", "encoded-password")).thenReturn(true);
        stubActorSnapshot(false);
        when(signatureEvidenceService.createEvidence(org.mockito.ArgumentMatchers.any(DccControlledFileSignatureEvidenceCreateReq.class)))
                .thenReturn(signatureEvidence());
        when(signatureMapper.insert(org.mockito.ArgumentMatchers.any(DccControlledFileSignatureDO.class))).thenReturn(1);

        signatureVerificationService.verifyPasswordAndCreateSignature(99L, 900L, "task-1",
                "MATRIX_REVIEW", "APPROVE", "secret", "looks good");

        ArgumentCaptor<DccControlledFileSignatureEvidenceCreateReq> evidenceCaptor =
                ArgumentCaptor.forClass(DccControlledFileSignatureEvidenceCreateReq.class);
        verify(signatureEvidenceService).createEvidence(evidenceCaptor.capture());
        assertEquals(99L, evidenceCaptor.getValue().getSignerUserId());
        assertNull(evidenceCaptor.getValue().getSignerDeptId());
        assertNull(evidenceCaptor.getValue().getSignerDeptName());
        assertEquals("QA岗位", evidenceCaptor.getValue().getSignerPostNames());
        assertEquals("质量审核员", evidenceCaptor.getValue().getSignerRoleNames());

        ArgumentCaptor<DccControlledFileSignatureDO> signatureCaptor =
                ArgumentCaptor.forClass(DccControlledFileSignatureDO.class);
        verify(signatureMapper).insert(signatureCaptor.capture());
        assertEquals(99L, signatureCaptor.getValue().getActorId());
        assertNull(signatureCaptor.getValue().getActorDeptIdSnapshot());
        assertEquals("VALID", signatureCaptor.getValue().getEvidenceStatus());
    }

    @Test
    void verifyPasswordAndCreateSignature_wrongPassword_rejects() {
        when(adminUserService.getUser(99L)).thenReturn(AdminUserDO.builder()
                .id(99L)
                .password("encoded-password")
                .build());
        when(adminUserService.isPasswordMatch("wrong", "encoded-password")).thenReturn(false);

        assertServiceException(() -> signatureVerificationService.verifyPasswordAndCreateSignature(
                99L, 900L, "task-1", "MATRIX_REVIEW", "APPROVE", "wrong", "no"),
                CONTROLLED_FILE_TASK_PASSWORD_INVALID);

        ArgumentCaptor<DccElectronicSignatureFailureAuditCommand> failureCaptor =
                ArgumentCaptor.forClass(DccElectronicSignatureFailureAuditCommand.class);
        verify(electronicSignatureFailureAuditService).recordPasswordFailure(failureCaptor.capture());
        assertEquals(99L, failureCaptor.getValue().getTargetUserId());
        assertEquals(900L, failureCaptor.getValue().getControlledFileId());
        assertEquals(900L, failureCaptor.getValue().getRevisionId());
        assertEquals("task-1", failureCaptor.getValue().getTaskId());
        assertEquals("APPROVED", failureCaptor.getValue().getActionType());
        assertEquals("MATRIX_REVIEW_APPROVE", failureCaptor.getValue().getMeaningCode());
        assertEquals("password verification failed", failureCaptor.getValue().getFailureMessage());
        verify(signatureMapper, never()).insert(org.mockito.ArgumentMatchers.any(DccControlledFileSignatureDO.class));
        verify(signatureEvidenceService, never()).createEvidence(
                org.mockito.ArgumentMatchers.any(DccControlledFileSignatureEvidenceCreateReq.class));
    }

    @Test
    void verifyPasswordAndCreateSignature_wrongPasswordThatReachesLockThreshold_usesLockedErrorCode() {
        when(adminUserService.getUser(99L)).thenReturn(AdminUserDO.builder()
                .id(99L)
                .password("encoded-password")
                .build());
        when(adminUserService.isPasswordMatch("wrong", "encoded-password")).thenReturn(false);
        when(electronicSignatureFailureAuditService.recordPasswordFailure(
                org.mockito.ArgumentMatchers.any(DccElectronicSignatureFailureAuditCommand.class))).thenReturn(true);

        assertServiceException(() -> signatureVerificationService.verifyPasswordAndCreateSignature(
                99L, 900L, "task-1", "MATRIX_REVIEW", "APPROVE", "wrong", "no"),
                CONTROLLED_FILE_SIGNATURE_LOCKED);

        ArgumentCaptor<DccElectronicSignatureFailureAuditCommand> failureCaptor =
                ArgumentCaptor.forClass(DccElectronicSignatureFailureAuditCommand.class);
        verify(electronicSignatureFailureAuditService).recordPasswordFailure(failureCaptor.capture());
        assertEquals(99L, failureCaptor.getValue().getTargetUserId());
        assertEquals(900L, failureCaptor.getValue().getControlledFileId());
        assertEquals("APPROVED", failureCaptor.getValue().getActionType());
        assertEquals("MATRIX_REVIEW_APPROVE", failureCaptor.getValue().getMeaningCode());
        verify(signatureMapper, never()).insert(org.mockito.ArgumentMatchers.any(DccControlledFileSignatureDO.class));
        verify(signatureEvidenceService, never()).createEvidence(
                org.mockito.ArgumentMatchers.any(DccControlledFileSignatureEvidenceCreateReq.class));
    }

    @Test
    void verifyPasswordAndCreateSignature_insertFailure_isExplicit() {
        TenantContextHolder.setTenantId(1L);
        when(adminUserService.getUser(99L)).thenReturn(snapshotUser(20L, "审核员"));
        when(adminUserService.isPasswordMatch(eq("secret"), eq("encoded-password"))).thenReturn(true);
        stubActorSnapshot(true);
        when(signatureEvidenceService.createEvidence(org.mockito.ArgumentMatchers.any(DccControlledFileSignatureEvidenceCreateReq.class)))
                .thenReturn(signatureEvidence());
        when(signatureMapper.insert(org.mockito.ArgumentMatchers.any(DccControlledFileSignatureDO.class))).thenReturn(0);

        assertServiceException(() -> signatureVerificationService.verifyPasswordAndCreateSignature(
                99L, 900L, "task-1", "DOC_CONTROL_APPROVAL", "REJECT", "secret", "fail"),
                CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED);
    }

    @Test
    void verifyPasswordAndCreateSignature_signatureAuthorizationDisabled_rejects() {
        doThrow(exception(CONTROLLED_FILE_SIGNATURE_DISABLED))
                .when(electronicSignatureAuthorizationService).validateElectronicSignatureEnabled(99L);

        assertServiceException(() -> signatureVerificationService.verifyPasswordAndCreateSignature(
                99L, 900L, "task-1", "MATRIX_REVIEW", "APPROVE", "secret", "no auth"),
                CONTROLLED_FILE_SIGNATURE_DISABLED);
        verify(adminUserService, never()).getUser(99L);
        verify(signatureMapper, never()).insert(org.mockito.ArgumentMatchers.any(DccControlledFileSignatureDO.class));
        verify(electronicSignatureFailureAuditService, never()).recordPasswordFailure(
                org.mockito.ArgumentMatchers.any(DccElectronicSignatureFailureAuditCommand.class));
        verify(signatureEvidenceService, never()).createEvidence(
                org.mockito.ArgumentMatchers.any(DccControlledFileSignatureEvidenceCreateReq.class));
    }

    @Test
    void verifyPasswordAndCreateSignature_lockedAuthorization_usesLockedErrorCode() {
        doThrow(exception(CONTROLLED_FILE_SIGNATURE_LOCKED))
                .when(electronicSignatureAuthorizationService).validateElectronicSignatureEnabled(99L);

        assertServiceException(() -> signatureVerificationService.verifyPasswordAndCreateSignature(
                99L, 900L, "task-1", "MATRIX_REVIEW", "APPROVE", "secret", "locked"),
                CONTROLLED_FILE_SIGNATURE_LOCKED);
        verify(adminUserService, never()).getUser(99L);
        verify(signatureMapper, never()).insert(org.mockito.ArgumentMatchers.any(DccControlledFileSignatureDO.class));
        verify(electronicSignatureFailureAuditService, never()).recordPasswordFailure(
                org.mockito.ArgumentMatchers.any(DccElectronicSignatureFailureAuditCommand.class));
        verify(signatureEvidenceService, never()).createEvidence(
                org.mockito.ArgumentMatchers.any(DccControlledFileSignatureEvidenceCreateReq.class));
    }

    private static DccControlledFileSignatureEvidence signatureEvidence() {
        return DccControlledFileSignatureEvidence.builder()
                .revisionId(901L)
                .versionNo("A.1")
                .sourceFileId(1001L)
                .sourceFileHash("0e7b12ca44fe")
                .sourceFileHashAlgorithm("SHA-256")
                .sourceFileHashStatus("BOUND")
                .controlledCopyHashStatus("NOT_APPLICABLE")
                .signatureImageId(501L)
                .signatureImageVersionNo(3)
                .signatureImageFileId(1501L)
                .signatureImageFileUrl("https://example.com/signature.png")
                .signatureImageSha256("image-sha256")
                .signatureImageContentType("image/png")
                .signatureImageFileSize(2048L)
                .signatureImageStatusSnapshot("ACTIVE")
                .signatureImageVerifiedStatus("VALID")
                .evidencePayloadVersion("v3-image")
                .evidenceKeyVersion("dcc-signature-2026-05")
                .evidenceHash("6f2c91ab03d4aabb")
                .evidenceHashAlgorithm("HMAC_SHA256")
                .evidenceStatus("VALID")
                .recordVersionSnapshot("A.1")
                .recordHashSnapshot("0e7b12ca44fe")
                .build();
    }

    private AdminUserDO snapshotUser(Long deptId, String nickname) {
        return AdminUserDO.builder()
                .id(99L)
                .username("auditor")
                .nickname(nickname)
                .deptId(deptId)
                .postIds(Set.of(30L))
                .password("encoded-password")
                .build();
    }

    private void stubActorSnapshot(boolean withDept) {
        if (withDept) {
            DeptDO dept = new DeptDO();
            dept.setId(20L);
            dept.setName("质量部");
            when(deptService.getDept(20L)).thenReturn(dept);
        }
        PostDO post = new PostDO();
        post.setId(30L);
        post.setName("QA岗位");
        RoleDO role = new RoleDO();
        role.setId(40L);
        role.setName("质量审核员");
        when(postService.getPostList(Set.of(30L))).thenReturn(List.of(post));
        when(permissionService.getUserRoleIdListByUserId(99L)).thenReturn(Set.of(40L));
        when(roleService.getRoleList(Set.of(40L))).thenReturn(List.of(role));
        when(signatureImageService.requireActiveSnapshot(99L)).thenReturn(signatureImageSnapshot());
    }

    private static DccElectronicSignatureImageSnapshot signatureImageSnapshot() {
        return DccElectronicSignatureImageSnapshot.builder()
                .imageId(501L)
                .versionNo(3)
                .fileId(1501L)
                .fileUrl("https://example.com/signature.png")
                .fileName("signature.png")
                .contentType("image/png")
                .fileSize(2048L)
                .sha256("image-sha256")
                .imageStatus("ACTIVE")
                .verifiedStatus("VALID")
                .build();
    }
}
