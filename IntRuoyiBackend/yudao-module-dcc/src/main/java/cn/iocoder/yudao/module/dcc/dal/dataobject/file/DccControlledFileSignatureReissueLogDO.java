package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("dcc_controlled_file_signature_reissue_log")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileSignatureReissueLogDO extends BaseDO {

    @TableId
    private Long id;
    private Long controlledFileId;
    private Long signatureId;
    private String beforeEvidenceHash;
    private String beforeEvidenceKeyVersion;
    private String beforeEvidenceStatus;
    private String afterEvidenceHash;
    private String afterEvidenceKeyVersion;
    private String afterEvidenceStatus;
    private Long reissuedBy;
    private LocalDateTime reissuedAt;
    private String requestId;
    private String reason;

}
