package cn.iocoder.yudao.module.srm.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * SRM 错误码枚举类。
 *
 * <p>SRM 模块使用 1-090-000-000 段。</p>
 */
public interface ErrorCodeConstants {

    // ========== SRM 基础配置-编码规则（1-090-100-000） ==========
    ErrorCode CODE_RULE_NOT_EXISTS = new ErrorCode(1_090_100_000, "编码规则不存在");
    ErrorCode CODE_RULE_RULE_CODE_DUPLICATE = new ErrorCode(1_090_100_001, "规则编码已存在");
    ErrorCode CODE_RULE_TARGET_FORM_DUPLICATE = new ErrorCode(1_090_100_002, "目标表单已存在编码规则");
    ErrorCode CODE_RULE_TARGET_FORM_NOT_EXISTS = new ErrorCode(1_090_100_003, "目标表单 {} 的编码规则不存在");
    ErrorCode CODE_RULE_DISABLED = new ErrorCode(1_090_100_004, "目标表单 {} 的编码规则已禁用");
    ErrorCode CODE_RULE_SERIAL_CONFIG_INVALID = new ErrorCode(1_090_100_005, "编码规则流水配置无效：{}");
    ErrorCode CODE_RULE_DATE_PATTERN_INVALID = new ErrorCode(1_090_100_006, "编码规则日期格式无效：{}");
    ErrorCode CODE_RULE_SERIAL_EXCEED_MAX = new ErrorCode(1_090_100_007, "编码规则最大流水已用尽");
    ErrorCode CODE_RULE_TARGET_FORM_INVALID = new ErrorCode(1_090_100_008, "目标表单不在 SRM D7-1 编码范围内：{}");

    // ========== SRM 供应商准入与风险（1-090-200-000） ==========
    ErrorCode SUPPLIER_ACCESS_NOT_EXISTS = new ErrorCode(1_090_200_000, "供应商准入档案不存在");
    ErrorCode SUPPLIER_ACCESS_DUPLICATE = new ErrorCode(1_090_200_001, "当前租户已存在该供应商的准入档案");
    ErrorCode SUPPLIER_REFERENCE_NOT_EXISTS = new ErrorCode(1_090_200_002, "ERP 供应商不存在：{}");
    ErrorCode SUPPLIER_REFERENCE_DISABLED = new ErrorCode(1_090_200_003, "ERP 供应商已停用：{}");
    ErrorCode SUPPLIER_ACCESS_STATUS_INVALID = new ErrorCode(1_090_200_004, "供应商准入状态非法：{}");
    ErrorCode SUPPLIER_RISK_LEVEL_INVALID = new ErrorCode(1_090_200_005, "供应商风险等级非法：{}");
    ErrorCode SUPPLIER_RISK_STATUS_INVALID = new ErrorCode(1_090_200_006, "供应商风险状态非法：{}");
    ErrorCode SUPPLIER_RISK_SOURCE_TYPE_INVALID = new ErrorCode(1_090_200_007, "供应商风险来源类型非法：{}");
    ErrorCode SUPPLIER_RISK_NOT_EXISTS = new ErrorCode(1_090_200_008, "供应商风险记录不存在");
    ErrorCode SUPPLIER_RISK_ALREADY_RESOLVED = new ErrorCode(1_090_200_009, "供应商风险记录已处理");
    ErrorCode SUPPLIER_ELIGIBILITY_BLOCKED = new ErrorCode(1_090_200_010, "供应商资格校验未通过：{}");
    ErrorCode SUPPLIER_LOGIN_CONTEXT_MISSING = new ErrorCode(1_090_200_011, "当前登录信息缺失，无法记录供应商准入或风险操作");
    ErrorCode SUPPLIER_ACCESS_SUPPLIER_MISMATCH = new ErrorCode(1_090_200_012, "风险记录绑定的准入档案与供应商不匹配");
    ErrorCode SUPPLIER_REFERENCE_CROSS_TENANT = new ErrorCode(1_090_200_013, "ERP 供应商不属于当前租户：{}");
    ErrorCode SUPPLIER_ACCESS_SELF_AUDIT_FORBIDDEN = new ErrorCode(1_090_200_014, "供应商准入提交人不能自审");
    ErrorCode SUPPLIER_ACCESS_APPROVE_HIGH_RISK_BLOCKED = new ErrorCode(1_090_200_015, "供应商存在未处理高风险，不能通过准入审核");
    ErrorCode SUPPLIER_ACCESS_REJECT_REMARK_REQUIRED = new ErrorCode(1_090_200_016, "驳回准入时必须填写审核意见");
    ErrorCode SUPPLIER_ACCESS_DISABLE_REASON_REQUIRED = new ErrorCode(1_090_200_017, "停用供应商准入时必须填写停用原因");
    ErrorCode SUPPLIER_RISK_RESOLUTION_REMARK_REQUIRED = new ErrorCode(1_090_200_018, "处理供应商风险时必须填写处理说明");
    ErrorCode SUPPLIER_ACCESS_SAMPLE_STAGE_BLOCKED = new ErrorCode(1_090_200_019, "样品测试未通过，不能继续后续流程");
    ErrorCode SUPPLIER_ACCESS_TRIAL_STAGE_BLOCKED = new ErrorCode(1_090_200_020, "小批试用未通过，不能继续后续流程");
    ErrorCode SUPPLIER_ACCESS_SAMPLE_REJECT_REMARK_REQUIRED = new ErrorCode(1_090_200_021, "驳回样品测试时必须填写审核意见");
    ErrorCode SUPPLIER_ACCESS_TRIAL_REJECT_REMARK_REQUIRED = new ErrorCode(1_090_200_022, "驳回小批试用时必须填写审核意见");
    ErrorCode SUPPLIER_PORTAL_APPLICATION_NOT_EXISTS = new ErrorCode(1_090_200_023, "供应商门户申请不存在");
    ErrorCode SUPPLIER_PORTAL_APPLICATION_STATUS_INVALID = new ErrorCode(1_090_200_024, "供应商门户申请当前状态不允许执行该操作：{}");
    ErrorCode SUPPLIER_PORTAL_APPLICATION_SUBMIT_REQUIRED_FIELDS = new ErrorCode(1_090_200_025, "供应商门户申请提交前必须完整填写企业、联系人、资质和付款资料");
    ErrorCode SUPPLIER_PORTAL_APPLICATION_REJECT_REMARK_REQUIRED = new ErrorCode(1_090_200_026, "驳回供应商门户申请时必须填写审核意见");
    ErrorCode SUPPLIER_PORTAL_APPLICATION_NOT_APPROVED = new ErrorCode(1_090_200_027, "供应商尚未完成门户资料审核通过，不能进入准入审批");

