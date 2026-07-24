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
 * MES 工艺路线边界节点关系 DO
 */
@TableName("mes_pro_route_process_flow_boundary_edge")
@KeySequence("mes_pro_route_process_flow_boundary_edge_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProRouteProcessFlowBoundaryEdgeDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long routeId;

    private Long graphVersion;

    private String boundaryType;

    private Long routeProcessId;

    private Integer sort;

}
