package cn.iocoder.yudao.module.mdm.dal.dataobject.companyscope;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mdm_user_company_scope")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmUserCompanyScopeDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long userId;
    private Long companyId;
    private String status;
    private Integer revision;

}