    // ========== SRM 采购计划与框架协议（1-090-300-000） ==========
    ErrorCode PROCUREMENT_PLAN_NOT_EXISTS = new ErrorCode(1_090_300_000, "采购计划不存在");
    ErrorCode PROCUREMENT_PLAN_LINE_REQUIRED = new ErrorCode(1_090_300_001, "采购计划至少需要一条行项目");
    ErrorCode PROCUREMENT_METHOD_INVALID = new ErrorCode(1_090_300_002, "采购方式非法：{}");
    ErrorCode PROCUREMENT_PLAN_STATUS_INVALID = new ErrorCode(1_090_300_003, "采购计划当前状态不允许该操作：{}");
    ErrorCode PROCUREMENT_PLAN_AUDIT_REMARK_REQUIRED = new ErrorCode(1_090_300_004, "驳回采购计划时必须填写审核意见");
    ErrorCode PROCUREMENT_PLAN_GENERATE_NOT_APPROVED = new ErrorCode(1_090_300_005, "采购计划必须审核通过后才能生成寻源项目");
    ErrorCode PROCUREMENT_PLAN_GENERATE_DUPLICATE = new ErrorCode(1_090_300_006, "采购计划已生成寻源项目，不能重复生成");
    ErrorCode SOURCING_PROJECT_NOT_EXISTS = new ErrorCode(1_090_300_007, "寻源项目不存在");
    ErrorCode PROCUREMENT_PLAN_AMOUNT_INVALID = new ErrorCode(1_090_300_008, "采购计划预计金额必须大于 0");
    ErrorCode PROCUREMENT_PLAN_LINE_QUANTITY_INVALID = new ErrorCode(1_090_300_009, "采购计划行数量必须大于 0");
    ErrorCode PURCHASE_ORDER_NOT_EXISTS = new ErrorCode(1_090_300_010, "采购订单协同单不存在");
    ErrorCode PURCHASE_ORDER_SOURCE_PLAN_NOT_APPROVED = new ErrorCode(1_090_300_011, "采购计划必须审核通过后才能生成采购订单协同单");
    ErrorCode PURCHASE_ORDER_SUPPLIER_REQUIRED = new ErrorCode(1_090_300_012, "采购订单协同单必须选择供应商");
    ErrorCode PURCHASE_ORDER_DUPLICATE = new ErrorCode(1_090_300_013, "当前采购计划与供应商的采购订单协同单已存在");
    ErrorCode PURCHASE_ORDER_STATUS_INVALID = new ErrorCode(1_090_300_014, "采购订单协同单当前状态不允许该操作：{}");
    ErrorCode PURCHASE_ORDER_CONFIRM_LINE_REQUIRED = new ErrorCode(1_090_300_015, "供应商确认采购订单时至少需要一条确认行");
    ErrorCode PURCHASE_ORDER_CONFIRM_LINE_INVALID = new ErrorCode(1_090_300_016, "采购订单确认行无效");
    ErrorCode PURCHASE_ORDER_CONFIRM_FORBIDDEN = new ErrorCode(1_090_300_017, "当前登录供应商不能确认该采购订单协同单");
    ErrorCode PURCHASE_ORDER_SUPPLIER_CONTEXT_MISSING = new ErrorCode(1_090_300_018, "当前登录供应商上下文缺失，不能查看或确认采购订单协同单");
    ErrorCode PURCHASE_ORDER_CHANGE_REMARK_REQUIRED = new ErrorCode(1_090_300_019, "提交采购订单变更时必须填写变更原因");
    ErrorCode PURCHASE_ORDER_CHANGE_NOT_EXISTS = new ErrorCode(1_090_300_031, "采购订单变更单不存在");
    ErrorCode PURCHASE_ORDER_CHANGE_LINE_REQUIRED = new ErrorCode(1_090_300_032, "采购订单变更时至少需要一条变更行");
    ErrorCode PURCHASE_ORDER_CHANGE_LINE_INVALID = new ErrorCode(1_090_300_033, "采购订单变更行无效");
    ErrorCode PURCHASE_ORDER_CHANGE_REJECT_REMARK_REQUIRED = new ErrorCode(1_090_300_034, "拒绝采购订单变更时必须填写拒绝原因");
    ErrorCode PURCHASE_ORDER_CHANGE_WITHDRAW_REMARK_REQUIRED = new ErrorCode(1_090_300_035, "撤回采购订单变更时必须填写撤回原因");
    ErrorCode FRAMEWORK_PLAN_NOT_EXISTS = new ErrorCode(1_090_300_020, "框架计划不存在");
    ErrorCode FRAMEWORK_PLAN_LINE_REQUIRED = new ErrorCode(1_090_300_021, "框架计划至少需要一条物料行");
    ErrorCode FRAMEWORK_PLAN_DATE_INVALID = new ErrorCode(1_090_300_022, "框架计划有效期无效");
    ErrorCode FRAMEWORK_PLAN_STATUS_INVALID = new ErrorCode(1_090_300_023, "框架计划当前状态不允许该操作：{}");
    ErrorCode FRAMEWORK_PLAN_AUDIT_REMARK_REQUIRED = new ErrorCode(1_090_300_024, "驳回框架计划时必须填写审核意见");
    ErrorCode FRAMEWORK_AGREEMENT_NOT_APPROVED = new ErrorCode(1_090_300_025, "框架计划必须审核通过后才能生成框架协议");
    ErrorCode FRAMEWORK_AGREEMENT_DUPLICATE = new ErrorCode(1_090_300_026, "框架计划已生成框架协议，不能重复生成");
    ErrorCode FRAMEWORK_AGREEMENT_NOT_EXISTS = new ErrorCode(1_090_300_027, "框架协议不存在");
    ErrorCode FRAMEWORK_PLAN_BUDGET_INVALID = new ErrorCode(1_090_300_028, "框架计划预算金额必须大于 0");
    ErrorCode FRAMEWORK_PLAN_LINE_QUANTITY_INVALID = new ErrorCode(1_090_300_029, "框架计划行数量必须大于 0");
    ErrorCode FRAMEWORK_PLAN_LINE_BUDGET_INVALID = new ErrorCode(1_090_300_030, "框架计划行预算金额必须大于 0");

