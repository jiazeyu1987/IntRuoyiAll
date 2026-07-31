package cn.iocoder.yudao.module.dcc.dal.mysql.category;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryMatchRuleDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DccFileCategoryMatchRuleMapper extends BaseMapperX<DccFileCategoryMatchRuleDO> {

    @Delete("DELETE FROM dcc_file_category_match_rule WHERE category_id = #{categoryId}")
    int deleteByCategoryIdHard(Long categoryId);
}
