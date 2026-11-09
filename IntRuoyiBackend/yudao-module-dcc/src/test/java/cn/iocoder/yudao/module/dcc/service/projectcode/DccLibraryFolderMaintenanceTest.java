package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessServiceImpl;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectConfigurationAuditService;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFolderMaintenanceService;
import cn.iocoder.yudao.module.dcc.service.file.DccStorageMappingMutationGuard;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccLibraryFolderMaintenanceTest {
    final DccProjectCodeMapper projects = mock(DccProjectCodeMapper.class);
    final DccProjectFolderMapper folders = mock(DccProjectFolderMapper.class);
    final PermissionApi permissions = mock(PermissionApi.class);
    final AdminUserApi users = mock(AdminUserApi.class);
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final GxpAuditService audit = mock(GxpAuditService.class);
    final DccProjectFolderMaintenanceService service = new DccProjectFolderMaintenanceService();
    final DccProjectAccessServiceImpl access = new DccProjectAccessServiceImpl();
    final DccProjectCodeDO project = DccProjectCodeDO.builder().id(10L).status("ENABLE").build();

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        project.setTenantId(1L);
        when(projects.selectById(10L)).thenReturn(project);
        when(projects.selectByIdForUpdate(10L)).thenReturn(project);
        when(users.getUser(7L)).thenReturn(new AdminUserRespDTO().setId(7L).setTenantId(1L).setStatus(0));
        ReflectionTestUtils.setField(access,"projectCodeMapper",projects);
        ReflectionTestUtils.setField(access,"permissionApi",permissions);
        ReflectionTestUtils.setField(access,"adminUserApi",users);
        ReflectionTestUtils.setField(service,"projects",projects);
        ReflectionTestUtils.setField(service,"folders",folders);
        ReflectionTestUtils.setField(service,"permissions",permissions);
        ReflectionTestUtils.setField(service,"access",access);
        ReflectionTestUtils.setField(service,"reasons",new DccProjectConfigurationAuditService());
        ReflectionTestUtils.setField(service,"audit",audit);
        ReflectionTestUtils.setField(service,"jdbc",jdbc);
        ReflectionTestUtils.setField(service,"storageMappingGuard",mock(DccStorageMappingMutationGuard.class));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    DccProjectFolderMaintenanceService.Save input() {
        return new DccProjectFolderMaintenanceService.Save(null,0L,"目录",0,"维护目录");
    }
    @ParameterizedTest @ValueSource(strings={"QMS","GENERAL"})
    void editAloneCreatesAndDeletesWithAudit(String scope) {
        project.setLibraryScope(scope);
        when(permissions.hasAnyPermissions(7L,"dcc:library:"+scope.toLowerCase(Locale.ROOT)+":edit")).thenReturn(true);
        var row = new DccProjectFolderDO();row.setId(20L);row.setTenantId(1L);row.setProjectCodeId(10L);row.setActive(true);
        when(folders.listByProjectForUpdate(10L)).thenReturn(List.of());
        when(folders.insert(any(DccProjectFolderDO.class))).thenAnswer(call -> {call.<DccProjectFolderDO>getArgument(0).setId(20L);return 1;});
        when(folders.selectById(20L)).thenReturn(row);
        assertSame(row,service.save(7L,10L,input()));
        when(jdbc.queryForList(anyString(),eq(Long.class),eq(1L),eq(10L),eq(20L)))
                .thenReturn(List.of(20L),List.of(),List.of(),List.of());
        when(folders.selectByIdForUpdate(10L,20L)).thenReturn(row);
        when(jdbc.update(anyString(),eq(1L),eq(10L),eq(20L))).thenReturn(1);
        when(jdbc.queryForMap(anyString(),eq(1L),eq(10L),eq(20L))).thenReturn(Map.of("deleted",1));
        assertDoesNotThrow(()->service.delete(7L,10L,20L,true,"删除空目录"));
        verify(audit,times(2)).append(any());
        verify(permissions,never()).hasAnyPermissions(7L,"dcc:project-code:update");
    }
    @ParameterizedTest @ValueSource(strings={"QMS","GENERAL"})
    void viewAndOtherLibraryEditCannotWrite(String scope) {
        project.setLibraryScope(scope);
        when(permissions.hasAnyPermissions(7L,"dcc:library:"+scope.toLowerCase(Locale.ROOT)+":view")).thenReturn(true);
        when(permissions.hasAnyPermissions(7L,"dcc:library:"+(scope.equals("QMS")?"general":"qms")+":edit")).thenReturn(true);
        assertThrows(RuntimeException.class,()->service.save(7L,10L,input()));
        assertThrows(RuntimeException.class,()->service.delete(7L,10L,20L,true,"拒绝删除"));
        verifyNoInteractions(folders,audit,jdbc);
    }
    @Test void crossTenantAndOrdinaryProjectStillDenied() {
        project.setLibraryScope("QMS");project.setTenantId(2L);
        when(permissions.hasAnyPermissions(7L,"dcc:library:qms:edit")).thenReturn(true);
        assertThrows(RuntimeException.class,()->service.save(7L,10L,input()));
        project.setTenantId(1L);project.setLibraryScope("PROJECT");
        assertThrows(RuntimeException.class,()->service.save(7L,10L,input()));
        verifyNoInteractions(folders,audit,jdbc);
    }
    @Test void editorCannotUseForeignParentOrFolderOrCreateCycle() {
        project.setLibraryScope("QMS");
        when(permissions.hasAnyPermissions(7L,"dcc:library:qms:edit")).thenReturn(true);
        var row = new DccProjectFolderDO();row.setId(20L);row.setTenantId(1L);row.setProjectCodeId(10L);
        row.setParentId(0L);row.setName("根目录");row.setActive(true);
        when(folders.listByProjectForUpdate(10L)).thenReturn(List.of(row));
        for (var input : List.of(
                new DccProjectFolderMaintenanceService.Save(null,99L,"目录",0,"跨库父级"),
                new DccProjectFolderMaintenanceService.Save(99L,0L,"目录",0,"跨库编辑"),
                new DccProjectFolderMaintenanceService.Save(20L,20L,"目录",0,"循环目录"),
                new DccProjectFolderMaintenanceService.Save(null,0L,"根目录",0,"重复目录"))) {
            assertThrows(RuntimeException.class,()->service.save(7L,10L,input));
        }
        verify(folders,never()).insert(any(DccProjectFolderDO.class));
        verifyNoInteractions(audit);
    }
    @Test void editorCannotDeleteForeignOrNonEmptyFolderOrSkipConfirmation() {
        project.setLibraryScope("GENERAL");
        when(permissions.hasAnyPermissions(7L,"dcc:library:general:edit")).thenReturn(true);
        assertThrows(RuntimeException.class,()->service.delete(7L,10L,20L,false,"未确认"));
        when(jdbc.queryForList(anyString(),eq(Long.class),eq(1L),eq(10L),eq(20L))).thenReturn(List.of());
        assertThrows(RuntimeException.class,()->service.delete(7L,10L,20L,true,"跨库目录"));
        var row = new DccProjectFolderDO();row.setId(20L);row.setActive(true);
        when(folders.selectByIdForUpdate(10L,20L)).thenReturn(row);
        when(jdbc.queryForList(anyString(),eq(Long.class),eq(1L),eq(10L),eq(20L)))
                .thenReturn(List.of(20L),List.of(21L));
        assertThrows(RuntimeException.class,()->service.delete(7L,10L,20L,true,"存在子目录"));
        verify(jdbc,never()).update(anyString(),eq(1L),eq(10L),eq(20L));
        verifyNoInteractions(audit);
    }
}