    // ========== SRM 非招标采购（1-090-400-000） ==========
    ErrorCode NON_BIDDING_PROJECT_NOT_EXISTS = new ErrorCode(1_090_400_000, "非招标项目不存在");
    ErrorCode NON_BIDDING_PROJECT_TYPE_INVALID = new ErrorCode(1_090_400_001, "项目不是非招标项目，不能执行非招标采购");
    ErrorCode NON_BIDDING_PROJECT_STATUS_INVALID = new ErrorCode(1_090_400_002, "非招标项目当前状态不允许该操作：{}");
    ErrorCode NON_BIDDING_PUBLISH_WINDOW_INVALID = new ErrorCode(1_090_400_003, "非招标项目报价时间窗无效");
    ErrorCode NON_BIDDING_PUBLISH_ATTACHMENT_REQUIRED = new ErrorCode(1_090_400_004, "非招标项目发布必须上传附件");
    ErrorCode NON_BIDDING_PUBLISH_SUPPLIER_REQUIRED = new ErrorCode(1_090_400_005, "非招标项目发布必须选择供应商范围");
    ErrorCode NON_BIDDING_QUOTE_MODE_INVALID = new ErrorCode(1_090_400_016, "非招标项目询价模式非法");
    ErrorCode NON_BIDDING_QUOTE_WINDOW_CLOSED = new ErrorCode(1_090_400_006, "当前不在非招标项目报价时间窗内");
    ErrorCode NON_BIDDING_QUOTE_SUPPLIER_NOT_INVITED = new ErrorCode(1_090_400_007, "供应商未受邀参与该非招标项目");
    ErrorCode NON_BIDDING_QUOTE_DUPLICATE = new ErrorCode(1_090_400_008, "供应商已报价，不能重复报价");
    ErrorCode NON_BIDDING_QUOTE_AMOUNT_INVALID = new ErrorCode(1_090_400_009, "报价金额必须大于 0");
    ErrorCode NON_BIDDING_QUOTE_ATTACHMENT_REQUIRED = new ErrorCode(1_090_400_010, "供应商报价必须上传附件");
    ErrorCode NON_BIDDING_QUOTE_LINE_REQUIRED = new ErrorCode(1_090_400_011, "供应商报价至少需要一条报价行");
    ErrorCode NON_BIDDING_QUOTE_LINE_INVALID = new ErrorCode(1_090_400_012, "报价行项目无效");
    ErrorCode NON_BIDDING_DEAL_QUOTE_NOT_EXISTS = new ErrorCode(1_090_400_013, "成交报价不存在");
    ErrorCode NON_BIDDING_DEAL_REMARK_REQUIRED = new ErrorCode(1_090_400_014, "确认成交必须填写成交说明");
    ErrorCode NON_BIDDING_QUOTE_AMOUNT_MISMATCH = new ErrorCode(1_090_400_015, "报价金额必须等于报价行金额合计");

