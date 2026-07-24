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

@TableName("mes_pro_edhr_validation_deviation")
@KeySequence("mes_pro_edhr_validation_deviation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrValidationDeviationDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private Long packageId;

    private Long caseId;

    private Long runId;

    private Long stepResultId;

    private String deviationCode;

    private String deviationTitle;

    private String deviationStatus;

    private String failedActualResult;

    private String rootCause;

    private String remediationAction;

    private String remediationOwnerName;

    private String retestResult;

    private String retestEvidence;

    private String retestReviewerName;

    private String closeSignoffName;

    private LocalDateTime closedAt;

    private String blockedReason;

    private String nextAction;

    private String remark;
}
