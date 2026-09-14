package cn.iocoder.yudao.module.mdm.dal.dataobject.enterprise;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mdm_enterprise")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmEnterpriseDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String enterpriseCode;
    private String name;
    private String type;
    private String status;
    private Integer revision;

}