    // ========== SRM 招标采购（1-090-500-000） ==========
    ErrorCode TENDER_PROJECT_NOT_EXISTS = new ErrorCode(1_090_500_000, "招标项目不存在");
    ErrorCode TENDER_PROJECT_TYPE_INVALID = new ErrorCode(1_090_500_001, "项目不是招标项目，不能执行招标采购");
    ErrorCode TENDER_PROJECT_STATUS_INVALID = new ErrorCode(1_090_500_002, "招标项目当前状态不允许该操作：{}");
    ErrorCode TENDER_PUBLISH_ATTACHMENT_REQUIRED = new ErrorCode(1_090_500_003, "招标项目发布必须填写公告和标书附件");
    ErrorCode TENDER_SUBMISSION_WINDOW_INVALID = new ErrorCode(1_090_500_004, "招标项目投标时间窗无效");
    ErrorCode TENDER_SUBMISSION_WINDOW_CLOSED = new ErrorCode(1_090_500_005, "当前不在招标项目投标时间窗内");
    ErrorCode TENDER_SUBMISSION_SUPPLIER_DUPLICATE = new ErrorCode(1_090_500_006, "供应商已投标，不能重复投标");
    ErrorCode TENDER_SUBMISSION_AMOUNT_INVALID = new ErrorCode(1_090_500_007, "投标金额必须大于 0");
    ErrorCode TENDER_EXPERT_NOT_EXISTS = new ErrorCode(1_090_500_008, "专家不存在");
    ErrorCode TENDER_EXPERT_STATUS_INVALID = new ErrorCode(1_090_500_009, "专家状态非法：{}");
    ErrorCode TENDER_EXPERT_SPECIALTY_MISMATCH = new ErrorCode(1_090_500_010, "专家专业类型不匹配");
    ErrorCode TENDER_COMMITTEE_MEMBER_DUPLICATE = new ErrorCode(1_090_500_011, "评委会专家不能重复");
    ErrorCode TENDER_COMMITTEE_MEMBER_INSUFFICIENT = new ErrorCode(1_090_500_012, "评委会专家人数不足");
    ErrorCode TENDER_CANDIDATE_SUBMISSION_NOT_EXISTS = new ErrorCode(1_090_500_013, "候选投标不存在");
    ErrorCode TENDER_WINNING_CANDIDATE_NOT_EXISTS = new ErrorCode(1_090_500_014, "中标候选不存在");
    ErrorCode TENDER_WINNING_REMARK_REQUIRED = new ErrorCode(1_090_500_015, "确认中标必须填写说明");
    ErrorCode TENDER_CANDIDATE_SUBMISSION_REQUIRED = new ErrorCode(1_090_500_016, "生成中标候选必须选择投标记录");

