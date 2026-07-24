package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProRouteFlowProcessConfigMapper extends BaseMapperX<MesProRouteFlowProcessConfigDO> {

    default List<MesProRouteFlowProcessConfigDO> selectListByRouteIdAndUseType(Long routeId, String useType) {
        return selectList(new LambdaQueryWrapperX<MesProRouteFlowProcessConfigDO>()
                .eq(MesProRouteFlowProcessConfigDO::getRouteId, routeId)
                .eq(MesProRouteFlowProcessConfigDO::getUseType, useType)
                .orderByAsc(MesProRouteFlowProcessConfigDO::getRouteProcessId));
    }

    default List<MesProRouteFlowProcessConfigDO> selectListByRouteIdsAndUseType(
            Collection<Long> routeIds, String useType) {
        if (routeIds == null || routeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProRouteFlowProcessConfigDO>()
                .in(MesProRouteFlowProcessConfigDO::getRouteId, routeIds)
                .eq(MesProRouteFlowProcessConfigDO::getUseType, useType)
                .orderByAsc(MesProRouteFlowProcessConfigDO::getRouteId)
                .orderByAsc(MesProRouteFlowProcessConfigDO::getRouteProcessId));
    }

    default MesProRouteFlowProcessConfigDO selectByRouteProcessIdAndUseType(Long routeProcessId, String useType) {
        return selectOne(MesProRouteFlowProcessConfigDO::getRouteProcessId, routeProcessId,
                MesProRouteFlowProcessConfigDO::getUseType, useType);
    }

    default List<MesProRouteFlowProcessConfigDO> selectListByRouteProcessIdsAndUseType(
            Collection<Long> routeProcessIds, String useType) {
        return selectList(new LambdaQueryWrapperX<MesProRouteFlowProcessConfigDO>()
                .in(MesProRouteFlowProcessConfigDO::getRouteProcessId, routeProcessIds)
                .eq(MesProRouteFlowProcessConfigDO::getUseType, useType)
                .orderByAsc(MesProRouteFlowProcessConfigDO::getRouteProcessId));
    }

    default Long countByBatchRecordReportId(String batchRecordReportId) {
        return selectCount(MesProRouteFlowProcessConfigDO::getBatchRecordReportId, batchRecordReportId);
    }

    default void deleteByRouteIdAndUseType(Long routeId, String useType) {
        deleteByRouteIdAndUseTypeAndTenantId(routeId, useType, TenantContextHolder.getRequiredTenantId());
    }

    @Delete("""
            DELETE FROM mes_pro_route_flow_process_config
            WHERE route_id = #{routeId}
              AND use_type = #{useType}
              AND tenant_id = #{tenantId}
            """)
    @InterceptorIgnore(tenantLine = "true")
    int deleteByRouteIdAndUseTypeAndTenantId(@Param("routeId") Long routeId,
                                             @Param("useType") String useType,
                                             @Param("tenantId") Long tenantId);

    @Update({
            "<script>",
            "UPDATE mes_pro_route_flow_process_config ",
            "SET batch_record_report_id = NULL, update_time = NOW() ",
            "WHERE deleted = FALSE AND batch_record_report_id IN ",
            "<foreach collection='batchRecordReportIds' item='reportId' open='(' separator=',' close=')'>#{reportId}</foreach>",
            "</script>"
    })
    int unbindBatchRecordReportIds(@Param("batchRecordReportIds") Collection<String> batchRecordReportIds);

}
