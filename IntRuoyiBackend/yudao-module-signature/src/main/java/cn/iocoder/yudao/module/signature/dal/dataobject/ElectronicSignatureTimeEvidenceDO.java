package cn.iocoder.yudao.module.signature.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("system_electronic_signature_time_evidence")
@KeySequence("system_electronic_signature_time_evidence_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectronicSignatureTimeEvidenceDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String trustedTimeSource;
    private LocalDateTime sourceReportedAt;
    private LocalDateTime serverObservedAt;
    private Long driftMillis;
    private String evidenceHash;
    private String status;

}
