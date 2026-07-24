package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProBatchRecordExecutionErrorCodeConstants {

    ErrorCode PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS =
            new ErrorCode(1_040_750_200, "批记录执行实例不存在");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_BATCH_CODE_REQUIRED =
            new ErrorCode(1_040_750_201, "批次号不能为空");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID =
            new ErrorCode(1_040_750_202, "当前执行状态不允许该操作");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_ROUTE_PROCESS_NOT_EXISTS =
            new ErrorCode(1_040_750_203, "执行上下文对应的工艺路线工序不存在");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_DEFAULT_REPORT_REQUIRED =
            new ErrorCode(1_040_750_204, "当前工艺路线工序未配置默认批记录报表");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_TEMPLATE_NOT_EXISTS =
            new ErrorCode(1_040_750_205, "当前工序未配置可用的批记录模板");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_SIGNATURE_NOT_AUTHORIZED =
            new ErrorCode(1_040_750_206, "当前用户未开通电子签名授权");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PASSWORD_INVALID =
            new ErrorCode(1_040_750_207, "当前密码校验失败");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PERSIST_FAILED =
            new ErrorCode(1_040_750_208, "执行签名记录保存失败");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_SNAPSHOT_SOURCE_UNAVAILABLE =
            new ErrorCode(1_040_750_209, "默认批记录报表缺少可用的运行态快照来源");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_LATEST_PUBLISHED_VERSION_REQUIRED =
            new ErrorCode(1_040_750_210, "新业务只能使用最新已发布的批记录表单");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_APPROVAL_PROCESS_DEFINITION_NOT_EXISTS =
            new ErrorCode(1_040_750_220, "eDHR 审批流程定义不存在或未启用");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_MISSING =
            new ErrorCode(1_040_750_221, "eDHR 审批快照缺失");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_APPROVAL_SNAPSHOT_INVALID =
            new ErrorCode(1_040_750_222, "eDHR 审批快照无效");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_NOT_EXISTS =
            new ErrorCode(1_040_750_223, "eDHR 审批任务不存在或已处理");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_APPROVAL_TASK_CONTEXT_INVALID =
            new ErrorCode(1_040_750_224, "BPM 任务与 eDHR 执行记录不匹配");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_APPROVAL_NOT_ALLOWED =
            new ErrorCode(1_040_750_225, "当前用户无权审批该 eDHR 任务");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_APPROVAL_REASON_REQUIRED =
            new ErrorCode(1_040_750_226, "驳回原因不能为空");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_ALREADY_CLOSED =
            new ErrorCode(1_040_750_227, "eDHR 记录已关闭，不允许该操作");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_APPROVAL_CONTEXT_MISSING =
            new ErrorCode(1_040_750_228, "eDHR 审批业务上下文缺失");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_BASELINE_MISSING =
            new ErrorCode(1_040_750_230, "eDHR 字段审计基线缺失，禁止保存字段变更");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_REASON_REQUIRED =
            new ErrorCode(1_040_750_231, "字段变更原因不能为空");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_SIGNATURE_REQUIRED =
            new ErrorCode(1_040_750_232, "字段变更必须完成电子签名");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_SIGNATURE_BIND_FAILED =
            new ErrorCode(1_040_750_233, "字段审计签名绑定失败");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_FIELD_NOT_DECLARED =
            new ErrorCode(1_040_750_234, "字段未在执行快照中声明");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_OLD_VALUE_MISMATCH =
            new ErrorCode(1_040_750_235, "字段旧值与当前记录不一致");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_CONFLICT =
            new ErrorCode(1_040_750_236, "字段审计 revision 或链头已变化，请刷新后重试");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_IDEMPOTENCY_CONFLICT =
            new ErrorCode(1_040_750_237, "幂等键已被不同请求使用");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_PERSIST_FAILED =
            new ErrorCode(1_040_750_238, "字段审计记录保存失败");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_INVALID =
            new ErrorCode(1_040_750_239, "字段审计链校验失败");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_EXPORT_FAILED =
            new ErrorCode(1_040_750_240, "字段审计导出失败");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED =
            new ErrorCode(1_040_750_241, "字段值类型不受支持");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_REASON_CATEGORY_INVALID =
            new ErrorCode(1_040_750_242, "字段变更原因分类不受支持");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_CELL_RULE_UNREVIEWED =
            new ErrorCode(1_040_750_243, "批记录模板存在未确认填写规则的可填单元格：{}");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_CELL_RULE_INVALID =
            new ErrorCode(1_040_750_244, "批记录模板填写规则无效：{}");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION =
            new ErrorCode(1_040_750_245, "字段值不符合模板填写规则：{}");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_SIGNATURE_CELL_VALUE_FORBIDDEN =
            new ErrorCode(1_040_750_263, "签名格必须通过电子签名完成，不能作为普通字段保存");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_REVIEW_SIGNATURE_CELL_MISSING =
            new ErrorCode(1_040_750_246, "eDHR 模板缺少审批签名单元格");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_REVIEW_SOURCE_INVALID =
            new ErrorCode(1_040_750_247, "eDHR 审批签字格来源无效：{}");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_NOT_UNIQUE =
            new ErrorCode(1_040_750_248, "eDHR 审批来源必须解析为唯一启用用户：{}");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_REVIEW_BPM_TASK_COUNT_MISMATCH =
            new ErrorCode(1_040_750_249, "eDHR BPM 并行审核任务数量与模板审核人不一致");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_REVIEW_BPM_TASK_CONTEXT_MISMATCH =
            new ErrorCode(1_040_750_260, "eDHR BPM 并行审核任务与模板审核人不匹配：{}");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_SELECTION_REQUIRED =
            new ErrorCode(1_040_750_261, "eDHR 提交审核必须选择审核/批准人：{}");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_REVIEW_ASSIGNEE_SELECTION_INVALID =
            new ErrorCode(1_040_750_262, "eDHR 提交审核选择的审核/批准人不在候选范围内：{}");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_REQUIRED_FIELD_MISSING =
            new ErrorCode(1_040_750_250, "eDHR 必填字段未填写：{}");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_FILE_METADATA_INVALID =
            new ErrorCode(1_040_750_251, "eDHR 附件文件元数据不完整或不合法");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_WORK_TASK_INVALID =
            new ErrorCode(1_040_750_252, "eDHR 附件绑定任务不存在、状态无效或操作者不匹配");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_PERSIST_FAILED =
            new ErrorCode(1_040_750_253, "eDHR 附件台账保存失败");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_CHANGE_REASON_REQUIRED =
            new ErrorCode(1_040_750_254, "eDHR 作废/重开/补录原因不能为空");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_CHANGE_DUPLICATED =
            new ErrorCode(1_040_750_255, "eDHR 已存在生效或待处理的同类变更事件");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_CHANGE_NOT_EXISTS =
            new ErrorCode(1_040_750_256, "eDHR 变更事件不存在");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_CHANGE_STATUS_INVALID =
            new ErrorCode(1_040_750_257, "eDHR 变更事件状态不允许该操作");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_GROUP_NOT_EXISTS =
            new ErrorCode(1_040_750_258, "eDHR 附件组不存在，不能替换或作废");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID =
            new ErrorCode(1_040_750_259, "eDHR 表单写入任务无效：{}");
    ErrorCode PRO_BATCH_RECORD_EXECUTION_SHARED_CONTEXT_REQUIRED =
            new ErrorCode(1_040_750_264, "批次共享表单执行上下文缺失");
}
