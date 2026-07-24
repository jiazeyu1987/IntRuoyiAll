package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeliveryProjectPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeliveryProjectDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrDeliveryProjectMapper extends BaseMapperX<MesProEdhrDeliveryProjectDO> {

    default PageResult<MesProEdhrDeliveryProjectDO> selectPage(MesProEdhrDeliveryProjectPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrDeliveryProjectDO>()
                .likeIfPresent(MesProEdhrDeliveryProjectDO::getProjectCode, reqVO.getProjectCode())
                .likeIfPresent(MesProEdhrDeliveryProjectDO::getProjectName, reqVO.getProjectName())
                .likeIfPresent(MesProEdhrDeliveryProjectDO::getCustomerName, reqVO.getCustomerName())
                .eqIfPresent(MesProEdhrDeliveryProjectDO::getProjectStatus, reqVO.getProjectStatus())
                .orderByDesc(MesProEdhrDeliveryProjectDO::getId));
    }
}
