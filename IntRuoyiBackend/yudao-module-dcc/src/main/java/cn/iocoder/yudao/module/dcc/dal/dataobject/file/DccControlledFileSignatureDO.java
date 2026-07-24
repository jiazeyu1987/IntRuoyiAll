package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("dcc_controlled_file_signature")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileSignatureDO extends BaseDO {

    @TableId
    private Long id;
    private Long controlledFileId;
    private Long revisionId;
    private String versionNo;
    private String taskId;
    private Long actorId;
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
    private String actionType;
    private String meaningCode;
    private String meaningLabel;
    private String signatureMode;
    private Boolean passwordVerified;
    private String comment;
    private LocalDateTime signedAt;
    private Long sourceFileId;
    private String sourceFileHash;
    private String sourceFileHashAlgorithm;
    private String sourceFileHashStatus;
    private Long controlledCopyFileId;
    private String controlledCopyHash;
    private String controlledCopyHashAlgorithm;
    private String controlledCopyHashStatus;
    private Long signatureImageId;
    private Integer signatureImageVersionNo;
    private Long signatureImageFileId;
    private String signatureImageFileUrl;
    private String signatureImageSha256;
    private String signatureImageContentType;
    private Long signatureImageFileSize;
    private String signatureImageStatusSnapshot;
    private String signatureImageVerifiedStatus;
    private String evidencePayloadVersion;
    private String evidenceKeyVersion;
    private String evidenceHash;
    private String evidenceHashAlgorithm;
    private String evidenceStatus;

}
