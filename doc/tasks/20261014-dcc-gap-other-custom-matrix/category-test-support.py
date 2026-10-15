"""Explicit isolated framework/unrelated ports. Target admin service/DO/VO are compiled unchanged from current source."""
from pathlib import Path
import sys

out = Path(sys.argv[1]).resolve()
task = Path(__file__).resolve().parent
if task not in out.parents:
    raise ValueError('Support output must stay in G03 task directory')

def java(package, name, body):
    file = out / Path(package.replace('.', '/')) / (name + '.java')
    file.parent.mkdir(parents=True, exist_ok=True)
    file.write_text('package ' + package + ';\n' + body + '\n', encoding='utf-8')

base='cn.iocoder.yudao.module.dcc.'
java('cn.iocoder.yudao.framework.mybatis.core.dataobject','BaseDO','public abstract class BaseDO {}')
java('cn.iocoder.yudao.framework.common.util.object','BeanUtils','public final class BeanUtils {public static <T>T toBean(Object source,Class<T> type){return cn.hutool.core.bean.BeanUtil.toBean(source,type);}}')
java('cn.iocoder.yudao.framework.common.util.collection','CollectionUtils','public final class CollectionUtils {public static <T,K,V> java.util.Map<K,V> convertMap(java.util.List<T> rows,java.util.function.Function<T,K> key,java.util.function.Function<T,V> value){var result=new java.util.HashMap<K,V>();for(var row:rows)result.put(key.apply(row),value.apply(row));return result;}public static <T,R>java.util.List<R> convertList(java.util.List<T> rows,java.util.function.Function<T,R> mapper){return rows.stream().map(mapper).toList();}}')
java('cn.iocoder.yudao.framework.common.pojo','CommonResult','public class CommonResult<T>{private T data;public T getData(){return data;}public static <T>CommonResult<T> success(T data){var result=new CommonResult<T>();result.data=data;return result;}}')
java('cn.iocoder.yudao.framework.common.exception','ErrorCode','public record ErrorCode(int code,String message) {}')
java('cn.iocoder.yudao.framework.common.exception.util','ServiceExceptionUtil','public final class ServiceExceptionUtil {public static RuntimeException exception(cn.iocoder.yudao.framework.common.exception.ErrorCode code,Object...args){return new IllegalStateException(code.message()+java.util.Arrays.toString(args));}}')
names=['FILE_CATEGORY_DELETE_CHILD_EXISTS','FILE_CATEGORY_DELETE_RELATION_EXISTS','FILE_CATEGORY_DELETE_REFERENCED','FILE_CATEGORY_LIFECYCLE_STAGE_INVALID','FILE_CATEGORY_NOT_EXISTS','FILE_DIRECTORY_NOT_EXISTS','INTAUTH_FILE_CATEGORY_SYNC_AMBIGUOUS']
names += ['FILE_TYPE_TAXONOMY_DELETE_CHILD_EXISTS','FILE_TYPE_TAXONOMY_DELETE_REFERENCED','FILE_TYPE_TAXONOMY_DUPLICATE_SIBLING','FILE_TYPE_TAXONOMY_INACTIVE','FILE_TYPE_TAXONOMY_LEVEL_INVALID','FILE_TYPE_TAXONOMY_NOT_EXISTS','FILE_TYPE_TAXONOMY_PARENT_CHANGE_FORBIDDEN','FILE_TYPE_TAXONOMY_PARENT_NOT_EXISTS','PROJECT_FILE_TEMPLATE_CATEGORY_INVALID']
java(base+'enums','ErrorCodeConstants','public final class ErrorCodeConstants {'+''.join('public static final cn.iocoder.yudao.framework.common.exception.ErrorCode '+n+'=new cn.iocoder.yudao.framework.common.exception.ErrorCode(1,"'+n+'");' for n in names)+'}')
java(base+'service.file','DccStorageMappingMutationGuard','public class DccStorageMappingMutationGuard {public void requireCategoryUnmapped(Long id){throw new UnsupportedOperationException("Unaccepted mutation port");}}')
for name in ['DccControlledFileDO','DccControlledFileMasterDO']:
    java(base+'dal.dataobject.file',name,'public class '+name+' {public Long getCategoryId(){return null;}public Long getFileTypeTaxonomyId(){return null;}}')
