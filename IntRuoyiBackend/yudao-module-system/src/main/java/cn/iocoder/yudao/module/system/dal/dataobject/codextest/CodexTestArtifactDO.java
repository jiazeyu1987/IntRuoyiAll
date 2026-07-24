package cn.iocoder.yudao.module.system.dal.dataobject.codextest;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("system_codex_test_artifact")
@KeySequence("system_codex_test_artifact_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CodexTestArtifactDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long executionId;
    private Long executionCaseId;
    private Long checkpointResultId;
    private String artifactType;
    private String relativeTempPath;
    private String contentType;
    private Long sizeBytes;
    private String sha256;
    private LocalDateTime expiresAt;

}
