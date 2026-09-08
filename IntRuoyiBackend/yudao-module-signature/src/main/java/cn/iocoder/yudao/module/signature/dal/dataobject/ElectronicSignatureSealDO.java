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

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("system_electronic_signature_seal")
@KeySequence("system_electronic_signature_seal_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectronicSignatureSealDO extends TenantBaseDO {

    @TableId
    private Long id;
    private LocalDate sealDate;
    private Integer recordCount;
    private Long firstSignatureId;
    private Long lastSignatureId;
    private String previousSealHash;
    private String sealHash;
    private String wormEvidenceId;
    private String status;
    private LocalDateTime sealedAt;

}
