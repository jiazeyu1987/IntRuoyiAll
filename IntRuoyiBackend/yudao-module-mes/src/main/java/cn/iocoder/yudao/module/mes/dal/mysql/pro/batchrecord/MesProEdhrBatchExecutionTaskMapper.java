package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collections;
import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProEdhrBatchExecutionTaskMapper extends BaseMapperX<MesProEdhrBatchExecutionTaskDO> {

    String NODE_TYPE_ROUTE_FORM = "ROUTE_FORM";

    default List<MesProEdhrBatchExecutionTaskDO> selectListByBatchExecutionId(Long batchExecutionId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionTaskDO>()
                .eq(MesProEdhrBatchExecutionTaskDO::getBatchExecutionId, batchExecutionId)
                .orderByAsc(MesProEdhrBatchExecutionTaskDO::getRouteProcessSort)
                .orderByAsc(MesProEdhrBatchExecutionTaskDO::getRouteProcessId)
                .orderByAsc(MesProEdhrBatchExecutionTaskDO::getBatchRecordSort)
                .orderByAsc(MesProEdhrBatchExecutionTaskDO::getId));
    }

    @Select("SELECT * FROM mes_pro_edhr_batch_execution_task WHERE id = #{id} FOR UPDATE")
    MesProEdhrBatchExecutionTaskDO selectByIdForUpdate(@Param("id") Long id);

    @Update("""
            UPDATE mes_pro_edhr_batch_execution_task
            SET special_payload_json = #{payloadJson}
            WHERE id = #{id}
              AND status = 40
            """)
    int updateReleaseReportCompletionPayload(@Param("id") Long id,
                                             @Param("payloadJson") String payloadJson);

    default MesProEdhrBatchExecutionTaskDO selectByExecutionId(Long executionId) {
        if (executionId == null) {
            return null;
        }
        List<MesProEdhrBatchExecutionTaskDO> tasks = selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionTaskDO>()
                .eq(MesProEdhrBatchExecutionTaskDO::getExecutionId, executionId)
                .orderByAsc(MesProEdhrBatchExecutionTaskDO::getRouteProcessSort)
                .orderByAsc(MesProEdhrBatchExecutionTaskDO::getRouteProcessId)
                .orderByAsc(MesProEdhrBatchExecutionTaskDO::getBatchRecordSort)
                .orderByAsc(MesProEdhrBatchExecutionTaskDO::getId));
        return tasks.stream()
                .filter(MesProEdhrBatchExecutionTaskMapper::isRouteFormExecutionTask)
                .findFirst()
                .orElse(null);
    }

    private static boolean isRouteFormExecutionTask(MesProEdhrBatchExecutionTaskDO task) {
        String nodeType = task.getNodeType();
        if (nodeType == null || nodeType.isBlank()) {
            String reportId = task.getBatchRecordReportId();
            return reportId != null && !reportId.isBlank();
        }
        return NODE_TYPE_ROUTE_FORM.equals(nodeType);
    }

    default List<MesProEdhrBatchExecutionTaskDO> selectListByExecutionIds(Collection<Long> executionIds) {
        if (executionIds == null || executionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionTaskDO>()
                .in(MesProEdhrBatchExecutionTaskDO::getExecutionId, executionIds)
                .orderByDesc(MesProEdhrBatchExecutionTaskDO::getId));
    }

    default Long countByBatchRecordVersionId(Long batchRecordVersionId) {
        return selectCount(MesProEdhrBatchExecutionTaskDO::getBatchRecordVersionId, batchRecordVersionId);
    }

    default List<MesProEdhrBatchExecutionTaskDO> selectListByBatchRecordVersionId(Long batchRecordVersionId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionTaskDO>()
                .eq(MesProEdhrBatchExecutionTaskDO::getBatchRecordVersionId, batchRecordVersionId)
                .orderByDesc(MesProEdhrBatchExecutionTaskDO::getId));
    }
}
