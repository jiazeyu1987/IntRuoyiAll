package cn.iocoder.yudao.module.dcc.dal.mysql.category;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DccFileCategoryPermissionRuleMapper extends BaseMapperX<DccFileCategoryPermissionRuleDO> {

    @Delete("DELETE FROM dcc_file_category_permission_rule WHERE category_id = #{categoryId}")
    int deleteByCategoryIdHard(Long categoryId);

    @Delete("""
            DELETE FROM dcc_file_category_permission_rule
            WHERE category_id = #{categoryId}
              AND UPPER(action_type) NOT IN ('REVIEW', 'APPROVE')
            """)
    int deleteConfigurableByCategoryIdHard(Long categoryId);
}
