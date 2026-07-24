package cn.iocoder.yudao.module.dcc.dal.mysql.category;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryViewMatrixRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccCategoryViewMatrixRuleMapper extends BaseMapperX<DccCategoryViewMatrixRuleDO> {

    default List<DccCategoryViewMatrixRuleDO> selectActiveListByCategoryId(Long categoryId) {
        return selectList(DccCategoryViewMatrixRuleDO::getCategoryId, categoryId,
                DccCategoryViewMatrixRuleDO::getActive, Boolean.TRUE);
    }
}
