package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowLayoutDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MES 工艺路线工序流转关系图布局 Mapper
 */
@Mapper
public interface MesProRouteProcessFlowLayoutMapper extends BaseMapperX<MesProRouteProcessFlowLayoutDO> {

    default List<MesProRouteProcessFlowLayoutDO> selectListByRouteId(Long routeId) {
        return selectList(new LambdaQueryWrapperX<MesProRouteProcessFlowLayoutDO>()
                .eq(MesProRouteProcessFlowLayoutDO::getRouteId, routeId)
                .orderByAsc(MesProRouteProcessFlowLayoutDO::getRouteProcessId));
    }

    default void deleteByRouteId(Long routeId) {
        deleteByRouteIdAndTenantId(routeId, TenantContextHolder.getRequiredTenantId());
    }

    default void deleteByRouteProcessId(Long routeId, Long routeProcessId) {
        deleteByRouteProcessIdAndTenantId(routeId, routeProcessId, TenantContextHolder.getRequiredTenantId());
    }

    @Delete("DELETE FROM mes_pro_route_process_flow_layout WHERE route_id = #{routeId} AND tenant_id = #{tenantId}")
    @InterceptorIgnore(tenantLine = "true")
    int deleteByRouteIdAndTenantId(@Param("routeId") Long routeId, @Param("tenantId") Long tenantId);

    @Delete("""
            DELETE FROM mes_pro_route_process_flow_layout
            WHERE route_id = #{routeId}
              AND route_process_id = #{routeProcessId}
              AND tenant_id = #{tenantId}
            """)
    @InterceptorIgnore(tenantLine = "true")
    int deleteByRouteProcessIdAndTenantId(@Param("routeId") Long routeId,
                                           @Param("routeProcessId") Long routeProcessId,
                                           @Param("tenantId") Long tenantId);

    @Select("SELECT COALESCE(MAX(graph_version), 0) FROM mes_pro_route_process_flow_layout " +
            "WHERE route_id = #{routeId} AND deleted = FALSE")
    Long selectMaxGraphVersionByRouteId(Long routeId);

}
