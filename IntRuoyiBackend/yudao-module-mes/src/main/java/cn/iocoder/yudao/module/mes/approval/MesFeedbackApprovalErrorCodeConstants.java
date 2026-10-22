package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/** Errors for the boundary between formal eDHR reviews and legacy inventory approvals. */
public interface MesFeedbackApprovalErrorCodeConstants {
    ErrorCode FORMAL_FEEDBACK_LEGACY_REVIEW_FORBIDDEN = new ErrorCode(1_040_506_050,
            "报工{}已关联正式eDHR生产事件，请在生产组长报工复核入口处理，不能执行旧报工库存审批或驳回");
    ErrorCode FORMAL_FEEDBACK_EVIDENCE_INVALID = new ErrorCode(1_040_506_051,
            "报工{}的正式生产事件或复核证据不完整或不唯一：{}");
}
