package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 生产组长运行数据清理 Mapper。
 *
 * 所有语句都显式带租户和稳定业务 ID，避免把清理范围扩大到其它租户或其它业务对象。
 */
@Mapper
public interface MesTeamLeaderDataCleanupMapper {

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_active_order_release_application WHERE tenant_id = #{tenantId} AND active_order_id IN",
            "<foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteReleaseApplications(@Param("tenantId") Long tenantId,
                                  @Param("activeOrderIds") Collection<Long> activeOrderIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_team_maintenance_audit WHERE tenant_id = #{tenantId}",
            "AND target_type = 'ACTIVE_ORDER' AND target_id IN",
            "<foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteActiveOrderAudits(@Param("tenantId") Long tenantId,
                                @Param("activeOrderIds") Collection<Long> activeOrderIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_active_order WHERE tenant_id = #{tenantId} AND id IN",
            "<foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteActiveOrders(@Param("tenantId") Long tenantId,
                           @Param("activeOrderIds") Collection<Long> activeOrderIds);

    @Update({
            "<script>",
            "UPDATE mes_pro_process_pool_active_order",
            "SET active_status = 'REMOVED',",
            "    business_status = 'REMOVED',",
            "    removed_at = #{removedAt},",
            "    version = version + 1,",
            "    updater = CAST(#{actorUserId} AS CHAR),",
            "    update_time = #{removedAt}",
            "WHERE tenant_id = #{tenantId}",
            "  AND deleted = b'0'",
            "  AND active_status = 'ACTIVE'",
            "  AND id IN",
            "<foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int softRemoveActiveOrders(@Param("tenantId") Long tenantId,
                               @Param("activeOrderIds") Collection<Long> activeOrderIds,
                               @Param("actorUserId") Long actorUserId,
                               @Param("removedAt") LocalDateTime removedAt);

    @Select({
            "<script>",
            "SELECT id FROM mes_pro_edhr_batch_execution",
            "WHERE tenant_id = #{tenantId} AND work_order_id IN",
            "<foreach collection='workOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectBatchExecutionIdsByWorkOrderIds(@Param("tenantId") Long tenantId,
                                                      @Param("workOrderIds") Collection<Long> workOrderIds);

    @Select("SELECT id FROM mes_pro_edhr_batch_execution WHERE tenant_id = #{tenantId} AND deleted = b'0' ORDER BY id")
    List<Long> selectAllBatchExecutionIds(@Param("tenantId") Long tenantId);

    @Select({
            "<script>",
            "SELECT DISTINCT work_order_id FROM mes_pro_edhr_batch_execution",
            "WHERE tenant_id = #{tenantId} AND deleted = b'0' AND work_order_id IS NOT NULL AND id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectWorkOrderIdsByBatchExecutionIds(@Param("tenantId") Long tenantId,
                                                     @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Select({
            "<script>",
            "SELECT id FROM mes_pro_batch_record_execution",
            "WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectBatchRecordExecutionIds(@Param("tenantId") Long tenantId,
                                              @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Select({
            "<script>",
            "SELECT id FROM mes_pro_edhr_batch_execution_task",
            "WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectBatchTaskIds(@Param("tenantId") Long tenantId,
                                  @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Select({
            "<script>",
            "SELECT id FROM mes_pro_edhr_work_task",
            "WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectWorkTaskIds(@Param("tenantId") Long tenantId,
                                 @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Select({
            "<script>",
            "SELECT id FROM mes_pqc_inspection_task",
            "WHERE tenant_id = #{tenantId} AND active_order_id IN",
            "<foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectPqcTaskIds(@Param("tenantId") Long tenantId,
                                @Param("activeOrderIds") Collection<Long> activeOrderIds);

    @Select({
            "<script>",
            "SELECT id FROM mes_pro_edhr_release_transaction",
            "WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectReleaseTransactionIds(@Param("tenantId") Long tenantId,
                                            @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Select({
            "<script>",
            "SELECT DISTINCT file_id FROM mes_pro_batch_record_execution_attachment",
            "WHERE tenant_id = #{tenantId} AND file_id IS NOT NULL AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectAttachmentFileIds(@Param("tenantId") Long tenantId,
                                       @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Select({
            "<script>",
            "SELECT id FROM mes_pro_process_pool_event",
            "WHERE tenant_id = #{tenantId} AND work_order_id IN",
            "<foreach collection='workOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectEventIdsByWorkOrderIds(@Param("tenantId") Long tenantId,
                                            @Param("workOrderIds") Collection<Long> workOrderIds);

    @Select({
            "<script>",
            "SELECT id FROM mes_pro_process_pool_event",
            "WHERE tenant_id = #{tenantId} AND deleted = b'0'",
            "AND event_type = 'PRODUCTION_SUBMIT'",
            "AND report_output_quantity &gt; 0",
            "AND route_process_id IN",
            "<foreach collection='routeProcessIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectProductionEventIdsByRouteProcessIds(@Param("tenantId") Long tenantId,
                                                         @Param("routeProcessIds") Collection<Long> routeProcessIds);

    @Select("SELECT id FROM mes_pro_process_pool_event WHERE tenant_id = #{tenantId} AND deleted = b'0' ORDER BY id")
    List<Long> selectAllCleanupEventIds(@Param("tenantId") Long tenantId);

    @Select({
            "<script>",
            "SELECT DISTINCT work_order_id FROM mes_pro_process_pool_event",
            "WHERE tenant_id = #{tenantId} AND deleted = b'0'",
            "AND work_order_id IS NOT NULL",
            "AND id IN",
            "<foreach collection='eventIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectWorkOrderIdsByEventIds(@Param("tenantId") Long tenantId,
                                            @Param("eventIds") Collection<Long> eventIds);

    @Select({
            "<script>",
            "SELECT DISTINCT pool_id FROM mes_pro_process_pool_event",
            "WHERE tenant_id = #{tenantId}",
            "AND pool_id IS NOT NULL",
            "AND id IN",
            "<foreach collection='eventIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectProcessPoolIdsByEventIds(@Param("tenantId") Long tenantId,
                                              @Param("eventIds") Collection<Long> eventIds);

    @Select("SELECT id FROM mes_pro_process_pool WHERE tenant_id = #{tenantId} AND deleted = b'0' ORDER BY id")
    List<Long> selectAllProcessPoolIds(@Param("tenantId") Long tenantId);

    @Select({
            "<script>",
            "SELECT * FROM mes_pro_process_pool_active_order",
            "WHERE tenant_id = #{tenantId} AND work_order_id IN",
            "<foreach collection='workOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "AND deleted = b'0'",
            "AND leader_user_id &lt;&gt; #{leaderUserId}",
            "<if test='includeRemoved == false'>AND active_status = 'ACTIVE'</if>",
            "<if test='excludeActiveOrderIds != null and excludeActiveOrderIds.size() > 0'>",
            "AND id NOT IN <foreach collection='excludeActiveOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</if>",
            "<if test='forUpdate'>FOR UPDATE</if>",
            "</script>"
    })
    List<MesProcessPoolActiveOrderDO> selectRelatedCleanupOrders(@Param("tenantId") Long tenantId,
                                                                 @Param("leaderUserId") Long leaderUserId,
                                                                 @Param("workOrderIds") Collection<Long> workOrderIds,
                                                                 @Param("excludeActiveOrderIds") Collection<Long> excludeActiveOrderIds,
                                                                 @Param("includeRemoved") boolean includeRemoved,
                                                                 @Param("forUpdate") boolean forUpdate);

    @Select({
            "<script>",
            "SELECT id FROM mes_pro_edhr_recordbook",
            "WHERE tenant_id = #{tenantId} AND business_object_type = 'BATCH_EXECUTION'",
            "AND business_object_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectRecordbookIds(@Param("tenantId") Long tenantId,
                                   @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Select({
            "<script>",
            "SELECT id FROM mes_pro_edhr_recordbook_entry",
            "WHERE tenant_id = #{tenantId} AND recordbook_id IN",
            "<foreach collection='recordbookIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectRecordbookEntryIds(@Param("tenantId") Long tenantId,
                                        @Param("recordbookIds") Collection<Long> recordbookIds);

    @Select({
            "<script>",
            "SELECT id FROM mes_pro_edhr_form_instance",
            "WHERE tenant_id = #{tenantId} AND business_object_type = 'BATCH_EXECUTION'",
            "AND business_object_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectFormInstanceIds(@Param("tenantId") Long tenantId,
                                     @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Select({
            "<script>",
            "SELECT id FROM mes_pro_edhr_traveler_instance",
            "WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectTravelerIds(@Param("tenantId") Long tenantId,
                                 @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Select({
            "<script>",
            "SELECT id FROM mes_pro_edhr_label_instance WHERE tenant_id = #{tenantId}",
            "<trim prefix='AND (' suffix=')' prefixOverrides='OR'>",
            "<if test='batchExecutionIds != null and batchExecutionIds.size() > 0'>OR (business_type = 'BATCH_EXECUTION' AND business_object_id IN <foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>)</if>",
            "<if test='travelerIds != null and travelerIds.size() > 0'>OR (business_type = 'TRAVELER' AND business_object_id IN <foreach collection='travelerIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>)</if>",
            "</trim>",
            "</script>"
    })
    List<Long> selectLabelInstanceIds(@Param("tenantId") Long tenantId,
                                      @Param("batchExecutionIds") Collection<Long> batchExecutionIds,
                                      @Param("travelerIds") Collection<Long> travelerIds);

    @Select({
            "<script>",
            "SELECT id FROM mes_pro_edhr_print_task WHERE tenant_id = #{tenantId}",
            "<trim prefix='AND (' suffix=')' prefixOverrides='OR'>",
            "<if test='labelInstanceIds != null and labelInstanceIds.size() > 0'>OR (source_type = 'LABEL' AND source_object_id IN <foreach collection='labelInstanceIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>)</if>",
            "<if test='travelerIds != null and travelerIds.size() > 0'>OR traveler_id IN <foreach collection='travelerIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "</trim>",
            "</script>"
    })
    List<Long> selectPrintTaskIds(@Param("tenantId") Long tenantId,
                                  @Param("labelInstanceIds") Collection<Long> labelInstanceIds,
                                  @Param("travelerIds") Collection<Long> travelerIds);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM mes_pro_process_pool_event",
            "WHERE tenant_id = #{tenantId} AND deleted = b'0'",
            "AND event_type = 'PRODUCTION_SUBMIT' AND work_order_id IN",
            "<foreach collection='workOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    Long countProductionEvents(@Param("tenantId") Long tenantId,
                               @Param("workOrderIds") Collection<Long> workOrderIds);

    @Select({
            "<script>",
            "SELECT id FROM mes_pro_process_pool_event",
            "WHERE tenant_id = #{tenantId} AND deleted = b'0'",
            "AND event_type IN ('PRODUCTION_SUBMIT', 'PQC_INSPECTION')",
            "AND work_order_id IN",
            "<foreach collection='workOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectRuntimeEventIds(@Param("tenantId") Long tenantId,
                                     @Param("workOrderIds") Collection<Long> workOrderIds);

    @Select({
            "<script>",
            "SELECT id FROM mes_pro_feedback",
            "WHERE tenant_id = #{tenantId} AND work_order_id IN",
            "<foreach collection='workOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectFeedbackIds(@Param("tenantId") Long tenantId,
                                 @Param("workOrderIds") Collection<Long> workOrderIds);

    @Select({
            "<script>",
            "SELECT id FROM mes_pro_feedback_import_record",
            "WHERE tenant_id = #{tenantId} AND feedback_id IN",
            "<foreach collection='feedbackIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectFeedbackImportRecordIds(@Param("tenantId") Long tenantId,
                                             @Param("feedbackIds") Collection<Long> feedbackIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_feedback_surplus_allocation WHERE tenant_id = #{tenantId}",
            "<trim prefix='AND (' suffix=')' prefixOverrides='OR'>",
            "<if test='feedbackIds != null and feedbackIds.size() > 0'>OR pool_id IN (SELECT id FROM mes_pro_feedback_surplus_pool WHERE tenant_id = #{tenantId} AND source_feedback_id IN <foreach collection='feedbackIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>)</if>",
            "<if test='importRecordIds != null and importRecordIds.size() > 0'>OR pool_id IN (SELECT id FROM mes_pro_feedback_surplus_pool WHERE tenant_id = #{tenantId} AND source_import_record_id IN <foreach collection='importRecordIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>)</if>",
            "<if test='importRecordIds != null and importRecordIds.size() > 0'>OR import_record_id IN <foreach collection='importRecordIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "</trim>",
            "</script>"
    })
    int deleteFeedbackSurplusAllocations(@Param("tenantId") Long tenantId,
                                         @Param("feedbackIds") Collection<Long> feedbackIds,
                                         @Param("importRecordIds") Collection<Long> importRecordIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_feedback_surplus_pool WHERE tenant_id = #{tenantId}",
            "<trim prefix='AND (' suffix=')' prefixOverrides='OR'>",
            "<if test='feedbackIds != null and feedbackIds.size() > 0'>OR source_feedback_id IN <foreach collection='feedbackIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "<if test='importRecordIds != null and importRecordIds.size() > 0'>OR source_import_record_id IN <foreach collection='importRecordIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "</trim>",
            "</script>"
    })
    int deleteFeedbackSurplusPools(@Param("tenantId") Long tenantId,
                                   @Param("feedbackIds") Collection<Long> feedbackIds,
                                   @Param("importRecordIds") Collection<Long> importRecordIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_print_event WHERE tenant_id = #{tenantId} AND print_task_id IN",
            "<foreach collection='printTaskIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deletePrintEvents(@Param("tenantId") Long tenantId,
                          @Param("printTaskIds") Collection<Long> printTaskIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_print_history_copy WHERE tenant_id = #{tenantId} AND source_print_task_id IN",
            "<foreach collection='printTaskIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deletePrintHistoryCopies(@Param("tenantId") Long tenantId,
                                 @Param("printTaskIds") Collection<Long> printTaskIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_reprint_request WHERE tenant_id = #{tenantId}",
            "AND (print_task_id IN",
            "<foreach collection='printTaskIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "OR original_print_task_id IN",
            "<foreach collection='printTaskIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>)",
            "</script>"
    })
    int deleteReprintRequests(@Param("tenantId") Long tenantId,
                              @Param("printTaskIds") Collection<Long> printTaskIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_print_task WHERE tenant_id = #{tenantId} AND id IN",
            "<foreach collection='printTaskIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deletePrintTasks(@Param("tenantId") Long tenantId,
                         @Param("printTaskIds") Collection<Long> printTaskIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_label_instance WHERE tenant_id = #{tenantId} AND id IN",
            "<foreach collection='labelInstanceIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteLabelInstances(@Param("tenantId") Long tenantId,
                             @Param("labelInstanceIds") Collection<Long> labelInstanceIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_feedback_import_record WHERE tenant_id = #{tenantId} AND feedback_id IN",
            "<foreach collection='feedbackIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteFeedbackImportRecords(@Param("tenantId") Long tenantId,
                                    @Param("feedbackIds") Collection<Long> feedbackIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_feedback_material WHERE tenant_id = #{tenantId} AND feedback_id IN",
            "<foreach collection='feedbackIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteFeedbackMaterialsByFeedbackIds(@Param("tenantId") Long tenantId,
                                             @Param("feedbackIds") Collection<Long> feedbackIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_feedback WHERE tenant_id = #{tenantId} AND id IN",
            "<foreach collection='feedbackIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteFeedbacks(@Param("tenantId") Long tenantId,
                        @Param("feedbackIds") Collection<Long> feedbackIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_active_order_version_upgrade_request WHERE tenant_id = #{tenantId}",
            "<trim prefix='AND (' suffix=')' prefixOverrides='OR'>",
            "<if test='activeOrderIds != null and activeOrderIds.size() > 0'>OR source_active_order_id IN <foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "<if test='activeOrderIds != null and activeOrderIds.size() > 0'>OR target_active_order_id IN <foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "<if test='workOrderIds != null and workOrderIds.size() > 0'>OR source_work_order_id IN <foreach collection='workOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "<if test='batchExecutionIds != null and batchExecutionIds.size() > 0'>OR source_batch_execution_id IN <foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "<if test='batchExecutionIds != null and batchExecutionIds.size() > 0'>OR target_batch_execution_id IN <foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "</trim>",
            "</script>"
    })
    int deleteActiveOrderVersionUpgradeRequests(@Param("tenantId") Long tenantId,
                                                @Param("activeOrderIds") Collection<Long> activeOrderIds,
                                                @Param("workOrderIds") Collection<Long> workOrderIds,
                                                @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_event_revision_diff WHERE tenant_id = #{tenantId} AND event_id IN",
            "<foreach collection='eventIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteEventRevisionDiffs(@Param("tenantId") Long tenantId, @Param("eventIds") Collection<Long> eventIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_event_revision WHERE tenant_id = #{tenantId} AND event_id IN",
            "<foreach collection='eventIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteEventRevisions(@Param("tenantId") Long tenantId, @Param("eventIds") Collection<Long> eventIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_submission_review WHERE tenant_id = #{tenantId} AND event_id IN",
            "<foreach collection='eventIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteSubmissionReviews(@Param("tenantId") Long tenantId, @Param("eventIds") Collection<Long> eventIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_review_copy_field WHERE tenant_id = #{tenantId} AND event_id IN",
            "<foreach collection='eventIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteReviewCopyFields(@Param("tenantId") Long tenantId, @Param("eventIds") Collection<Long> eventIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_review_copy WHERE tenant_id = #{tenantId} AND event_id IN",
            "<foreach collection='eventIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteReviewCopies(@Param("tenantId") Long tenantId, @Param("eventIds") Collection<Long> eventIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_quantity_fragment WHERE tenant_id = #{tenantId} AND event_id IN",
            "<foreach collection='eventIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteQuantityFragments(@Param("tenantId") Long tenantId, @Param("eventIds") Collection<Long> eventIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_pqc_record WHERE tenant_id = #{tenantId} AND event_id IN",
            "<foreach collection='eventIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deletePqcRecords(@Param("tenantId") Long tenantId, @Param("eventIds") Collection<Long> eventIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_event WHERE tenant_id = #{tenantId} AND id IN",
            "<foreach collection='eventIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteEvents(@Param("tenantId") Long tenantId, @Param("eventIds") Collection<Long> eventIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_fifo_allocation_line WHERE tenant_id = #{tenantId}",
            "<trim prefix='AND (' suffix=')' prefixOverrides='OR'>",
            "<if test='eventIds != null and eventIds.size() > 0'>OR source_event_id IN <foreach collection='eventIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "<if test='workOrderIds != null and workOrderIds.size() > 0'>OR target_work_order_id IN <foreach collection='workOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "</trim>",
            "</script>"
    })
    int deleteFifoAllocationLines(@Param("tenantId") Long tenantId,
                                  @Param("eventIds") Collection<Long> eventIds,
                                  @Param("workOrderIds") Collection<Long> workOrderIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool WHERE tenant_id = #{tenantId} AND work_order_id IN",
            "<foreach collection='workOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteProcessPoolRows(@Param("tenantId") Long tenantId,
                              @Param("workOrderIds") Collection<Long> workOrderIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool WHERE tenant_id = #{tenantId} AND id IN",
            "<foreach collection='processPoolIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteProcessPoolRowsByIds(@Param("tenantId") Long tenantId,
                                   @Param("processPoolIds") Collection<Long> processPoolIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_report_allocation_adjustment_audit WHERE tenant_id = #{tenantId} AND active_order_id IN",
            "<foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteAllocationAdjustmentAudits(@Param("tenantId") Long tenantId,
                                         @Param("activeOrderIds") Collection<Long> activeOrderIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_report_allocation_state WHERE tenant_id = #{tenantId} AND event_id IN",
            "<foreach collection='eventIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteAllocationStates(@Param("tenantId") Long tenantId,
                               @Param("eventIds") Collection<Long> eventIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_report_allocation WHERE tenant_id = #{tenantId} AND active_order_id IN",
            "<foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteAllocations(@Param("tenantId") Long tenantId,
                          @Param("activeOrderIds") Collection<Long> activeOrderIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pqc_process_inspection_aggregate_detail WHERE tenant_id = #{tenantId} AND active_order_id IN",
            "<foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deletePqcAggregateDetails(@Param("tenantId") Long tenantId,
                                   @Param("activeOrderIds") Collection<Long> activeOrderIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pqc_inspection_piece_detail WHERE tenant_id = #{tenantId} AND task_id IN",
            "<foreach collection='pqcTaskIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deletePqcPieceDetails(@Param("tenantId") Long tenantId,
                              @Param("pqcTaskIds") Collection<Long> pqcTaskIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pqc_inspection_task WHERE tenant_id = #{tenantId} AND active_order_id IN",
            "<foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deletePqcTasks(@Param("tenantId") Long tenantId,
                       @Param("activeOrderIds") Collection<Long> activeOrderIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_active_order_process_snapshot WHERE tenant_id = #{tenantId} AND active_order_id IN",
            "<foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteProcessSnapshots(@Param("tenantId") Long tenantId,
                               @Param("activeOrderIds") Collection<Long> activeOrderIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_order_process_completion WHERE tenant_id = #{tenantId} AND work_order_id IN",
            "<foreach collection='workOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteOrderProcessCompletions(@Param("tenantId") Long tenantId,
                                      @Param("workOrderIds") Collection<Long> workOrderIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_active_order_transfer_trace WHERE tenant_id = #{tenantId} AND active_order_id IN",
            "<foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteTransferTraces(@Param("tenantId") Long tenantId,
                             @Param("activeOrderIds") Collection<Long> activeOrderIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_active_order_pick_list_binding_item WHERE tenant_id = #{tenantId} AND binding_id IN",
            "<foreach collection='bindingIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deletePickListBindingItems(@Param("tenantId") Long tenantId,
                                   @Param("bindingIds") Collection<Long> bindingIds);

    @Select({
            "<script>",
            "SELECT id FROM mes_pro_process_pool_active_order_pick_list_binding",
            "WHERE tenant_id = #{tenantId} AND active_order_id IN",
            "<foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    List<Long> selectPickListBindingIds(@Param("tenantId") Long tenantId,
                                        @Param("activeOrderIds") Collection<Long> activeOrderIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_active_order_pick_list_binding WHERE tenant_id = #{tenantId} AND active_order_id IN",
            "<foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deletePickListBindings(@Param("tenantId") Long tenantId,
                               @Param("activeOrderIds") Collection<Long> activeOrderIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_work_order_abnormal WHERE tenant_id = #{tenantId} AND work_order_id IN",
            "<foreach collection='workOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteWorkOrderAbnormals(@Param("tenantId") Long tenantId,
                                 @Param("workOrderIds") Collection<Long> workOrderIds);


    @Delete({
            "<script>",
            "DELETE FROM mes_pro_feedback_material WHERE tenant_id = #{tenantId} AND active_order_id IN",
            "<foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteFeedbackMaterials(@Param("tenantId") Long tenantId,
                                @Param("activeOrderIds") Collection<Long> activeOrderIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_active_order_completion_backfill",
            "WHERE tenant_id = #{tenantId} AND active_order_id IN",
            "<foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteCompletionBackfills(@Param("tenantId") Long tenantId,
                                  @Param("activeOrderIds") Collection<Long> activeOrderIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_process_pool_active_order_completion_receipt",
            "WHERE tenant_id = #{tenantId} AND active_order_id IN",
            "<foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteCompletionReceipts(@Param("tenantId") Long tenantId,
                                 @Param("activeOrderIds") Collection<Long> activeOrderIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_batch_record_execution_attachment WHERE tenant_id = #{tenantId}",
            "<trim prefix='AND (' suffix=')' prefixOverrides='OR'>",
            "<if test='batchExecutionIds != null and batchExecutionIds.size() > 0'>OR batch_execution_id IN <foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "<if test='executionIds != null and executionIds.size() > 0'>OR execution_id IN <foreach collection='executionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "</trim>",
            "</script>"
    })
    int deleteExecutionAttachments(@Param("tenantId") Long tenantId,
                                   @Param("batchExecutionIds") Collection<Long> batchExecutionIds,
                                   @Param("executionIds") Collection<Long> executionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_batch_record_execution_field_audit_item WHERE tenant_id = #{tenantId} AND execution_id IN",
            "<foreach collection='executionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteExecutionFieldAuditItems(@Param("tenantId") Long tenantId,
                                       @Param("executionIds") Collection<Long> executionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_batch_record_execution_field_audit_batch WHERE tenant_id = #{tenantId} AND execution_id IN",
            "<foreach collection='executionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteExecutionFieldAuditBatches(@Param("tenantId") Long tenantId,
                                         @Param("executionIds") Collection<Long> executionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_batch_record_execution_signature WHERE tenant_id = #{tenantId} AND execution_id IN",
            "<foreach collection='executionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteExecutionSignatures(@Param("tenantId") Long tenantId,
                                  @Param("executionIds") Collection<Long> executionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_batch_record_approval_snapshot WHERE tenant_id = #{tenantId} AND execution_id IN",
            "<foreach collection='executionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteApprovalSnapshots(@Param("tenantId") Long tenantId,
                                @Param("executionIds") Collection<Long> executionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_batch_record_domain_trace_item WHERE tenant_id = #{tenantId} AND execution_id IN",
            "<foreach collection='executionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteDomainTraceItems(@Param("tenantId") Long tenantId,
                               @Param("executionIds") Collection<Long> executionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_batch_record_domain_trace_snapshot WHERE tenant_id = #{tenantId} AND execution_id IN",
            "<foreach collection='executionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteDomainTraceSnapshots(@Param("tenantId") Long tenantId,
                                   @Param("executionIds") Collection<Long> executionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_batch_record_execution_archive_event WHERE tenant_id = #{tenantId} AND execution_id IN",
            "<foreach collection='executionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteExecutionArchiveEvents(@Param("tenantId") Long tenantId,
                                     @Param("executionIds") Collection<Long> executionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_batch_record_execution_archive WHERE tenant_id = #{tenantId} AND execution_id IN",
            "<foreach collection='executionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteExecutionArchives(@Param("tenantId") Long tenantId,
                                @Param("executionIds") Collection<Long> executionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_batch_record_execution WHERE tenant_id = #{tenantId} AND id IN",
            "<foreach collection='executionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteExecutions(@Param("tenantId") Long tenantId,
                         @Param("executionIds") Collection<Long> executionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_operation_audit_event WHERE tenant_id = #{tenantId}",
            "<trim prefix='AND (' suffix=')' prefixOverrides='OR'>",
            "<if test='batchExecutionIds != null and batchExecutionIds.size() > 0'>OR batch_execution_id IN <foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "<if test='executionIds != null and executionIds.size() > 0'>OR execution_id IN <foreach collection='executionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "</trim>",
            "</script>"
    })
    int deleteOperationAuditEvents(@Param("tenantId") Long tenantId,
                                   @Param("batchExecutionIds") Collection<Long> batchExecutionIds,
                                   @Param("executionIds") Collection<Long> executionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_record_change_event WHERE tenant_id = #{tenantId}",
            "<trim prefix='AND (' suffix=')' prefixOverrides='OR'>",
            "<if test='batchExecutionIds != null and batchExecutionIds.size() > 0'>OR batch_execution_id IN <foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "<if test='executionIds != null and executionIds.size() > 0'>OR execution_id IN <foreach collection='executionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "</trim>",
            "</script>"
    })
    int deleteRecordChangeEvents(@Param("tenantId") Long tenantId,
                                 @Param("batchExecutionIds") Collection<Long> batchExecutionIds,
                                 @Param("executionIds") Collection<Long> executionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_work_task WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteWorkTasks(@Param("tenantId") Long tenantId,
                        @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_work_task WHERE tenant_id = #{tenantId}",
            "AND business_scope_type = 'RELEASE_APPLICATION' AND business_scope_id IN",
            "<foreach collection='releaseApplicationIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteReleaseWorkTasks(@Param("tenantId") Long tenantId,
                               @Param("releaseApplicationIds") Collection<Long> releaseApplicationIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_batch_execution_task WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteBatchTasks(@Param("tenantId") Long tenantId,
                         @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_batch_execution_signature WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteBatchSignatures(@Param("tenantId") Long tenantId,
                              @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_batch_execution_archive WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteBatchArchives(@Param("tenantId") Long tenantId,
                            @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_batch_dossier_item WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteDossierItems(@Param("tenantId") Long tenantId,
                           @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_batch_execution_origin WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteBatchOrigins(@Param("tenantId") Long tenantId,
                           @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_batch_execution_trace_link WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteTraceLinks(@Param("tenantId") Long tenantId,
                         @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_batch_execution_trace_manifest WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteTraceManifests(@Param("tenantId") Long tenantId,
                             @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_batch_provisioning_record WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteProvisioningRecords(@Param("tenantId") Long tenantId,
                                  @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_batch_trace_outbox_event WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteTraceOutboxEvents(@Param("tenantId") Long tenantId,
                                @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_material_gate_receipt WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteMaterialGateReceipts(@Param("tenantId") Long tenantId,
                                   @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_nonconformance_review WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteNonconformanceReviews(@Param("tenantId") Long tenantId,
                                    @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_nonconformance_review WHERE tenant_id = #{tenantId}",
            "AND source_type = 'PQC_RELEASE' AND source_id IN",
            "<foreach collection='releaseApplicationIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deletePqcReleaseNonconformanceReviews(@Param("tenantId") Long tenantId,
                                              @Param("releaseApplicationIds") Collection<Long> releaseApplicationIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_nonconformance_review WHERE tenant_id = #{tenantId}",
            "AND active_order_id IN",
            "<foreach collection='activeOrderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteNonconformanceReviewsByActiveOrderIds(@Param("tenantId") Long tenantId,
                                                    @Param("activeOrderIds") Collection<Long> activeOrderIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_traveler_event WHERE tenant_id = #{tenantId} AND traveler_id IN",
            "<foreach collection='travelerIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteTravelerEvents(@Param("tenantId") Long tenantId,
                             @Param("travelerIds") Collection<Long> travelerIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_traveler_instance WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteTravelerInstances(@Param("tenantId") Long tenantId,
                                @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_recordbook_tag_binding WHERE tenant_id = #{tenantId}",
            "<trim prefix='AND (' suffix=')' prefixOverrides='OR'>",
            "<if test='recordbookIds != null and recordbookIds.size() > 0'>OR recordbook_id IN <foreach collection='recordbookIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "<if test='entryIds != null and entryIds.size() > 0'>OR entry_id IN <foreach collection='entryIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "</trim>",
            "</script>"
    })
    int deleteRecordbookTagBindings(@Param("tenantId") Long tenantId,
                                    @Param("recordbookIds") Collection<Long> recordbookIds,
                                    @Param("entryIds") Collection<Long> entryIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_recordbook_event WHERE tenant_id = #{tenantId}",
            "<trim prefix='AND (' suffix=')' prefixOverrides='OR'>",
            "<if test='recordbookIds != null and recordbookIds.size() > 0'>OR recordbook_id IN <foreach collection='recordbookIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "<if test='entryIds != null and entryIds.size() > 0'>OR entry_id IN <foreach collection='entryIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>",
            "</trim>",
            "</script>"
    })
    int deleteRecordbookEvents(@Param("tenantId") Long tenantId,
                               @Param("recordbookIds") Collection<Long> recordbookIds,
                               @Param("entryIds") Collection<Long> entryIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_recordbook_entry WHERE tenant_id = #{tenantId} AND recordbook_id IN",
            "<foreach collection='recordbookIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteRecordbookEntries(@Param("tenantId") Long tenantId,
                                @Param("recordbookIds") Collection<Long> recordbookIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_recordbook WHERE tenant_id = #{tenantId} AND id IN",
            "<foreach collection='recordbookIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteRecordbooks(@Param("tenantId") Long tenantId,
                          @Param("recordbookIds") Collection<Long> recordbookIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_form_event WHERE tenant_id = #{tenantId} AND instance_id IN",
            "<foreach collection='instanceIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteFormEvents(@Param("tenantId") Long tenantId,
                         @Param("instanceIds") Collection<Long> instanceIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_form_value WHERE tenant_id = #{tenantId} AND instance_id IN",
            "<foreach collection='instanceIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteFormValues(@Param("tenantId") Long tenantId,
                         @Param("instanceIds") Collection<Long> instanceIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_form_instance WHERE tenant_id = #{tenantId} AND id IN",
            "<foreach collection='instanceIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteFormInstances(@Param("tenantId") Long tenantId,
                            @Param("instanceIds") Collection<Long> instanceIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_release_check_item WHERE tenant_id = #{tenantId} AND release_transaction_id IN",
            "<foreach collection='releaseTransactionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteReleaseCheckItems(@Param("tenantId") Long tenantId,
                                @Param("releaseTransactionIds") Collection<Long> releaseTransactionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_release_transaction_event WHERE tenant_id = #{tenantId} AND release_transaction_id IN",
            "<foreach collection='releaseTransactionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteReleaseTransactionEvents(@Param("tenantId") Long tenantId,
                                       @Param("releaseTransactionIds") Collection<Long> releaseTransactionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_release_decision WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteReleaseDecisions(@Param("tenantId") Long tenantId,
                               @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_release_decision WHERE tenant_id = #{tenantId} AND release_transaction_id IN",
            "<foreach collection='releaseTransactionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteReleaseDecisionsByTransactionIds(@Param("tenantId") Long tenantId,
                                               @Param("releaseTransactionIds") Collection<Long> releaseTransactionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_release_transaction WHERE tenant_id = #{tenantId} AND batch_execution_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteReleaseTransactions(@Param("tenantId") Long tenantId,
                                  @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_release_transaction WHERE tenant_id = #{tenantId} AND id IN",
            "<foreach collection='releaseTransactionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteReleaseTransactionsByIds(@Param("tenantId") Long tenantId,
                                       @Param("releaseTransactionIds") Collection<Long> releaseTransactionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_flow_event WHERE tenant_id = #{tenantId}",
            "AND business_object_type = 'EDHR_BATCH_EXECUTION' AND business_object_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteBatchFlowEvents(@Param("tenantId") Long tenantId,
                              @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_flow_intervention WHERE tenant_id = #{tenantId}",
            "AND business_object_type = 'EDHR_BATCH_EXECUTION' AND business_object_id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteBatchFlowInterventions(@Param("tenantId") Long tenantId,
                                     @Param("batchExecutionIds") Collection<Long> batchExecutionIds);

    @Delete({
            "<script>",
            "DELETE FROM mes_pro_edhr_batch_execution WHERE tenant_id = #{tenantId} AND id IN",
            "<foreach collection='batchExecutionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int deleteBatchExecutions(@Param("tenantId") Long tenantId,
                              @Param("batchExecutionIds") Collection<Long> batchExecutionIds);
}
