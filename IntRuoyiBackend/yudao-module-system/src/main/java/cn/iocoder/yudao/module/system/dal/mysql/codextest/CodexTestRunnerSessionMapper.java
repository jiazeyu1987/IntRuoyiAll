package cn.iocoder.yudao.module.system.dal.mysql.codextest;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestRunnerSessionDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CodexTestRunnerSessionMapper extends BaseMapperX<CodexTestRunnerSessionDO> {

    default List<CodexTestRunnerSessionDO> selectOnlineSessions(LocalDateTime threshold) {
        return selectList(new LambdaQueryWrapperX<CodexTestRunnerSessionDO>()
                .eq(CodexTestRunnerSessionDO::getStatus, "ONLINE")
                .ge(CodexTestRunnerSessionDO::getLastHeartbeatTime, threshold)
                .orderByDesc(CodexTestRunnerSessionDO::getLastHeartbeatTime));
    }

    default int heartbeat(Long id, LocalDateTime heartbeatTime, Integer runningCount) {
        return update(null, new LambdaUpdateWrapper<CodexTestRunnerSessionDO>()
                .eq(CodexTestRunnerSessionDO::getId, id)
                .eq(CodexTestRunnerSessionDO::getStatus, "ONLINE")
                .set(CodexTestRunnerSessionDO::getLastHeartbeatTime, heartbeatTime)
                .set(CodexTestRunnerSessionDO::getCurrentRunningCount, runningCount));
    }

}
