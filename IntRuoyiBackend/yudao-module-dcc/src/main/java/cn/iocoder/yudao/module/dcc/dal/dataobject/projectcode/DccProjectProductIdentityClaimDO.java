package cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TableName("dcc_project_product_identity_claim")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccProjectProductIdentityClaimDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long requestId;
    private String identityType;
    private String identityValue;
}
