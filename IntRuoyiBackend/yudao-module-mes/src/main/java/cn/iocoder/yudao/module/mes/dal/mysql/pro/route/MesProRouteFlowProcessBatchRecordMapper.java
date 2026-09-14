package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProRouteFlowProcessBatchRecordMapper
        extends BaseMapperX<MesProRouteFlowProcessBatchRecordDO> {

    default List<MesProRouteFlowProcessBatchRecordDO> selectListByRouteIdAndUseType(Long routeId, String useType) {
        return selectCurrentProjectionListByRouteIdAndUseType(
                routeId, useType, TenantContextHolder.getRequiredTenantId());
    }

    default List<MesProRouteFlowProcessBatchRecordDO> selectListByRouteIdsAndUseType(
            Collection<Long> routeIds, String useType) {
        if (routeIds == null || routeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectCurrentProjectionListByRouteIdsAndUseType(
                routeIds, useType, TenantContextHolder.getRequiredTenantId());
    }

    default List<MesProRouteFlowProcessBatchRecordDO> selectListByRouteIds(Collection<Long> routeIds) {
        if (routeIds == null || routeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectCurrentProjectionListByRouteIds(routeIds, TenantContextHolder.getRequiredTenantId());
    }

    @Select("""
            SELECT br.*
            FROM mes_pro_route_flow_process_batch_record br
            INNER JOIN mes_pro_route_flow_process_config pc
              ON pc.id = br.route_flow_process_config_id
             AND pc.tenant_id = br.tenant_id
             AND pc.deleted = FALSE
             AND pc.route_id = br.route_id
             AND pc.route_process_id = br.route_process_id
             AND pc.use_type = br.use_type
            WHERE br.deleted = FALSE
              AND br.route_id = #{routeId}
              AND br.use_type = #{useType}
              AND br.tenant_id = #{tenantId}
            ORDER BY br.route_process_id ASC, br.report_sort ASC, br.id ASC
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<MesProRouteFlowProcessBatchRecordDO> selectCurrentProjectionListByRouteIdAndUseType(
            @Param("routeId") Long routeId,
            @Param("useType") String useType,
            @Param("tenantId") Long tenantId);

    @Select({
            "<script>",
            "SELECT br.* ",
            "FROM mes_pro_route_flow_process_batch_record br ",
            "INNER JOIN mes_pro_route_flow_process_config pc ",
            "  ON pc.id = br.route_flow_process_config_id ",
            " AND pc.tenant_id = br.tenant_id ",
            " AND pc.deleted = FALSE ",
            " AND pc.route_id = br.route_id ",
            " AND pc.route_process_id = br.route_process_id ",
            " AND pc.use_type = br.use_type ",
            "WHERE br.deleted = FALSE ",
            "  AND br.route_id IN ",
            "<foreach collection='routeIds' item='routeId' open='(' separator=',' close=')'>#{routeId}</foreach>",
            "  AND br.use_type = #{useType} ",
            "  AND br.tenant_id = #{tenantId} ",
            "ORDER BY br.route_id ASC, br.route_process_id ASC, br.report_sort ASC, br.id ASC",
            "</script>"
    })
    @InterceptorIgnore(tenantLine = "true")
    List<MesProRouteFlowProcessBatchRecordDO> selectCurrentProjectionListByRouteIdsAndUseType(
            @Param("routeIds") Collection<Long> routeIds,
            @Param("useType") String useType,
            @Param("tenantId") Long tenantId);

    @Select({
            "<script>",
            "SELECT br.* ",
            "FROM mes_pro_route_flow_process_batch_record br ",
            "INNER JOIN mes_pro_route_flow_process_config pc ",
            "  ON pc.id = br.route_flow_process_config_id ",
            " AND pc.tenant_id = br.tenant_id ",
            " AND pc.deleted = FALSE ",
            " AND pc.route_id = br.route_id ",
            " AND pc.route_process_id = br.route_process_id ",
            " AND pc.use_type = br.use_type ",
            "WHERE br.deleted = FALSE ",
            "  AND br.route_id IN ",
            "<foreach collection='routeIds' item='routeId' open='(' separator=',' close=')'>#{routeId}</foreach>",
            "  AND br.tenant_id = #{tenantId} ",
            "ORDER BY br.route_id ASC, br.use_type ASC, br.route_process_id ASC, br.report_sort ASC, br.id ASC",
            "</script>"
    })
    @InterceptorIgnore(tenantLine = "true")
    List<MesProRouteFlowProcessBatchRecordDO> selectCurrentProjectionListByRouteIds(
            @Param("routeIds") Collection<Long> routeIds,
            @Param("tenantId") Long tenantId);

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

    default List<MesProRouteFlowProcessBatchRecordDO> selectListByBatchRecordReportIds(
            Collection<String> batchRecordReportIds) {
        if (batchRecordReportIds == null || batchRecordReportIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProRouteFlowProcessBatchRecordDO>()
                .in(MesProRouteFlowProcessBatchRecordDO::getBatchRecordReportId, batchRecordReportIds)
                .orderByAsc(MesProRouteFlowProcessBatchRecordDO::getRouteId)
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