for group,name in [('protection','DccControlledFileUploadPolicyDO'),('route','DccCategoryApprovalRouteDO'),('category','DccCategoryViewMatrixRuleDO'),('category','DccFileCategoryPermissionRuleDO'),('category','DccFileCategoryDistributionRuleDO'),('category','DccFileCategoryTrainingRuleDO')]:
    java(base+'dal.dataobject.'+group,name,'public class '+name+' {public Long getCategoryId(){return null;}}')
entity=base+'dal.dataobject.category.DccFileCategoryDO'
java(base+'dal.mysql.category','DccFileCategoryMapper','public interface DccFileCategoryMapper {int insert('+entity+' row);int updateById('+entity+' row);'+entity+' selectById(Long id);java.util.List<'+entity+'> selectList();java.util.List<'+entity+'> selectMappingCategoriesForUpdate(Long tenant,Long type);<T>Long selectCount(java.util.function.Function<'+entity+',T> f,Object value);int deleteById(Long id);}')
entity=base+'dal.dataobject.category.DccCategoryDirectoryBindingDO'
java(base+'dal.mysql.category','DccCategoryDirectoryBindingMapper','public interface DccCategoryDirectoryBindingMapper {<T>java.util.List<'+entity+'> selectList(java.util.function.Function<'+entity+',T> f,Object v);<T>Long selectCount(java.util.function.Function<'+entity+',T> f,Object v);int deleteAllByCategoryIdForce(Long id);int insert('+entity+' row);}')
ports=[('category','DccCategoryViewMatrixRuleMapper','category','DccCategoryViewMatrixRuleDO'),('category','DccFileCategoryPermissionRuleMapper','category','DccFileCategoryPermissionRuleDO'),('category','DccFileCategoryDistributionRuleMapper','category','DccFileCategoryDistributionRuleDO'),('category','DccFileCategoryTrainingRuleMapper','category','DccFileCategoryTrainingRuleDO'),('file','DccControlledFileMapper','file','DccControlledFileDO'),('file','DccControlledFileMasterMapper','file','DccControlledFileMasterDO'),('protection','DccControlledFileUploadPolicyMapper','protection','DccControlledFileUploadPolicyDO'),('route','DccCategoryApprovalRouteMapper','route','DccCategoryApprovalRouteDO')]
for group,name,eg,en in ports:
    java(base+'dal.mysql.'+group,name,'public interface '+name+' {<T>Long selectCount(java.util.function.Function<'+base+'dal.dataobject.'+eg+'.'+en+',T> f,Object value);}')
java(base+'dal.mysql.directory','DccFileDirectoryMapper','public interface DccFileDirectoryMapper {Object selectById(Long id);}')
java(base+'service.category','DccIntAuthFileCategoryClient','public interface DccIntAuthFileCategoryClient {record IntAuthFileCategory(Long id,String name,Boolean active){}java.util.List<IntAuthFileCategory> listFileCategories();}')
java('cn.iocoder.yudao.framework.tenant.core.context','TenantContextHolder','public final class TenantContextHolder {public static Long getRequiredTenantId(){return 1L;}}')
entity=base+'dal.dataobject.category.DccFileTypeTaxonomyDO'
java(base+'dal.mysql.category','DccFileTypeTaxonomyMapper','public interface DccFileTypeTaxonomyMapper {int insert('+entity+' row);int updateById('+entity+' row);'+entity+' selectById(Long id);'+entity+' selectMappingTaxonomyForUpdate(Long tenant,Long id);java.util.List<'+entity+'> selectList();<T>Long selectCount(java.util.function.Function<'+entity+',T> f,Object v);int deleteById(Long id);}')
java(base+'dal.dataobject.projectcode','DccProjectFileTemplateItemDO','public class DccProjectFileTemplateItemDO {public Long getFileTypeTaxonomyId(){return null;}}')
java(base+'dal.mysql.projectcode','DccProjectFileTemplateItemMapper','public interface DccProjectFileTemplateItemMapper {<T>Long selectCount(java.util.function.Function<'+base+'dal.dataobject.projectcode.DccProjectFileTemplateItemDO,T> f,Object value);}')
print('Generated explicit isolated support; no production logic reimplemented; real Hutool copy + real H2 Category port.')
