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

@TableName("dcc_nas_acl_descriptor")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccNasAclDescriptorDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String descriptorHash;
    private String ownerSid;
    private String groupSid;
    private String controlFlags;
    private Boolean daclPresent;
    private Boolean daclProtected;
    private Boolean saclPresent;
    private String rawDescriptorSha256;
    private byte[] rawDescriptorBlob;
    private String normalizedDescriptorJson;
    private String captureCapability;

}
