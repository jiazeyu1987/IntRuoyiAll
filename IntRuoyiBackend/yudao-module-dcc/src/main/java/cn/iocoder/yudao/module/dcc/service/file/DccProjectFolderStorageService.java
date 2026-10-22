package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFolderStorageMappingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.Objects;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.*;
@Service
public class DccProjectFolderStorageService {
 @Resource private DccProjectFolderStorageMappingMapper mappings;
 @Resource private DccFileDirectoryMapper directories;
 @Resource private DccFileTypeTaxonomyAdminService taxonomy;
 @Resource private DccControlledFileCategoryPermissionSupport permissions;
 @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
 public DccDerivedUploadStorage resolve(Long actor,DccProjectCodeDO project,DccProjectFolderDO folder,Long category,Long type) {
  Long tenant=TenantContextHolder.getRequiredTenantId();
  if(TenantContextHolder.isIgnore() || actor==null || actor<=0 || project==null || folder==null
    || !Objects.equals(project.getTenantId(),tenant) || !Objects.equals(folder.getTenantId(),tenant)
    || !Objects.equals(folder.getProjectCodeId(),project.getId()) || !Boolean.TRUE.equals(folder.getActive())
    || !"ENABLE".equals(project.getStatus()) || !Objects.equals(category,taxonomy.resolveActiveCategoryId(type))
    || !permissions.hasCategoryPermission(category,actor,cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.UPLOAD)) throw fail(FOLDER_INVALID);
  var currentCategory=mappings.lockCurrentCategory(tenant,category);
  if(currentCategory==null || !Boolean.TRUE.equals(currentCategory.getActive())
     || !Objects.equals(type,currentCategory.getFileTypeTaxonomyId())) throw fail(FOLDER_INVALID);
  var bases=mappings.lockConfiguredBases(tenant,category);
  if(bases.size()!=1) throw fail(FOLDER_INVALID);
  Long baseId=bases.get(0);var base=directories.selectTenantForUpdate(tenant,baseId);
  if(base==null || !Boolean.TRUE.equals(base.getActive())) throw fail(FOLDER_INVALID);
  var mapping=mappings.lockMapping(tenant,folder.getId(),category);boolean created=false;
  if(mapping==null) {
   var leaf=DccFileDirectoryDO.builder().tenantId(tenant).parentId(baseId)
      .code("PFS-"+Long.toString(tenant,36)+"-"+Long.toString(folder.getId(),36)+"-"+Long.toString(category,36))
      .name("项目#"+project.getId()+" 文件夹#"+folder.getId()).active(true).sort(0).accessRuleManuallyBound(false).build();
   if(directories.insert(leaf)!=1 || leaf.getId()==null) throw fail(FOLDER_INVALID);
   mapping=new DccProjectFolderStorageMappingDO();mapping.setTenantId(tenant);mapping.setProjectCodeId(project.getId());
   mapping.setProjectFolderId(folder.getId());mapping.setCategoryId(category);mapping.setBaseDirectoryId(baseId);
   mapping.setStorageDirectoryId(leaf.getId());
   if(mappings.insert(mapping)!=1 || mapping.getId()==null) throw fail(FOLDER_INVALID);
   created=true;
  }
  if(!Objects.equals(mapping.getTenantId(),tenant) || Boolean.TRUE.equals(mapping.getDeleted())
    || !Objects.equals(mapping.getProjectCodeId(),project.getId()) || !Objects.equals(mapping.getProjectFolderId(),folder.getId())
    || !Objects.equals(mapping.getCategoryId(),category) || !Objects.equals(mapping.getBaseDirectoryId(),baseId)) throw fail(FOLDER_INVALID);
  var leaf=directories.selectTenantForUpdate(tenant,mapping.getStorageDirectoryId());
  if(leaf==null || !Boolean.TRUE.equals(leaf.getActive()) || !Objects.equals(leaf.getParentId(),baseId)
    || directories.countActiveChildren(tenant,leaf.getId())!=0) throw fail(FOLDER_INVALID);
  return new DccDerivedUploadStorage(tenant,actor,project.getId(),folder.getId(),category,mapping.getId(),baseId,leaf.getId(),created);
 }
}
