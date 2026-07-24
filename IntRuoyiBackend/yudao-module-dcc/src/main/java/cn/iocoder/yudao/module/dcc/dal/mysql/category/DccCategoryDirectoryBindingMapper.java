package cn.iocoder.yudao.module.dcc.dal.mysql.category;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;

/**
 * DCC category directory binding mapper.
 */
@Mapper
public interface DccCategoryDirectoryBindingMapper extends BaseMapperX<DccCategoryDirectoryBindingDO> {

    default DccCategoryDirectoryBindingDO selectActiveByCategoryId(Long categoryId) {
        return selectFirstOne(DccCategoryDirectoryBindingDO::getCategoryId, categoryId,
                DccCategoryDirectoryBindingDO::getActive, Boolean.TRUE);
    }

    @Delete("DELETE FROM dcc_category_directory_binding WHERE category_id = #{categoryId}")
    int deleteAllByCategoryIdForce(@Param("categoryId") Long categoryId);
}
