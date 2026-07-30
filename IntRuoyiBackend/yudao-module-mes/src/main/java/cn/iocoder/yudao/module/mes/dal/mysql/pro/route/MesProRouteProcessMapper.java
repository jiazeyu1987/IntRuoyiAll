package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * MES 工艺路线工序 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesProRouteProcessMapper extends BaseMapperX<MesProRouteProcessDO> {

    default List<MesProRouteProcessDO> selectListByRouteId(Long routeId) {
        return selectList(new LambdaQueryWrapperX<MesProRouteProcessDO>()
                .eq(MesProRouteProcessDO::getRouteId, routeId)
                .orderByAsc(MesProRouteProcessDO::getSort));
    }

    default MesProRouteProcessDO selectByRouteIdAndSort(Long routeId, Integer sort) {
        return selectOne(new LambdaQueryWrapperX<MesProRouteProcessDO>()
                .eq(MesProRouteProcessDO::getRouteId, routeId)
                .eq(MesProRouteProcessDO::getSort, sort));
    }

    default MesProRouteProcessDO selectByRouteIdAndProcessId(Long routeId, Long processId) {
        return selectOne(new LambdaQueryWrapperX<MesProRouteProcessDO>()
                .eq(MesProRouteProcessDO::getRouteId, routeId)
                .eq(MesProRouteProcessDO::getProcessId, processId));
    }

    default List<MesProRouteProcessDO> selectListByRouteIdAndProcessIds(Long routeId,
                                                                        Collection<Long> processIds) {
        if (processIds == null || processIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProRouteProcessDO>()
                .eq(MesProRouteProcessDO::getRouteId, routeId)
                .in(MesProRouteProcessDO::getProcessId, processIds)
                .orderByAsc(MesProRouteProcessDO::getSort));
    }

    @Select("SELECT * FROM mes_pro_route_process WHERE id = #{id} LIMIT 1")
    MesProRouteProcessDO selectByIdIgnoreDeleted(@Param("id") Long id);

    default MesProRouteProcessDO selectKeyProcessByRouteId(Long routeId) {
        return selectOne(new LambdaQueryWrapperX<MesProRouteProcessDO>()
                .eq(MesProRouteProcessDO::getRouteId, routeId)
                .eq(MesProRouteProcessDO::getKeyFlag, true));
    }

    default void deleteByRouteId(Long routeId) {
        delete(new LambdaQueryWrapperX<MesProRouteProcessDO>()
                .eq(MesProRouteProcessDO::getRouteId, routeId));
    }

    default List<MesProRouteProcessDO> selectListByProcessId(Long processId) {
        return selectList(MesProRouteProcessDO::getProcessId, processId);
    }

    default List<MesProRouteProcessDO> selectListByProcessIds(Collection<Long> processIds) {
        if (processIds == null || processIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProRouteProcessDO>()
                .in(MesProRouteProcessDO::getProcessId, processIds)
                .orderByAsc(MesProRouteProcessDO::getRouteId)
                .orderByAsc(MesProRouteProcessDO::getSort));
    }

    default List<MesProRouteProcessDO> selectListByRouteIds(Collection<Long> routeIds) {
        return selectList(new LambdaQueryWrapperX<MesProRouteProcessDO>()
                .in(MesProRouteProcessDO::getRouteId, routeIds));
    }

    default List<MesProRouteProcessDO> selectListByWorkstationIds(Collection<Long> workstationIds) {
        if (workstationIds == null || workstationIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProRouteProcessDO>()
                .in(MesProRouteProcessDO::getWorkstationId, workstationIds)
                .orderByAsc(MesProRouteProcessDO::getRouteId)
                .orderByAsc(MesProRouteProcessDO::getSort)
                .orderByAsc(MesProRouteProcessDO::getId));
    }

    default Long countByBatchRecordReportId(String batchRecordReportId) {
        return selectCount(MesProRouteProcessDO::getBatchRecordReportId, batchRecordReportId);
    }

    default Long countByBatchRecordReportIds(Collection<String> batchRecordReportIds) {
        if (batchRecordReportIds == null || batchRecordReportIds.isEmpty()) {
            return 0L;
        }
        return selectCount(new LambdaQueryWrapperX<MesProRouteProcessDO>()
                .in(MesProRouteProcessDO::getBatchRecordReportId, batchRecordReportIds));
    }

    @Select({
            "<script>",
            "SELECT * FROM mes_pro_route_process ",
            "WHERE route_id IN ",
            "<foreach collection='routeIds' item='routeId' open='(' separator=',' close=')'>#{routeId}</foreach>",
            "</script>"
    })
    List<MesProRouteProcessDO> selectListByRouteIdsIgnoreDeleted(@Param("routeIds") Collection<Long> routeIds);

    @Select({
            "<script>",
            "SELECT DISTINCT route_id FROM mes_pro_route_process ",
            "WHERE deleted = FALSE ",
            "AND next_process_id IS NOT NULL ",
            "AND route_id IN ",
            "<foreach collection='routeIds' item='routeId' open='(' separator=',' close=')'>#{routeId}</foreach>",
            "</script>"
    })
    List<Long> selectRelationConfiguredRouteIdsByRouteIds(@Param("routeIds") Collection<Long> routeIds);

    @Update({
            "<script>",
            "UPDATE mes_pro_route_process ",
            "SET batch_record_report_id = NULL, update_time = NOW() ",
            "WHERE deleted = FALSE AND batch_record_report_id IN ",
            "<foreach collection='batchRecordReportIds' item='reportId' open='(' separator=',' close=')'>#{reportId}</foreach>",
            "</script>"
    })
    int unbindBatchRecordReportIds(@Param("batchRecordReportIds") Collection<String> batchRecordReportIds);

    @Update({
            "<script>",
            "UPDATE mes_pro_route_process ",
            "SET next_process_id = #{nextProcessId,jdbcType=BIGINT}, ",
            "<if test='linkType != null'>link_type = #{linkType}, </if>",
            "update_time = NOW() ",
            "WHERE deleted = FALSE AND route_id = #{routeId} AND id = #{routeProcessId}",
            "</script>"
    })
    int updateFlowNextProcess(@Param("routeId") Long routeId,
                              @Param("routeProcessId") Long routeProcessId,
                              @Param("nextProcessId") Long nextProcessId,
                              @Param("linkType") Integer linkType);

}
