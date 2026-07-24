package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionArchiveEventDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProBatchRecordExecutionArchiveEventMapper extends BaseMapperX<MesProBatchRecordExecutionArchiveEventDO> {

    default List<MesProBatchRecordExecutionArchiveEventDO> selectListByArchiveId(Long archiveId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionArchiveEventDO>()
                .eq(MesProBatchRecordExecutionArchiveEventDO::getArchiveId, archiveId)
                .orderByDesc(MesProBatchRecordExecutionArchiveEventDO::getEventTime)
                .orderByDesc(MesProBatchRecordExecutionArchiveEventDO::getId));
    }
}
