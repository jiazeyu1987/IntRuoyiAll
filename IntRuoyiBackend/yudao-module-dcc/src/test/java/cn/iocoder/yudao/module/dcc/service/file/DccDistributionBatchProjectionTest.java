package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.*;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class DccDistributionBatchProjectionTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileMapper files;@Mock DccDistributionBatchMapper batches;@Mock PermissionApi permissions;@Mock DccControlledFileCategoryPermissionSupport category;
    @Mock cn.iocoder.yudao.module.system.api.user.AdminUserApi users;
    @Mock cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService projects;
    @InjectMocks DccDistributionTaskServiceImpl service;
    @BeforeEach void fixture(){TenantContextHolder.setTenantId(1L);var file=DccControlledFileDO.builder().id(42L).categoryId(15L).dccProjectCodeId(31L).versionNo("B/1").processInstanceId("round42").build();file.setTenantId(1L);
        lenient().when(permissions.hasAnyPermissions(99L,"dcc:controlled-file:distribute")).thenReturn(true);
        lenient().when(users.getUser(99L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0));
        lenient().when(projects.listReadableProjectIds(99L)).thenReturn(java.util.List.of(31L));
        lenient().when(files.selectById(42L)).thenReturn(file);lenient().when(permissions.hasAnyRoles(99L,"doc_control")).thenReturn(true);lenient().when(category.hasCategoryPermission(any(),any(),any())).thenReturn(true);}
    @AfterEach void clear(){TenantContextHolder.clear();}
    Object status()throws Exception{return service.getClass().getMethod("getAutomaticDistributionResult",Long.class,Long.class).invoke(service,99L,42L);}
    @Test void missingBatchReturnsNotCreatedRatherThanSuccessfulEmptyReceipts()throws Exception{var result=status();assertEquals("NOT_CREATED",result.getClass().getMethod("getStatus").invoke(result));}
    @Test void actualFailureAndCompleteReasonAreProjectedForExactVersion()throws Exception{when(batches.selectVersion(1L,42L,"round42")).thenReturn(DccDistributionBatchDO.builder().id(51L).controlledFileId(42L).versionNo("B/1").approvalProcessInstanceId("round42").status("FAILED").errorCode("ROSTER_POLICY_UNDECIDED").errorMessage("真实错误".repeat(200)).build());
        var result=status();assertEquals("FAILED",result.getClass().getMethod("getStatus").invoke(result));assertEquals("真实错误".repeat(200),result.getClass().getMethod("getErrorMessage").invoke(result));}
    @Test void foreignVersionOrMissingDocumentControlRoleCannotReadBatchResult(){
        when(batches.selectVersion(1L,42L,"round42")).thenReturn(DccDistributionBatchDO.builder().id(51L).controlledFileId(43L).versionNo("B/1").approvalProcessInstanceId("round42").status("FAILED").build());
        assertThrows(Exception.class,this::status);when(permissions.hasAnyRoles(99L,"doc_control")).thenReturn(false);assertThrows(Exception.class,this::status);
    }
}
