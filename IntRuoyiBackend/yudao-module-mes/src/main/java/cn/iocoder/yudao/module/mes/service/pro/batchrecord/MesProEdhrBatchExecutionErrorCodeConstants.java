package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProEdhrBatchExecutionErrorCodeConstants {

    ErrorCode PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS =
            new ErrorCode(1_040_750_400, "eDHR 批次执行不存在");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_WORK_ORDER_NOT_EXISTS =
            new ErrorCode(1_040_750_401, "eDHR 批次执行对应工单不存在");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_WORK_ORDER_INVALID =
            new ErrorCode(1_040_750_413, "eDHR 批次执行只能选择未取消且未冻结的生产工单");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_ROUTE_NOT_EXISTS =
            new ErrorCode(1_040_750_402, "eDHR 批次执行对应工艺路线不存在");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED =
            new ErrorCode(1_040_750_403, "eDHR 批次执行缺少工艺流程批记录配置流程配置或默认批记录");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_TASK_NOT_EXISTS =
            new ErrorCode(1_040_750_404, "eDHR 批次工序任务不存在");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_TASK_BLOCKED =
            new ErrorCode(1_040_750_405, "eDHR 批次工序任务被阻塞");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID =
            new ErrorCode(1_040_750_406, "当前 eDHR 批次状态不允许该操作");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_CLOSE_BLOCKED =
            new ErrorCode(1_040_750_407, "eDHR 批次存在未完成或证据不完整的工序，禁止关闭：{}");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_SIGNATURE_REQUIRED =
            new ErrorCode(1_040_750_408, "eDHR 批次关闭缺少必要电子签名");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_AUDIT_CHAIN_INVALID =
            new ErrorCode(1_040_750_409, "eDHR 批次关闭审计链校验失败");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_ARCHIVE_NOT_CLOSED =
            new ErrorCode(1_040_750_410, "只有关闭后的 eDHR 批次才允许生成最终归档");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_ARCHIVE_NOT_EXISTS =
            new ErrorCode(1_040_750_411, "eDHR 批次最终归档不存在");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_ARCHIVE_REGENERATE_REQUIRED =
            new ErrorCode(1_040_750_418, "请先重新生成最终归档后再下载打印版 PDF");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_TASK_CONTEXT_REQUIRED =
            new ErrorCode(1_040_750_412, "eDHR 批次缺少唯一批记录路线");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_SPECIAL_NODE_INVALID =
            new ErrorCode(1_040_750_414, "必填路线表单不允许跳过");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_STERILIZATION_BATCH_REQUIRED =
            new ErrorCode(1_040_750_415, "灭菌批次必填");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_SCHEDULE_PREREQUISITE_MISSING =
            new ErrorCode(1_040_750_416, "排产完成创建 eDHR 批次缺少前置条件：{}");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_OWNER_INVALID =
            new ErrorCode(1_040_750_417, "仅批次负责人可关闭 eDHR 批次：{}");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_TASK_NOT_VISIBLE =
            new ErrorCode(1_040_750_419, "当前用户无权查看该 eDHR 批次工序任务");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_PRODUCT_ROUTE_DUPLICATE =
            new ErrorCode(1_040_750_420, "eDHR 批次工单产品对应多条同名工艺路线，请先清理唯一保留路线：{}");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_ROUTE_MISMATCH =
            new ErrorCode(1_040_750_421, "eDHR 批次请求工艺路线与工单产品对应路线不一致：{}");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_ROUTE_VERSION_REQUIRED =
            new ErrorCode(1_040_750_422, "eDHR 批次执行对应工艺路线缺少生效版本，routeId={}");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_PENDING_VOID_ACTION_LOCKED =
            new ErrorCode(1_040_750_423, "作废申请待处理，只能撤回作废申请");
    ErrorCode PRO_EDHR_RELEASE_PRECHECK_REQUIRED =
            new ErrorCode(1_040_750_430, "eDHR 放行前检查未通过，禁止提交放行");
    ErrorCode PRO_EDHR_RELEASE_STATUS_INVALID =
            new ErrorCode(1_040_750_431, "当前 eDHR 放行状态不允许该操作");
    ErrorCode PRO_EDHR_RELEASE_REASON_REQUIRED =
            new ErrorCode(1_040_750_432, "eDHR 放行驳回或撤回必须填写原因");
    ErrorCode PRO_EDHR_RELEASE_SIGNOFF_REQUIRED =
            new ErrorCode(1_040_750_433, "eDHR 放行批准缺少签核证据");
    ErrorCode PRO_EDHR_RELEASE_IDEMPOTENCY_KEY_REQUIRED =
            new ErrorCode(1_040_750_434, "eDHR 放行动作缺少幂等键");
    ErrorCode PRO_EDHR_RELEASE_OWNER_INVALID =
            new ErrorCode(1_040_750_435, "仅批次负责人可放行 eDHR 批次：{}");
    ErrorCode PRO_EDHR_RELEASE_SIGNATURE_PASSWORD_REQUIRED =
            new ErrorCode(1_040_750_436, "eDHR 放行必须填写负责人电子签名密码");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_CLOSE_PRECHECK_REQUIRED =
            new ErrorCode(1_040_750_437, "eDHR 放行预检未通过，禁止关闭批次");
    ErrorCode PRO_EDHR_FLOW_INTERVENTION_REASON_REQUIRED =
            new ErrorCode(1_040_750_440, "eDHR 流程干预必须填写原因");
    ErrorCode PRO_EDHR_FLOW_INTERVENTION_SIGNOFF_REQUIRED =
            new ErrorCode(1_040_750_441, "eDHR 流程干预缺少签核证据");
    ErrorCode PRO_EDHR_FLOW_INTERVENTION_AUTHORIZATION_REQUIRED =
            new ErrorCode(1_040_750_442, "eDHR 管理员干预必须填写授权依据");
    ErrorCode PRO_EDHR_FLOW_INTERVENTION_IDEMPOTENCY_KEY_REQUIRED =
            new ErrorCode(1_040_750_443, "eDHR 流程干预缺少幂等键");
    ErrorCode PRO_EDHR_FLOW_INTERVENTION_BACKEND_MUTATION_FORBIDDEN =
            new ErrorCode(1_040_750_444, "后台修数、SQL 或直接改状态不能作为合规流程干预");
    ErrorCode PRO_EDHR_FLOW_INTERVENTION_ACTION_INVALID =
            new ErrorCode(1_040_750_445, "当前 eDHR 流程干预动作缺少必要上下文");
    ErrorCode PRO_EDHR_UNIFIED_CHANGE_OBJECT_TYPE_INVALID =
            new ErrorCode(1_040_750_446, "eDHR 统一变更对象类型不在受控范围内");
    ErrorCode PRO_EDHR_UNIFIED_CHANGE_REASON_REQUIRED =
            new ErrorCode(1_040_750_447, "eDHR 统一变更必须填写原因");
    ErrorCode PRO_EDHR_UNIFIED_CHANGE_DIFF_REQUIRED =
            new ErrorCode(1_040_750_448, "eDHR 统一变更必须提供差异快照");
    ErrorCode PRO_EDHR_UNIFIED_CHANGE_IMPACT_REQUIRED =
            new ErrorCode(1_040_750_449, "eDHR 统一变更必须提供显式影响范围");
    ErrorCode PRO_EDHR_UNIFIED_CHANGE_SIGNOFF_REQUIRED =
            new ErrorCode(1_040_750_450, "eDHR 统一变更缺少签核证据");
    ErrorCode PRO_EDHR_UNIFIED_CHANGE_IDEMPOTENCY_KEY_REQUIRED =
            new ErrorCode(1_040_750_451, "eDHR 统一变更缺少幂等键");
    ErrorCode PRO_EDHR_UNIFIED_CHANGE_OVERWRITE_FORBIDDEN =
            new ErrorCode(1_040_750_452, "eDHR 统一变更禁止覆盖当前历史版本");
    ErrorCode PRO_EDHR_UNIFIED_CHANGE_STATUS_INVALID =
            new ErrorCode(1_040_750_453, "当前 eDHR 统一变更状态不允许该操作");
    ErrorCode PRO_EDHR_LOCAL_STATE_SAMPLE_STATE_INVALID =
            new ErrorCode(1_040_750_454, "本地 eDHR 状态样本枚举无效");
    ErrorCode PRO_EDHR_LOCAL_STATE_SAMPLE_PROFILE_FORBIDDEN =
            new ErrorCode(1_040_750_455, "本地 eDHR 状态样本只能在 local profile 创建");
    ErrorCode PRO_EDHR_LOCAL_STATE_SAMPLE_CONTEXT_FORBIDDEN =
            new ErrorCode(1_040_750_456, "本地 eDHR 状态样本只能由芋道源码/admin 当前租户创建");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_NOT_VISIBLE =
            new ErrorCode(1_040_750_457, "当前用户不可查看该 eDHR 批次执行");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_GOLDEN_FINGER_REQUIRED =
            new ErrorCode(1_040_750_458, "仅金手指角色允许执行该 eDHR 批次批量直通作废");
    ErrorCode PRO_EDHR_BATCH_EXECUTION_BULK_VOID_EMPTY =
            new ErrorCode(1_040_750_459, "当前筛选条件下没有可作废的 eDHR 批次执行");
    ErrorCode PRO_EDHR_RECORDBOOK_GLOBAL_CONFIG_MISSING =
            new ErrorCode(1_040_750_460, "eDHR 记录本全局开关配置缺失：{}");
    ErrorCode PRO_EDHR_RECORDBOOK_GLOBAL_CONFIG_INVALID =
            new ErrorCode(1_040_750_461, "eDHR 记录本全局开关配置值非法：{}={}");
    ErrorCode PRO_EDHR_RECORDBOOK_GLOBAL_DISABLED =
            new ErrorCode(1_040_750_462, "记录本全局开关已关闭，只允许使用批记录流程");
}
