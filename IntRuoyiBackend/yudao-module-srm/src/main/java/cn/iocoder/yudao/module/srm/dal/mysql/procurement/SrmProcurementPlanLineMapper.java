package cn.iocoder.yudao.module.srm.dal.mysql.procurement;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.SrmProcurementPlanLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmProcurementPlanLineMapper extends BaseMapperX<SrmProcurementPlanLineDO> {

    default List<SrmProcurementPlanLineDO> selectListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<SrmProcurementPlanLineDO>()
                .eq(SrmProcurementPlanLineDO::getPlanId, planId)
                .orderByAsc(SrmProcurementPlanLineDO::getId));
    }
}
