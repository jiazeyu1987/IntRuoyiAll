package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationAccessPolicyImpl;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual synchronized Query authorization methods behind the D bridge; external policy sources are isolated. */
class DccRelationFormalQueryGuardTest extends BaseMockitoUnitTest {
    @InjectMocks DccControlledFileQueryServiceImpl query;
    @Mock DccControlledFileMapper controlledFileMapper;
    @Mock DccDirectoryAccessPermissionService directoryAccessPermissionService;
    @Mock DccControlledFileAssignmentScopeService assignmentScopeService;
    @Mock DccControlledFileViewMatrixAccessService viewMatrixAccessService;
    @Mock DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Mock DccControlledFileCategoryPermissionSupport permissionSupport;
    @Mock DccProjectAccessService projectAccessService;
    DccRelationAccessPolicyImpl bridge;
    @BeforeEach void setup(){
        TenantContextHolder.setTenantId(1L);bridge=new DccRelationAccessPolicyImpl();ReflectionTestUtils.setField(bridge,"query",query);
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    DccControlledFileDO file(){return DccControlledFileDO.builder().id(20L).tenantId(1L).masterId(10L).dccProjectCodeId(5L).categoryId(1L).directoryId(99L).requesterId(99L).status("ACTIVE").publishedFileId(200L).build();}
    @Test void realQueryNameDiscoverabilityDoesNotGrantPreviewOrEditAndDoesNotCallEnrichedDetails(){
        var file=file();when(controlledFileMapper.selectById(20L)).thenReturn(file);
        when(assignmentScopeService.isWithinAssignedFileScope(7L,20L)).thenReturn(true);
        when(directoryAccessPermissionService.getAuthorizedDirectoryIds(7L,DccAccessTypeEnum.QUERY)).thenReturn(Set.of(99L));
        when(directoryAccessPermissionService.getAuthorizedDirectoryIds(7L,DccAccessTypeEnum.PREVIEW)).thenReturn(Set.of());
        assertDoesNotThrow(()->bridge.assertNameVisible(7L,20L));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->bridge.assertContentReadable(7L,20L));
        doThrow(new IllegalArgumentException("formal project edit denied")).when(projectAccessService).assertProjectEditorOrOwner(7L,5L);
        assertThrows(IllegalArgumentException.class,()->bridge.assertCanEditRelations(7L,20L));
        verify(permissionSupport,never()).hasCategoryPermission(any(),any(),any());
    }
    @Test void previewRequiresBothAssignedFileScopeAndExactAuthorizedDirectory(){
        when(controlledFileMapper.selectById(20L)).thenReturn(file());
        when(assignmentScopeService.isWithinAssignedFileScope(7L,20L)).thenReturn(true);
        when(directoryAccessPermissionService.getAuthorizedDirectoryIds(7L,DccAccessTypeEnum.PREVIEW)).thenReturn(Set.of(99L));
        assertDoesNotThrow(()->bridge.assertContentReadable(7L,20L));
        when(assignmentScopeService.isWithinAssignedFileScope(7L,20L)).thenReturn(false);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->bridge.assertContentReadable(7L,20L));
    }
    @Test void wrongTenantActualFileRejectsBeforeNameAndContentPolicyResolution(){
        var file=file();file.setTenantId(2L);when(controlledFileMapper.selectById(20L)).thenReturn(file);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->bridge.assertNameVisible(7L,20L));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->bridge.assertContentReadable(7L,20L));
        verifyNoInteractions(directoryAccessPermissionService,assignmentScopeService,permissionSupport,projectAccessService);
    }
}
