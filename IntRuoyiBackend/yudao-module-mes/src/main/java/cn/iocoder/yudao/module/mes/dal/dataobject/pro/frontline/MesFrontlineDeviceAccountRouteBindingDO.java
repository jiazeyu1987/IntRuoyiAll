package cn.iocoder.yudao.module.mes.dal.dataobject.pro.frontline;

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

@TableName("mes_frontline_device_account_route_binding")
@KeySequence("mes_frontline_device_account_route_binding_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesFrontlineDeviceAccountRouteBindingDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long deviceAccountUserId;
    private Long routeId;
    private Long deviceId;
    private Long workstationId;
    private Long defaultApproveUserId;
    private Long recordbookId;
    private Integer feedbackType;
    private Integer status;
    private String remark;
}
