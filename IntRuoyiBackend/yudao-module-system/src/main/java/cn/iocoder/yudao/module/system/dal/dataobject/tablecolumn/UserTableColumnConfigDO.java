package cn.iocoder.yudao.module.system.dal.dataobject.tablecolumn;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("system_user_table_column_config")
@KeySequence("system_user_table_column_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserTableColumnConfigDO extends TenantBaseDO {

    private Long id;

    private Long userId;

    private String tableKey;

    private String configJson;

}
