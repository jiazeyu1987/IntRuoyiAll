package cn.iocoder.yudao.module.signature.gxp.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("gxp_audit_coverage_report")
@KeySequence("gxp_audit_coverage_report_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GxpAuditCoverageReportDO {

    @TableId
    private Long id;
    private Long tenantId;
    private String policyVersion;
    private String sourceCommit;
    private String registrySha256;
    private String discoveredInventorySha256;
    private Integer registeredCount;
    private Integer notApplicableCount;
    private Integer gapCount;
    private Integer testMappingCount;
    private String reportSha256;
    private LocalDateTime generatedAtUtc;
    private LocalDateTime createTime;

}
