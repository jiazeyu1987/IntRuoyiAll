package cn.iocoder.yudao.module.system.dal.dataobject.codextest;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("system_codex_test_execution")
@KeySequence("system_codex_test_execution_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CodexTestExecutionDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long targetTenantId;
    private String executionMode;
    private String status;
    private Long requestedBy;
    private Long runnerSessionId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String summary;

}
