package cn.iocoder.yudao.module.system.dal.mysql.codextest;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestExecutionCaseDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface CodexTestExecutionCaseMapper extends BaseMapperX<CodexTestExecutionCaseDO> {

    default List<CodexTestExecutionCaseDO> selectListByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapperX<CodexTestExecutionCaseDO>()
                .eq(CodexTestExecutionCaseDO::getExecutionId, executionId)
                .orderByAsc(CodexTestExecutionCaseDO::getId));
    }

    default List<CodexTestExecutionCaseDO> selectPendingClaimCandidates(int limit) {
        return selectList(new LambdaQueryWrapperX<CodexTestExecutionCaseDO>()
                .eq(CodexTestExecutionCaseDO::getStatus, "PENDING")
                .orderByAsc(CodexTestExecutionCaseDO::getExecutionId)
                .orderByAsc(CodexTestExecutionCaseDO::getId)
                .last("LIMIT " + limit));
    }

    default Long selectRunningCountByCaseId(Long caseId) {
        return selectCount(new LambdaQueryWrapperX<CodexTestExecutionCaseDO>()
                .eq(CodexTestExecutionCaseDO::getCaseId, caseId)
                .in(CodexTestExecutionCaseDO::getStatus, List.of("PENDING", "CLAIMED", "RUNNING")));
    }

    default Long selectUnfinishedCountByExecutionId(Long executionId) {
        return selectCount(new LambdaQueryWrapperX<CodexTestExecutionCaseDO>()
                .eq(CodexTestExecutionCaseDO::getExecutionId, executionId)
                .in(CodexTestExecutionCaseDO::getStatus, List.of("PENDING", "CLAIMED", "RUNNING")));
    }

    default Long selectFailedCountByExecutionId(Long executionId) {
        return selectCount(new LambdaQueryWrapperX<CodexTestExecutionCaseDO>()
                .eq(CodexTestExecutionCaseDO::getExecutionId, executionId)
                .in(CodexTestExecutionCaseDO::getStatus, List.of("FAIL", "BLOCKED", "TIMEOUT")));
    }

    default int claim(Long id, Long runnerSessionId, LocalDateTime claimTime) {
        return update(null, new LambdaUpdateWrapper<CodexTestExecutionCaseDO>()
                .eq(CodexTestExecutionCaseDO::getId, id)
                .eq(CodexTestExecutionCaseDO::getStatus, "PENDING")
                .set(CodexTestExecutionCaseDO::getStatus, "CLAIMED")
                .set(CodexTestExecutionCaseDO::getRunnerSessionId, runnerSessionId)
                .set(CodexTestExecutionCaseDO::getClaimTime, claimTime));
    }

    default int markRunning(Long id, Long runnerSessionId, LocalDateTime startedAt) {
        return update(null, new LambdaUpdateWrapper<CodexTestExecutionCaseDO>()
                .eq(CodexTestExecutionCaseDO::getId, id)
                .eq(CodexTestExecutionCaseDO::getRunnerSessionId, runnerSessionId)
                .in(CodexTestExecutionCaseDO::getStatus, List.of("CLAIMED", "RUNNING"))
                .set(CodexTestExecutionCaseDO::getStatus, "RUNNING")
                .set(CodexTestExecutionCaseDO::getStartedAt, startedAt));
    }

    default int updateProgress(Long id, Long runnerSessionId, String phase, Integer currentMethodSort,
                               Integer currentCheckpointSort, String progressMessage, LocalDateTime startedAt) {
        return update(null, new LambdaUpdateWrapper<CodexTestExecutionCaseDO>()
                .eq(CodexTestExecutionCaseDO::getId, id)
                .eq(CodexTestExecutionCaseDO::getRunnerSessionId, runnerSessionId)
                .in(CodexTestExecutionCaseDO::getStatus, List.of("CLAIMED", "RUNNING"))
                .set(CodexTestExecutionCaseDO::getStatus, "RUNNING")
                .set(startedAt != null, CodexTestExecutionCaseDO::getStartedAt, startedAt)
                .set(CodexTestExecutionCaseDO::getProgressPhase, phase)
                .set(CodexTestExecutionCaseDO::getCurrentMethodSort, currentMethodSort)
                .set(CodexTestExecutionCaseDO::getCurrentCheckpointSort, currentCheckpointSort)
                .set(CodexTestExecutionCaseDO::getProgressMessage, progressMessage));
    }

    default int complete(Long id, Long runnerSessionId, String status, String summary,
                         LocalDateTime startedAt, LocalDateTime finishedAt) {
        return update(null, new LambdaUpdateWrapper<CodexTestExecutionCaseDO>()
                .eq(CodexTestExecutionCaseDO::getId, id)
                .eq(CodexTestExecutionCaseDO::getRunnerSessionId, runnerSessionId)
                .in(CodexTestExecutionCaseDO::getStatus, List.of("CLAIMED", "RUNNING"))
                .set(CodexTestExecutionCaseDO::getStatus, status)
                .set(startedAt != null, CodexTestExecutionCaseDO::getStartedAt, startedAt)
                .set(CodexTestExecutionCaseDO::getFinishedAt, finishedAt)
                .set(CodexTestExecutionCaseDO::getFailureReason, summary)
                .set(CodexTestExecutionCaseDO::getProgressPhase, "DONE")
                .set(CodexTestExecutionCaseDO::getCurrentMethodSort, null)
                .set(CodexTestExecutionCaseDO::getCurrentCheckpointSort, null)
                .set(CodexTestExecutionCaseDO::getProgressMessage, summary));
    }

    default int cancelByExecutionId(Long executionId) {
        return update(null, new LambdaUpdateWrapper<CodexTestExecutionCaseDO>()
                .eq(CodexTestExecutionCaseDO::getExecutionId, executionId)
                .in(CodexTestExecutionCaseDO::getStatus, List.of("PENDING", "CLAIMED", "RUNNING"))
                .set(CodexTestExecutionCaseDO::getStatus, "CANCELED"));
    }

    default List<CodexTestExecutionCaseDO> selectListByIds(Collection<Long> ids) {
        return selectList(CodexTestExecutionCaseDO::getId, ids);
    }

}
