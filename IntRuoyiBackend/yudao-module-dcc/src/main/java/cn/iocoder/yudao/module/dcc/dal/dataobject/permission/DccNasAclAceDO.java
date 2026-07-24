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

@TableName("dcc_nas_acl_ace")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccNasAclAceDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long descriptorId;
    private Integer aceIndex;
    private String aceHash;
    private String aceType;
    private Integer aceFlags;
    private Long accessMask;
    private String trusteeSid;
    private String trusteeSidHash;
    private Boolean inherited;
    private String inheritanceFlags;
    private String propagationFlags;
    private String objectTypeGuid;
    private String inheritedObjectTypeGuid;
    private String rawAceJson;

}
