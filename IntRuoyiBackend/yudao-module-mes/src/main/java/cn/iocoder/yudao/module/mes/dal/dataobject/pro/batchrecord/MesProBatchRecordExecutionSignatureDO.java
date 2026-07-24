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

@TableName("mes_pro_batch_record_execution_signature")
@KeySequence("mes_pro_batch_record_execution_signature_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProBatchRecordExecutionSignatureDO extends BaseDO {

    @TableId
    private Long id;

    private Long executionId;

    private Long actorId;

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

    private String processInstanceId;

    private String bpmTaskId;

    private String bpmTaskDefinitionKey;

    private String bpmTaskName;

    private String signatureCellKey;

    private Integer signatureRowIndex;

    private Integer signatureColumnIndex;

    private String reviewSourceType;

    private Long reviewSourceId;

    private String reviewSourceName;

    private String approvalResult;

    private String reason;

    private String actorName;

    private String actorUsernameSnapshot;

    private String actorNicknameSnapshot;

    private Long actorDeptIdSnapshot;

    private String actorDeptNameSnapshot;

    private String actorPostNamesSnapshot;

    private String actorRoleNamesSnapshot;

    private String signaturePurpose;

    private String authorizationBasis;

    private String authenticationMethod;

    private String recordVersionSnapshot;

    private String recordHashSnapshot;

    private String clientIpSnapshot;

    private String userAgentSnapshot;

    private String snapshotStatus;

    private String reasonCategory;

    private Long auditBatchId;

    private String signatureChallengeHash;

    private Long fieldAuditRevision;

    private String fieldAuditHeadHash;

    private String cellValuesHash;
}
