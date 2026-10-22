package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** Explicit tenant projections of the existing writer relationships; no LIMIT or latest-row rule. */
@Mapper
public interface MesEdhrEngineOwnershipMapper {
    @Data
    class ExecutionFact {
        private Long id;
        private Long tenantId;
        private Integer revisionNo;
        private Integer status;
        private String processInstanceId;
        private String processDefinitionKey;
    }

    @Data
    class SnapshotFact {
        private Long id;
        private Long tenantId;
        private Long executionId;
        private String processInstanceId;
        private String processDefinitionId;
        private String processDefinitionKey;
        private String currentBpmTaskId;
        private String currentTaskDefinitionKey;
        private String approvalStatus;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    class WorkTaskFact extends MesProEdhrWorkTaskDO {
        private Long tenantId;
    }

    @Select("SELECT id, tenant_id, revision_no, status, process_instance_id, process_definition_key "
            + "FROM mes_pro_batch_record_execution WHERE tenant_id = #{tenant} AND id = #{execution} AND deleted = 0")
    List<ExecutionFact> selectExecutions(@Param("tenant") Long tenant, @Param("execution") Long execution);

    @Select("SELECT id, tenant_id, revision_no, status, process_instance_id, process_definition_key "
            + "FROM mes_pro_batch_record_execution WHERE tenant_id = #{tenant} AND id = #{execution} AND deleted = 0 FOR UPDATE")
    List<ExecutionFact> selectExecutionsForUpdate(@Param("tenant") Long tenant, @Param("execution") Long execution);

    @Select("SELECT id, tenant_id, execution_id, process_instance_id, process_definition_id, process_definition_key, "
            + "current_bpm_task_id, current_task_definition_key, approval_status "
            + "FROM mes_pro_batch_record_approval_snapshot WHERE tenant_id = #{tenant} "
            + "AND execution_id = #{execution} AND deleted = 0")
    List<SnapshotFact> selectSnapshots(@Param("tenant") Long tenant, @Param("execution") Long execution);

    @Select("SELECT id, tenant_id, execution_id, process_instance_id, process_definition_id, process_definition_key, "
            + "current_bpm_task_id, current_task_definition_key, approval_status "
            + "FROM mes_pro_batch_record_approval_snapshot WHERE tenant_id = #{tenant} "
            + "AND execution_id = #{execution} AND deleted = 0 FOR UPDATE")
    List<SnapshotFact> selectSnapshotsForUpdate(@Param("tenant") Long tenant, @Param("execution") Long execution);

    @Select("SELECT * FROM mes_pro_edhr_work_task WHERE tenant_id = #{tenant} "
            + "AND execution_id = #{execution} AND bpm_task_id = #{task} AND deleted = 0 "
            + "AND task_type IN ('REVIEW', 'APPROVE')")
    List<WorkTaskFact> selectWorkTasks(@Param("tenant") Long tenant, @Param("execution") Long execution,
            @Param("task") String task);

    @Select("SELECT * FROM mes_pro_edhr_work_task WHERE tenant_id = #{tenant} "
            + "AND execution_id = #{execution} AND deleted = 0 AND task_type IN ('REVIEW', 'APPROVE')")
    List<WorkTaskFact> selectPhaseWorkTasks(@Param("tenant") Long tenant, @Param("execution") Long execution);

    @Select("SELECT * FROM mes_pro_edhr_work_task WHERE tenant_id = #{tenant} "
            + "AND execution_id = #{execution} AND deleted = 0 AND task_type IN ('REVIEW', 'APPROVE') ORDER BY id FOR UPDATE")
    List<WorkTaskFact> selectPhaseWorkTasksForUpdate(@Param("tenant") Long tenant, @Param("execution") Long execution);
}
