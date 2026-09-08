package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureResult;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
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
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SIGNATURE_NOT_AUTHORIZED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PASSWORD_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_PASSWORD_FAILED;
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
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ElectronicSignatureService electronicSignatureService;

    @InjectMocks
    private MesProBatchRecordExecutionSignatureService signatureService;

    @Test
    void recordSubmitSignature_success() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            when(authorizationService.isElectronicSignatureEnabled(99L)).thenReturn(true);
            when(adminUserService.getUser(99L)).thenReturn(snapshotUser("签名人"));
            stubActorSnapshot();
            when(electronicSignatureService.sign(any(ElectronicSignatureCommand.class)))
                    .thenReturn(unifiedSignatureResult(7001L));

            Long signatureId = signatureService.recordSubmitSignature(900L, "secret", "提交执行");

            ArgumentCaptor<ElectronicSignatureCommand> captor =
                    ArgumentCaptor.forClass(ElectronicSignatureCommand.class);
            verify(adminUserApi).reauthenticateForSignature(99L, "secret");
            verify(electronicSignatureService).sign(captor.capture());
            assertEquals(7001L, signatureId);
            assertEquals("MES", captor.getValue().moduleCode());
            assertEquals("SUBMIT", captor.getValue().actionCode());
            assertEquals("MES_BATCH_RECORD", captor.getValue().subjectType());
            assertEquals("secret", captor.getValue().credential());
            assertEquals("提交执行", captor.getValue().reason());
            assertTrue(captor.getValue().idempotencyKey().startsWith("MES|99|SUBMIT|"));
            verify(signatureMapper, never()).insert(any(MesProBatchRecordExecutionSignatureDO.class));
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
            stubActorSnapshotForUser(9102L);
            when(electronicSignatureService.sign(any(ElectronicSignatureCommand.class)))
                    .thenReturn(unifiedSignatureResult(7102L));

            Long signatureId = signatureService.recordProductionSubmitSignature(9102L, "selected-secret", "一线生产报工提交");

            ArgumentCaptor<ElectronicSignatureCommand> captor =
                    ArgumentCaptor.forClass(ElectronicSignatureCommand.class);
            verify(adminUserApi).reauthenticateForSignature(9102L, "selected-secret");
            verify(electronicSignatureService).sign(captor.capture());
            assertEquals(7102L, signatureId);
            assertEquals("PRODUCTION_SUBMIT", captor.getValue().actionCode());
            assertEquals("一线生产报工提交", captor.getValue().reason());
            verify(authorizationService).isElectronicSignatureEnabled(9102L);
            verify(adminUserService).getUser(9102L);
            verify(adminUserService, never()).getUser(9001L);
            verify(signatureMapper, never()).insert(any(MesProBatchRecordExecutionSignatureDO.class));
        }
    }

    @Test
    void recordProductionSubmitSignature_rejectsTemporaryEmployeeProfilePasswordHash() {
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

        assertServiceException(() -> signatureService.recordProductionSubmitSignature(8801L, "tmp-secret", "一线生产报工提交"),
                PRO_BATCH_RECORD_EXECUTION_SIGNATURE_NOT_AUTHORIZED);

        verify(passwordEncoder, never()).matches("tmp-secret", "bcrypt-temp-sign");
        verify(authorizationService, never()).isElectronicSignatureEnabled(8801L);
        verify(electronicSignatureService, never()).sign(any(ElectronicSignatureCommand.class));
        verify(signatureMapper, never()).insert(any(MesProBatchRecordExecutionSignatureDO.class));
    }

    @Test
    void recordStage1SimulationSignature_persistsFormalLoginSessionSignatureRecord() {
        when(adminUserService.getUser(99L)).thenReturn(snapshotUser("签名人"));
        stubActorSnapshot();
        when(signatureMapper.insert(any(MesProBatchRecordExecutionSignatureDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProBatchRecordExecutionSignatureDO.class).setId(10001L);
            return 1;
        });

        Long signatureId = signatureService.recordStage1SimulationSignature(99L,
                MesProBatchRecordExecutionSignatureService.ACTION_PRODUCTION_SUBMIT, 8101L,
                "stage1", "run-001");

        assertEquals(10001L, signatureId);
        ArgumentCaptor<MesProBatchRecordExecutionSignatureDO> captor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionSignatureDO.class);
        verify(signatureMapper).insert(captor.capture());
        MesProBatchRecordExecutionSignatureDO signature = captor.getValue();
        assertEquals(0L, signature.getExecutionId());
        assertEquals(99L, signature.getActorId());
        assertEquals("PRODUCTION_SUBMIT", signature.getActionType());
        assertEquals("LOGIN_SESSION", signature.getSignatureMode());
        assertTrue(Boolean.FALSE.equals(signature.getPasswordVerified()));
        assertEquals("MES_ACTIVE_ORDER_SIMULATION", signature.getReviewSourceType());
        assertEquals(8101L, signature.getReviewSourceId());
        assertEquals("签名人", signature.getActorName());
        assertEquals("operator", signature.getActorUsernameSnapshot());
        assertEquals("签名人", signature.getActorNicknameSnapshot());
        assertEquals("一线生产报工提交", signature.getSignaturePurpose());
        assertEquals("LOGIN_SESSION", signature.getAuthenticationMethod());
        assertTrue(signature.getAuthorizationBasis().contains("Stage1模拟"));
        assertNotNull(signature.getSignedAt());
        verify(adminUserService, never()).isPasswordMatch(any(), any());
        verify(authorizationService, never()).isElectronicSignatureEnabled(any());
    }

    @Test
    void recordSubmitSignature_withSelectedTimePersistsDualTimeAudit() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            when(authorizationService.isElectronicSignatureEnabled(99L)).thenReturn(true);
            when(adminUserService.getUser(99L)).thenReturn(snapshotUser("签名人"));
            stubActorSnapshot();
            when(electronicSignatureService.sign(any(ElectronicSignatureCommand.class)))
                    .thenReturn(unifiedSignatureResult(7002L));
            LocalDateTime selectedSignedAt = LocalDateTime.of(2026, 6, 15, 9, 5, 30);

            Long signatureId = signatureService.recordSubmitSignature(900L, "secret", "提交执行",
                    new MesProBatchRecordExecutionSignatureTimeCommand()
                            .setSelectedSignedAt(selectedSignedAt)
                            .setSelectedTimeZone("Asia/Shanghai")
                            .setSelectedTimeReason("补录纸质记录签名时间"));

            ArgumentCaptor<ElectronicSignatureCommand> captor =
                    ArgumentCaptor.forClass(ElectronicSignatureCommand.class);
            verify(adminUserApi).reauthenticateForSignature(99L, "secret");
            verify(electronicSignatureService).sign(captor.capture());
            assertEquals(7002L, signatureId);
            assertEquals("SUBMIT", captor.getValue().actionCode());
            assertEquals("提交执行", captor.getValue().reason());
            verify(signatureMapper, never()).insert(any(MesProBatchRecordExecutionSignatureDO.class));
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
            DeptDO dept = new DeptDO();
            dept.setId(166L);
            dept.setName("璞润医疗");
            when(deptService.getDept(166L)).thenReturn(dept);
            when(permissionService.getUserRoleIdListByUserId(810L)).thenReturn(Set.of());
            when(electronicSignatureService.sign(any(ElectronicSignatureCommand.class)))
                    .thenReturn(unifiedSignatureResult(7003L));

            Long signatureId = signatureService.recordSubmitSignature(784L, "secret", "提交执行");

            ArgumentCaptor<ElectronicSignatureCommand> captor =
                    ArgumentCaptor.forClass(ElectronicSignatureCommand.class);
            verify(adminUserApi).reauthenticateForSignature(810L, "secret");
            verify(electronicSignatureService).sign(captor.capture());
            assertEquals(7003L, signatureId);
            assertEquals("SUBMIT", captor.getValue().actionCode());
            assertEquals("提交执行", captor.getValue().reason());
            verify(signatureMapper, never()).insert(any(MesProBatchRecordExecutionSignatureDO.class));
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
            org.mockito.Mockito.doThrow(exception(USER_PASSWORD_FAILED))
                    .when(adminUserApi).reauthenticateForSignature(99L, "wrong");

            assertServiceException(() -> signatureService.recordSubmitSignature(900L, "wrong", "提交执行"),
                    USER_PASSWORD_FAILED);
            verify(signatureMapper, never()).insert(any(MesProBatchRecordExecutionSignatureDO.class));
            verify(electronicSignatureService, never()).sign(any(ElectronicSignatureCommand.class));
        }
    }

    @Test
    void recordSubmitSignature_unifiedSignatureFailure_isExplicit() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            when(authorizationService.isElectronicSignatureEnabled(99L)).thenReturn(true);
            when(adminUserService.getUser(99L)).thenReturn(snapshotUser("签名人"));
            stubActorSnapshot();
            org.mockito.Mockito.doThrow(exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED))
                    .when(electronicSignatureService).sign(any(ElectronicSignatureCommand.class));

            assertServiceException(() -> signatureService.recordSubmitSignature(900L, "secret", "提交执行"),
                    PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED);
            verify(adminUserApi).reauthenticateForSignature(99L, "secret");
            verify(signatureMapper, never()).insert(any(MesProBatchRecordExecutionSignatureDO.class));
        }
    }

    @Test
    void recordFieldChangeSignature_success() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            when(authorizationService.isElectronicSignatureEnabled(99L)).thenReturn(true);
            when(adminUserService.getUser(99L)).thenReturn(snapshotUser("QA"));
            stubActorSnapshot();
            when(electronicSignatureService.sign(any(ElectronicSignatureCommand.class)))
                    .thenReturn(unifiedSignatureResult(777L));

            MesProBatchRecordExecutionFieldAuditSignatureResult result =
                    signatureService.recordFieldChangeSignature(new MesProBatchRecordExecutionFieldAuditSignatureCommand()
                            .setExecutionId(900L)
                            .setPassword("secret")
                            .setReasonCategory("CORRECTION")
                            .setReasonText("operator correction")
                            .setSignatureChallengeHash("a".repeat(64)));

            assertEquals(777L, result.getSignatureId());
            assertEquals(99L, result.getActorId());
            assertEquals("QA", result.getActorName());
            ArgumentCaptor<ElectronicSignatureCommand> captor =
                    ArgumentCaptor.forClass(ElectronicSignatureCommand.class);
            verify(adminUserApi).reauthenticateForSignature(99L, "secret");
            verify(electronicSignatureService).sign(captor.capture());
            assertEquals("FIELD_CHANGE", captor.getValue().actionCode());
            assertEquals("operator correction", captor.getValue().reason());
            assertEquals("SERVER_TIME", result.getSignatureTimeMode());
            assertEquals("Asia/Shanghai", result.getSelectedTimeZone());
            assertEquals("", result.getSelectedTimeReason());
            assertEquals("EDHR_SIGNATURE_TIME_V1", result.getSelectedTimePolicyVersion());
            assertNotNull(result.getSelectedTimeAuditHash());
            assertTrue(result.getSelectedTimeAuditHash().matches("[0-9a-f]{64}"));
            verify(signatureMapper, never()).insert(any(MesProBatchRecordExecutionSignatureDO.class));
        }
    }

    @Test
    void recordFormReviewSignature_success() {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            when(authorizationService.isElectronicSignatureEnabled(99L)).thenReturn(true);
            when(adminUserService.getUser(99L)).thenReturn(snapshotUser("复核人"));
            stubActorSnapshot();
            when(electronicSignatureService.sign(any(ElectronicSignatureCommand.class)))
                    .thenReturn(unifiedSignatureResult(778L));

            Long signatureId = signatureService.recordFormReviewSignature(900L, "review-secret", "复核无异常",
                    2L, "c".repeat(64), "d".repeat(64));

            ArgumentCaptor<ElectronicSignatureCommand> captor =
                    ArgumentCaptor.forClass(ElectronicSignatureCommand.class);
            verify(adminUserApi).reauthenticateForSignature(99L, "review-secret");
            verify(electronicSignatureService).sign(captor.capture());
            assertEquals(778L, signatureId);
            assertEquals("FORM_REVIEW", captor.getValue().actionCode());
            assertEquals("复核无异常", captor.getValue().reason());
            verify(signatureMapper, never()).insert(any(MesProBatchRecordExecutionSignatureDO.class));
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

    private static ElectronicSignatureResult unifiedSignatureResult(Long signatureId) {
        LocalDateTime signedAt = LocalDateTime.of(2026, 9, 8, 11, 0, 0);
        return new ElectronicSignatureResult(signatureId, "VALID", signedAt, "SERVER_CLOCK:" + signedAt,
                "mes-subject-version", "mes-content-hash", "mes-evidence-hash", "SHA-256", "system-local-v1");
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
