package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.dept.PostService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SIGNATURE_NOT_AUTHORIZED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PASSWORD_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesProBatchRecordExecutionSignatureServiceTest extends BaseMockitoUnitTest {

    @Mock
    private AdminUserService adminUserService;
    @Mock
    private DccElectronicSignatureAuthorizationService authorizationService;
    @Mock
    private MesProBatchRecordExecutionSignatureMapper signatureMapper;
    @Mock
    private MesProcessPoolTeamEmployeeProfileMapper employeeProfileMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private DeptService deptService;
    @Mock
    private PostService postService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private RoleService roleService;

    @InjectMocks
    private MesProBatchRecordExecutionSignatureService signatureService;

    @Test
    void recordSubmitSignature_success() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            when(authorizationService.isElectronicSignatureEnabled(99L)).thenReturn(true);
            when(adminUserService.getUser(99L)).thenReturn(snapshotUser("签名人"));
            when(adminUserService.isPasswordMatch("secret", "encoded-password")).thenReturn(true);
            stubActorSnapshot();
            when(signatureMapper.insert(any(MesProBatchRecordExecutionSignatureDO.class))).thenReturn(1);

            signatureService.recordSubmitSignature(900L, "secret", "提交执行");

            ArgumentCaptor<MesProBatchRecordExecutionSignatureDO> captor =
                    ArgumentCaptor.forClass(MesProBatchRecordExecutionSignatureDO.class);
            verify(signatureMapper).insert(captor.capture());
            assertEquals(900L, captor.getValue().getExecutionId());
            assertEquals(99L, captor.getValue().getActorId());
            assertEquals("SUBMIT", captor.getValue().getActionType());
            assertEquals("PASSWORD", captor.getValue().getSignatureMode());
            assertEquals("提交执行", captor.getValue().getComment());
            assertEquals("operator", captor.getValue().getActorUsernameSnapshot());
            assertEquals("签名人", captor.getValue().getActorNicknameSnapshot());
            assertEquals(20L, captor.getValue().getActorDeptIdSnapshot());
            assertEquals("质量部", captor.getValue().getActorDeptNameSnapshot());
            assertEquals("QA岗位", captor.getValue().getActorPostNamesSnapshot());
            assertEquals("质量审核员", captor.getValue().getActorRoleNamesSnapshot());
            assertEquals("提交审批", captor.getValue().getSignaturePurpose());
            assertEquals("统一电子签名授权启用；系统角色/岗位快照已记录", captor.getValue().getAuthorizationBasis());
            assertEquals("PASSWORD", captor.getValue().getAuthenticationMethod());
            assertEquals("CAPTURED", captor.getValue().getSnapshotStatus());
            assertTrue(Boolean.TRUE.equals(captor.getValue().getPasswordVerified()));
        }
    }

    @Test
    void recordProductionSubmitSignature_usesSelectedEmployeeActorInsteadOfLoginUser() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9001L);
            when(authorizationService.isElectronicSignatureEnabled(9102L)).thenReturn(true);
            when(adminUserService.getUser(9102L)).thenReturn(AdminUserDO.builder()
                    .id(9102L)
                    .username("selected_employee")
                    .nickname("实际填写员工")
                    .deptId(20L)
                    .postIds(Set.of(30L))
                    .password("selected-password-hash")
                    .build());
            when(adminUserService.isPasswordMatch("selected-secret", "selected-password-hash")).thenReturn(true);
            stubActorSnapshotForUser(9102L);
            when(signatureMapper.insert(any(MesProBatchRecordExecutionSignatureDO.class))).thenReturn(1);

            signatureService.recordProductionSubmitSignature(9102L, "selected-secret", "一线生产报工提交");

            ArgumentCaptor<MesProBatchRecordExecutionSignatureDO> captor =
                    ArgumentCaptor.forClass(MesProBatchRecordExecutionSignatureDO.class);
            verify(signatureMapper).insert(captor.capture());
            assertEquals(9102L, captor.getValue().getActorId());
            assertEquals("PRODUCTION_SUBMIT", captor.getValue().getActionType());
            assertEquals("selected_employee", captor.getValue().getActorUsernameSnapshot());
            assertEquals("实际填写员工", captor.getValue().getActorNicknameSnapshot());
            assertEquals("一线生产报工提交", captor.getValue().getSignaturePurpose());
            verify(authorizationService).isElectronicSignatureEnabled(9102L);
            verify(adminUserService).getUser(9102L);
            verify(adminUserService, never()).getUser(9001L);
        }
    }

    @Test
    void recordProductionSubmitSignature_usesTemporaryEmployeeProfilePasswordHash() {
        when(adminUserService.getUser(8801L)).thenReturn(null);
        when(employeeProfileMapper.selectById(8801L)).thenReturn(MesProcessPoolTeamEmployeeProfileDO.builder()
                .id(8801L)
                .leaderUserId(3001L)
                .employeeCode("TMP-8801")
                .employeeName("临时工甲")
                .displayName("临时工甲")
                .employeeType("TEMPORARY")
                .signaturePasswordHash("bcrypt-temp-sign")
                .enabled(Boolean.TRUE)
                .build());
        when(passwordEncoder.matches("tmp-secret", "bcrypt-temp-sign")).thenReturn(true);
        when(signatureMapper.insert(any(MesProBatchRecordExecutionSignatureDO.class))).thenReturn(1);

        signatureService.recordProductionSubmitSignature(8801L, "tmp-secret", "一线生产报工提交");

        ArgumentCaptor<MesProBatchRecordExecutionSignatureDO> captor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionSignatureDO.class);
        verify(signatureMapper).insert(captor.capture());
        assertEquals(8801L, captor.getValue().getActorId());
        assertEquals("PRODUCTION_SUBMIT", captor.getValue().getActionType());
        assertEquals("TMP-8801", captor.getValue().getActorUsernameSnapshot());
        assertEquals("临时工甲", captor.getValue().getActorNicknameSnapshot());
        assertEquals("临时工甲", captor.getValue().getActorName());
        assertEquals("一线生产报工提交", captor.getValue().getSignaturePurpose());
        verify(passwordEncoder).matches("tmp-secret", "bcrypt-temp-sign");
        verify(authorizationService, never()).isElectronicSignatureEnabled(8801L);
        verify(adminUserService, never()).isPasswordMatch(any(), any());
    }

    @Test
    void recordSubmitSignature_withSelectedTimePersistsDualTimeAudit() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            when(authorizationService.isElectronicSignatureEnabled(99L)).thenReturn(true);
            when(adminUserService.getUser(99L)).thenReturn(snapshotUser("签名人"));
            when(adminUserService.isPasswordMatch("secret", "encoded-password")).thenReturn(true);
            stubActorSnapshot();
            when(signatureMapper.insert(any(MesProBatchRecordExecutionSignatureDO.class))).thenReturn(1);
            LocalDateTime selectedSignedAt = LocalDateTime.of(2026, 6, 15, 9, 5, 30);

            signatureService.recordSubmitSignature(900L, "secret", "提交执行",
                    new MesProBatchRecordExecutionSignatureTimeCommand()
                            .setSelectedSignedAt(selectedSignedAt)
                            .setSelectedTimeZone("Asia/Shanghai")
                            .setSelectedTimeReason("补录纸质记录签名时间"));

            ArgumentCaptor<MesProBatchRecordExecutionSignatureDO> captor =
                    ArgumentCaptor.forClass(MesProBatchRecordExecutionSignatureDO.class);
            verify(signatureMapper).insert(captor.capture());
            MesProBatchRecordExecutionSignatureDO signature = captor.getValue();
            assertEquals("SUBMIT", signature.getActionType());
            assertNotNull(signature.getSignedAt());
            assertEquals(selectedSignedAt, signature.getSelectedSignedAt());
            assertEquals(selectedSignedAt, signature.getSignatureDisplayAt());
            assertEquals("USER_SELECTED", signature.getSignatureTimeMode());
            assertEquals("Asia/Shanghai", signature.getSelectedTimeZone());
            assertEquals("补录纸质记录签名时间", signature.getSelectedTimeReason());
            assertEquals("EDHR_SIGNATURE_TIME_V1", signature.getSelectedTimePolicyVersion());
            assertNotNull(signature.getSelectedTimeAuditHash());
            assertTrue(signature.getSelectedTimeAuditHash().matches("[0-9a-f]{64}"));
        }
    }

    @Test
    void recordSubmitSignature_dynamicFillerWithoutStaticPostOrRoleStillPersistsAuditedSignature() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(810L);
            when(authorizationService.isElectronicSignatureEnabled(810L)).thenReturn(true);
            when(adminUserService.getUser(810L)).thenReturn(AdminUserDO.builder()
                    .id(810L)
                    .username("wangxin")
                    .nickname("王歆")
                    .deptId(166L)
                    .postIds(Set.of())
                    .password("encoded-password")
                    .build());
            when(adminUserService.isPasswordMatch("secret", "encoded-password")).thenReturn(true);
            DeptDO dept = new DeptDO();
            dept.setId(166L);
            dept.setName("璞润医疗");
            when(deptService.getDept(166L)).thenReturn(dept);
            when(permissionService.getUserRoleIdListByUserId(810L)).thenReturn(Set.of());
            when(signatureMapper.insert(any(MesProBatchRecordExecutionSignatureDO.class))).thenReturn(1);

            signatureService.recordSubmitSignature(784L, "secret", "提交执行");

            ArgumentCaptor<MesProBatchRecordExecutionSignatureDO> captor =
                    ArgumentCaptor.forClass(MesProBatchRecordExecutionSignatureDO.class);
            verify(signatureMapper).insert(captor.capture());
            MesProBatchRecordExecutionSignatureDO signature = captor.getValue();
            assertEquals(784L, signature.getExecutionId());
            assertEquals(810L, signature.getActorId());
            assertEquals("wangxin", signature.getActorUsernameSnapshot());
            assertEquals("王歆", signature.getActorNicknameSnapshot());
            assertEquals("璞润医疗", signature.getActorDeptNameSnapshot());
            assertNull(signature.getActorPostNamesSnapshot());
            assertNull(signature.getActorRoleNamesSnapshot());
            assertEquals("CAPTURED_PARTIAL_ORG", signature.getSnapshotStatus());
            assertEquals("统一电子签名授权启用；组织快照缺少岗位/角色配置", signature.getAuthorizationBasis());
        }
    }

    @Test
    void recordSubmitSignature_rejectsWhenSignatureAuthorizationDisabled() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            when(authorizationService.isElectronicSignatureEnabled(99L)).thenReturn(false);

            assertServiceException(() -> signatureService.recordSubmitSignature(900L, "secret", "提交执行"),
                    PRO_BATCH_RECORD_EXECUTION_SIGNATURE_NOT_AUTHORIZED);
            verify(adminUserService, never()).getUser(99L);
            verify(signatureMapper, never()).insert(any(MesProBatchRecordExecutionSignatureDO.class));
        }
    }

    @Test
    void recordSubmitSignature_rejectsWhenPasswordDoesNotMatch() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            when(authorizationService.isElectronicSignatureEnabled(99L)).thenReturn(true);
            when(adminUserService.getUser(99L)).thenReturn(AdminUserDO.builder()
                    .id(99L)
                    .password("encoded-password")
                    .build());
            when(adminUserService.isPasswordMatch("wrong", "encoded-password")).thenReturn(false);

            assertServiceException(() -> signatureService.recordSubmitSignature(900L, "wrong", "提交执行"),
                    PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PASSWORD_INVALID);
            verify(signatureMapper, never()).insert(any(MesProBatchRecordExecutionSignatureDO.class));
        }
    }

    @Test
    void recordSubmitSignature_insertFailure_isExplicit() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            when(authorizationService.isElectronicSignatureEnabled(99L)).thenReturn(true);
            when(adminUserService.getUser(99L)).thenReturn(snapshotUser("签名人"));
            when(adminUserService.isPasswordMatch(eq("secret"), eq("encoded-password"))).thenReturn(true);
            stubActorSnapshot();
            when(signatureMapper.insert(any(MesProBatchRecordExecutionSignatureDO.class))).thenReturn(0);

            assertServiceException(() -> signatureService.recordSubmitSignature(900L, "secret", "提交执行"),
                    PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        }
    }

    @Test
    void recordFieldChangeSignature_success() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            when(authorizationService.isElectronicSignatureEnabled(99L)).thenReturn(true);
            when(adminUserService.getUser(99L)).thenReturn(snapshotUser("QA"));
            when(adminUserService.isPasswordMatch("secret", "encoded-password")).thenReturn(true);
            stubActorSnapshot();
            when(signatureMapper.insert(any(MesProBatchRecordExecutionSignatureDO.class))).thenAnswer(invocation -> {
                MesProBatchRecordExecutionSignatureDO signature = invocation.getArgument(0);
                signature.setId(777L);
                return 1;
            });

            MesProBatchRecordExecutionFieldAuditSignatureResult result =
                    signatureService.recordFieldChangeSignature(new MesProBatchRecordExecutionFieldAuditSignatureCommand()
                            .setExecutionId(900L)
                            .setPassword("secret")
                            .setReasonCategory("CORRECTION")
                            .setReasonText("operator correction")
                            .setSignatureChallengeHash("a".repeat(64)));

            ArgumentCaptor<MesProBatchRecordExecutionSignatureDO> captor =
                    ArgumentCaptor.forClass(MesProBatchRecordExecutionSignatureDO.class);
            verify(signatureMapper).insert(captor.capture());
            assertEquals(777L, result.getSignatureId());
            assertEquals(99L, result.getActorId());
            assertEquals("QA", result.getActorName());
            assertEquals("FIELD_CHANGE", captor.getValue().getActionType());
            assertEquals("CORRECTION", captor.getValue().getReasonCategory());
            assertEquals("operator correction", captor.getValue().getReason());
            assertEquals("a".repeat(64), captor.getValue().getSignatureChallengeHash());
            assertEquals(0, captor.getValue().getSignedAt().getNano());
            assertEquals(captor.getValue().getSignedAt(), result.getSignedAt());
            assertEquals("SERVER_TIME", captor.getValue().getSignatureTimeMode());
            assertEquals("Asia/Shanghai", captor.getValue().getSelectedTimeZone());
            assertEquals("", captor.getValue().getSelectedTimeReason());
            assertEquals("EDHR_SIGNATURE_TIME_V1", captor.getValue().getSelectedTimePolicyVersion());
            assertNotNull(captor.getValue().getSelectedTimeAuditHash());
            assertTrue(captor.getValue().getSelectedTimeAuditHash().matches("[0-9a-f]{64}"));
            assertEquals(captor.getValue().getSignatureDisplayAt(), result.getSignatureDisplayAt());
            assertEquals(captor.getValue().getSignatureTimeMode(), result.getSignatureTimeMode());
            assertEquals(captor.getValue().getSelectedTimeZone(), result.getSelectedTimeZone());
            assertEquals(captor.getValue().getSelectedTimeReason(), result.getSelectedTimeReason());
            assertEquals(captor.getValue().getSelectedTimePolicyVersion(), result.getSelectedTimePolicyVersion());
            assertEquals(captor.getValue().getSelectedTimeAuditHash(), result.getSelectedTimeAuditHash());
            assertTrue(Boolean.TRUE.equals(captor.getValue().getPasswordVerified()));
        }
    }

    @Test
    void recordFormReviewSignature_success() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            when(authorizationService.isElectronicSignatureEnabled(99L)).thenReturn(true);
            when(adminUserService.getUser(99L)).thenReturn(snapshotUser("复核人"));
            when(adminUserService.isPasswordMatch("review-secret", "encoded-password")).thenReturn(true);
            stubActorSnapshot();
            when(signatureMapper.insert(any(MesProBatchRecordExecutionSignatureDO.class))).thenAnswer(invocation -> {
                MesProBatchRecordExecutionSignatureDO signature = invocation.getArgument(0);
                signature.setId(778L);
                return 1;
            });

            Long signatureId = signatureService.recordFormReviewSignature(900L, "review-secret", "复核无异常",
                    2L, "c".repeat(64), "d".repeat(64));

            ArgumentCaptor<MesProBatchRecordExecutionSignatureDO> captor =
                    ArgumentCaptor.forClass(MesProBatchRecordExecutionSignatureDO.class);
            verify(signatureMapper).insert(captor.capture());
            assertEquals(778L, signatureId);
            assertEquals(900L, captor.getValue().getExecutionId());
            assertEquals(99L, captor.getValue().getActorId());
            assertEquals("复核人", captor.getValue().getActorName());
            assertEquals("FORM_REVIEW", captor.getValue().getActionType());
            assertEquals("PASSWORD", captor.getValue().getSignatureMode());
            assertEquals("复核无异常", captor.getValue().getComment());
            assertEquals("表单复核", captor.getValue().getSignaturePurpose());
            assertEquals("2", captor.getValue().getRecordVersionSnapshot());
            assertEquals("d".repeat(64), captor.getValue().getRecordHashSnapshot());
            assertEquals(2L, captor.getValue().getFieldAuditRevision());
            assertEquals("c".repeat(64), captor.getValue().getFieldAuditHeadHash());
            assertEquals("d".repeat(64), captor.getValue().getCellValuesHash());
            assertTrue(Boolean.TRUE.equals(captor.getValue().getPasswordVerified()));
        }
    }

    private AdminUserDO snapshotUser(String nickname) {
        return AdminUserDO.builder()
                .id(99L)
                .username("operator")
                .nickname(nickname)
                .deptId(20L)
                .postIds(Set.of(30L))
                .password("encoded-password")
                .build();
    }

    private void stubActorSnapshot() {
        stubActorSnapshotForUser(99L);
    }

    private void stubActorSnapshotForUser(Long actorId) {
        DeptDO dept = new DeptDO();
        dept.setId(20L);
        dept.setName("质量部");
        PostDO post = new PostDO();
        post.setId(30L);
        post.setName("QA岗位");
        RoleDO role = new RoleDO();
        role.setId(40L);
        role.setName("质量审核员");
        when(deptService.getDept(20L)).thenReturn(dept);
        when(postService.getPostList(Set.of(30L))).thenReturn(List.of(post));
        when(permissionService.getUserRoleIdListByUserId(actorId)).thenReturn(Set.of(40L));
        when(roleService.getRoleList(Set.of(40L))).thenReturn(List.of(role));
    }

    @Test
    void attachSubmitSignatureProcessInstance_updatesPersistedSubmitSignature() {
        when(signatureMapper.selectById(501L)).thenReturn(MesProBatchRecordExecutionSignatureDO.builder()
                .id(501L)
                .executionId(900L)
                .actionType("SUBMIT")
                .build());
        when(signatureMapper.updateById(any(MesProBatchRecordExecutionSignatureDO.class))).thenReturn(1);

        signatureService.attachSubmitSignatureProcessInstance(501L, 900L, "process-submit");

        ArgumentCaptor<MesProBatchRecordExecutionSignatureDO> captor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionSignatureDO.class);
        verify(signatureMapper).updateById(captor.capture());
        assertEquals(501L, captor.getValue().getId());
        assertEquals("process-submit", captor.getValue().getProcessInstanceId());
    }

    @Test
    void attachSubmitSignatureProcessInstance_mismatchFailsFastWithoutUpdate() {
        when(signatureMapper.selectById(501L)).thenReturn(MesProBatchRecordExecutionSignatureDO.builder()
                .id(501L)
                .executionId(901L)
                .actionType("SUBMIT")
                .build());

        assertServiceException(() -> signatureService.attachSubmitSignatureProcessInstance(501L, 900L, "process-submit"),
                PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
        verify(signatureMapper, never()).updateById(any(MesProBatchRecordExecutionSignatureDO.class));
    }

    @Test
    void attachFieldChangeSignature_updatesFieldAuditBinding() {
        when(signatureMapper.selectById(777L)).thenReturn(MesProBatchRecordExecutionSignatureDO.builder()
                .id(777L)
                .executionId(900L)
                .actionType("FIELD_CHANGE")
                .build());
        when(signatureMapper.updateById(any(MesProBatchRecordExecutionSignatureDO.class))).thenReturn(1);

        signatureService.attachFieldChangeSignature(new MesProBatchRecordExecutionFieldAuditSignatureAttachCommand()
                .setSignatureId(777L)
                .setExecutionId(900L)
                .setAuditBatchId(888L)
                .setSignatureChallengeHash("b".repeat(64))
                .setFieldAuditRevision(2L)
                .setFieldAuditHeadHash("c".repeat(64))
                .setCellValuesHash("d".repeat(64)));

        ArgumentCaptor<MesProBatchRecordExecutionSignatureDO> captor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionSignatureDO.class);
        verify(signatureMapper).updateById(captor.capture());
        assertEquals(777L, captor.getValue().getId());
        assertEquals(888L, captor.getValue().getAuditBatchId());
        assertEquals("b".repeat(64), captor.getValue().getSignatureChallengeHash());
        assertEquals(2L, captor.getValue().getFieldAuditRevision());
        assertEquals("c".repeat(64), captor.getValue().getFieldAuditHeadHash());
        assertEquals("d".repeat(64), captor.getValue().getCellValuesHash());
    }
}
