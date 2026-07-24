package cn.iocoder.yudao.module.srm.enums.coderule;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * SRM D7-1 编码规则目标表单枚举。
 */
@Getter
@AllArgsConstructor
public enum SrmCodeRuleTargetFormEnum {

    PROCUREMENT_PLAN("PROCUREMENT_PLAN", "采购计划"),
    PROCUREMENT_PLAN_LINE("PROCUREMENT_PLAN_LINE", "采购计划行"),
    FRAMEWORK_PLAN("FRAMEWORK_PLAN", "框架采购计划"),
    FRAMEWORK_AGREEMENT("FRAMEWORK_AGREEMENT", "框架协议"),
    TENDER_PROJECT("TENDER_PROJECT", "招标项目"),
    NON_TENDER_PROJECT("NON_TENDER_PROJECT", "非招标项目"),
    PROCUREMENT_CONTRACT("PROCUREMENT_CONTRACT", "采购合同"),
    PURCHASE_ORDER("PURCHASE_ORDER", "采购订单协同单"),
    PURCHASE_ORDER_LINE("PURCHASE_ORDER_LINE", "采购订单协同行"),
    PURCHASE_ORDER_CHANGE("PURCHASE_ORDER_CHANGE", "采购订单变更单"),
    OUTSOURCE_EXECUTION("OUTSOURCE_EXECUTION", "委外执行单"),
    OUTSOURCE_EXECUTION_EVENT("OUTSOURCE_EXECUTION_EVENT", "委外执行事件"),
    OUTSOURCE_RECONCILIATION("OUTSOURCE_RECONCILIATION", "委外对账单"),
    PAYMENT_EXECUTION("PAYMENT_EXECUTION", "付款执行单"),
    PAYMENT_EXECUTION_EVENT("PAYMENT_EXECUTION_EVENT", "付款执行事件"),
    EXPERT_DRAW_APPLICATION("EXPERT_DRAW_APPLICATION", "专家抽取申请");

    private final String targetForm;
    private final String name;

    public static boolean contains(String targetForm) {
        return Arrays.stream(values()).anyMatch(item -> item.targetForm.equals(targetForm));
    }

}
