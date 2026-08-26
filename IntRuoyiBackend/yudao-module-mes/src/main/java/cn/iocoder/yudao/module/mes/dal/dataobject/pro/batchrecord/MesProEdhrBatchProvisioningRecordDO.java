package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/** Durable Flow-6 provisioning state consumed by the Flow-7 mapping worker. */
@TableName("mes_pro_edhr_batch_provisioning_record")
@KeySequence("mes_pro_edhr_batch_provisioning_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class MesProEdhrBatchProvisioningRecordDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    private Long batchExecutionId;
    private String entryType;
    private String entryBusinessId;
    private String sourceCredentialId;
    private String sourceCredentialHash;
    private String sourceSnapshotHash;
    private String sourceBundleHash;
    private String sourceVersion;
    private String idempotencyKey;
    private String status;
    private String errorCode;
    private Integer attemptCount;
    private String mappingEventId;
    private String mappingIdempotencyKey;
}
