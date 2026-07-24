package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeEventPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrUnifiedChangeEventDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrUnifiedChangeEventMapper extends BaseMapperX<MesProEdhrUnifiedChangeEventDO> {

    default MesProEdhrUnifiedChangeEventDO selectByChangeRequestIdAndEventTypeAndIdempotencyKey(
            Long changeRequestId, String eventType, String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrUnifiedChangeEventDO>()
                .eq(MesProEdhrUnifiedChangeEventDO::getChangeRequestId, changeRequestId)
                .eq(MesProEdhrUnifiedChangeEventDO::getEventType, eventType)
                .eq(MesProEdhrUnifiedChangeEventDO::getIdempotencyKey, idempotencyKey));
    }

    default PageResult<MesProEdhrUnifiedChangeEventDO> selectPage(MesProEdhrUnifiedChangeEventPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrUnifiedChangeEventDO>()
                .eqIfPresent(MesProEdhrUnifiedChangeEventDO::getChangeRequestId, reqVO.getChangeRequestId())
                .eqIfPresent(MesProEdhrUnifiedChangeEventDO::getEventType, reqVO.getEventType())
                .orderByDesc(MesProEdhrUnifiedChangeEventDO::getOccurredAt)
                .orderByDesc(MesProEdhrUnifiedChangeEventDO::getId));
    }
}
