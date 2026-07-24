package cn.iocoder.yudao.module.dcc.dal.dataobject.permission;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("dcc_nas_acl_identity_mapping")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccNasAclIdentityMappingDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String sid;
    private String sidHash;
    private String domainName;
    private String accountName;
    private String accountDisplayName;
    private String accountType;
    private String mappingStatus;
    private String dccSubjectType;
    private Long dccSubjectId;
    private String mappingMethod;
    private LocalDateTime verifiedAt;
    private Long mappedByUserId;
    private String blockReason;

}
