package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccNasControlAuditTaskDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DccNasControlAuditTaskMapper extends BaseMapperX<DccNasControlAuditTaskDO> {

    default DccNasControlAuditTaskDO selectActiveTask() {
        return selectOne(new LambdaQueryWrapperX<DccNasControlAuditTaskDO>()
                .in(DccNasControlAuditTaskDO::getStatus, "WAITING", "RUNNING")
                .orderByDesc(DccNasControlAuditTaskDO::getId)
                .last("LIMIT 1"));
    }

    default List<DccNasControlAuditTaskDO> selectWaitingTasks() {
        return selectList(new LambdaQueryWrapperX<DccNasControlAuditTaskDO>()
                .eq(DccNasControlAuditTaskDO::getStatus, "WAITING")
                .orderByAsc(DccNasControlAuditTaskDO::getId));
    }

    default int claimWaitingTask(Long taskId, LocalDateTime startedAt) {
        return update(null, new LambdaUpdateWrapper<DccNasControlAuditTaskDO>()
                .eq(DccNasControlAuditTaskDO::getId, taskId)
                .eq(DccNasControlAuditTaskDO::getStatus, "WAITING")
                .set(DccNasControlAuditTaskDO::getStatus, "RUNNING")
                .set(DccNasControlAuditTaskDO::getStartedAt, startedAt));
    }

    default int recoverRunningTasksToWaiting() {
        return update(null, new LambdaUpdateWrapper<DccNasControlAuditTaskDO>()
                .eq(DccNasControlAuditTaskDO::getStatus, "RUNNING")
                .set(DccNasControlAuditTaskDO::getStatus, "WAITING"));
    }
}
