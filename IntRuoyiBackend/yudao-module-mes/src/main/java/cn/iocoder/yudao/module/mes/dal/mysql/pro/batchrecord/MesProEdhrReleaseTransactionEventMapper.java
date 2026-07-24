package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseEventPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionEventDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrReleaseTransactionEventMapper extends BaseMapperX<MesProEdhrReleaseTransactionEventDO> {

    default MesProEdhrReleaseTransactionEventDO selectByReleaseTransactionIdAndEventTypeAndIdempotencyKey(
            Long releaseTransactionId, String eventType, String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrReleaseTransactionEventDO>()
                .eq(MesProEdhrReleaseTransactionEventDO::getReleaseTransactionId, releaseTransactionId)
                .eq(MesProEdhrReleaseTransactionEventDO::getEventType, eventType)
                .eq(MesProEdhrReleaseTransactionEventDO::getIdempotencyKey, idempotencyKey));
    }

    default PageResult<MesProEdhrReleaseTransactionEventDO> selectPage(MesProEdhrReleaseEventPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrReleaseTransactionEventDO>()
                .eq(MesProEdhrReleaseTransactionEventDO::getReleaseTransactionId, reqVO.getReleaseTransactionId())
                .eqIfPresent(MesProEdhrReleaseTransactionEventDO::getEventType, reqVO.getEventType())
                .orderByDesc(MesProEdhrReleaseTransactionEventDO::getOccurredAt)
                .orderByDesc(MesProEdhrReleaseTransactionEventDO::getId));
    }
}
