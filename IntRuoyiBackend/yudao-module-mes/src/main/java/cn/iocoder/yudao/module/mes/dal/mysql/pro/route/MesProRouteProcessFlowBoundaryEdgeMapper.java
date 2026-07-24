package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowBoundaryEdgeDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * MES 工艺路线边界节点关系 Mapper
 */
@Mapper
public interface MesProRouteProcessFlowBoundaryEdgeMapper extends BaseMapperX<MesProRouteProcessFlowBoundaryEdgeDO> {

    default List<MesProRouteProcessFlowBoundaryEdgeDO> selectListByRouteId(Long routeId) {
        return selectList(new LambdaQueryWrapperX<MesProRouteProcessFlowBoundaryEdgeDO>()
                .eq(MesProRouteProcessFlowBoundaryEdgeDO::getRouteId, routeId)
                .orderByAsc(MesProRouteProcessFlowBoundaryEdgeDO::getBoundaryType)
                .orderByAsc(MesProRouteProcessFlowBoundaryEdgeDO::getSort)
                .orderByAsc(MesProRouteProcessFlowBoundaryEdgeDO::getId));
    }

    default List<Long> selectConfiguredRouteIdsByRouteIds(Collection<Long> routeIds) {
        if (routeIds == null || routeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectConfiguredRouteIdsByRouteIdsAndTenantId(routeIds, TenantContextHolder.getRequiredTenantId());
    }

    default void deleteByRouteId(Long routeId) {
        deleteByRouteIdAndTenantId(routeId, TenantContextHolder.getRequiredTenantId());
    }

    default void deleteByRouteProcessId(Long routeId, Long routeProcessId) {
        deleteByRouteProcessIdAndTenantId(routeId, routeProcessId, TenantContextHolder.getRequiredTenantId());
    }

    @Delete("DELETE FROM mes_pro_route_process_flow_boundary_edge WHERE route_id = #{routeId} AND tenant_id = #{tenantId}")
    @InterceptorIgnore(tenantLine = "true")
    int deleteByRouteIdAndTenantId(@Param("routeId") Long routeId, @Param("tenantId") Long tenantId);

    @Delete("""
            DELETE FROM mes_pro_route_process_flow_boundary_edge
            WHERE route_id = #{routeId}
              AND tenant_id = #{tenantId}
              AND route_process_id = #{routeProcessId}
            """)
    @InterceptorIgnore(tenantLine = "true")
    int deleteByRouteProcessIdAndTenantId(@Param("routeId") Long routeId,
                                           @Param("routeProcessId") Long routeProcessId,
                                           @Param("tenantId") Long tenantId);

    @Select("""
            <script>
            SELECT DISTINCT route_id
            FROM mes_pro_route_process_flow_boundary_edge
            WHERE tenant_id = #{tenantId}
              AND route_id IN
              <foreach collection="routeIds" item="routeId" open="(" separator="," close=")">
                  #{routeId}
              </foreach>
            </script>
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<Long> selectConfiguredRouteIdsByRouteIdsAndTenantId(@Param("routeIds") Collection<Long> routeIds,
                                                             @Param("tenantId") Long tenantId);

}
