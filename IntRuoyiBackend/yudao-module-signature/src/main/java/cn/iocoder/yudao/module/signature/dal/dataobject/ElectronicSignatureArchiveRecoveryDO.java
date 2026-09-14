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

@TableName("system_electronic_signature_archive_recovery")
@KeySequence("system_electronic_signature_archive_recovery_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectronicSignatureArchiveRecoveryDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long archiveId;
    private LocalDateTime restoredAt;
    private Long restoredBy;
    private String businessRecordHash;
    private String signatureRecordHash;
    private String snapshotHash;
    private String restoreEvidenceHash;
    private String resultStatus;

}
