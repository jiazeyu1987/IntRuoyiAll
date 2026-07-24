package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * MES 工艺路线工序流转关系边 Mapper
 */
@Mapper
public interface MesProRouteProcessFlowEdgeMapper extends BaseMapperX<MesProRouteProcessFlowEdgeDO> {

    default List<MesProRouteProcessFlowEdgeDO> selectListByRouteId(Long routeId) {
        return selectList(new LambdaQueryWrapperX<MesProRouteProcessFlowEdgeDO>()
                .eq(MesProRouteProcessFlowEdgeDO::getRouteId, routeId)
                .orderByAsc(MesProRouteProcessFlowEdgeDO::getSort)
                .orderByAsc(MesProRouteProcessFlowEdgeDO::getId));
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

    @Delete("DELETE FROM mes_pro_route_process_flow_edge WHERE route_id = #{routeId} AND tenant_id = #{tenantId}")
    @InterceptorIgnore(tenantLine = "true")
    int deleteByRouteIdAndTenantId(@Param("routeId") Long routeId, @Param("tenantId") Long tenantId);

    @Select("""
            SELECT MAX(graph_version)
            FROM mes_pro_route_process_flow_edge
            WHERE route_id = #{routeId}
              AND tenant_id = #{tenantId}
              AND deleted = 0
            """)
    @InterceptorIgnore(tenantLine = "true")
    Long selectMaxGraphVersionByRouteIdAndTenantId(@Param("routeId") Long routeId,
                                                    @Param("tenantId") Long tenantId);

    default Long selectMaxGraphVersionByRouteId(Long routeId) {
        return selectMaxGraphVersionByRouteIdAndTenantId(routeId, TenantContextHolder.getRequiredTenantId());
    }

    @Select("""
            <script>
            SELECT DISTINCT route_id
            FROM mes_pro_route_process_flow_edge
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

    @Delete("""
            DELETE FROM mes_pro_route_process_flow_edge
            WHERE route_id = #{routeId}
              AND tenant_id = #{tenantId}
              AND (source_route_process_id = #{routeProcessId} OR target_route_process_id = #{routeProcessId})
            """)
    @InterceptorIgnore(tenantLine = "true")
    int deleteByRouteProcessIdAndTenantId(@Param("routeId") Long routeId,
                                           @Param("routeProcessId") Long routeProcessId,
                                           @Param("tenantId") Long tenantId);

}
