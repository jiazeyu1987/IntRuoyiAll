package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesPqcInspectionPieceDetailMapper extends BaseMapperX<MesPqcInspectionPieceDetailDO> {

    @Select("""
            SELECT d.*
            FROM mes_pqc_inspection_piece_detail d
            INNER JOIN mes_pqc_inspection_task t
              ON t.id = d.task_id
             AND t.deleted = b'0'
            INNER JOIN mes_pro_process_pool_event e
              ON e.feedback_source_id = t.id
             AND e.event_type = 'PQC_INSPECTION'
             AND e.feedback_source_type = 'MES_PQC_INSPECTION_TASK'
             AND e.deleted = b'0'
            WHERE d.deleted = b'0'
              AND e.actual_employee_id = #{actualEmployeeId}
              AND d.item_code = #{itemCode}
              AND d.selected_equipment_id IS NOT NULL
              AND d.selected_equipment_number IS NOT NULL
              AND d.selected_equipment_number <> ''
            ORDER BY e.server_submit_time DESC, e.id DESC, d.id DESC
            LIMIT 1
            """)
    MesPqcInspectionPieceDetailDO selectLatestSelectedEquipmentByActualEmployeeAndItemCode(
            @Param("actualEmployeeId") Long actualEmployeeId,
            @Param("itemCode") String itemCode);

    default List<MesPqcInspectionPieceDetailDO> selectListByTaskId(Long taskId) {
        if (taskId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesPqcInspectionPieceDetailDO>()
                .eq(MesPqcInspectionPieceDetailDO::getTaskId, taskId)
                .orderByAsc(MesPqcInspectionPieceDetailDO::getSampleNo)
                .orderByAsc(MesPqcInspectionPieceDetailDO::getId));
    }

    default int deleteByTaskId(Long taskId) {
        if (taskId == null) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<MesPqcInspectionPieceDetailDO>()
                .eq(MesPqcInspectionPieceDetailDO::getTaskId, taskId));
    }

    default int deleteByTaskIds(Collection<Long> taskIds) {
        return taskIds == null || taskIds.isEmpty() ? 0 : physicalDeleteByTaskIds(taskIds);
    }

    @Delete({
            "<script>",
            "DELETE FROM mes_pqc_inspection_piece_detail WHERE task_id IN",
            "<foreach collection='taskIds' item='taskId' open='(' separator=',' close=')'>#{taskId}</foreach>",
            "</script>"
    })
    int physicalDeleteByTaskIds(@Param("taskIds") Collection<Long> taskIds);
}
