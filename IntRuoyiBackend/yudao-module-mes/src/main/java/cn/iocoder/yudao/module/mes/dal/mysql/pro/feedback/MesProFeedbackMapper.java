package cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.time.LocalDate;

/**
 * MES 生产报工 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesProFeedbackMapper extends BaseMapperX<MesProFeedbackDO> {

    java.util.Map<String, ListFilterQueryUtils.Field> LIST_FILTER_FIELDS = java.util.Map.ofEntries(
            java.util.Map.entry("id", ListFilterQueryUtils.number("id")),
            java.util.Map.entry("code", ListFilterQueryUtils.text("code")),
            java.util.Map.entry("type", ListFilterQueryUtils.number("type")),
            java.util.Map.entry("channel", ListFilterQueryUtils.text("channel")),
            java.util.Map.entry("feedbackTime", ListFilterQueryUtils.dateTime("feedback_time")),
            java.util.Map.entry("workstationId", ListFilterQueryUtils.number("workstation_id")),
            java.util.Map.entry("routeId", ListFilterQueryUtils.number("route_id")),
            java.util.Map.entry("processId", ListFilterQueryUtils.number("process_id")),
            java.util.Map.entry("workOrderId", ListFilterQueryUtils.number("work_order_id")),
            java.util.Map.entry("taskId", ListFilterQueryUtils.number("task_id")),
            java.util.Map.entry("scheduleOrderId", ListFilterQueryUtils.number("schedule_order_id")),
            java.util.Map.entry("scheduleOrderProcessId", ListFilterQueryUtils.number("schedule_order_process_id")),
            java.util.Map.entry("sourceImportRecordId", ListFilterQueryUtils.number("source_import_record_id")),
            java.util.Map.entry("itemId", ListFilterQueryUtils.number("item_id")),
            java.util.Map.entry("expireDate", ListFilterQueryUtils.dateTime("expire_date")),
            java.util.Map.entry("lotNumber", ListFilterQueryUtils.text("lot_number")),
            java.util.Map.entry("scheduledQuantity", ListFilterQueryUtils.number("scheduled_quantity")),
            java.util.Map.entry("feedbackQuantity", ListFilterQueryUtils.number("feedback_quantity")),
            java.util.Map.entry("qualifiedQuantity", ListFilterQueryUtils.number("qualified_quantity")),
            java.util.Map.entry("unqualifiedQuantity", ListFilterQueryUtils.number("unqualified_quantity")),
            java.util.Map.entry("uncheckQuantity", ListFilterQueryUtils.number("uncheck_quantity")),
            java.util.Map.entry("laborScrapQuantity", ListFilterQueryUtils.number("labor_scrap_quantity")),
            java.util.Map.entry("materialScrapQuantity", ListFilterQueryUtils.number("material_scrap_quantity")),
            java.util.Map.entry("otherScrapQuantity", ListFilterQueryUtils.number("other_scrap_quantity")),
            java.util.Map.entry("lossReasonId", ListFilterQueryUtils.number("loss_reason_id")),
            java.util.Map.entry("lossReasonCodeSnapshot", ListFilterQueryUtils.text("loss_reason_code_snapshot")),
            java.util.Map.entry("lossReasonNameSnapshot", ListFilterQueryUtils.text("loss_reason_name_snapshot")),
            java.util.Map.entry("feedbackUserId", ListFilterQueryUtils.number("feedback_user_id")),
            java.util.Map.entry("approveUserId", ListFilterQueryUtils.number("approve_user_id")),
            java.util.Map.entry("status", ListFilterQueryUtils.number("status")),
            java.util.Map.entry("remark", ListFilterQueryUtils.text("remark")),
            java.util.Map.entry("createTime", ListFilterQueryUtils.dateTime("create_time")),
            java.util.Map.entry("creator", ListFilterQueryUtils.text("creator")),
            java.util.Map.entry("excelProductCode", ListFilterQueryUtils.text("COALESCE(NULLIF(TRIM((SELECT NULLIF(JSON_UNQUOTE(JSON_EXTRACT(x.source_payload_json,'$.itemCode')),'null') FROM mes_pro_feedback_import_record x WHERE x.tenant_id=mes_pro_feedback.tenant_id AND x.deleted=0 AND (x.id=mes_pro_feedback.source_import_record_id OR (x.feedback_id=mes_pro_feedback.id AND x.attribution_status='ATTRIBUTED')) ORDER BY (x.id=mes_pro_feedback.source_import_record_id) DESC,x.id DESC LIMIT 1)),''),NULLIF(TRIM((SELECT x.code FROM mes_md_item x WHERE x.id=mes_pro_feedback.item_id AND x.tenant_id=mes_pro_feedback.tenant_id AND x.deleted=0)),''),'--')")),
            java.util.Map.entry("excelProductName", ListFilterQueryUtils.text("COALESCE(NULLIF(TRIM((SELECT NULLIF(JSON_UNQUOTE(JSON_EXTRACT(x.source_payload_json,'$.itemName')),'null') FROM mes_pro_feedback_import_record x WHERE x.tenant_id=mes_pro_feedback.tenant_id AND x.deleted=0 AND (x.id=mes_pro_feedback.source_import_record_id OR (x.feedback_id=mes_pro_feedback.id AND x.attribution_status='ATTRIBUTED')) ORDER BY (x.id=mes_pro_feedback.source_import_record_id) DESC,x.id DESC LIMIT 1)),''),NULLIF(TRIM((SELECT x.name FROM mes_md_item x WHERE x.id=mes_pro_feedback.item_id AND x.tenant_id=mes_pro_feedback.tenant_id AND x.deleted=0)),''),'--')")),
            java.util.Map.entry("excelProcessCode", ListFilterQueryUtils.text("COALESCE(NULLIF(TRIM((SELECT NULLIF(JSON_UNQUOTE(JSON_EXTRACT(x.source_payload_json,'$.processCode')),'null') FROM mes_pro_feedback_import_record x WHERE x.tenant_id=mes_pro_feedback.tenant_id AND x.deleted=0 AND (x.id=mes_pro_feedback.source_import_record_id OR (x.feedback_id=mes_pro_feedback.id AND x.attribution_status='ATTRIBUTED')) ORDER BY (x.id=mes_pro_feedback.source_import_record_id) DESC,x.id DESC LIMIT 1)),''),NULLIF(TRIM((SELECT x.code FROM mes_pro_process x WHERE x.id=mes_pro_feedback.process_id AND x.tenant_id=mes_pro_feedback.tenant_id AND x.deleted=0)),''),'--')")),
            java.util.Map.entry("excelProcessName", ListFilterQueryUtils.text("COALESCE(NULLIF(TRIM((SELECT NULLIF(JSON_UNQUOTE(JSON_EXTRACT(x.source_payload_json,'$.processName')),'null') FROM mes_pro_feedback_import_record x WHERE x.tenant_id=mes_pro_feedback.tenant_id AND x.deleted=0 AND (x.id=mes_pro_feedback.source_import_record_id OR (x.feedback_id=mes_pro_feedback.id AND x.attribution_status='ATTRIBUTED')) ORDER BY (x.id=mes_pro_feedback.source_import_record_id) DESC,x.id DESC LIMIT 1)),''),NULLIF(TRIM((SELECT x.name FROM mes_pro_process x WHERE x.id=mes_pro_feedback.process_id AND x.tenant_id=mes_pro_feedback.tenant_id AND x.deleted=0)),''),'--')")),
            java.util.Map.entry("excelEmployeeName", ListFilterQueryUtils.text("COALESCE(NULLIF(TRIM((SELECT NULLIF(JSON_UNQUOTE(JSON_EXTRACT(x.source_payload_json,'$.feedbackUserName')),'null') FROM mes_pro_feedback_import_record x WHERE x.tenant_id=mes_pro_feedback.tenant_id AND x.deleted=0 AND (x.id=mes_pro_feedback.source_import_record_id OR (x.feedback_id=mes_pro_feedback.id AND x.attribution_status='ATTRIBUTED')) ORDER BY (x.id=mes_pro_feedback.source_import_record_id) DESC,x.id DESC LIMIT 1)),''),NULLIF(TRIM((SELECT x.nickname FROM system_users x WHERE x.id=mes_pro_feedback.feedback_user_id AND x.tenant_id=mes_pro_feedback.tenant_id AND x.deleted=0)),''),'--')")),
            java.util.Map.entry("excelDepartment", ListFilterQueryUtils.text("(SELECT NULLIF(JSON_UNQUOTE(JSON_EXTRACT(x.source_payload_json,'$.department')),'null') FROM mes_pro_feedback_import_record x WHERE x.tenant_id=mes_pro_feedback.tenant_id AND x.deleted=0 AND (x.id=mes_pro_feedback.source_import_record_id OR (x.feedback_id=mes_pro_feedback.id AND x.attribution_status='ATTRIBUTED')) ORDER BY (x.id=mes_pro_feedback.source_import_record_id) DESC,x.id DESC LIMIT 1)")),
            java.util.Map.entry("excelEmployeeNo", ListFilterQueryUtils.text("(SELECT NULLIF(JSON_UNQUOTE(JSON_EXTRACT(x.source_payload_json,'$.feedbackUserCode')),'null') FROM mes_pro_feedback_import_record x WHERE x.tenant_id=mes_pro_feedback.tenant_id AND x.deleted=0 AND (x.id=mes_pro_feedback.source_import_record_id OR (x.feedback_id=mes_pro_feedback.id AND x.attribution_status='ATTRIBUTED')) ORDER BY (x.id=mes_pro_feedback.source_import_record_id) DESC,x.id DESC LIMIT 1)")),
            java.util.Map.entry("excelSectionLeader", ListFilterQueryUtils.text("(SELECT NULLIF(JSON_UNQUOTE(JSON_EXTRACT(x.source_payload_json,'$.approverName')),'null') FROM mes_pro_feedback_import_record x WHERE x.tenant_id=mes_pro_feedback.tenant_id AND x.deleted=0 AND (x.id=mes_pro_feedback.source_import_record_id OR (x.feedback_id=mes_pro_feedback.id AND x.attribution_status='ATTRIBUTED')) ORDER BY (x.id=mes_pro_feedback.source_import_record_id) DESC,x.id DESC LIMIT 1)")),
            java.util.Map.entry("excelFeedbackTime", ListFilterQueryUtils.dateTime("COALESCE((SELECT NULLIF(JSON_UNQUOTE(JSON_EXTRACT(x.source_payload_json,'$.feedbackTime')),'null') FROM mes_pro_feedback_import_record x WHERE x.tenant_id=mes_pro_feedback.tenant_id AND x.deleted=0 AND (x.id=mes_pro_feedback.source_import_record_id OR (x.feedback_id=mes_pro_feedback.id AND x.attribution_status='ATTRIBUTED')) ORDER BY (x.id=mes_pro_feedback.source_import_record_id) DESC,x.id DESC LIMIT 1),feedback_time)"))
    );



    default PageResult<MesProFeedbackDO> selectPage(MesProFeedbackPageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<MesProFeedbackDO>()
                .eqIfPresent(MesProFeedbackDO::getId, reqVO.getId())
                .likeIfPresent(MesProFeedbackDO::getCode, reqVO.getCode())
                .eqIfPresent(MesProFeedbackDO::getType, reqVO.getType())
                .eqIfPresent(MesProFeedbackDO::getWorkOrderId, reqVO.getWorkOrderId())
                .eqIfPresent(MesProFeedbackDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesProFeedbackDO::getFeedbackTime, reqVO.getFeedbackTime())
                .eqIfPresent(MesProFeedbackDO::getItemId, reqVO.getItemId())
                .eqIfPresent(MesProFeedbackDO::getFeedbackUserId, reqVO.getFeedbackUserId())
                .eqIfPresent(MesProFeedbackDO::getCreator, reqVO.getCreator())
                .orderByDesc(MesProFeedbackDO::getId), LIST_FILTER_FIELDS));
    }

    default List<MesProFeedbackDO> selectListByTaskIds(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackDO>()
                .in(MesProFeedbackDO::getTaskId, taskIds));
    }

    default List<MesProFeedbackDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackDO>()
                .in(MesProFeedbackDO::getId, ids)
                .orderByAsc(MesProFeedbackDO::getId));
    }

    default List<MesProFeedbackDO> selectListByIdsForUpdate(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackDO>()
                .in(MesProFeedbackDO::getId, ids)
                .orderByAsc(MesProFeedbackDO::getId)
                .last("FOR UPDATE"));
    }

    default int updateCorrectedProductionReport(Long id,
                                                BigDecimal feedbackQuantity,
                                                BigDecimal qualifiedQuantity,
                                                BigDecimal lossQuantity,
                                                Long lossReasonId,
                                                String lossReasonCodeSnapshot,
                                                String lossReasonNameSnapshot) {
        return update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MesProFeedbackDO>()
                .eq(MesProFeedbackDO::getId, id)
                .set(MesProFeedbackDO::getFeedbackQuantity, feedbackQuantity)
                .set(MesProFeedbackDO::getQualifiedQuantity, qualifiedQuantity)
                .set(MesProFeedbackDO::getUnqualifiedQuantity, lossQuantity)
                .set(MesProFeedbackDO::getLaborScrapQuantity, lossQuantity)
                .set(MesProFeedbackDO::getMaterialScrapQuantity, BigDecimal.ZERO)
                .set(MesProFeedbackDO::getOtherScrapQuantity, BigDecimal.ZERO)
                .set(MesProFeedbackDO::getLossReasonId, lossReasonId)
                .set(MesProFeedbackDO::getLossReasonCodeSnapshot, lossReasonCodeSnapshot)
                .set(MesProFeedbackDO::getLossReasonNameSnapshot, lossReasonNameSnapshot));
    }

    default List<MesProFeedbackDO> selectListBySourceImportRecordId(Long sourceImportRecordId) {
        if (sourceImportRecordId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackDO>()
                .eq(MesProFeedbackDO::getSourceImportRecordId, sourceImportRecordId)
                .orderByAsc(MesProFeedbackDO::getId));
    }

    default List<MesProFeedbackDO> selectListBySourceImportRecordIds(Collection<Long> sourceImportRecordIds) {
        if (sourceImportRecordIds == null || sourceImportRecordIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackDO>()
                .in(MesProFeedbackDO::getSourceImportRecordId, sourceImportRecordIds)
                .orderByAsc(MesProFeedbackDO::getSourceImportRecordId)
                .orderByAsc(MesProFeedbackDO::getId));
    }

    default List<MesProFeedbackDO> selectFinishedListByScheduleOrderId(Long scheduleOrderId) {
        if (scheduleOrderId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackDO>()
                .eq(MesProFeedbackDO::getScheduleOrderId, scheduleOrderId)
                .eq(MesProFeedbackDO::getStatus, MesProFeedbackStatusEnum.FINISHED.getStatus())
                .orderByAsc(MesProFeedbackDO::getFeedbackTime)
                .orderByAsc(MesProFeedbackDO::getId));
    }

    default List<MesProFeedbackDO> selectProgressListByScheduleOrderId(Long scheduleOrderId) {
        if (scheduleOrderId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackDO>()
                .eq(MesProFeedbackDO::getScheduleOrderId, scheduleOrderId)
                .in(MesProFeedbackDO::getStatus, List.of(
                        MesProFeedbackStatusEnum.APPROVING.getStatus(),
                        MesProFeedbackStatusEnum.UNCHECK.getStatus(),
                        MesProFeedbackStatusEnum.FINISHED.getStatus()))
                .orderByAsc(MesProFeedbackDO::getFeedbackTime)
                .orderByAsc(MesProFeedbackDO::getId));
    }

    default List<MesProFeedbackDO> selectFinishedListByScheduleOrderProcessIdsToday(
            Collection<Long> scheduleOrderProcessIds, LocalDate date) {
        if (scheduleOrderProcessIds == null || scheduleOrderProcessIds.isEmpty() || date == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackDO>()
                .in(MesProFeedbackDO::getScheduleOrderProcessId, scheduleOrderProcessIds)
                .eq(MesProFeedbackDO::getStatus, MesProFeedbackStatusEnum.FINISHED.getStatus())
                .ge(MesProFeedbackDO::getFeedbackTime, date.atStartOfDay())
                .lt(MesProFeedbackDO::getFeedbackTime, date.plusDays(1).atStartOfDay())
                .orderByAsc(MesProFeedbackDO::getFeedbackTime)
                .orderByAsc(MesProFeedbackDO::getId));
    }

    default List<MesProFeedbackDO> selectUnifiedFormalApprovalSources(Collection<Long> feedbackIds, String keyword) {
        if (feedbackIds.isEmpty()) return Collections.emptyList();
        LambdaQueryWrapperX<MesProFeedbackDO> query = new LambdaQueryWrapperX<>();
        query.apply("tenant_id = {0}", cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId());
        return selectList(query
                .in(MesProFeedbackDO::getId, feedbackIds)
                .likeIfPresent(MesProFeedbackDO::getCode, keyword)
                .orderByDesc(MesProFeedbackDO::getId));
    }

    default List<MesProFeedbackDO> selectUnifiedApprovalList(Long approveUserId, Long feedbackUserId,
                                                             Collection<Integer> statuses, String keyword) {
        if (statuses == null || statuses.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapperX<MesProFeedbackDO> query = new LambdaQueryWrapperX<>();
        query.apply("tenant_id = {0}", cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId());
        return selectList(query
                .eqIfPresent(MesProFeedbackDO::getApproveUserId, approveUserId)
                .eqIfPresent(MesProFeedbackDO::getFeedbackUserId, feedbackUserId)
                .in(MesProFeedbackDO::getStatus, statuses)
                .likeIfPresent(MesProFeedbackDO::getCode, keyword)
                .orderByDesc(MesProFeedbackDO::getId));
    }

}
