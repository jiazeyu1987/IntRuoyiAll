package cn.iocoder.yudao.module.system.dal.mysql.codextest;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestExecutionPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestExecutionDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface CodexTestExecutionMapper extends BaseMapperX<CodexTestExecutionDO> {

    default PageResult<CodexTestExecutionDO> selectPage(CodexTestExecutionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CodexTestExecutionDO>()
                .eqIfPresent(CodexTestExecutionDO::getTargetTenantId, reqVO.getTargetTenantId())
                .eqIfPresent(CodexTestExecutionDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(BaseDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CodexTestExecutionDO::getId));
    }

    default int updateStatus(Long id, String status, LocalDateTime startedAt, LocalDateTime finishedAt,
                             String summary, Long runnerSessionId) {
        return update(null, new LambdaUpdateWrapper<CodexTestExecutionDO>()
                .eq(CodexTestExecutionDO::getId, id)
                .set(CodexTestExecutionDO::getStatus, status)
                .set(startedAt != null, CodexTestExecutionDO::getStartedAt, startedAt)
                .set(CodexTestExecutionDO::getFinishedAt, finishedAt)
                .set(CodexTestExecutionDO::getSummary, summary)
                .set(runnerSessionId != null, CodexTestExecutionDO::getRunnerSessionId, runnerSessionId));
    }

}
