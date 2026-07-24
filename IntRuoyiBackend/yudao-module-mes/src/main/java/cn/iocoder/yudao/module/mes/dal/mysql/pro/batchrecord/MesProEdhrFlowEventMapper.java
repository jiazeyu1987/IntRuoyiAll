package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowEventPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrFlowEventDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProEdhrFlowEventMapper extends BaseMapperX<MesProEdhrFlowEventDO> {

    default PageResult<MesProEdhrFlowEventDO> selectPage(MesProEdhrFlowEventPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrFlowEventDO>()
                .eq(MesProEdhrFlowEventDO::getBusinessObjectType, reqVO.getBusinessObjectType())
                .eq(MesProEdhrFlowEventDO::getBusinessObjectId, reqVO.getBusinessObjectId())
                .eqIfPresent(MesProEdhrFlowEventDO::getFlowInstanceId, reqVO.getFlowInstanceId())
                .eqIfPresent(MesProEdhrFlowEventDO::getEventType, reqVO.getEventType())
                .orderByDesc(MesProEdhrFlowEventDO::getOccurredAt)
                .orderByDesc(MesProEdhrFlowEventDO::getId));
    }

    default List<MesProEdhrFlowEventDO> selectListByTaskIds(Collection<String> taskIds, String eventType) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProEdhrFlowEventDO>()
                .in(MesProEdhrFlowEventDO::getTaskId, taskIds)
                .eqIfPresent(MesProEdhrFlowEventDO::getEventType, eventType)
                .orderByDesc(MesProEdhrFlowEventDO::getOccurredAt)
                .orderByDesc(MesProEdhrFlowEventDO::getId));
    }
}
