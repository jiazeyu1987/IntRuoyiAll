package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("dcc_controlled_file_source_global_claim")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileSourceGlobalClaimDO extends BaseDO {

    @TableId
    private Long id;
    private Long sourceFileId;
    private Long tenantId;
    private Long controlledFileId;
    private Long governanceBatchId;
    private Long governanceItemId;
    private String claimStatus;
    private String sourceSha256;
    private Long claimedBy;
    private LocalDateTime claimedTime;
}
