package cn.iocoder.yudao.module.srm.dal.mysql.framework;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.controller.admin.framework.vo.SrmFrameworkPlanPageReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.framework.SrmFrameworkPlanDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmFrameworkPlanMapper extends BaseMapperX<SrmFrameworkPlanDO> {

    default PageResult<SrmFrameworkPlanDO> selectPage(SrmFrameworkPlanPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmFrameworkPlanDO>()
                .likeIfPresent(SrmFrameworkPlanDO::getFrameworkPlanNo, reqVO.getFrameworkPlanNo())
                .likeIfPresent(SrmFrameworkPlanDO::getPlanTitle, reqVO.getPlanTitle())
                .likeIfPresent(SrmFrameworkPlanDO::getSupplierName, reqVO.getSupplierName())
                .eqIfPresent(SrmFrameworkPlanDO::getPlanStatus, reqVO.getPlanStatus())
                .orderByDesc(SrmFrameworkPlanDO::getId));
    }
}
