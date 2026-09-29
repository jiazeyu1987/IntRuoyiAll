package cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("gxp_audit_legacy_fact_baseline")
@Data
public class GxpAuditLegacyFactBaselineDO {

    @TableId
    private Long tenantId;
    private String sourceType;
    private String sourceId;
    private String sourceHash;
    private String policyVersion;
    private LocalDateTime capturedAtUtc;
    private String approvalReference;
    private String manifestHash;

}
