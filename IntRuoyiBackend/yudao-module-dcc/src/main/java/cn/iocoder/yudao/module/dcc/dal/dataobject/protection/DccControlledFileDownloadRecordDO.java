package cn.iocoder.yudao.module.dcc.dal.dataobject.protection;

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

/**
 * DCC controlled file encrypted download record.
 */
@TableName("dcc_controlled_file_download_record")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileDownloadRecordDO extends BaseDO {

    @TableId
    private Long id;
    private String downloadRequestId;
    private Long accessEventId;
    private String accessEventCode;
    private Long controlledFileId;
    private String fileVersionNo;
    private Long userId;
    private String policyVersion;
    private String encryptionStatus;
    private String encryptionPolicyVersion;
    private String artifactId;
    private String cipherFileRef;
    private String plainSha256;
    private String cipherSha256;
    private String failureCode;
    private String failureReason;
    private LocalDateTime requestedAt;
    private LocalDateTime encryptedAt;
    private LocalDateTime returnedAt;

}
