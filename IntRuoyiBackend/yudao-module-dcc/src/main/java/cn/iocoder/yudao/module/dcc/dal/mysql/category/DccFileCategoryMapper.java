package cn.iocoder.yudao.module.dcc.dal.mysql.category;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * DCC file category mapper.
 */
@Mapper
public interface DccFileCategoryMapper extends BaseMapperX<DccFileCategoryDO> {
    @Select("SELECT * FROM dcc_file_category WHERE tenant_id=#{tenantId} AND id=#{categoryId} AND deleted=0 FOR UPDATE")
    DccFileCategoryDO selectMatrixCategoryForUpdate(@Param("tenantId") Long tenantId,
                                                   @Param("categoryId") Long categoryId);
}
