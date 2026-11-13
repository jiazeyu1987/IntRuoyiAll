package cn.iocoder.yudao.module.system.dal.mysql.codextest;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestExecutionPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestExecutionDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CodexTestExecutionMapper extends BaseMapperX<CodexTestExecutionDO> {

    default PageResult<CodexTestExecutionDO> selectPage(CodexTestExecutionPageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<CodexTestExecutionDO>()
                .eqIfPresent(CodexTestExecutionDO::getTargetTenantId, reqVO.getTargetTenantId())
                .eqIfPresent(CodexTestExecutionDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(BaseDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CodexTestExecutionDO::getId), java.util.Map.ofEntries(
                java.util.Map.entry("id", number("id")),
                java.util.Map.entry("startedAt", dateTime("started_at")),
                java.util.Map.entry("finishedAt", dateTime("finished_at")),
                java.util.Map.entry("summary", text("summary")),
                java.util.Map.entry("targetTenantId", text("COALESCE((SELECT t.name FROM system_tenant t WHERE t.id=system_codex_test_execution.target_tenant_id AND t.deleted=0),CAST(target_tenant_id AS CHAR))")),
                java.util.Map.entry("targetTenant", text("COALESCE((SELECT t.name FROM system_tenant t WHERE t.id=system_codex_test_execution.target_tenant_id AND t.deleted=0),CAST(target_tenant_id AS CHAR))")),
                java.util.Map.entry("executionMode", text("CASE execution_mode WHEN 'SEQUENTIAL' THEN '顺序执行' WHEN 'PARALLEL' THEN '并行执行' ELSE execution_mode END")),
                java.util.Map.entry("status", text("CASE status WHEN 'PENDING' THEN '待执行' WHEN 'CLAIMED' THEN '已领取' WHEN 'RUNNING' THEN '执行中' WHEN 'PASS' THEN '通过' WHEN 'FAIL' THEN '失败' WHEN 'BLOCKED' THEN '阻塞' WHEN 'CANCELED' THEN '已取消' WHEN 'TIMEOUT' THEN '超时' WHEN 'NOT_RUN' THEN '未执行' ELSE status END"))
        )));
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

    default List<CodexTestExecutionDO> selectMonitorList() {
        return selectList(new LambdaQueryWrapperX<CodexTestExecutionDO>()
                .in(CodexTestExecutionDO::getStatus, List.of("PENDING", "CLAIMED", "RUNNING"))
                .orderByDesc(CodexTestExecutionDO::getId));
    }

}
