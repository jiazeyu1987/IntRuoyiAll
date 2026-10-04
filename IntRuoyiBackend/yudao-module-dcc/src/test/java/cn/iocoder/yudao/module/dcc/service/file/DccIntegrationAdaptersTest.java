package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectLeaderService;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccIntegrationAdaptersTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileMapper fileMapper;
    @Mock DccControlledFileMasterMapper masterMapper;
    @Mock DccProjectCodeMapper projectMapper;
    @Mock DccProjectFolderMapper folderMapper;
    @Mock AdminUserApi userApi;
    @Mock org.springframework.jdbc.core.JdbcTemplate jdbc;
    final DccLatestControlledFileResolverImpl resolver = new DccLatestControlledFileResolverImpl();
    final DccProjectReferenceAuthorityImpl authority = new DccProjectReferenceAuthorityImpl();
    final LocalDateTime controlledAt = LocalDateTime.of(2026,10,1,8,0);
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(resolver,"fileMapper",fileMapper);
        ReflectionTestUtils.setField(resolver,"masterMapper",masterMapper);
        ReflectionTestUtils.setField(resolver,"jdbc",jdbc);
        var leader = new DccProjectLeaderService();
        ReflectionTestUtils.setField(leader,"projectCodeMapper",projectMapper);
        ReflectionTestUtils.setField(leader,"adminUserApi",userApi);
        var folders = new DccFolderTemplateService();
        ReflectionTestUtils.setField(folders,"folderMapper",folderMapper);
        ReflectionTestUtils.setField(authority,"leaderService",leader);
        ReflectionTestUtils.setField(authority,"folderService",folders);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    DccControlledFileDO future() {
        return DccControlledFileDO.builder().id(20L).masterId(10L).tenantId(1L).dccProjectCodeId(5L)
                .fileName("SOP.pdf").fileNumber("SOP-1").versionNo("A/2")
                .status("CONTROLLED_PENDING_EFFECTIVE").controlledTime(controlledAt)
                .processInstanceId("round-2").build();
    }
    @Test void latestFutureDoesNotResolveOldExecutionPointer() {
        when(masterMapper.selectById(10L)).thenReturn(DccControlledFileMasterDO.builder().id(10L).tenantId(1L)
                .latestControlledFileId(20L).currentActiveControlledFileId(19L).build());
        when(fileMapper.selectById(20L)).thenReturn(future());
        var result=resolver.resolveLatest(10L);
        assertEquals(20L,result.controlledFileId()); assertTrue(result.pendingEffect()); assertFalse(result.executable());
        verify(fileMapper,never()).selectById(19L);
    }
    @Test void missingLatestFailsWithoutExecutionFallback() {
        when(masterMapper.selectById(10L)).thenReturn(DccControlledFileMasterDO.builder().id(10L).tenantId(1L)
                .currentActiveControlledFileId(19L).build());
        assertThrows(DccRelationFailure.class,()->resolver.resolveLatest(10L));
        verifyNoInteractions(fileMapper);
    }
    @Test void lockedLatestUsesFormalPointerAndExactLockedVersionWithoutOrdinaryReads(){
        when(masterMapper.selectByIdForUpdate(10L)).thenReturn(DccControlledFileMasterDO.builder().id(10L).tenantId(1L)
                .latestControlledFileId(20L).currentActiveControlledFileId(19L).build());
        when(fileMapper.selectByIdAndTenantForUpdate(1L,20L)).thenReturn(future());
        var result=resolver.resolveLatestForUpdate(10L);
        assertEquals(20L,result.controlledFileId());assertTrue(result.pendingEffect());assertFalse(result.executable());
        verify(masterMapper,never()).selectById(any());verify(fileMapper,never()).selectById(any());
        verify(fileMapper,never()).selectByIdAndTenantForUpdate(1L,19L);
    }
    @Test void lockedMissingOrForeignTenantMasterCannotResolveExecutionFallback(){
        when(masterMapper.selectByIdForUpdate(10L)).thenReturn(DccControlledFileMasterDO.builder().id(10L).tenantId(1L).currentActiveControlledFileId(19L).build());
        assertThrows(DccRelationFailure.class,()->resolver.resolveLatestForUpdate(10L));
        when(masterMapper.selectByIdForUpdate(10L)).thenReturn(DccControlledFileMasterDO.builder().id(10L).tenantId(2L).latestControlledFileId(20L).build());
        assertThrows(DccRelationFailure.class,()->resolver.resolveLatestForUpdate(10L));verifyNoInteractions(fileMapper);
    }
    @Test void lockedLatestPointerCannotReferenceAnotherMaster(){
        when(masterMapper.selectByIdForUpdate(10L)).thenReturn(DccControlledFileMasterDO.builder().id(10L).tenantId(1L).latestControlledFileId(20L).build());
        var file=future();file.setMasterId(11L);when(fileMapper.selectByIdAndTenantForUpdate(1L,20L)).thenReturn(file);
        assertEquals("DCC_LATEST_CONTROLLED_IDENTITY_INVALID",assertThrows(DccRelationFailure.class,()->resolver.resolveLatestForUpdate(10L)).getMessage());
    }
    @Test void sourceTenantMismatchCannotBeProjected() {
        var f=future();f.setTenantId(2L);when(fileMapper.selectById(20L)).thenReturn(f);
        assertThrows(DccRelationFailure.class,()->resolver.resolveSelected(20L));
    }
    @Test void controlledEventUsesExactVersionRoundTimeAndTenant() {
        when(fileMapper.selectById(20L)).thenReturn(future());
        when(jdbc.queryForObject(anyString(),eq(Long.class),eq(1L),eq("event-key"),eq(10L),eq(20L),eq("round-2"),eq(controlledAt))).thenReturn(1L);
        resolver.assertControlledEvent(new DccRelationContracts.ControlledEvent(1L,10L,20L,"round-2","event-key",controlledAt));
        assertThrows(DccRelationFailure.class,()->resolver.assertControlledEvent(
                new DccRelationContracts.ControlledEvent(1L,10L,20L,"other-round","event-key",controlledAt)));
    }
    @Test void obsoleteHistoryIsNotAnExecutableControlledCandidate() {
        var f=future();f.setStatus("OBSOLETE");when(fileMapper.selectById(20L)).thenReturn(f);
        var result=resolver.resolveSelected(20L);assertFalse(result.controlled());assertFalse(result.executable());
    }
    @Test void realBLeaderAndFolderContractsAcceptOnlyOwnProject() {
        var project=DccProjectCodeDO.builder().id(5L).projectLeaderUserId(99L).status("ENABLE").build();project.setTenantId(1L);
        when(projectMapper.selectById(5L)).thenReturn(project);
        when(userApi.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setStatus(0));
        var folder=new DccProjectFolderDO();folder.setId(7L);folder.setTenantId(1L);folder.setProjectCodeId(5L);folder.setActive(true);
        when(folderMapper.selectById(7L)).thenReturn(folder);
        authority.assertProjectLeader(99L,5L);authority.assertFolderBelongsToProject(5L,7L);
        assertThrows(RuntimeException.class,()->authority.assertProjectLeader(100L,5L));
        assertThrows(RuntimeException.class,()->authority.assertFolderBelongsToProject(6L,7L));
    }
}
