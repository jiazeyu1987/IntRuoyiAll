package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderStorageMappingDO;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface DccProjectFolderStorageMappingMapper extends BaseMapperX<DccProjectFolderStorageMappingDO> {
 @Select("SELECT id FROM dcc_file_category WHERE tenant_id=#{tenant} AND id=#{category} AND deleted=0 FOR UPDATE")
 List<Long> lockCategory(@Param("tenant") Long tenant,@Param("category") Long category);
 @Select("SELECT * FROM dcc_file_category WHERE tenant_id=#{tenant} AND id=#{category} AND deleted=0 FOR UPDATE")
 cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO lockCurrentCategory(@Param("tenant") Long tenant,@Param("category") Long category);
 @Select("SELECT directory_id FROM dcc_category_directory_binding WHERE tenant_id=#{tenant} AND category_id=#{category} AND active=1 AND deleted=0 ORDER BY id FOR UPDATE")
 List<Long> lockConfiguredBases(@Param("tenant") Long tenant,@Param("category") Long category);
 @Select("SELECT * FROM dcc_project_folder_storage_mapping WHERE tenant_id=#{tenant} AND project_folder_id=#{folder} AND category_id=#{category} FOR UPDATE")
 DccProjectFolderStorageMappingDO lockMapping(@Param("tenant") Long tenant,@Param("folder") Long folder,@Param("category") Long category);
 @Select("SELECT id FROM dcc_project_folder_storage_mapping WHERE tenant_id=#{tenant} AND category_id=#{category} FOR UPDATE")
 List<Long> lockCategoryUses(@Param("tenant") Long tenant,@Param("category") Long category);
 @Select("SELECT id FROM dcc_project_folder_storage_mapping WHERE tenant_id=#{tenant} AND project_folder_id=#{folder} FOR UPDATE")
 List<Long> lockFolderUses(@Param("tenant") Long tenant,@Param("folder") Long folder);
 @Select("SELECT * FROM dcc_project_folder_storage_mapping WHERE tenant_id=#{tenant} AND (base_directory_id=#{directory} OR storage_directory_id=#{directory}) FOR UPDATE")
 List<DccProjectFolderStorageMappingDO> lockDirectoryUses(@Param("tenant") Long tenant,@Param("directory") Long directory);
 @Select("SELECT * FROM dcc_project_folder_storage_mapping WHERE tenant_id=#{tenant} AND storage_directory_id=#{directory}")
 DccProjectFolderStorageMappingDO findLeaf(@Param("tenant") Long tenant,@Param("directory") Long directory);
 @Select("""
 <script>
 SELECT m.* FROM dcc_project_folder_storage_mapping m
 JOIN dcc_project_code p ON p.id=m.project_code_id AND p.tenant_id=m.tenant_id AND p.status='ENABLE' AND p.deleted=0
 JOIN dcc_project_folder f ON f.id=m.project_folder_id AND f.project_code_id=p.id AND f.tenant_id=m.tenant_id AND f.active=1 AND f.deleted=0
 JOIN dcc_file_category c ON c.id=m.category_id AND c.tenant_id=m.tenant_id AND c.active=1 AND c.deleted=0
 JOIN dcc_file_directory b ON b.id=m.base_directory_id AND b.tenant_id=m.tenant_id AND b.active=1 AND b.deleted=0
 JOIN dcc_file_directory d ON d.id=m.storage_directory_id AND d.parent_id=b.id AND d.tenant_id=m.tenant_id AND d.active=1 AND d.deleted=0 AND d.access_rule_manually_bound=0
 WHERE m.tenant_id=#{tenant} AND m.deleted=0 AND m.base_directory_id IN
 <foreach collection="bases" item="base" open="(" separator="," close=")">#{base}</foreach>
 AND (SELECT COUNT(*) FROM dcc_category_directory_binding x WHERE x.tenant_id=m.tenant_id AND x.category_id=m.category_id AND x.active=1 AND x.deleted=0)=1
 AND EXISTS(SELECT 1 FROM dcc_category_directory_binding x WHERE x.tenant_id=m.tenant_id AND x.category_id=m.category_id AND x.directory_id=m.base_directory_id AND x.active=1 AND x.deleted=0)
 </script>
 """)
 java.util.List<DccProjectFolderStorageMappingDO> validDerivedLeaves(@Param("tenant") Long tenant,@Param("bases") java.util.Collection<Long> bases);
}
