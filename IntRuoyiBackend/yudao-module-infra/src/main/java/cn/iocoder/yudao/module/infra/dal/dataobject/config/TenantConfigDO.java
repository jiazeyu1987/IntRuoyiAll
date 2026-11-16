package cn.iocoder.yudao.module.infra.dal.dataobject.config;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Mutable configuration owned by exactly one tenant. */
@TableName("infra_tenant_config")
@Data
@EqualsAndHashCode(callSuper = true)
public class TenantConfigDO extends TenantBaseDO {
    private Long id;
    private String category;
    private Integer type;
    private String name;
    private String configKey;
    private String value;
    private Boolean visible;
    private String remark;
}
