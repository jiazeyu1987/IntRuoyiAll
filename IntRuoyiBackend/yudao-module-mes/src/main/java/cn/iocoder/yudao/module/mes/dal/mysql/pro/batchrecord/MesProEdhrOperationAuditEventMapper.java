package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOperationAuditPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrOperationAuditEventMapper extends BaseMapperX<MesProEdhrOperationAuditEventDO> {

    default PageResult<MesProEdhrOperationAuditEventDO> selectPage(MesProEdhrOperationAuditPageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, MesMarketReleaseHandoverMapper.readable(new LambdaQueryWrapperX<MesProEdhrOperationAuditEventDO>(),"batch_execution_id")
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getBatchExecutionId, reqVO.getBatchExecutionId())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getExecutionId, reqVO.getExecutionId())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getWorkTaskId, reqVO.getWorkTaskId())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getRouteId, reqVO.getRouteId())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getRouteProcessId, reqVO.getRouteProcessId())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getReportId, reqVO.getReportId())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getRecordCategory, reqVO.getRecordCategory())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getObjectType, reqVO.getObjectType())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getObjectId, reqVO.getObjectId())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getOperationType, reqVO.getOperationType())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getActorUserId, reqVO.getActorUserId())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getPermissionDecision, reqVO.getPermissionDecision())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getResultStatus, reqVO.getResultStatus())
                .betweenIfPresent(MesProEdhrOperationAuditEventDO::getOccurredAt, reqVO.getOccurredAt())
                .orderByDesc(MesProEdhrOperationAuditEventDO::getOccurredAt)
                .orderByDesc(MesProEdhrOperationAuditEventDO::getId), java.util.Map.ofEntries(
                java.util.Map.entry("id", number("id")),
                java.util.Map.entry("object", text("CONCAT(CASE UPPER(TRIM(object_type)) WHEN 'BATCH_EXECUTION_PAGE' THEN '批次执行页面' WHEN 'BATCH_EXECUTION' THEN '批次执行' WHEN 'BATCH_EXECUTION_TASK' THEN '批次执行任务' WHEN 'BATCH_RECORD_EXECUTION' THEN '批记录执行' WHEN 'BATCH_ARCHIVE' THEN '批次归档' WHEN 'EXECUTION_ARCHIVE_PAGE' THEN '归档页面' WHEN 'CONTROLLED_FILE' THEN '受控文件' WHEN 'FORM_TEMPLATE' THEN '表单模板' WHEN 'DHR_TEMPLATE' THEN '电子批记录模板' WHEN 'RECORDBOOK_TEMPLATE' THEN '记录册模板' WHEN 'BATCH_RECORD_VERSION' THEN '批记录版本' WHEN 'ROUTE_PROCESS_BATCH_RECORD' THEN '工序批记录配置' WHEN 'SPECIAL_NODE_ATTACHMENT' THEN '特殊节点附件' WHEN 'WORK_TASK_ASSIGNMENT_RULE' THEN '工作任务规则' WHEN 'WORK_TASK' THEN '工作任务' WHEN 'RELEASE_TRANSACTION' THEN '放行事务' ELSE '未知对象' END,' ',COALESCE(object_id,'--'),' ',COALESCE(execution_id,'--'))")),
                java.util.Map.entry("action", text("CONCAT(COALESCE(NULLIF(TRIM(action_name),''),CASE UPPER(TRIM(operation_type)) WHEN 'QUERY' THEN '查询' WHEN 'VIEW' THEN '查看' WHEN 'OPEN' THEN '打开' WHEN 'LIST' THEN '列表查询' WHEN 'SKIP' THEN '跳过' WHEN 'COMPLETE' THEN '完成' WHEN 'SYNC' THEN '同步' WHEN 'CLOSE' THEN '关闭' WHEN 'QUALITY_REJECT' THEN '质量拒收' WHEN 'ARCHIVE' THEN '归档' WHEN 'DOWNLOAD' THEN '下载' WHEN 'PRINT_PDF' THEN '生成打印或导出PDF' WHEN 'FIELD_CHANGE' THEN '字段变更' WHEN 'VERIFY' THEN '校验' WHEN 'EXPORT' THEN '导出' WHEN 'EXPORT_AUDIT' THEN '导出审计' WHEN 'PERMISSION_RULE_SAVE' THEN '保存权限规则' WHEN 'PERMISSION_EVALUATE' THEN '权限评估' WHEN 'LOCAL_STATE_SAMPLE_CREATE' THEN '本地状态样本创建' WHEN 'ATTACHMENT_PREPARE_UPLOAD' THEN '附件上传预登记' WHEN 'ATTACHMENT_PENDING_DELETE' THEN '待提交附件删除' WHEN 'ATTACHMENT_SAVE_PENDING' THEN '待提交附件保存' WHEN 'WORK_TASK_RULE_SAVE' THEN '工作任务规则保存' WHEN 'CANDIDATE_SIGNATURE_COMPLETE' THEN '候选签名完成' WHEN 'FILL_TASK_REASSIGN' THEN '填写任务重新派发' WHEN 'PRECHECK' THEN '预检' ELSE '未知动作类型' END),' ',CASE UPPER(TRIM(operation_type)) WHEN 'QUERY' THEN '查询' WHEN 'VIEW' THEN '查看' WHEN 'OPEN' THEN '打开' WHEN 'LIST' THEN '列表查询' WHEN 'SKIP' THEN '跳过' WHEN 'COMPLETE' THEN '完成' WHEN 'SYNC' THEN '同步' WHEN 'CLOSE' THEN '关闭' WHEN 'QUALITY_REJECT' THEN '质量拒收' WHEN 'ARCHIVE' THEN '归档' WHEN 'DOWNLOAD' THEN '下载' WHEN 'PRINT_PDF' THEN '生成打印或导出PDF' WHEN 'FIELD_CHANGE' THEN '字段变更' WHEN 'VERIFY' THEN '校验' WHEN 'EXPORT' THEN '导出' WHEN 'EXPORT_AUDIT' THEN '导出审计' WHEN 'PERMISSION_RULE_SAVE' THEN '保存权限规则' WHEN 'PERMISSION_EVALUATE' THEN '权限评估' WHEN 'LOCAL_STATE_SAMPLE_CREATE' THEN '本地状态样本创建' WHEN 'ATTACHMENT_PREPARE_UPLOAD' THEN '附件上传预登记' WHEN 'ATTACHMENT_PENDING_DELETE' THEN '待提交附件删除' WHEN 'ATTACHMENT_SAVE_PENDING' THEN '待提交附件保存' WHEN 'WORK_TASK_RULE_SAVE' THEN '工作任务规则保存' WHEN 'CANDIDATE_SIGNATURE_COMPLETE' THEN '候选签名完成' WHEN 'FILL_TASK_REASSIGN' THEN '填写任务重新派发' WHEN 'PRECHECK' THEN '预检' ELSE '未知动作类型' END)")),
                java.util.Map.entry("resultStatus", text("CASE UPPER(TRIM(result_status)) WHEN 'SUCCESS' THEN '成功' WHEN 'FAILED' THEN '失败' WHEN 'REJECTED' THEN '拒绝' ELSE '未知结果' END")),
                java.util.Map.entry("permissionDecision", text("CASE UPPER(TRIM(permission_decision)) WHEN 'ALLOW' THEN '允许' WHEN 'DENY' THEN '拒绝' ELSE '未知权限决策' END")),
                java.util.Map.entry("actor", text("COALESCE(NULLIF(actor_username,''),CAST(actor_user_id AS CHAR),'--')")),
                java.util.Map.entry("occurredAt", dateTime("occurred_at")))));
    }

    default List<MesProEdhrOperationAuditEventDO> selectListByObject(String objectType, String objectId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrOperationAuditEventDO>()
                .eq(MesProEdhrOperationAuditEventDO::getObjectType, objectType)
                .eq(MesProEdhrOperationAuditEventDO::getObjectId, objectId)
                .orderByDesc(MesProEdhrOperationAuditEventDO::getOccurredAt)
                .orderByDesc(MesProEdhrOperationAuditEventDO::getId));
    }

    default List<MesProEdhrOperationAuditEventDO> selectRevisionListByObject(String objectType,
                                                                              String objectId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrOperationAuditEventDO>()
                .eq(MesProEdhrOperationAuditEventDO::getObjectType, objectType)
                .eq(MesProEdhrOperationAuditEventDO::getObjectId, objectId)
                .eq(MesProEdhrOperationAuditEventDO::getOperationType, "UPDATE")
                .eq(MesProEdhrOperationAuditEventDO::getResultStatus, "SUCCESS")
                .orderByAsc(MesProEdhrOperationAuditEventDO::getOccurredAt)
                .orderByAsc(MesProEdhrOperationAuditEventDO::getId));
    }

    default List<MesProEdhrOperationAuditEventDO> selectSuccessfulListByBatchExecutionIdAndOperation(
            Long batchExecutionId, String operationType) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrOperationAuditEventDO>()
                .eq(MesProEdhrOperationAuditEventDO::getBatchExecutionId, batchExecutionId)
                .eq(MesProEdhrOperationAuditEventDO::getOperationType, operationType)
                .eq(MesProEdhrOperationAuditEventDO::getResultStatus, "SUCCESS")
                .orderByDesc(MesProEdhrOperationAuditEventDO::getOccurredAt)
                .orderByDesc(MesProEdhrOperationAuditEventDO::getId));
    }

    default List<MesProEdhrOperationAuditEventDO> selectSuccessfulOpenListByActiveOrderId(Long activeOrderId) {
        if (activeOrderId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProEdhrOperationAuditEventDO>()
                .eq(MesProEdhrOperationAuditEventDO::getOperationType, "OPEN")
                .eq(MesProEdhrOperationAuditEventDO::getResultStatus, "SUCCESS")
                .apply("JSON_UNQUOTE(JSON_EXTRACT(metadata_json, '$.activeOrderId')) = {0}",
                        String.valueOf(activeOrderId))
                .orderByDesc(MesProEdhrOperationAuditEventDO::getOccurredAt)
                .orderByDesc(MesProEdhrOperationAuditEventDO::getId));
    }

    default List<MesProEdhrOperationAuditEventDO> selectSuccessfulListByActiveOrderId(Long activeOrderId) {
        if (activeOrderId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProEdhrOperationAuditEventDO>()
                .eq(MesProEdhrOperationAuditEventDO::getResultStatus, "SUCCESS")
                .apply("JSON_UNQUOTE(JSON_EXTRACT(metadata_json, '$.activeOrderId')) = {0}",
                        String.valueOf(activeOrderId))
                .orderByDesc(MesProEdhrOperationAuditEventDO::getOccurredAt)
                .orderByDesc(MesProEdhrOperationAuditEventDO::getId));
    }
}
