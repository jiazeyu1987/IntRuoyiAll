package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormInstancePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrFormInstanceDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrFormInstanceMapper extends BaseMapperX<MesProEdhrFormInstanceDO> {

    default PageResult<MesProEdhrFormInstanceDO> selectPage(MesProEdhrFormInstancePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrFormInstanceDO>()
                .likeIfPresent(MesProEdhrFormInstanceDO::getInstanceCode, reqVO.getInstanceCode())
                .eqIfPresent(MesProEdhrFormInstanceDO::getTemplateId, reqVO.getTemplateId())
                .likeIfPresent(MesProEdhrFormInstanceDO::getTemplateCode, reqVO.getTemplateCode())
                .eqIfPresent(MesProEdhrFormInstanceDO::getStatus, reqVO.getStatus())
                .likeIfPresent(MesProEdhrFormInstanceDO::getBusinessObjectCode, reqVO.getBusinessObjectCode())
                .betweenIfPresent(MesProEdhrFormInstanceDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesProEdhrFormInstanceDO::getId));
    }
}
