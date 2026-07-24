package cn.iocoder.yudao.module.srm.dal.mysql.procurement;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanPageReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.SrmProcurementPlanDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmProcurementPlanMapper extends BaseMapperX<SrmProcurementPlanDO> {

    default PageResult<SrmProcurementPlanDO> selectPage(SrmProcurementPlanPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmProcurementPlanDO>()
                .likeIfPresent(SrmProcurementPlanDO::getPlanNo, reqVO.getPlanNo())
                .likeIfPresent(SrmProcurementPlanDO::getPlanTitle, reqVO.getPlanTitle())
                .eqIfPresent(SrmProcurementPlanDO::getPlanStatus, reqVO.getPlanStatus())
                .orderByDesc(SrmProcurementPlanDO::getId));
    }
}
