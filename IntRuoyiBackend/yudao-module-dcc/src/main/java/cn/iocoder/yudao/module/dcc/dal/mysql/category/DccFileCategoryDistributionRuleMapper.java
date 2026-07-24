package cn.iocoder.yudao.module.dcc.dal.mysql.category;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDistributionRuleDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DccFileCategoryDistributionRuleMapper extends BaseMapperX<DccFileCategoryDistributionRuleDO> {

    @Delete("DELETE FROM dcc_file_category_distribution_rule WHERE category_id = #{categoryId}")
    void deleteByCategoryIdHard(Long categoryId);
}
