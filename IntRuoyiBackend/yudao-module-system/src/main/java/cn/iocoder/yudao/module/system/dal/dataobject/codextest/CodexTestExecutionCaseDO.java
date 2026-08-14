package cn.iocoder.yudao.module.system.dal.dataobject.codextest;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("system_codex_test_execution_case")
@KeySequence("system_codex_test_execution_case_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CodexTestExecutionCaseDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long executionId;
    private Long caseId;
    private String caseNameSnapshot;
    private String methodTextSnapshot;
    private String testDataTextSnapshot;
    private String analysisModeSnapshot;
    private Integer checkpointCount;
    private String status;
    private Long runnerSessionId;
    private LocalDateTime claimTime;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String failureReason;
    private String progressPhase;
    private Integer currentMethodSort;
    private Integer currentCheckpointSort;
    private String progressMessage;

}
