package cn.iocoder.yudao.module.signature.gxp.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("gxp_audit_daily_manifest")
@KeySequence("gxp_audit_daily_manifest_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GxpAuditDailyManifestDO {

    @TableId
    private Long id;
    private Long tenantId;
    private LocalDate businessDate;
    private Long firstSequence;
    private Long lastSequence;
    private Integer eventCount;
    private String merkleRoot;
    private String previousManifestHash;
    private String manifestHash;
    private LocalDateTime sealedAtUtc;
    private LocalDateTime createTime;

}