    // ========== SRM 采购合同（1-090-600-000） ==========
    ErrorCode PROCUREMENT_CONTRACT_NOT_EXISTS = new ErrorCode(1_090_600_000, "采购合同不存在");
    ErrorCode PROCUREMENT_CONTRACT_SOURCE_NOT_EXISTS = new ErrorCode(1_090_600_001, "合同来源项目不存在");
    ErrorCode PROCUREMENT_CONTRACT_SOURCE_TYPE_INVALID = new ErrorCode(1_090_600_002, "合同来源类型非法：{}");
    ErrorCode PROCUREMENT_CONTRACT_SOURCE_STATUS_INVALID = new ErrorCode(1_090_600_003, "合同来源当前状态不允许创建合同：{}");
    ErrorCode PROCUREMENT_CONTRACT_SOURCE_ALREADY_CONTRACTED = new ErrorCode(1_090_600_004, "合同来源已创建合同，不能重复创建");
    ErrorCode PROCUREMENT_CONTRACT_SOURCE_DEAL_REQUIRED = new ErrorCode(1_090_600_005, "合同来源缺少成交或中标供应商信息");
    ErrorCode PROCUREMENT_CONTRACT_PAYMENT_REQUIRED = new ErrorCode(1_090_600_006, "采购合同至少需要一条付款约定");
    ErrorCode PROCUREMENT_CONTRACT_SIGNING_REQUIRED = new ErrorCode(1_090_600_007, "采购合同至少需要一条签署信息");
    ErrorCode PROCUREMENT_CONTRACT_ATTACHMENT_REQUIRED = new ErrorCode(1_090_600_008, "采购合同至少需要一个附件");
    ErrorCode PROCUREMENT_CONTRACT_AMOUNT_INVALID = new ErrorCode(1_090_600_009, "采购合同金额必须大于 0");
    ErrorCode PROCUREMENT_CONTRACT_DATE_INVALID = new ErrorCode(1_090_600_010, "采购合同有效期无效");
    ErrorCode PROCUREMENT_CONTRACT_PAYMENT_INVALID = new ErrorCode(1_090_600_011, "采购合同付款约定无效");
    ErrorCode PROCUREMENT_CONTRACT_SIGNING_INVALID = new ErrorCode(1_090_600_012, "采购合同签署信息无效");
    ErrorCode PROCUREMENT_CONTRACT_ATTACHMENT_INVALID = new ErrorCode(1_090_600_013, "采购合同附件信息无效");
    ErrorCode PROCUREMENT_CONTRACT_STATUS_INVALID = new ErrorCode(1_090_600_014, "采购合同当前状态不允许该操作：{}");
    ErrorCode PROCUREMENT_CONTRACT_CANCEL_REASON_REQUIRED = new ErrorCode(1_090_600_015, "作废采购合同必须填写作废原因");
    ErrorCode PROCUREMENT_CONTRACT_HEADER_INVALID = new ErrorCode(1_090_600_016, "采购合同基础信息无效");

