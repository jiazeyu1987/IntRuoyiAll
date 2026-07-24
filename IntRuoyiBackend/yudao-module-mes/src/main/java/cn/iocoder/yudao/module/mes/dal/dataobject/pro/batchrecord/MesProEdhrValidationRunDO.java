package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("mes_pro_edhr_validation_run")
@KeySequence("mes_pro_edhr_validation_run_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrValidationRunDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private Long packageId;

    private Long caseId;

    private String caseType;

    private String runCode;

    private String runStatus;

    private String executionEnvironment;

    private String releaseTag;

    private String schemaVersion;

    private String executorName;

    private String reviewerName;

    private LocalDateTime executedAt;

    private String realBusinessPath;

    private String realTestDataSource;

    private String targetEnvironmentProof;

    private String attachmentEvidence;

    private String evidenceChecksum;

    private Integer openDeviationCount;

    private String conclusion;

    private String blockedReason;

    private String nextAction;

    private String remark;
}
