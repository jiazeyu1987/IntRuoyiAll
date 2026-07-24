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

@TableName("mes_pro_edhr_validation_step_result")
@KeySequence("mes_pro_edhr_validation_step_result_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrValidationStepResultDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private Long packageId;

    private Long caseId;

    private Long runId;

    private String stepNo;

    private String stepTitle;

    private String expectedResult;

    private String actualResult;

    private String stepResult;

    private String executorName;

    private String reviewerName;

    private LocalDateTime executedAt;

    private String attachmentEvidence;

    private String evidenceChecksum;

    private Long deviationId;

    private String nextAction;

    private String remark;
}
