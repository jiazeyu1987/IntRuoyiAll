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

/**
 * MES 工艺路线工序流转关系图布局 DO
 */
@TableName("mes_pro_route_process_flow_layout")
@KeySequence("mes_pro_route_process_flow_layout_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProRouteProcessFlowLayoutDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long routeId;

    private Long routeProcessId;

    private Integer x;

    private Integer y;

    private Integer width;

    private Integer height;

    private Long graphVersion;

}
