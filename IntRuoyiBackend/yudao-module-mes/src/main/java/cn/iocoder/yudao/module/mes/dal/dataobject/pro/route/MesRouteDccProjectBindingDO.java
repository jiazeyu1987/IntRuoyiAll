package cn.iocoder.yudao.module.mes.dal.dataobject.pro.route;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@TableName("mes_pro_route_dcc_project_binding")
@KeySequence("mes_pro_route_dcc_project_binding_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesRouteDccProjectBindingDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long routeId;

    private Long dccProjectCodeId;

    private Long version;
}
