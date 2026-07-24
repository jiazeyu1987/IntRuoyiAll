package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProRouteFlowConfigMapper extends BaseMapperX<MesProRouteFlowConfigDO> {

    default MesProRouteFlowConfigDO selectByRouteIdAndUseType(Long routeId, String useType) {
        return selectOne(MesProRouteFlowConfigDO::getRouteId, routeId,
                MesProRouteFlowConfigDO::getUseType, useType);
    }

    default List<MesProRouteFlowConfigDO> selectListByRouteIds(Collection<Long> routeIds) {
        if (routeIds == null || routeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProRouteFlowConfigDO>()
                .in(MesProRouteFlowConfigDO::getRouteId, routeIds));
    }

    default void deleteByRouteIdAndUseType(Long routeId, String useType) {
        deleteByRouteIdAndUseTypeAndTenantId(routeId, useType, TenantContextHolder.getRequiredTenantId());
    }

    @Delete("""
            DELETE FROM mes_pro_route_flow_config
            WHERE route_id = #{routeId}
              AND use_type = #{useType}
              AND tenant_id = #{tenantId}
            """)
    @InterceptorIgnore(tenantLine = "true")
    int deleteByRouteIdAndUseTypeAndTenantId(@Param("routeId") Long routeId,
                                             @Param("useType") String useType,
                                             @Param("tenantId") Long tenantId);

}
