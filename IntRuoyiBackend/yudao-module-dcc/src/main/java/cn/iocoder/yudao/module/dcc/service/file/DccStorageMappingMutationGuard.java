package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFolderStorageMappingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.Collection;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.*;
@Service
public class DccStorageMappingMutationGuard {
 @Resource private DccProjectFolderStorageMappingMapper mappings;
 @Resource private DccFileDirectoryMapper directories;
 @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
 public void requireCategoryUnmapped(Long category) {
  Long tenant=TenantContextHolder.getRequiredTenantId();
  if(mappings.lockCategory(tenant,category).size()!=1 || !mappings.lockCategoryUses(tenant,category).isEmpty()) throw fail(FOLDER_USED);
 }
 @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
 public void requireFolderUnmapped(Long folder) {
  if(!mappings.lockFolderUses(TenantContextHolder.getRequiredTenantId(),folder).isEmpty()) throw fail(FOLDER_USED);
 }
 @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
 public void requireDirectoriesUnmapped(Collection<Long> ids) {
  Long tenant=TenantContextHolder.getRequiredTenantId();
  var bases=new java.util.TreeSet<Long>();
  for(Long id:ids) {var hint=mappings.findLeaf(tenant,id);if(hint!=null)bases.add(hint.getBaseDirectoryId());}
  // Existing leaf mapping's immutable base is a lock-order hint, never permission evidence.
  for(Long base:bases) if(directories.selectTenantForUpdate(tenant,base)==null) throw fail(FOLDER_INVALID);
  // Lock actual directory rows before current mapping reads; creation locks the same configured base.
  for(Long id:ids.stream().sorted().toList()) if(directories.selectTenantForUpdate(tenant,id)==null) throw fail(FOLDER_INVALID);
  for(Long id:ids) if(!mappings.lockDirectoryUses(tenant,id).isEmpty()) throw fail(FOLDER_USED);
 }
 @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
 public void requireParentNotMappedLeaf(Long parent) {
  if(parent==null || parent<=0)return;
  if(directories.selectTenantForUpdate(TenantContextHolder.getRequiredTenantId(),parent)==null) throw fail(FOLDER_INVALID);
  var uses=mappings.lockDirectoryUses(TenantContextHolder.getRequiredTenantId(),parent);
  if(uses.stream().anyMatch(row->parent.equals(row.getStorageDirectoryId()))) throw fail(FOLDER_USED);
 }
}
