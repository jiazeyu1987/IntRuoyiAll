package cn.iocoder.yudao.module.system.dal.mysql.codextest;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestCheckpointResultDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface CodexTestCheckpointResultMapper extends BaseMapperX<CodexTestCheckpointResultDO> {

    default List<CodexTestCheckpointResultDO> selectListByExecutionCaseIds(Collection<Long> executionCaseIds) {
        return selectList(new LambdaQueryWrapperX<CodexTestCheckpointResultDO>()
                .in(CodexTestCheckpointResultDO::getExecutionCaseId, executionCaseIds)
                .orderByAsc(CodexTestCheckpointResultDO::getExecutionCaseId)
                .orderByAsc(CodexTestCheckpointResultDO::getCheckpointSort));
    }

    default List<CodexTestCheckpointResultDO> selectListByExecutionCaseId(Long executionCaseId) {
        return selectList(new LambdaQueryWrapperX<CodexTestCheckpointResultDO>()
                .eq(CodexTestCheckpointResultDO::getExecutionCaseId, executionCaseId)
                .orderByAsc(CodexTestCheckpointResultDO::getCheckpointSort));
    }

    default CodexTestCheckpointResultDO selectByCaseAndSort(Long executionCaseId, Integer sort) {
        return selectOne(new LambdaQueryWrapperX<CodexTestCheckpointResultDO>()
                .eq(CodexTestCheckpointResultDO::getExecutionCaseId, executionCaseId)
                .eq(CodexTestCheckpointResultDO::getCheckpointSort, sort));
    }

    default int updateResult(Long id, String status, String actualText, String mismatchDescription,
                             Long screenshotArtifactId, LocalDateTime completedAt) {
        return update(null, new LambdaUpdateWrapper<CodexTestCheckpointResultDO>()
                .eq(CodexTestCheckpointResultDO::getId, id)
                .set(CodexTestCheckpointResultDO::getStatus, status)
                .set(CodexTestCheckpointResultDO::getActualText, actualText)
                .set(CodexTestCheckpointResultDO::getMismatchDescription, mismatchDescription)
                .set(CodexTestCheckpointResultDO::getScreenshotArtifactId, screenshotArtifactId)
                .set(CodexTestCheckpointResultDO::getCompletedAt, completedAt));
    }

}