    // ========== SRM 委外执行与对账（1-090-700-000） ==========
    ErrorCode OUTSOURCE_EXECUTION_NOT_EXISTS = new ErrorCode(1_090_700_000, "委外执行单不存在");
    ErrorCode OUTSOURCE_EXECUTION_DUPLICATE = new ErrorCode(1_090_700_001, "当前采购订单已创建委外执行单");
    ErrorCode OUTSOURCE_EXECUTION_SOURCE_ORDER_NOT_CONFIRMED = new ErrorCode(1_090_700_002, "采购订单协同单必须已确认后才能创建委外执行单");
    ErrorCode OUTSOURCE_EXECUTION_STATUS_INVALID = new ErrorCode(1_090_700_003, "委外执行单当前状态不允许该操作：{}");
    ErrorCode OUTSOURCE_EXECUTION_SUPPLIER_FORBIDDEN = new ErrorCode(1_090_700_004, "当前登录供应商不能操作该委外执行单");
    ErrorCode OUTSOURCE_EXECUTION_SUPPLIER_CONTEXT_MISSING = new ErrorCode(1_090_700_005, "当前登录供应商上下文缺失，不能查看或操作委外执行单");
    ErrorCode OUTSOURCE_EXECUTION_QUANTITY_INVALID = new ErrorCode(1_090_700_006, "委外执行数量无效");
    ErrorCode OUTSOURCE_EXECUTION_PROGRESS_INVALID = new ErrorCode(1_090_700_007, "委外执行进度无效");
    ErrorCode OUTSOURCE_EXECUTION_INSPECT_INVALID = new ErrorCode(1_090_700_008, "检验数量无效");
    ErrorCode OUTSOURCE_EXECUTION_RECONCILIATION_PREREQUISITE_MISSING = new ErrorCode(1_090_700_009, "委外执行单缺少对账前置数据：{}");
    ErrorCode RECONCILIATION_NOT_EXISTS = new ErrorCode(1_090_700_010, "委外对账单不存在");
    ErrorCode RECONCILIATION_STATUS_INVALID = new ErrorCode(1_090_700_011, "委外对账单当前状态不允许该操作：{}");

    // ========== SRM 付款执行（1-090-800-000） ==========
    ErrorCode PAYMENT_EXECUTION_NOT_EXISTS = new ErrorCode(1_090_800_000, "付款执行单不存在");
    ErrorCode PAYMENT_EXECUTION_DUPLICATE = new ErrorCode(1_090_800_001, "当前对账单已创建付款执行单");
    ErrorCode PAYMENT_EXECUTION_STATUS_INVALID = new ErrorCode(1_090_800_002, "付款执行单当前状态不允许该操作：{}");
    ErrorCode PAYMENT_EXECUTION_CONTRACT_SUPPLIER_MISMATCH = new ErrorCode(1_090_800_003, "采购合同与对账单供应商不匹配");
    ErrorCode PAYMENT_EXECUTION_CONTRACT_PAYMENT_REQUIRED = new ErrorCode(1_090_800_004, "采购合同缺少付款约定，不能创建付款执行单");
    ErrorCode PAYMENT_EXECUTION_RECONCILIATION_REQUIRED = new ErrorCode(1_090_800_005, "委外对账单不存在或尚未完成对账");
    ErrorCode PAYMENT_EXECUTION_REJECT_REMARK_REQUIRED = new ErrorCode(1_090_800_006, "驳回付款申请时必须填写驳回原因");
    ErrorCode PAYMENT_EXECUTION_PUSH_REMARK_REQUIRED = new ErrorCode(1_090_800_007, "记录财务推送状态时必须填写说明");

    // ========== SRM NAS 定位（1-090-900-000） ==========
    ErrorCode NAS_LOCATOR_NO_SUCCESS_SNAPSHOT = new ErrorCode(1_090_900_000, "请先刷新 NAS 索引");
    ErrorCode NAS_LOCATOR_REFRESH_RUNNING = new ErrorCode(1_090_900_001, "当前租户已有 NAS 索引刷新任务正在执行，请稍后再试");
    ErrorCode NAS_LOCATOR_ENTRY_NOT_EXISTS = new ErrorCode(1_090_900_002, "NAS 索引文件记录不存在");
    ErrorCode NAS_LOCATOR_ENTRY_NOT_FILE = new ErrorCode(1_090_900_003, "该 NAS 索引记录不是文件，不能下载");
    ErrorCode NAS_LOCATOR_SHARE_CONFIG_INVALID = new ErrorCode(1_090_900_004, "当前 NAS 配置不是受保护共享 \\\\172.30.30.4\\质量体系文件 或 \\\\172.30.30.4\\生产部");
    ErrorCode NAS_LOCATOR_REFRESH_TASK_NOT_EXISTS = new ErrorCode(1_090_900_005, "NAS 索引刷新任务不存在");
    ErrorCode NAS_LOCATOR_BLACKLIST_CONFIG_INVALID = new ErrorCode(1_090_900_006, "NAS 定位黑名单配置不是合法 JSON 数组");

}
