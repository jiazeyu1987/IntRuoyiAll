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

/**
 * MES 工艺路线版本工序损耗原因投影 DO
 */
@TableName("mes_pro_route_process_loss_reason")
@KeySequence("mes_pro_route_process_loss_reason_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProRouteProcessLossReasonDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long routeVersionId;

    private Long routeProcessId;

    private Long processId;

    private String reasonCode;

    private String reasonName;

    private Boolean enabled;

    private String remark;

    private Integer sort;

}
