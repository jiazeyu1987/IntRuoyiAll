package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** Narrow adjunct to the explicitly authorized fixed test-order reset. No audit/file deletion. */
@Mapper
public interface MesFixedTestOrderDownstreamCleanupMapper {
    /** Candidate rows are explicitly tenant-scoped; parent existence must include all tenants to fail closed. */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            <script>
            SELECT COUNT(*) FROM (SELECT task.id FROM mes_pro_edhr_work_task task WHERE task.tenant_id = #{tenantId} AND (task.work_order_id = #{workOrderId}
              <if test='!batchIds.isEmpty()'> OR task.batch_execution_id IN <foreach collection='batchIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>
              <if test='!executionIds.isEmpty()'> OR task.execution_id IN <foreach collection='executionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>
              <if test='!applicationIds.isEmpty()'> OR (task.business_scope_type = 'RELEASE_APPLICATION' AND task.business_scope_id IN <foreach collection='applicationIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>)</if>)
            AND ( (task.work_order_id IS NOT NULL AND task.work_order_id != #{workOrderId})  OR (task.batch_execution_id IS NOT NULL
              <if test='!batchIds.isEmpty()'> AND task.batch_execution_id NOT IN <foreach collection='batchIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>)  OR (task.execution_id IS NOT NULL
              <if test='!executionIds.isEmpty()'> AND task.execution_id NOT IN <foreach collection='executionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>)  OR (task.business_scope_type = 'RELEASE_APPLICATION' AND task.business_scope_id IS NOT NULL
              AND (<choose><when test='!applicationIds.isEmpty()'>task.business_scope_id NOT IN <foreach collection='applicationIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></when><otherwise>1 = 1</otherwise></choose>)
              AND NOT (task.work_order_id IS NOT NULL AND task.work_order_id = #{workOrderId} AND NOT EXISTS (
                SELECT 1 FROM mes_pro_process_pool_active_order_release_application application_parent
                WHERE application_parent.id = task.business_scope_id
              )) )
            )
            UNION ALL
            SELECT id FROM mes_pro_edhr_deviation WHERE tenant_id = #{tenantId} AND (work_order_id = #{workOrderId}
              <if test='!batchIds.isEmpty()'> OR batch_execution_id IN <foreach collection='batchIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if> )
            AND ( (work_order_id IS NOT NULL AND work_order_id != #{workOrderId})  OR (batch_execution_id IS NOT NULL
              <if test='!batchIds.isEmpty()'> AND batch_execution_id NOT IN <foreach collection='batchIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>) )
            UNION ALL
            SELECT review.id FROM mes_pro_edhr_nonconformance_review review WHERE review.tenant_id = #{tenantId} AND (review.work_order_id = #{workOrderId}
              <if test='!activeOrderIds.isEmpty()'> OR review.active_order_id IN <foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>
              <if test='!batchIds.isEmpty()'> OR review.batch_execution_id IN <foreach collection='batchIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if> )
            AND ( (review.work_order_id IS NOT NULL AND review.work_order_id != #{workOrderId})  OR (review.active_order_id IS NOT NULL
              AND (<choose><when test='!activeOrderIds.isEmpty()'>review.active_order_id NOT IN <foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></when><otherwise>1 = 1</otherwise></choose>)
              AND NOT (review.work_order_id IS NOT NULL AND review.work_order_id = #{workOrderId} AND NOT EXISTS (
                SELECT 1 FROM mes_pro_process_pool_active_order active_order_parent
                WHERE active_order_parent.id = review.active_order_id
              )) ) OR (review.batch_execution_id IS NOT NULL
              <if test='!batchIds.isEmpty()'> AND review.batch_execution_id NOT IN <foreach collection='batchIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>)
            ) ) AS ownership_conflicts
            </script>
            """)
    long countOwnershipConflicts(@Param("tenantId") Long tenantId, @Param("workOrderId") Long workOrderId,
            @Param("activeOrderIds") List<Long> activeOrderIds, @Param("batchIds") List<Long> batchIds,
            @Param("executionIds") List<Long> executionIds, @Param("applicationIds") List<Long> applicationIds);

    /** Review object ids are record executions; VOID/RELEASE object ids are batch executions. */
    @Select("""
            <script>
            SELECT DISTINCT process_id FROM (
              SELECT TRIM(CAST(process_instance_id AS CHAR(128))) AS process_id FROM mes_pro_batch_record_execution
              WHERE tenant_id = #{tenantId} AND
              <choose><when test='!executionIds.isEmpty()'>id IN
                <foreach collection='executionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>
              </when><otherwise>1 = 0</otherwise></choose>
              UNION ALL
              SELECT TRIM(CAST(bpm_process_instance_id AS CHAR(128))) FROM mes_pro_edhr_record_change_event
              WHERE tenant_id = #{tenantId} AND
              <choose><when test='!batchIds.isEmpty()'>batch_execution_id IN
                <foreach collection='batchIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>
              </when><otherwise>1 = 0</otherwise></choose>
              UNION ALL
              SELECT TRIM(CAST(process_instance_id AS CHAR(128))) FROM bpm_business_approval_request
              WHERE tenant_id = #{tenantId} AND data_domain = 'MES' AND system_code = 'MES'
                AND object_type = 'EDHR_BATCH_EXECUTION' AND (1 = 0
                <if test='!executionIds.isEmpty()'>OR (action_code = 'SUBMIT_REVIEW' AND CAST(object_id AS CHAR(128)) IN
                  <foreach collection='executionIds' item='id' open='(' separator=',' close=')'>CAST(#{id} AS CHAR(30))</foreach>)</if>
                <if test='!batchIds.isEmpty()'>OR (action_code IN ('VOID','RELEASE') AND CAST(object_id AS CHAR(128)) IN
                  <foreach collection='batchIds' item='id' open='(' separator=',' close=')'>CAST(#{id} AS CHAR(30))</foreach>)</if>)
              UNION ALL
              SELECT TRIM(CAST(bpm_process_instance_id AS CHAR(128))) FROM bpm_form_action_instance
              WHERE tenant_id = #{tenantId} AND data_domain = 'MES' AND system_code = 'MES'
                AND object_type = 'EDHR_BATCH_EXECUTION' AND (1 = 0
                <if test='!executionIds.isEmpty()'>OR (action_code = 'SUBMIT_REVIEW' AND CAST(object_id AS CHAR(128)) IN
                  <foreach collection='executionIds' item='id' open='(' separator=',' close=')'>CAST(#{id} AS CHAR(30))</foreach>)</if>
                <if test='!batchIds.isEmpty()'>OR (action_code IN ('VOID','RELEASE') AND CAST(object_id AS CHAR(128)) IN
                  <foreach collection='batchIds' item='id' open='(' separator=',' close=')'>CAST(#{id} AS CHAR(30))</foreach>)</if>)
            ) owned_processes WHERE process_id IS NOT NULL AND TRIM(process_id) != '' ORDER BY process_id
            </script>
            """)
    List<String> selectProcessInstanceIds(@Param("tenantId") Long tenantId,
            @Param("batchIds") List<Long> batchIds, @Param("executionIds") List<Long> executionIds);

    @Select({"<script>",
            "SELECT id FROM mes_pro_edhr_deviation WHERE tenant_id = #{tenantId} AND (",
            "work_order_id = #{workOrderId}",
            "<if test='batchIds != null and !batchIds.isEmpty()'>",
            "OR (work_order_id IS NULL AND batch_execution_id IN",
            "<foreach collection='batchIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>)",
            "</if>) ORDER BY id FOR UPDATE", "</script>"})
    List<Long> selectDeviationIdsForUpdate(@Param("tenantId") Long tenantId,
            @Param("workOrderId") Long workOrderId, @Param("batchIds") List<Long> batchIds);

    @Select({"<script>",
            "SELECT id FROM mes_pro_edhr_work_task WHERE tenant_id = #{tenantId} AND deleted = b'0' AND (",
            "work_order_id = #{workOrderId} OR (work_order_id IS NULL AND (1 = 0",
            "<if test='batchIds != null and !batchIds.isEmpty()'> OR batch_execution_id IN",
            "<foreach collection='batchIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "<if test='executionIds != null and !executionIds.isEmpty()'> OR execution_id IN",
            "<foreach collection='executionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "<if test='applicationIds != null and !applicationIds.isEmpty()'>",
            "OR (business_scope_type = 'RELEASE_APPLICATION' AND business_scope_id IN",
            "<foreach collection='applicationIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>)</if>",
            "))) ORDER BY id FOR UPDATE", "</script>"})
    List<Long> selectWorkTaskIdsForUpdate(@Param("tenantId") Long tenantId,
            @Param("workOrderId") Long workOrderId, @Param("batchIds") List<Long> batchIds,
            @Param("executionIds") List<Long> executionIds, @Param("applicationIds") List<Long> applicationIds);

    @Delete({"<script>", "DELETE FROM mes_pro_edhr_deviation_handling WHERE tenant_id = #{tenantId} AND deviation_id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>", "</script>"})
    int deleteDeviationHandling(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);

    @Delete({"<script>", "DELETE FROM mes_pro_edhr_deviation_create_request WHERE tenant_id = #{tenantId} AND deviation_id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>", "</script>"})
    int deleteDeviationRequests(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);

    @Delete({"<script>", "DELETE FROM mes_pro_edhr_deviation WHERE tenant_id = #{tenantId} AND id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>", "</script>"})
    int deleteDeviations(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);

    @Delete({"<script>", "DELETE FROM mes_pro_edhr_work_task WHERE tenant_id = #{tenantId} AND id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>", "</script>"})
    int deleteWorkTasks(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);

    @Delete("DELETE FROM mes_pro_edhr_nonconformance_review WHERE tenant_id = #{tenantId} AND work_order_id = #{workOrderId}")
    int deleteNonconformanceReviewsForWorkOrder(@Param("tenantId") Long tenantId,
            @Param("workOrderId") Long workOrderId);
}
