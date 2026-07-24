package cn.iocoder.yudao.module.bpm.dal.dataobject.signature;

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

@TableName("bpm_approval_signature_record")
@KeySequence("bpm_approval_signature_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class BpmApprovalSignatureRecordDO extends BaseDO {

    @TableId
    private Long id;

    private String moduleCode;

    private String sourceTaskType;

    private String sourceTaskId;

    private String businessKey;

    private String processInstanceId;

    private Long signerUserId;

    private String reviewResult;

    private String reason;

    private Boolean passwordVerified;

    private Long signatureImageId;

    private Integer signatureImageVersionNo;

    private Long signatureImageFileId;

    private String signatureImageFileUrl;

    private String signatureImageSha256;

    private String signatureImageContentType;

    private Long signatureImageFileSize;

    private String signatureImageStatusSnapshot;

    private String signatureImageVerifiedStatus;

    private LocalDateTime signedAt;

    private Long tenantId;

}
