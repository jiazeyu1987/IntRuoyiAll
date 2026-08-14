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

@TableName("dcc_controlled_file_signature_binding")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileSignatureBindingDO extends BaseDO {

    @TableId
    private Long id;
    private Long signatureId;
    private Long controlledFileId;
    private String originalEvidenceHash;
    private Long controlledCopyFileId;
    private String controlledCopySha256;
    private String controlledCopyHashAlgorithm;
    private LocalDateTime boundAt;
    private Long boundBy;
    private String bindingEventKey;
    private String bindingPayloadVersion;
    private String bindingHashAlgorithm;
    private String bindingHash;

}
