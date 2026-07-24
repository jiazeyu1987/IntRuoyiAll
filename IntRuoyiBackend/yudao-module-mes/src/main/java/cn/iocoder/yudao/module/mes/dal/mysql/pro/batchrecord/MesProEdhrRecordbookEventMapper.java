package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEventPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordbookEventDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrRecordbookEventMapper extends BaseMapperX<MesProEdhrRecordbookEventDO> {

    default PageResult<MesProEdhrRecordbookEventDO> selectPage(MesProEdhrRecordbookEventPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrRecordbookEventDO>()
                .eqIfPresent(MesProEdhrRecordbookEventDO::getRecordbookId, reqVO.getRecordbookId())
                .eqIfPresent(MesProEdhrRecordbookEventDO::getEntryId, reqVO.getEntryId())
                .eqIfPresent(MesProEdhrRecordbookEventDO::getEventType, reqVO.getEventType())
                .eqIfPresent(MesProEdhrRecordbookEventDO::getResultStatus, reqVO.getResultStatus())
                .betweenIfPresent(MesProEdhrRecordbookEventDO::getOccurredAt, reqVO.getOccurredAt())
                .orderByDesc(MesProEdhrRecordbookEventDO::getId));
    }
}
