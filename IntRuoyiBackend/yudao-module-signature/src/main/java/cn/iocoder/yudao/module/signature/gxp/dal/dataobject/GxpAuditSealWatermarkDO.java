package cn.iocoder.yudao.module.signature.gxp.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("gxp_audit_seal_watermark")
@KeySequence("gxp_audit_seal_watermark_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GxpAuditSealWatermarkDO {

    @TableId
    private Long id;
    private Long tenantId;
    private String watermarkType;
    private Long sealedThroughSequence;
    private String lastManifestHash;
    private Integer unsealedEventCount;
    private String watermarkHash;
    private LocalDateTime generatedAtUtc;
    private LocalDateTime createTime;

}
