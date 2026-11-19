package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFolderStorageMappingMapper;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.category.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.FOLDER_INVALID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccUploadTypeAdmissionTest {
    DccControlledFileCategoryPermissionSupport permissions;
    DccFileTypeTaxonomyAdminService taxonomy;
    DccControlledFileEditPermissionSupport edit;
    DccProjectFolderStorageService storage;
    DccProjectFolderStorageMappingMapper mappings;
    DccFileDirectoryMapper directories;
    DccProjectCodeDO project;
    DccProjectFolderDO folder;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        permissions=mock(DccControlledFileCategoryPermissionSupport.class);
        taxonomy=mock(DccFileTypeTaxonomyAdminService.class);
        edit=new DccControlledFileEditPermissionSupport();
        ReflectionTestUtils.setField(edit,"permissions",permissions);
        ReflectionTestUtils.setField(edit,"taxonomy",taxonomy);
        storage=new DccProjectFolderStorageService();
        mappings=mock(DccProjectFolderStorageMappingMapper.class);
        directories=mock(DccFileDirectoryMapper.class);
        ReflectionTestUtils.setField(storage,"mappings",mappings);
        ReflectionTestUtils.setField(storage,"directories",directories);
        ReflectionTestUtils.setField(storage,"taxonomy",taxonomy);
        ReflectionTestUtils.setField(storage,"editPermission",edit);
        project=new DccProjectCodeDO();project.setId(10L);project.setTenantId(1L);project.setStatus("ENABLE");
        folder=new DccProjectFolderDO();folder.setId(11L);folder.setTenantId(1L);folder.setProjectCodeId(10L);folder.setActive(true);
        when(taxonomy.resolveActiveCategoryId(30L)).thenReturn(20L);
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();}
    DccDerivedUploadStorage resolve(){return storage.resolve(88L,project,folder,20L,30L);}
    void authorized(){when(permissions.hasEditableProjectCategory(88L,10L,20L)).thenReturn(true);}
    void configuredCategory(String name){
        when(mappings.lockCurrentCategory(1L,20L)).thenReturn(DccFileCategoryDO.builder()
                .id(20L).name(name).active(true).fileTypeTaxonomyId(30L).build());
    }

    @ParameterizedTest @ValueSource(strings={"包装设计","检验规范","测试类型五级"})
    void deniedTypeUsesTheActualSelectedLeafAndCannotCreateStorage(String name){
        when(taxonomy.validateCategoryMapping(30L,20L,true)).thenReturn(
                new DccFileTypeTaxonomyPath(30L,"技术文档","设计输出",name.equals("测试类型五级")?"三级":name,
                        name.equals("测试类型五级")?"四级":null,name.equals("测试类型五级")?name:null));
        var error=assertThrows(ServiceException.class,this::resolve);
        assertEquals(CONTROLLED_FILE_TYPE_UPLOAD_FORBIDDEN.getCode(),error.getCode());
        assertEquals("当前账号无权上传“"+name+"”类型的文件，请选择已授权的文件类型，或联系文控管理员申请授权。",error.getMessage());
        verifyNoInteractions(mappings,directories);
    }
    @Test void projectOrLibraryAccessFailureIsNotMisreportedAsATypeDenial(){
        when(permissions.hasCategoryPermission(20L,88L,DccFileCategoryPermissionActionEnum.EDIT)).thenReturn(true);
        assertEquals(CONTROLLED_FILE_ACCESS_DENIED.getCode(),assertThrows(ServiceException.class,this::resolve).getCode());
        when(permissions.isLibrary(10L)).thenReturn(true);
        when(permissions.hasCategoryPermission(20L,88L,DccFileCategoryPermissionActionEnum.EDIT)).thenReturn(false);
        assertEquals(CONTROLLED_FILE_ACCESS_DENIED.getCode(),assertThrows(ServiceException.class,this::resolve).getCode());
        verify(taxonomy,never()).validateCategoryMapping(any(),any(),anyBoolean());
        verifyNoInteractions(mappings,directories);
    }
    @Test void invalidFolderKeepsItsOwnError(){
        folder.setProjectCodeId(99L);
        assertEquals(FOLDER_INVALID.getCode(),assertThrows(ServiceException.class,this::resolve).getCode());
        verifyNoInteractions(permissions,mappings,directories);
    }
    @Test void absentOrAmbiguousStorageConfigurationIsNotAnAuthorizationError(){
        authorized();configuredCategory("包装设计");
        for(var bases:List.of(List.<Long>of(),List.of(40L,41L))){
            when(mappings.lockConfiguredBases(1L,20L)).thenReturn(bases);
            var error=assertThrows(ServiceException.class,this::resolve);
            assertEquals(CONTROLLED_FILE_TYPE_STORAGE_INVALID.getCode(),error.getCode());
            assertEquals("文件类型“包装设计”未配置有效且唯一的存储目录，请联系文控管理员维护。",error.getMessage());
        }
        verifyNoInteractions(directories);
    }
    @Test void inactiveStorageIsAConfigurationError(){
        authorized();configuredCategory("检验规范");
        when(mappings.lockConfiguredBases(1L,20L)).thenReturn(List.of(40L));
        when(directories.selectTenantForUpdate(1L,40L)).thenReturn(DccFileDirectoryDO.builder().id(40L).active(false).build());
        assertEquals(CONTROLLED_FILE_TYPE_STORAGE_INVALID.getCode(),assertThrows(ServiceException.class,this::resolve).getCode());
        verify(mappings,never()).lockMapping(any(),any(),any());
    }
    @Test void authorizedUploadRetainsTheExactConfiguredStorage(){
        authorized();configuredCategory("有效类型");
        when(mappings.lockConfiguredBases(1L,20L)).thenReturn(List.of(40L));
        when(directories.selectTenantForUpdate(1L,40L)).thenReturn(DccFileDirectoryDO.builder().id(40L).active(true).build());
        var mapping=new DccProjectFolderStorageMappingDO();mapping.setId(50L);mapping.setTenantId(1L);
        mapping.setProjectCodeId(10L);mapping.setProjectFolderId(11L);mapping.setCategoryId(20L);
        mapping.setBaseDirectoryId(40L);mapping.setStorageDirectoryId(41L);
        when(mappings.lockMapping(1L,11L,20L)).thenReturn(mapping);
        when(directories.selectTenantForUpdate(1L,41L)).thenReturn(DccFileDirectoryDO.builder().id(41L).parentId(40L).active(true).build());
        assertEquals(41L,resolve().storageDirectoryId);
        verify(taxonomy,never()).validateCategoryMapping(any(),any(),anyBoolean());
        verify(directories,never()).insert(any(DccFileDirectoryDO.class));
    }
}
