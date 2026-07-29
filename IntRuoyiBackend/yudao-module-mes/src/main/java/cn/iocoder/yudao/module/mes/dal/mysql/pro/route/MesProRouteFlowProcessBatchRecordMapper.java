package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProRouteFlowProcessBatchRecordMapper
        extends BaseMapperX<MesProRouteFlowProcessBatchRecordDO> {

    default List<MesProRouteFlowProcessBatchRecordDO> selectListByRouteIdAndUseType(Long routeId, String useType) {
        return selectList(new LambdaQueryWrapperX<MesProRouteFlowProcessBatchRecordDO>()
                .eq(MesProRouteFlowProcessBatchRecordDO::getRouteId, routeId)
                .eq(MesProRouteFlowProcessBatchRecordDO::getUseType, useType)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getRouteProcessId)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getReportSort)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getId));
    }

    default List<MesProRouteFlowProcessBatchRecordDO> selectListByRouteIdsAndUseType(
            Collection<Long> routeIds, String useType) {
        if (routeIds == null || routeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProRouteFlowProcessBatchRecordDO>()
                .in(MesProRouteFlowProcessBatchRecordDO::getRouteId, routeIds)
                .eq(MesProRouteFlowProcessBatchRecordDO::getUseType, useType)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getRouteId)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getRouteProcessId)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getReportSort)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getId));
    }

    default List<MesProRouteFlowProcessBatchRecordDO> selectListByRouteIds(Collection<Long> routeIds) {
        if (routeIds == null || routeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProRouteFlowProcessBatchRecordDO>()
                .in(MesProRouteFlowProcessBatchRecordDO::getRouteId, routeIds)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getRouteId)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getUseType)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getRouteProcessId)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getReportSort)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getId));
    }

    default List<MesProRouteFlowProcessBatchRecordDO> selectListByRouteProcessIdsAndUseType(
            Collection<Long> routeProcessIds, String useType) {
        if (routeProcessIds == null || routeProcessIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProRouteFlowProcessBatchRecordDO>()
                .in(MesProRouteFlowProcessBatchRecordDO::getRouteProcessId, routeProcessIds)
                .eq(MesProRouteFlowProcessBatchRecordDO::getUseType, useType)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getRouteProcessId)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getReportSort)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getId));
    }

    default Long countByBatchRecordReportId(String batchRecordReportId) {
        return selectCount(MesProRouteFlowProcessBatchRecordDO::getBatchRecordReportId, batchRecordReportId);
    }

    default List<MesProRouteFlowProcessBatchRecordDO> selectListByBatchRecordReportIdAndUseType(
            String batchRecordReportId, String useType) {
        return selectList(new LambdaQueryWrapperX<MesProRouteFlowProcessBatchRecordDO>()
                .eq(MesProRouteFlowProcessBatchRecordDO::getBatchRecordReportId, batchRecordReportId)
                .eq(MesProRouteFlowProcessBatchRecordDO::getUseType, useType)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getRouteProcessId)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getReportSort)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getId));
    }

    default Long countByBatchRecordVersionId(Long batchRecordVersionId) {
        return selectCount(MesProRouteFlowProcessBatchRecordDO::getBatchRecordVersionId, batchRecordVersionId);
    }

    default List<MesProRouteFlowProcessBatchRecordDO> selectListByBatchRecordVersionId(Long batchRecordVersionId) {
        return selectList(new LambdaQueryWrapperX<MesProRouteFlowProcessBatchRecordDO>()
                .eq(MesProRouteFlowProcessBatchRecordDO::getBatchRecordVersionId, batchRecordVersionId)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getRouteProcessId)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getReportSort)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getId));
    }

    default void deleteByRouteIdAndUseType(Long routeId, String useType) {
        deleteByRouteIdAndUseTypeAndTenantId(routeId, useType, TenantContextHolder.getRequiredTenantId());
    }

    @Delete("""
            DELETE FROM mes_pro_route_flow_process_batch_record
            WHERE route_id = #{routeId}
              AND use_type = #{useType}
              AND tenant_id = #{tenantId}
            """)
    @InterceptorIgnore(tenantLine = "true")
    int deleteByRouteIdAndUseTypeAndTenantId(@Param("routeId") Long routeId,
                                             @Param("useType") String useType,
                                             @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM mes_pro_route_flow_process_batch_record "
            + "WHERE route_process_id = #{routeProcessId} AND use_type = #{useType}")
    int deleteByRouteProcessIdAndUseType(@Param("routeProcessId") Long routeProcessId,
                                         @Param("useType") String useType);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_route_flow_process_batch_record ",
            "WHERE deleted = FALSE AND batch_record_report_id IN ",
            "<foreach collection='batchRecordReportIds' item='reportId' open='(' separator=',' close=')'>#{reportId}</foreach>",
            "</script>"
    })
    int deleteByBatchRecordReportIds(@Param("batchRecordReportIds") Collection<String> batchRecordReportIds);

}
