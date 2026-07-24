package cn.iocoder.yudao.module.srm.dal.mysql.framework;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.framework.SrmFrameworkPlanLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmFrameworkPlanLineMapper extends BaseMapperX<SrmFrameworkPlanLineDO> {

    default List<SrmFrameworkPlanLineDO> selectListByFrameworkPlanId(Long frameworkPlanId) {
        return selectList(new LambdaQueryWrapperX<SrmFrameworkPlanLineDO>()
                .eq(SrmFrameworkPlanLineDO::getFrameworkPlanId, frameworkPlanId)
                .orderByAsc(SrmFrameworkPlanLineDO::getId));
    }
}
