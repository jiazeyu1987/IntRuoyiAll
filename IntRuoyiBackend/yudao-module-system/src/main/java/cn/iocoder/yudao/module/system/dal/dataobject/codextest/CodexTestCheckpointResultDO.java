package cn.iocoder.yudao.module.system.dal.dataobject.codextest;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("system_codex_test_checkpoint_result")
@KeySequence("system_codex_test_checkpoint_result_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CodexTestCheckpointResultDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long executionCaseId;
    private Integer checkpointSort;
    private String checkpointNameSnapshot;
    private String expectedTextSnapshot;
    private String actualText;
    private String status;
    private String mismatchDescription;
    private Long screenshotArtifactId;
    private LocalDateTime completedAt;

}
