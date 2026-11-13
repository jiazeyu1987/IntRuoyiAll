package cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProFeedbackImportRecordMapper extends BaseMapperX<MesProFeedbackImportRecordDO> {

    java.util.Map<String, ListFilterQueryUtils.Field> LIST_FILTER_FIELDS = java.util.Map.ofEntries(
            java.util.Map.entry("id", ListFilterQueryUtils.number("id")),
            java.util.Map.entry("sourceFileName", ListFilterQueryUtils.text("source_file_name")),
            java.util.Map.entry("sourceFileSha256", ListFilterQueryUtils.text("source_file_sha256")),
            java.util.Map.entry("sheetName", ListFilterQueryUtils.text("sheet_name")),
            java.util.Map.entry("rowNo", ListFilterQueryUtils.number("row_no")),
            java.util.Map.entry("feedbackId", ListFilterQueryUtils.number("feedback_id")),
            java.util.Map.entry("attributionStatus", ListFilterQueryUtils.text("CONCAT(CASE WHEN attribution_status='ATTRIBUTED' THEN CASE WHEN attribution_target_type='EXTERNAL_OTHER_ORDER' OR (feedback_id IS NULL AND NOT EXISTS(SELECT 1 FROM mes_pro_feedback f WHERE f.source_import_record_id=mes_pro_feedback_import_record.id AND f.tenant_id=mes_pro_feedback_import_record.tenant_id AND f.deleted=0)) THEN '其他订单' ELSE '已归属' END ELSE '待归属' END,' ',COALESCE(CAST(NULLIF(feedback_id,0) AS CHAR),''),' ',CASE WHEN attribution_status='ATTRIBUTED' THEN DATE_FORMAT(update_time,'%Y-%m-%d %H:%i:%s') ELSE '' END,' ',CASE WHEN attribution_status='ATTRIBUTED' THEN (SELECT COUNT(*) FROM mes_pro_feedback f WHERE f.source_import_record_id=mes_pro_feedback_import_record.id AND f.tenant_id=mes_pro_feedback_import_record.tenant_id AND f.deleted=0) ELSE 0 END)")),
            java.util.Map.entry("taskCode", ListFilterQueryUtils.text("task_code")),
            java.util.Map.entry("workOrderCode", ListFilterQueryUtils.text("JSON_UNQUOTE(JSON_EXTRACT(source_payload_json,'$.workOrderCode'))")),
            java.util.Map.entry("itemCode", ListFilterQueryUtils.text("JSON_UNQUOTE(JSON_EXTRACT(source_payload_json,'$.itemCode'))")),
            java.util.Map.entry("processCode", ListFilterQueryUtils.text("CONCAT(COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(source_payload_json,'$.processCode')),''),'--'),' ',COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(source_payload_json,'$.processName')),''),'--'))")),
            java.util.Map.entry("sourcePayloadJson", ListFilterQueryUtils.text("source_payload_json")),
            java.util.Map.entry("scheduleOrderId", ListFilterQueryUtils.number("schedule_order_id")),
            java.util.Map.entry("scheduleOrderProcessId", ListFilterQueryUtils.number("schedule_order_process_id")),
            java.util.Map.entry("attributionTargetType", ListFilterQueryUtils.text("attribution_target_type")),
            java.util.Map.entry("candidateCount", ListFilterQueryUtils.number("candidate_count")),
            java.util.Map.entry("progressSourceType", ListFilterQueryUtils.text("progress_source_type")),
            java.util.Map.entry("progressQuantity", ListFilterQueryUtils.number("progress_quantity")),
            java.util.Map.entry("progressAppliedTime", ListFilterQueryUtils.dateTime("progress_applied_time")),
            java.util.Map.entry("progressWarningCode", ListFilterQueryUtils.text("progress_warning_code")),
            java.util.Map.entry("progressWarningMessage", ListFilterQueryUtils.text("progress_warning_message")),
            java.util.Map.entry("remark", ListFilterQueryUtils.text("CASE WHEN EXISTS(SELECT 1 FROM mes_pro_feedback f WHERE f.source_import_record_id=mes_pro_feedback_import_record.id AND f.tenant_id=mes_pro_feedback_import_record.tenant_id AND f.deleted=0) THEN (SELECT f.remark FROM mes_pro_feedback f WHERE f.source_import_record_id=mes_pro_feedback_import_record.id AND f.tenant_id=mes_pro_feedback_import_record.tenant_id AND f.deleted=0 ORDER BY f.id LIMIT 1) ELSE remark END")),
            java.util.Map.entry("createTime", ListFilterQueryUtils.dateTime("create_time")),
            java.util.Map.entry("creator", ListFilterQueryUtils.text("creator")),
            java.util.Map.entry("itemName", ListFilterQueryUtils.text("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(source_payload_json,'$.itemName')),'null')")),
            java.util.Map.entry("specification", ListFilterQueryUtils.text("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(source_payload_json,'$.specification')),'null')")),
            java.util.Map.entry("processName", ListFilterQueryUtils.text("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(source_payload_json,'$.processName')),'null')")),
            java.util.Map.entry("feedbackQuantity", ListFilterQueryUtils.number("JSON_UNQUOTE(JSON_EXTRACT(source_payload_json,'$.feedbackQuantity'))")),
            java.util.Map.entry("feedbackTime", ListFilterQueryUtils.dateTime("JSON_UNQUOTE(JSON_EXTRACT(source_payload_json,'$.feedbackTime'))")),
            java.util.Map.entry("displayColumn12", ListFilterQueryUtils.text("CASE WHEN EXISTS(SELECT 1 FROM mes_pro_feedback f WHERE f.source_import_record_id=mes_pro_feedback_import_record.id AND f.tenant_id=mes_pro_feedback_import_record.tenant_id AND f.deleted=0) THEN (SELECT f.remark FROM mes_pro_feedback f WHERE f.source_import_record_id=mes_pro_feedback_import_record.id AND f.tenant_id=mes_pro_feedback_import_record.tenant_id AND f.deleted=0 ORDER BY f.id LIMIT 1) ELSE remark END")),
            java.util.Map.entry("displayColumn9", ListFilterQueryUtils.text("(SELECT COALESCE(u.nickname,CAST(f.feedback_user_id AS CHAR)) FROM mes_pro_feedback f LEFT JOIN system_users u ON u.id=f.feedback_user_id AND u.tenant_id=f.tenant_id AND u.deleted=0 WHERE f.source_import_record_id=mes_pro_feedback_import_record.id AND f.tenant_id=mes_pro_feedback_import_record.tenant_id AND f.deleted=0 ORDER BY f.id LIMIT 1)")),
            java.util.Map.entry("displayColumn11", ListFilterQueryUtils.text("(SELECT COALESCE(u.nickname,CAST(f.approve_user_id AS CHAR)) FROM mes_pro_feedback f LEFT JOIN system_users u ON u.id=f.approve_user_id AND u.tenant_id=f.tenant_id AND u.deleted=0 WHERE f.source_import_record_id=mes_pro_feedback_import_record.id AND f.tenant_id=mes_pro_feedback_import_record.tenant_id AND f.deleted=0 ORDER BY f.id LIMIT 1)")),
            java.util.Map.entry("displayColumn10", ListFilterQueryUtils.dateTime("CASE WHEN EXISTS(SELECT 1 FROM mes_pro_feedback f WHERE f.source_import_record_id=mes_pro_feedback_import_record.id AND f.tenant_id=mes_pro_feedback_import_record.tenant_id AND f.deleted=0) THEN (SELECT f.feedback_time FROM mes_pro_feedback f WHERE f.source_import_record_id=mes_pro_feedback_import_record.id AND f.tenant_id=mes_pro_feedback_import_record.tenant_id AND f.deleted=0 ORDER BY f.id LIMIT 1) ELSE JSON_UNQUOTE(JSON_EXTRACT(source_payload_json,'$.feedbackTime')) END")),
            java.util.Map.entry("surplusPoolQuantity", ListFilterQueryUtils.number("(SELECT COALESCE(SUM(pool.available_quantity),0) FROM mes_pro_feedback_surplus_pool pool JOIN mes_pro_process pr ON pr.id=pool.process_id AND pr.tenant_id=pool.tenant_id AND pr.deleted=0 WHERE pr.code=JSON_UNQUOTE(JSON_EXTRACT(mes_pro_feedback_import_record.source_payload_json,'$.processCode')) AND pool.tenant_id=mes_pro_feedback_import_record.tenant_id AND pool.deleted=0 AND pool.status='AVAILABLE' AND pool.available_quantity>0)"))
    );



    default MesProFeedbackImportRecordDO selectBySourceFingerprint(String sourceFileSha256, String sheetName, Integer rowNo) {
        return selectOne(new LambdaQueryWrapperX<MesProFeedbackImportRecordDO>()
                .eq(MesProFeedbackImportRecordDO::getSourceFileSha256, sourceFileSha256)
                .eq(MesProFeedbackImportRecordDO::getSheetName, sheetName)
                .eq(MesProFeedbackImportRecordDO::getRowNo, rowNo));
    }

    default PageResult<MesProFeedbackImportRecordDO> selectPage(MesProFeedbackImportRecordPageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<MesProFeedbackImportRecordDO>()
                .eqIfPresent(MesProFeedbackImportRecordDO::getId, reqVO.getId())
                .inIfPresent(MesProFeedbackImportRecordDO::getId, reqVO.getImportRecordIds())
                .eqIfPresent(MesProFeedbackImportRecordDO::getFeedbackId, reqVO.getFeedbackId())
                .eqIfPresent(MesProFeedbackImportRecordDO::getAttributionStatus, reqVO.getAttributionStatus())
                .orderByDesc(MesProFeedbackImportRecordDO::getId), LIST_FILTER_FIELDS));
    }

    default List<MesProFeedbackImportRecordDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackImportRecordDO>()
                .in(MesProFeedbackImportRecordDO::getId, ids));
    }

    default List<MesProFeedbackImportRecordDO> selectListByFeedbackIds(Collection<Long> feedbackIds) {
        if (feedbackIds == null || feedbackIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackImportRecordDO>()
                .in(MesProFeedbackImportRecordDO::getFeedbackId, feedbackIds)
                .eq(MesProFeedbackImportRecordDO::getAttributionStatus, MesProFeedbackImportRecordDO.ATTRIBUTION_STATUS_ATTRIBUTED)
                .orderByDesc(MesProFeedbackImportRecordDO::getId));
    }

    default List<MesProFeedbackImportRecordDO> selectAppliedDirectProgressListByScheduleOrderId(Long scheduleOrderId) {
        if (scheduleOrderId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackImportRecordDO>()
                .eq(MesProFeedbackImportRecordDO::getScheduleOrderId, scheduleOrderId)
                .eq(MesProFeedbackImportRecordDO::getProgressSourceType,
                        MesProFeedbackImportRecordDO.PROGRESS_SOURCE_TYPE_DIRECT_WORK_REPORT)
                .eq(MesProFeedbackImportRecordDO::getAttributionStatus,
                        MesProFeedbackImportRecordDO.ATTRIBUTION_STATUS_ATTRIBUTED)
                .gt(MesProFeedbackImportRecordDO::getProgressQuantity, java.math.BigDecimal.ZERO)
                .orderByAsc(MesProFeedbackImportRecordDO::getProgressAppliedTime)
                .orderByAsc(MesProFeedbackImportRecordDO::getId));
    }
}
