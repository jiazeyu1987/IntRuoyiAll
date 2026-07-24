package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerEventPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrTravelerEventDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrTravelerEventMapper extends BaseMapperX<MesProEdhrTravelerEventDO> {

    default PageResult<MesProEdhrTravelerEventDO> selectPage(MesProEdhrTravelerEventPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrTravelerEventDO>()
                .eqIfPresent(MesProEdhrTravelerEventDO::getTravelerId, reqVO.getTravelerId())
                .likeIfPresent(MesProEdhrTravelerEventDO::getTravelerCode, reqVO.getTravelerCode())
                .eqIfPresent(MesProEdhrTravelerEventDO::getEventType, reqVO.getEventType())
                .eqIfPresent(MesProEdhrTravelerEventDO::getResultStatus, reqVO.getResultStatus())
                .betweenIfPresent(MesProEdhrTravelerEventDO::getOccurredAt, reqVO.getOccurredAt())
                .orderByDesc(MesProEdhrTravelerEventDO::getOccurredAt)
                .orderByDesc(MesProEdhrTravelerEventDO::getId));
    }
}
