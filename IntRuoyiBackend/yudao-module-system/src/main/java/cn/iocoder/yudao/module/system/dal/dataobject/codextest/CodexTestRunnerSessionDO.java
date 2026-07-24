package cn.iocoder.yudao.module.system.dal.dataobject.codextest;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("system_codex_test_runner_session")
@KeySequence("system_codex_test_runner_session_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CodexTestRunnerSessionDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String runnerName;
    private String status;
    private String capabilitiesJson;
    private Integer maxParallelism;
    private String playwrightVersion;
    private String codexVersion;
    private LocalDateTime lastHeartbeatTime;
    private Integer currentRunningCount;

}
