package cn.iocoder.yudao.module.dcc.dal.mysql.category;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * DCC file category mapper.
 */
@Mapper
public interface DccFileCategoryMapper extends BaseMapperX<DccFileCategoryDO> {
    // 唯一索引包含已删除类别；编码检查必须使用同样的租户与删除范围。
    @Select("<script>SELECT COUNT(*) FROM dcc_file_category WHERE tenant_id=#{tenantId} AND code=#{code}"
            + "<if test='excludeId != null'> AND id != #{excludeId}</if></script>")
    long countReservedCode(@Param("tenantId") Long tenantId, @Param("code") String code,
                           @Param("excludeId") Long excludeId);

    // 冲突后使用当前读，避免 MySQL 可重复读事务仍看到写入前的快照。
    @Select("<script>SELECT id FROM dcc_file_category WHERE tenant_id=#{tenantId} AND code=#{code}"
            + "<if test='excludeId != null'> AND id != #{excludeId}</if> FOR UPDATE</script>")
    List<Long> selectReservedCodeForUpdate(@Param("tenantId") Long tenantId, @Param("code") String code,
                                          @Param("excludeId") Long excludeId);

    @Select("SELECT * FROM dcc_file_category WHERE tenant_id=#{tenantId} AND file_type_taxonomy_id=#{taxonomyId} AND deleted=0 FOR UPDATE")
    List<DccFileCategoryDO> selectMappingCategoriesForUpdate(@Param("tenantId") Long tenantId,
                                                            @Param("taxonomyId") Long taxonomyId);
    @Select("SELECT * FROM dcc_file_category WHERE tenant_id=#{tenantId} AND id=#{categoryId} AND deleted=0 FOR UPDATE")
    DccFileCategoryDO selectMatrixCategoryForUpdate(@Param("tenantId") Long tenantId,
                                                   @Param("categoryId") Long categoryId);
}
