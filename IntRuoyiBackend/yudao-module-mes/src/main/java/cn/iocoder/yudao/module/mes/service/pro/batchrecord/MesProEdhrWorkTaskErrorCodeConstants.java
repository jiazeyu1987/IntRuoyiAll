package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProEdhrWorkTaskErrorCodeConstants {

    ErrorCode PRO_EDHR_WORK_TASK_NOT_EXISTS =
            new ErrorCode(1_040_751_400, "eDHR 工作任务不存在");
    ErrorCode PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH =
            new ErrorCode(1_040_751_401, "当前用户不是该 eDHR 工作任务责任人");
    ErrorCode PRO_EDHR_WORK_TASK_STATUS_INVALID =
            new ErrorCode(1_040_751_402, "当前 eDHR 工作任务状态不允许该操作");
    ErrorCode PRO_EDHR_WORK_TASK_ASSIGNMENT_RULE_MISSING =
            new ErrorCode(1_040_751_403, "eDHR 工序缺少任务分配规则");
    ErrorCode PRO_EDHR_WORK_TASK_REVIEW_USER_MISSING =
            new ErrorCode(1_040_751_404, "eDHR 填写任务缺少审核人配置");
    ErrorCode PRO_EDHR_WORK_TASK_REVIEW_CONTEXT_INVALID =
            new ErrorCode(1_040_751_405, "eDHR 审核任务签字格上下文无效：{}");
    ErrorCode PRO_EDHR_WORK_TASK_DUE_RULE_MISSING =
            new ErrorCode(1_040_751_406, "eDHR 工作任务缺少处理时限规则");
    ErrorCode PRO_EDHR_WORK_TASK_OVERDUE_JOB_PARAM_INVALID =
            new ErrorCode(1_040_751_407, "eDHR 逾期处理任务参数无效：{}");
    ErrorCode PRO_EDHR_WORK_TASK_ASSIGNEE_INVALID =
            new ErrorCode(1_040_751_408, "eDHR 工作任务责任人不存在或已禁用");
    ErrorCode PRO_EDHR_WORK_TASK_CANDIDATE_POOL_EMPTY =
            new ErrorCode(1_040_751_409, "eDHR 工作任务候选池为空");
    ErrorCode PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID =
            new ErrorCode(1_040_751_410, "eDHR 工作任务候选来源无效");
    ErrorCode PRO_EDHR_WORK_TASK_ADVANCE_PREREQUISITE_MISSING =
            new ErrorCode(1_040_751_411, "eDHR 推进前置条件缺失：{}");
    ErrorCode PRO_EDHR_WORK_TASK_OWNERSHIP_SOURCE_MISSING =
            new ErrorCode(1_040_751_412, "eDHR 工作任务缺少责任来源标记：{}");
    ErrorCode PRO_EDHR_WORK_TASK_OWNERSHIP_TRANSFER_LOCKED =
            new ErrorCode(1_040_751_413, "eDHR 工作任务所有权已锁定，不能自动转移：{}");
    ErrorCode PRO_EDHR_WORK_TASK_RESPONSIBILITY_SCOPE_INVALID =
            new ErrorCode(1_040_751_414, "eDHR 工作任务责任范围快照无效：{}");
}
