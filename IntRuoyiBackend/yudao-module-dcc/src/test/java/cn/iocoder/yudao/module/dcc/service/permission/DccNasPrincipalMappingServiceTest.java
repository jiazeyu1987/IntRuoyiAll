package cn.iocoder.yudao.module.dcc.service.permission;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclAceDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclDirectorySnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclIdentityMappingDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclAceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclDirectorySnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclIdentityMappingMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.PostApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DccNasPrincipalMappingServiceTest extends BaseMockitoUnitTest {

    private static final String NAS_USER_SID = "S-1-5-21-1000-2000-3000-1101";
    private static final String NAS_ROLE_SID = "S-1-5-21-1000-2000-3000-2202";

    @Mock
    private DccNasAclIdentityMappingMapper identityMappingMapper;
    @Mock
    private DccNasAclAceMapper aceMapper;
    @Mock
    private DccNasAclDirectorySnapshotMapper directorySnapshotMapper;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private RoleApi roleApi;
    @Mock
    private PostApi postApi;

    @InjectMocks
    private DccNasPrincipalMappingServiceImpl mappingService;

    private DccNasPrincipalMappingService serviceContract;

    @BeforeEach
    void setUpContract() {
        serviceContract = mappingService;
    }

    @Test
    void saveMapping_persistsManualUserMappingWithAuditableFields() {
        when(identityMappingMapper.selectOne(anyMappingWrapper())).thenReturn(null);
        doAnswer(invocation -> {
            DccNasAclIdentityMappingDO mapping = invocation.getArgument(0);
            mapping.setId(7001L);
            return 1;
        }).when(identityMappingMapper).insert(any(DccNasAclIdentityMappingDO.class));

        DccNasAclIdentityMappingDO saved = serviceContract.saveMapping(mappingCommand(
                NAS_USER_SID,
                "INT",
                "zhangsan",
                "张三",
                "USER",
                "USER",
                901L,
                99L
        ));

        verify(adminUserApi).validateUserList(List.of(901L));
        ArgumentCaptor<DccNasAclIdentityMappingDO> mappingCaptor =
                ArgumentCaptor.forClass(DccNasAclIdentityMappingDO.class);
        verify(identityMappingMapper).insert(mappingCaptor.capture());
        DccNasAclIdentityMappingDO inserted = mappingCaptor.getValue();
        assertEquals(7001L, saved.getId());
        assertEquals(NAS_USER_SID, inserted.getSid());
        assertNotNull(inserted.getSidHash());
        assertNotEquals(NAS_USER_SID, inserted.getSidHash());
        assertEquals("INT", inserted.getDomainName());
        assertEquals("zhangsan", inserted.getAccountName());
        assertEquals("张三", inserted.getAccountDisplayName());
        assertEquals("USER", inserted.getAccountType());
        assertEquals("MAPPED", inserted.getMappingStatus());
        assertEquals("USER", inserted.getDccSubjectType());
        assertEquals(901L, inserted.getDccSubjectId());
        assertEquals("MANUAL", inserted.getMappingMethod());
        assertEquals(99L, inserted.getMappedByUserId());
        assertNotNull(inserted.getVerifiedAt());
    }

    @Test
    void saveMapping_rejectsConflictWhenSameSidAlreadyMapsToDifferentDccSubject() {
        when(identityMappingMapper.selectOne(anyMappingWrapper())).thenReturn(DccNasAclIdentityMappingDO.builder()
                .id(7001L)
                .sid(NAS_USER_SID)
                .sidHash("existing-sid-hash")
                .mappingStatus("MAPPED")
                .dccSubjectType("USER")
                .dccSubjectId(901L)
                .build());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> serviceContract.saveMapping(mappingCommand(
                        NAS_USER_SID, "INT", "zhangsan", "张三", "USER", "ROLE", 3001L, 99L)));

        verify(roleApi).validRoleList(List.of(3001L));
        assertTrue(ex.getMessage().contains("conflict") || ex.getMessage().contains("冲突"));
        verify(identityMappingMapper, never()).insert(any(DccNasAclIdentityMappingDO.class));
    }

    @Test
    void saveMapping_reactivatesExistingInactiveSidInsteadOfInsertingDuplicate() {
        when(identityMappingMapper.selectOne(anyMappingWrapper())).thenReturn(DccNasAclIdentityMappingDO.builder()
                .id(7001L)
                .sid(NAS_USER_SID)
                .sidHash("existing-sid-hash")
                .mappingStatus("INACTIVE")
                .dccSubjectType("USER")
                .dccSubjectId(901L)
                .build());

        DccNasAclIdentityMappingDO saved = serviceContract.saveMapping(mappingCommand(
                NAS_USER_SID,
                "INT",
                "zhangsan",
                "张三",
                "USER",
                "USER",
                901L,
                99L
        ));

        verify(adminUserApi).validateUserList(List.of(901L));
        ArgumentCaptor<DccNasAclIdentityMappingDO> mappingCaptor =
                ArgumentCaptor.forClass(DccNasAclIdentityMappingDO.class);
        verify(identityMappingMapper).updateById(mappingCaptor.capture());
        verify(identityMappingMapper, never()).insert(any(DccNasAclIdentityMappingDO.class));
        DccNasAclIdentityMappingDO updated = mappingCaptor.getValue();
        assertEquals(7001L, saved.getId());
        assertEquals(7001L, updated.getId());
        assertEquals(NAS_USER_SID, updated.getSid());
        assertEquals("MAPPED", updated.getMappingStatus());
        assertEquals("USER", updated.getDccSubjectType());
        assertEquals(901L, updated.getDccSubjectId());
        assertEquals("MANUAL", updated.getMappingMethod());
        assertEquals(99L, updated.getMappedByUserId());
        assertNotNull(updated.getVerifiedAt());
    }

    @Test
    void saveMapping_validatesRoleDeptAndPositionTargetsThroughSystemApis() {
        when(identityMappingMapper.selectOne(anyMappingWrapper())).thenReturn(null, null, null);
        doAnswer(invocation -> {
            DccNasAclIdentityMappingDO mapping = invocation.getArgument(0);
            mapping.setId(7001L);
            return 1;
        }).when(identityMappingMapper).insert(any(DccNasAclIdentityMappingDO.class));

        serviceContract.saveMapping(mappingCommand(
                NAS_ROLE_SID, "INT", "dcc-viewer", "DCC Viewer", "GROUP", "ROLE", 3001L, 99L));
        serviceContract.saveMapping(mappingCommand(
                "S-1-5-21-1000-2000-3000-3303", "INT", "dcc-rd", "研发部", "GROUP", "DEPT", 4001L, 99L));
        serviceContract.saveMapping(mappingCommand(
                "S-1-5-21-1000-2000-3000-4404", "INT", "dcc-engineer", "工程师", "GROUP", "POSITION", 5001L, 99L));

        verify(roleApi).validRoleList(List.of(3001L));
        verify(deptApi).validateDeptList(List.of(4001L));
        verify(postApi).validPostList(List.of(5001L));
    }

    @Test
    void saveMapping_rejectsUnknownTargetSubjectTypeBeforeLookupOrInsert() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> serviceContract.saveMapping(mappingCommand(
                        NAS_USER_SID, "INT", "zhangsan", "张三", "USER", "GROUP", 901L, 99L)));

        assertTrue(ex.getMessage().contains("targetSubjectType") || ex.getMessage().contains("主体类型"));
        verifyNoInteractions(adminUserApi, deptApi, roleApi, postApi);
        verify(identityMappingMapper, never()).insert(any(DccNasAclIdentityMappingDO.class));
    }

    @Test
    void saveMapping_rejectsMissingOrDisabledTargetSubjectBeforeInsert() {
        when(identityMappingMapper.selectOne(anyMappingWrapper())).thenReturn(null, null);
        doThrow(new RuntimeException("target user missing"))
                .when(adminUserApi).validateUserList(List.of(901L));
        doThrow(new RuntimeException("target role disabled"))
                .when(roleApi).validRoleList(List.of(3001L));

        RuntimeException missingUser = assertThrows(RuntimeException.class,
                () -> serviceContract.saveMapping(mappingCommand(
                        NAS_USER_SID, "INT", "zhangsan", "张三", "USER", "USER", 901L, 99L)));
        RuntimeException disabledTarget = assertThrows(RuntimeException.class,
                () -> serviceContract.saveMapping(mappingCommand(
                        NAS_ROLE_SID, "INT", "dcc-viewer", "DCC Viewer", "GROUP", "ROLE", 3001L, 99L)));

        assertTrue(missingUser.getMessage().contains("target") || missingUser.getMessage().contains("目标"));
        assertTrue(disabledTarget.getMessage().contains("disabled") || disabledTarget.getMessage().contains("停用"));
        verify(identityMappingMapper, never()).insert(any(DccNasAclIdentityMappingDO.class));
    }

    @Test
    void saveMapping_rejectsMissingSchemaRequiredAccountTypeBeforeInsert() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> serviceContract.saveMapping(mappingCommand(
                        NAS_USER_SID, "INT", "zhangsan", "张三", null, "USER", 901L, 99L)));

        assertTrue(ex.getMessage().contains("accountType"));
        verify(adminUserApi, never()).validateUserList(any());
        verify(identityMappingMapper, never()).insert(any(DccNasAclIdentityMappingDO.class));
    }

    @Test
    void listUnmappedPrincipals_returnsOnlyTrusteeSidsWithoutActiveMappingWithAceCountAndFirstNasPath() {
        String unmappedSid = "S-1-5-21-1000-2000-3000-3303";
        String mappedSid = "S-1-5-21-1000-2000-3000-4404";
        when(aceMapper.selectList(anyAceWrapper())).thenReturn(List.of(
                ace(7101L, unmappedSid, "hash-unmapped"),
                ace(7101L, unmappedSid, "hash-unmapped"),
                ace(7102L, mappedSid, "hash-mapped")
        ));
        when(directorySnapshotMapper.selectList(anyDirectorySnapshotWrapper())).thenReturn(List.of(
                directorySnapshot(7301L, 10L, 7101L, "3.DMR/01.图纸"),
                directorySnapshot(7302L, 10L, 7102L, "3.DMR/02.图纸")
        ));
        when(identityMappingMapper.selectList(anyMappingWrapper())).thenReturn(List.of(DccNasAclIdentityMappingDO.builder()
                .id(7001L)
                .sid(mappedSid)
                .sidHash("hash-mapped")
                .mappingStatus("MAPPED")
                .dccSubjectType("USER")
                .dccSubjectId(901L)
                .build()));

        List<DccNasPrincipalMappingService.UnmappedPrincipal> unmapped =
                serviceContract.listUnmappedPrincipals(10L);

        assertEquals(1, unmapped.size());
        assertEquals(unmappedSid, unmapped.get(0).sid());
        assertEquals("hash-unmapped", unmapped.get(0).sidHash());
        assertEquals(2, unmapped.get(0).aceCount());
        assertEquals("3.DMR/01.图纸", unmapped.get(0).firstNasPath());
    }

    @Test
    void listUnmappedPrincipals_countsSharedDescriptorAceForEachSuccessfulDirectorySnapshot() {
        String unmappedSid = "S-1-5-21-1000-2000-3000-3303";
        String mappedSid = "S-1-5-21-1000-2000-3000-4404";
        when(aceMapper.selectList(anyAceWrapper())).thenReturn(List.of(
                ace(7101L, unmappedSid, "hash-unmapped"),
                ace(7101L, mappedSid, "hash-mapped")
        ));
        when(directorySnapshotMapper.selectList(anyDirectorySnapshotWrapper())).thenReturn(List.of(
                directorySnapshot(7301L, 10L, 7101L, "3.DMR/01.图纸"),
                directorySnapshot(7302L, 10L, 7101L, "3.DMR/02.图纸"),
                directorySnapshot(7303L, 10L, 7101L, "3.DMR/03.失败目录", "FAILED")
        ));
        when(identityMappingMapper.selectList(anyMappingWrapper())).thenReturn(List.of(DccNasAclIdentityMappingDO.builder()
                .id(7001L)
                .sid(mappedSid)
                .sidHash("hash-mapped")
                .mappingStatus("MAPPED")
                .dccSubjectType("USER")
                .dccSubjectId(901L)
                .build()));

        List<DccNasPrincipalMappingService.UnmappedPrincipal> unmapped =
                serviceContract.listUnmappedPrincipals(10L);

        assertEquals(1, unmapped.size());
        assertEquals(unmappedSid, unmapped.get(0).sid());
        assertEquals("hash-unmapped", unmapped.get(0).sidHash());
        assertEquals(2, unmapped.get(0).aceCount());
        assertEquals("3.DMR/01.图纸", unmapped.get(0).firstNasPath());
    }

    private static Wrapper<DccNasAclIdentityMappingDO> anyMappingWrapper() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static Wrapper<DccNasAclAceDO> anyAceWrapper() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static Wrapper<DccNasAclDirectorySnapshotDO> anyDirectorySnapshotWrapper() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static DccNasPrincipalMappingService.SaveMappingCommand mappingCommand(String sid,
                                                                                  String domainName,
                                                                                  String accountName,
                                                                                  String accountDisplayName,
                                                                                  String accountType,
                                                                                  String targetSubjectType,
                                                                                  Long targetSubjectId,
                                                                                  Long mappedByUserId) {
        return new DccNasPrincipalMappingService.SaveMappingCommand(
                domainName,
                sid,
                accountDisplayName,
                accountName,
                accountType,
                targetSubjectType,
                targetSubjectId,
                Boolean.TRUE,
                null,
                mappedByUserId);
    }

    private static DccNasAclAceDO ace(Long descriptorId, String trusteeSid, String trusteeSidHash) {
        return DccNasAclAceDO.builder()
                .descriptorId(descriptorId)
                .trusteeSid(trusteeSid)
                .trusteeSidHash(trusteeSidHash)
                .build();
    }

    private static DccNasAclDirectorySnapshotDO directorySnapshot(Long id, Long taskId,
                                                                  Long descriptorId, String nasPath) {
        return directorySnapshot(id, taskId, descriptorId, nasPath, "SUCCESS");
    }

    private static DccNasAclDirectorySnapshotDO directorySnapshot(Long id, Long taskId,
                                                                  Long descriptorId, String nasPath,
                                                                  String collectStatus) {
        return DccNasAclDirectorySnapshotDO.builder()
                .id(id)
                .transferTaskId(taskId)
                .descriptorId(descriptorId)
                .nasPath(nasPath)
                .collectStatus(collectStatus)
                .build();
    }
}
