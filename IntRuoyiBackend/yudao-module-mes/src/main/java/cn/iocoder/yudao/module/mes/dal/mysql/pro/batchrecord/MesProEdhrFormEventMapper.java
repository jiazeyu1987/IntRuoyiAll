package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormEventPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrFormEventDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrFormEventMapper extends BaseMapperX<MesProEdhrFormEventDO> {

    default PageResult<MesProEdhrFormEventDO> selectPage(MesProEdhrFormEventPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrFormEventDO>()
                .eqIfPresent(MesProEdhrFormEventDO::getInstanceId, reqVO.getInstanceId())
                .eqIfPresent(MesProEdhrFormEventDO::getTemplateId, reqVO.getTemplateId())
                .likeIfPresent(MesProEdhrFormEventDO::getInstanceCode, reqVO.getInstanceCode())
                .eqIfPresent(MesProEdhrFormEventDO::getEventType, reqVO.getEventType())
                .eqIfPresent(MesProEdhrFormEventDO::getResultStatus, reqVO.getResultStatus())
                .betweenIfPresent(MesProEdhrFormEventDO::getOccurredAt, reqVO.getOccurredAt())
                .orderByDesc(MesProEdhrFormEventDO::getOccurredAt)
                .orderByDesc(MesProEdhrFormEventDO::getId));
    }
}
