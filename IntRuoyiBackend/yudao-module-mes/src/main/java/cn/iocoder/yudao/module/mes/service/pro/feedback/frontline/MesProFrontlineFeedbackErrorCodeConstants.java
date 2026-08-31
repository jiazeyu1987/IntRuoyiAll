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
    ErrorCode PRO_FRONTLINE_FEEDBACK_LOSS_REASON_REQUIRED =
            new ErrorCode(1_040_753_606, "损耗数量大于 0 时必须选择当前工序启用的损耗原因");
    ErrorCode PRO_FRONTLINE_FEEDBACK_LOSS_REASON_INVALID =
            new ErrorCode(1_040_753_607, "损耗原因不属于当前工序或已禁用：{}");
    ErrorCode PRO_FRONTLINE_FEEDBACK_DEVICE_INVALID =
            new ErrorCode(1_040_753_608, "选用设备不属于当前工序或不可用：{}");
    ErrorCode PRO_FRONTLINE_FEEDBACK_DEVICE_PARAMETER_INVALID =
            new ErrorCode(1_040_753_609, "设备参数不属于当前工序/设备配置或不可用：{}");
    ErrorCode PRO_FRONTLINE_PROCESS_MATERIAL_REQUIRED =
            new ErrorCode(1_040_753_610, "当前工序未配置报工物料：activeOrderId={}，processId={}");
    ErrorCode PRO_FRONTLINE_PROCESS_MATERIAL_INVALID =
            new ErrorCode(1_040_753_611, "冻结工序报工物料无效：{}");
    ErrorCode PRO_FRONTLINE_FEEDBACK_MATERIAL_INVALID =
            new ErrorCode(1_040_753_612, "一线报工物料明细无效：{}");
    ErrorCode PRO_FRONTLINE_FEEDBACK_MATERIAL_PERSIST_FAILED =
            new ErrorCode(1_040_753_613, "一线报工物料明细保存失败：feedbackId={}");
    ErrorCode PRO_FRONTLINE_FEEDBACK_MATERIAL_BATCH_SOURCE_INVALID =
            new ErrorCode(1_040_753_614, "一线报工物料同步批号来源无效：{}");

}
