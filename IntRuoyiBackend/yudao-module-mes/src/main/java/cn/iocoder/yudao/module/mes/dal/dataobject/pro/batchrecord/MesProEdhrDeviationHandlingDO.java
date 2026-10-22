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

@TableName("mes_pro_edhr_deviation_handling")
@KeySequence("mes_pro_edhr_deviation_handling_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrDeviationHandlingDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;
    private Long tenantId;
    private Long deviationId;
    private LocalDateTime investigationStartedAt;
    private LocalDateTime plannedCompletedAt;
    private LocalDateTime completedAt;
    private String investigationMembersJson;
    private String rootCauseAnalysis;
    private String impactScope;
    private String riskAssessment;
    private String productDisposition;
    private String nonconformanceReviewCode;
    private Long correctiveOwnerId;
    private String correctiveOwnerName;
    private LocalDateTime correctiveDueAt;
    private Boolean capaRequired;
    private String capaCode;
    private String capaAttachmentsJson;
    private String handlingConclusion;
    private String verificationResult;
    private String verificationContent;
    private Integer contentVersion;
    private String contentHash;
}
