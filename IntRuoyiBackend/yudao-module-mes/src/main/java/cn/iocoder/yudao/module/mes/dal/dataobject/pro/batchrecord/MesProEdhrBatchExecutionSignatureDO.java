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

@TableName("mes_pro_edhr_batch_execution_signature")
@KeySequence("mes_pro_edhr_batch_execution_signature_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrBatchExecutionSignatureDO extends BaseDO {

    @TableId
    private Long id;

    private Long batchExecutionId;

    private Long actorId;

    private String actorName;

    private String actionType;

    private String signatureMode;

    private Boolean passwordVerified;

    private String comment;

    private LocalDateTime signedAt;

    private LocalDateTime selectedSignedAt;

    private LocalDateTime signatureDisplayAt;

    private String signatureTimeMode;

    private String selectedTimeZone;

    private String selectedTimeReason;

    private String selectedTimePolicyVersion;

    private String selectedTimeAuditHash;

    private String signatureChallengeHash;

    private String aggregateHash;
}
