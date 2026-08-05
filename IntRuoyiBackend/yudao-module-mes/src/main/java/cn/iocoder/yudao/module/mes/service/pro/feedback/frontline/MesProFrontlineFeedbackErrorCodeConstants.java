package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProFrontlineFeedbackErrorCodeConstants {

    ErrorCode PRO_FRONTLINE_FEEDBACK_SUBMIT_CONTEXT_REQUIRED =
            new ErrorCode(1_040_753_600, "一线报工组合提交上下文缺失：{}");
    ErrorCode PRO_FRONTLINE_FEEDBACK_LOGIN_USER_REQUIRED =
            new ErrorCode(1_040_753_601, "一线报工组合提交必须存在当前登录设备账号");
    ErrorCode PRO_FRONTLINE_FEEDBACK_DEVICE_ACCOUNT_MISMATCH =
            new ErrorCode(1_040_753_602, "一线报工设备账号与当前登录用户不一致：{}");
    ErrorCode PRO_FRONTLINE_FEEDBACK_SIGNATURE_EMPLOYEE_MISMATCH =
            new ErrorCode(1_040_753_603, "一线报工签名员工与实际操作员工不一致");
    ErrorCode PRO_FRONTLINE_RECORD_BOOK_EVENT_MISSING =
            new ErrorCode(1_040_753_604, "一线报工记录本原始条目事件缺失：{}");
    ErrorCode PRO_FRONTLINE_FEEDBACK_QUANTITY_INVALID =
            new ErrorCode(1_040_753_605, "一线报工数量关系非法：{}");

}
