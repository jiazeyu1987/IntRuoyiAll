package cn.iocoder.yudao.module.mes.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * MES 错误码枚举类
 * <p>
 * mes 系统，使用 1-040-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== MES 基础数据-物料分类（1-040-100-000） ==========
    ErrorCode MD_ITEM_TYPE_NOT_EXISTS = new ErrorCode(1_040_100_000, "物料分类不存在");
    ErrorCode MD_ITEM_TYPE_EXITS_CHILDREN = new ErrorCode(1_040_100_001, "存在子分类，无法删除");
    ErrorCode MD_ITEM_TYPE_PARENT_NOT_EXITS = new ErrorCode(1_040_100_002, "父级分类不存在");
    ErrorCode MD_ITEM_TYPE_PARENT_ERROR = new ErrorCode(1_040_100_003, "不能设置自己为父分类");
    ErrorCode MD_ITEM_TYPE_NAME_DUPLICATE = new ErrorCode(1_040_100_004, "同一父分类下已存在该名称的分类");
    ErrorCode MD_ITEM_TYPE_CODE_DUPLICATE = new ErrorCode(1_040_100_005, "同一父分类下已存在该编码的分类");
    ErrorCode MD_ITEM_TYPE_PARENT_IS_CHILD = new ErrorCode(1_040_100_006, "不能设置自己的子分类为父分类");
    ErrorCode MD_ITEM_TYPE_EXITS_ITEM = new ErrorCode(1_040_100_007, "该分类下存在物料，无法删除");
    ErrorCode MD_ITEM_TYPE_NOT_LEAF = new ErrorCode(1_040_100_008, "只能将物料挂载到叶子分类（该分类下存在子分类）");

    // ========== MES 基础数据-计量单位（1-040-101-000） ==========
    ErrorCode MD_UNIT_MEASURE_NOT_EXISTS = new ErrorCode(1_040_101_000, "计量单位不存在");
    ErrorCode MD_UNIT_MEASURE_CODE_DUPLICATE = new ErrorCode(1_040_101_001, "计量单位编码已存在");
    ErrorCode MD_UNIT_MEASURE_HAS_ITEM = new ErrorCode(1_040_101_002, "该计量单位下存在物料，无法删除");
    ErrorCode MD_UNIT_MEASURE_HAS_SECONDARY = new ErrorCode(1_040_101_003, "该主单位下存在辅单位，无法删除");
    ErrorCode MD_UNIT_MEASURE_HAS_TASK_ISSUE = new ErrorCode(1_040_101_004, "该计量单位已被生产投料引用，无法删除");
    ErrorCode MD_UNIT_MEASURE_HAS_QC_TEMPLATE_INDICATOR = new ErrorCode(1_040_101_005, "该计量单位已被质检方案指标项引用，无法删除");
    ErrorCode MD_UNIT_MEASURE_HAS_QC_LINE = new ErrorCode(1_040_101_006, "该计量单位已被质检单据行引用，无法删除");

    // ========== MES 基础数据-物料（1-040-102-000） ==========
    ErrorCode MD_ITEM_NOT_EXISTS = new ErrorCode(1_040_102_000, "物料不存在");
    ErrorCode MD_ITEM_CODE_DUPLICATE = new ErrorCode(1_040_102_001, "物料编码已存在");
    ErrorCode MD_ITEM_NAME_DUPLICATE = new ErrorCode(1_040_102_002, "物料名称已存在");
    ErrorCode MD_ITEM_IMPORT_LIST_IS_EMPTY = new ErrorCode(1_040_102_003, "导入物料数据不能为空");
    ErrorCode MD_ITEM_BATCH_REQUIRED = new ErrorCode(1_040_102_004, "当前物料启用了批次管理，请选择批次");
    ErrorCode MD_ITEM_IS_DISABLE = new ErrorCode(1_040_102_005, "物料已禁用");

    // ========== MES 基础数据-物料批次属性配置（1-040-102-100） ==========
    ErrorCode MD_ITEM_BATCH_CONFIG_NOT_EXISTS = new ErrorCode(1_040_102_100, "物料批次属性配置不存在");
    ErrorCode MD_ITEM_BATCH_CONFIG_AT_LEAST_ONE_FLAG = new ErrorCode(1_040_102_101, "批次管理已启用，至少需要配置一个批次属性");
    ErrorCode MD_ITEM_PRODUCT_BOM_REQUIRED = new ErrorCode(1_040_102_102, "产品类物料启用前，必须配置至少一个 BOM 组成");

    // ========== MES 仓库管理-批次管理（1-040-717-000） ==========
    ErrorCode WM_BATCH_PRODUCE_DATE_REQUIRED = new ErrorCode(1_040_717_000, "批次配置要求生产日期不能为空");
    ErrorCode WM_BATCH_RECEIPT_DATE_REQUIRED = new ErrorCode(1_040_717_001, "批次配置要求入库日期不能为空");
    ErrorCode WM_BATCH_EXPIRE_DATE_REQUIRED = new ErrorCode(1_040_717_002, "批次配置要求有效期不能为空");
    ErrorCode WM_BATCH_VENDOR_REQUIRED = new ErrorCode(1_040_717_003, "批次配置要求供应商不能为空");
    ErrorCode WM_BATCH_CLIENT_REQUIRED = new ErrorCode(1_040_717_004, "批次配置要求客户不能为空");
    ErrorCode WM_BATCH_PURCHASE_ORDER_CODE_REQUIRED = new ErrorCode(1_040_717_005, "批次配置要求采购订单编号不能为空");
    ErrorCode WM_BATCH_CUSTOMER_ORDER_CODE_REQUIRED = new ErrorCode(1_040_717_006, "批次配置要求销售订单编号不能为空");
    ErrorCode WM_BATCH_WORK_ORDER_REQUIRED = new ErrorCode(1_040_717_007, "批次配置要求生产工单不能为空");
    ErrorCode WM_BATCH_TASK_REQUIRED = new ErrorCode(1_040_717_008, "批次配置要求生产任务不能为空");
    ErrorCode WM_BATCH_WORKSTATION_REQUIRED = new ErrorCode(1_040_717_009, "批次配置要求工作站不能为空");
    ErrorCode WM_BATCH_TOOL_REQUIRED = new ErrorCode(1_040_717_010, "批次配置要求工具不能为空");
    ErrorCode WM_BATCH_MOLD_REQUIRED = new ErrorCode(1_040_717_011, "批次配置要求模具不能为空");
    ErrorCode WM_BATCH_LOT_NUMBER_REQUIRED = new ErrorCode(1_040_717_012, "批次配置要求生产批号不能为空");
    ErrorCode WM_BATCH_QUALITY_STATUS_REQUIRED = new ErrorCode(1_040_717_013, "批次配置要求质量状态不能为空");
    ErrorCode WM_BATCH_NOT_EXISTS = new ErrorCode(1_040_717_014, "批次不存在");
    ErrorCode WM_BATCH_ITEM_MISMATCH = new ErrorCode(1_040_717_015, "批次不属于当前物料");
    ErrorCode WM_BATCH_CLIENT_MISMATCH = new ErrorCode(1_040_717_016, "批次不属于当前客户");
    ErrorCode WM_BATCH_VENDOR_MISMATCH = new ErrorCode(1_040_717_017, "批次不属于当前供应商");

    // ========== MES 基础数据-客户（1-040-103-000） ==========
    ErrorCode MD_CLIENT_NOT_EXISTS = new ErrorCode(1_040_103_000, "客户不存在");
    ErrorCode MD_CLIENT_CODE_DUPLICATE = new ErrorCode(1_040_103_001, "客户编码已存在");
    ErrorCode MD_CLIENT_NAME_DUPLICATE = new ErrorCode(1_040_103_002, "客户名称已存在");
    ErrorCode MD_CLIENT_NICKNAME_DUPLICATE = new ErrorCode(1_040_103_003, "客户简称已存在");
    ErrorCode MD_CLIENT_IMPORT_LIST_IS_EMPTY = new ErrorCode(1_040_103_004, "导入客户数据不能为空");
    ErrorCode MD_CLIENT_IS_DISABLE = new ErrorCode(1_040_103_005, "客户已禁用");

    // ========== MES 基础数据-供应商（1-040-104-000） ==========
    ErrorCode MD_VENDOR_NOT_EXISTS = new ErrorCode(1_040_104_000, "供应商不存在");
    ErrorCode MD_VENDOR_CODE_DUPLICATE = new ErrorCode(1_040_104_001, "供应商编码已存在");
    ErrorCode MD_VENDOR_NAME_DUPLICATE = new ErrorCode(1_040_104_002, "供应商名称已存在");
    ErrorCode MD_VENDOR_NICKNAME_DUPLICATE = new ErrorCode(1_040_104_003, "供应商简称已存在");
    ErrorCode MD_VENDOR_IMPORT_LIST_IS_EMPTY = new ErrorCode(1_040_104_004, "导入供应商数据不能为空");
    ErrorCode MD_VENDOR_HAS_REFERENCE = new ErrorCode(1_040_104_005, "该供应商已被其他业务引用，无法删除");
    ErrorCode MD_VENDOR_IS_DISABLE = new ErrorCode(1_040_104_006, "供应商已禁用");

    // ========== MES 基础数据-车间（1-040-105-000） ==========
    ErrorCode MD_WORKSHOP_NOT_EXISTS = new ErrorCode(1_040_105_000, "车间不存在");
    ErrorCode MD_WORKSHOP_CODE_DUPLICATE = new ErrorCode(1_040_105_001, "车间编码已存在");
    ErrorCode MD_WORKSHOP_NAME_DUPLICATE = new ErrorCode(1_040_105_002, "车间名称已存在");
    ErrorCode MD_WORKSHOP_HAS_WORKSTATION = new ErrorCode(1_040_105_003, "车间下存在工作站，无法删除");

    // ========== MES 基础数据-工作站（1-040-106-000） ==========
    ErrorCode MD_WORKSTATION_NOT_EXISTS = new ErrorCode(1_040_106_000, "工作站不存在");
    ErrorCode MD_WORKSTATION_CODE_DUPLICATE = new ErrorCode(1_040_106_001, "工作站编码已存在");
    ErrorCode MD_WORKSTATION_NAME_DUPLICATE = new ErrorCode(1_040_106_002, "工作站名称已存在");
    ErrorCode MD_WORKSTATION_IS_DISABLE = new ErrorCode(1_040_106_003, "工作站已禁用");
    ErrorCode MD_WORKSTATION_PRODUCTION_LINE_MISMATCH = new ErrorCode(1_040_106_004, "工作站所属车间与产线不一致");
    ErrorCode MD_WORKSTATION_EFFECTIVE_HOURS_REQUIRED = new ErrorCode(1_040_106_005, "工作站产能计算有效工时必须大于 0");
    ErrorCode MD_WORKSTATION_SHIFT_HOURS_REQUIRED = new ErrorCode(1_040_106_006, "工作站缺少排产员工作台班次小时配置，workstationId={}");
    // ========== MES 基础数据-设备资源（1-040-106-100） ==========
    ErrorCode MD_WORKSTATION_MACHINE_NOT_EXISTS = new ErrorCode(1_040_106_100, "设备资源记录不存在");
    ErrorCode MD_WORKSTATION_MACHINE_EXISTS = new ErrorCode(1_040_106_101, "该设备已分配至工作站：{}");
    // ========== MES 基础数据-工装夹具资源（1-040-106-200） ==========
    ErrorCode MD_WORKSTATION_TOOL_NOT_EXISTS = new ErrorCode(1_040_106_200, "工装夹具资源记录不存在");
    ErrorCode MD_WORKSTATION_TOOL_TYPE_EXISTS = new ErrorCode(1_040_106_201, "该工具类型已在此工作站中存在");
    // ========== MES 基础数据-人力资源（1-040-106-300） ==========
    ErrorCode MD_WORKSTATION_WORKER_NOT_EXISTS = new ErrorCode(1_040_106_300, "人力资源记录不存在");
    ErrorCode MD_WORKSTATION_WORKER_POST_EXISTS = new ErrorCode(1_040_106_301, "该岗位已在此工作站中存在");

    // ========== MES 基础数据-产品BOM（1-040-107-000） ==========
    ErrorCode MD_PRODUCT_BOM_NOT_EXISTS = new ErrorCode(1_040_107_000, "产品BOM不存在");
    ErrorCode MD_PRODUCT_BOM_SELF_REFERENCE = new ErrorCode(1_040_107_001, "产品不能作为自身的BOM物料");
    ErrorCode MD_PRODUCT_BOM_CIRCULAR = new ErrorCode(1_040_107_002, "BOM物料存在闭环，无法新增");
    ErrorCode MD_PRODUCT_BOM_ITEM_INVALID = new ErrorCode(1_040_107_003, "选择的 BOM 物料不属于当前产品");

    // ========== MES 基础数据-产品SOP（1-040-108-000） ==========
    ErrorCode MD_PRODUCT_SOP_NOT_EXISTS = new ErrorCode(1_040_108_000, "产品SOP不存在");
    ErrorCode MD_PRODUCT_SOP_SORT_DUPLICATE = new ErrorCode(1_040_108_001, "该展示序号已存在");

    // ========== MES 基础数据-产品SIP（1-040-109-000） ==========
    ErrorCode MD_PRODUCT_SIP_NOT_EXISTS = new ErrorCode(1_040_109_000, "产品SIP不存在");
    ErrorCode MD_PRODUCT_SIP_SORT_DUPLICATE = new ErrorCode(1_040_109_001, "该展示序号已存在");

    // ========== MES 基础数据-编码规则（1-040-110-000） ==========
    ErrorCode AUTO_CODE_RULE_NOT_EXISTS = new ErrorCode(1_040_110_000, "编码规则不存在");
    ErrorCode AUTO_CODE_RULE_CODE_DUPLICATE = new ErrorCode(1_040_110_001, "规则编码已存在");
    ErrorCode AUTO_CODE_PART_NOT_EXISTS = new ErrorCode(1_040_110_002, "规则组成不存在");
    ErrorCode AUTO_CODE_REDIS_ERROR = new ErrorCode(1_040_110_003, "编码生成服务不可用，请稍后重试");
    ErrorCode AUTO_CODE_GENERATE_FAILED = new ErrorCode(1_040_110_004, "编码生成失败");
    ErrorCode AUTO_CODE_PART_SERIAL_NUMBER_DUPLICATE = new ErrorCode(1_040_110_005, "流水号分段只能存在一个");

    // ========== MES 基础数据-产线（1-040-111-000） ==========
    ErrorCode MD_PRODUCTION_LINE_NOT_EXISTS = new ErrorCode(1_040_111_000, "产线不存在");
    ErrorCode MD_PRODUCTION_LINE_CODE_DUPLICATE = new ErrorCode(1_040_111_001, "产线编码已存在");
    ErrorCode MD_PRODUCTION_LINE_NAME_DUPLICATE = new ErrorCode(1_040_111_002, "产线名称已存在");
    ErrorCode MD_PRODUCTION_LINE_IS_DISABLE = new ErrorCode(1_040_111_003, "产线已禁用");

    // ========== MES 日历排班-计划班次（1-040-200-000） ==========
    ErrorCode CAL_PLAN_SHIFT_NOT_EXISTS = new ErrorCode(1_040_200_000, "计划班次不存在");
    ErrorCode CAL_PLAN_SHIFT_COUNT_EXCEED = new ErrorCode(1_040_200_001, "班次数量已达到轮班方式的上限");

    // ========== MES 日历排班-班组（1-040-201-000） ==========
    ErrorCode CAL_TEAM_NOT_EXISTS = new ErrorCode(1_040_201_000, "班组不存在");
    ErrorCode CAL_TEAM_CODE_DUPLICATE = new ErrorCode(1_040_201_001, "班组编码已存在");
    // ========== MES 日历排班-班组成员（1-040-201-100） ==========
    ErrorCode CAL_TEAM_MEMBER_NOT_EXISTS = new ErrorCode(1_040_201_100, "班组成员不存在");
    ErrorCode CAL_TEAM_MEMBER_USER_DUPLICATE = new ErrorCode(1_040_201_101, "该用户已分配到其他班组");
    ErrorCode CAL_TEAM_MEMBER_USER_NOT_EXISTS = new ErrorCode(1_040_201_102, "用户不存在");
    // ========== MES 日历排班-班组排班（1-040-201-200） ==========
    ErrorCode CAL_TEAM_SHIFT_NOT_EXISTS = new ErrorCode(1_040_201_200, "班组排班记录不存在");
    ErrorCode CAL_TEAM_SHIFT_GENERATE_TEAM_NOT_ENOUGH = new ErrorCode(1_040_201_201, "班组数量不满足轮班方式要求");
    ErrorCode CAL_TEAM_SHIFT_GENERATE_SHIFT_NOT_ENOUGH = new ErrorCode(1_040_201_202, "班次数量不满足轮班方式要求");

    // ========== MES 日历排班-排班计划（1-040-202-000） ==========
    ErrorCode CAL_PLAN_NOT_EXISTS = new ErrorCode(1_040_202_000, "排班计划不存在");
    ErrorCode CAL_PLAN_CODE_DUPLICATE = new ErrorCode(1_040_202_001, "排班计划编码已存在");
    ErrorCode CAL_PLAN_NOT_PREPARE = new ErrorCode(1_040_202_002, "排班计划已确认，不允许修改或删除");
    ErrorCode CAL_PLAN_TEAM_COUNT_NOT_MATCH = new ErrorCode(1_040_202_003, "确认排班计划时，分配的班组数量与轮班方式不匹配");
    // ========== MES 日历排班-计划班组关联（1-040-202-100） ==========
    ErrorCode CAL_PLAN_TEAM_NOT_EXISTS = new ErrorCode(1_040_202_100, "计划班组关联不存在");
    ErrorCode CAL_PLAN_TEAM_DUPLICATE = new ErrorCode(1_040_202_101, "该班组已分配到此计划");

    // ========== MES 日历排班-假期设置（1-040-203-000） ==========
    ErrorCode CAL_HOLIDAY_NOT_EXISTS = new ErrorCode(1_040_203_000, "假期设置不存在");

    // ========== MES 自动排产（1-040-250-000） ==========
    ErrorCode PRO_AUTO_SCHEDULE_SCOPE_EMPTY = new ErrorCode(1_040_250_000, "排产范围不能为空");
    ErrorCode PRO_AUTO_SCHEDULE_ROUTE_REQUIRED = new ErrorCode(1_040_250_001, "工单缺少工艺路线配置");
    ErrorCode PRO_AUTO_SCHEDULE_ROUTE_PROCESS_REQUIRED = new ErrorCode(1_040_250_002, "工艺路线缺少工序配置");
    ErrorCode PRO_AUTO_SCHEDULE_WORKSTATION_REQUIRED = new ErrorCode(1_040_250_003, "工序缺少可用工作站");
    ErrorCode PRO_AUTO_SCHEDULE_PRODUCTION_LINE_REQUIRED = new ErrorCode(1_040_250_004, "工作站未绑定产线");
    ErrorCode PRO_AUTO_SCHEDULE_CALENDAR_REQUIRED = new ErrorCode(1_040_250_005, "产线缺少排班计划");
    ErrorCode PRO_AUTO_SCHEDULE_CAPACITY_REQUIRED = new ErrorCode(1_040_250_006, "产线班次产能缺失");
    ErrorCode PRO_AUTO_SCHEDULE_ACTUAL_CAPACITY_REQUIRED = new ErrorCode(1_040_250_007, "产线实际产能缺失");
    ErrorCode PRO_AUTO_SCHEDULE_PRODUCTION_MATERIAL_REQUIRED = new ErrorCode(1_040_250_008, "工单缺少生产用料清单");
    ErrorCode PRO_AUTO_SCHEDULE_MATERIAL_SHORTAGE_BLOCKED = new ErrorCode(1_040_250_009, "物料缺料，无法执行自动排产");
    ErrorCode PRO_AUTO_SCHEDULE_PROTECTED_TASK_CONFLICT = new ErrorCode(1_040_250_010, "受保护任务冲突，无法拼接现有工序链");
    ErrorCode PRO_AUTO_SCHEDULE_CALENDAR_CONTEXT_REQUIRED = new ErrorCode(1_040_250_011, "Auto schedule apply requires a preview calendar context token");
    ErrorCode PRO_AUTO_SCHEDULE_CALENDAR_CONTEXT_CHANGED = new ErrorCode(1_040_250_012, "Schedule calendar context changed; regenerate preview before apply");
    ErrorCode PRO_AUTO_SCHEDULE_FROZEN_WORK_ORDER = new ErrorCode(1_040_250_013, "冻结工单不允许参与自动排产");
    ErrorCode PRO_AUTO_SCHEDULE_INFINITE_DURATION_FORMULA_REQUIRED = new ErrorCode(1_040_250_014, "无限产能工序必须配置持续时间公式");
    ErrorCode PRO_AUTO_SCHEDULE_PREFLIGHT_BLOCKED = new ErrorCode(1_040_250_015, "排产前检查未通过：{}");
    ErrorCode PRO_AUTO_SCHEDULE_LATEST_START_ZERO_TASK_BLOCKED = new ErrorCode(1_040_250_016, "最晚开工约束导致未生成任何任务，禁止发布：{}");
    ErrorCode PRO_AUTO_SCHEDULE_ACTIVE_TASK_REQUIRED = new ErrorCode(1_040_250_017, "排产工序仍有剩余报工量，但未生成活动任务，不能发布重排");
    ErrorCode PRO_AUTO_SCHEDULE_ORDER_NOT_SCHEDULABLE = new ErrorCode(1_040_250_018, "排产范围包含不可自动排产、已冻结或不存在的排产工单：{}");
    ErrorCode PRO_AUTO_SCHEDULE_ORDER_BLOCKED = new ErrorCode(1_040_250_019, "排产工单{}排产失败：{}");
    ErrorCode PRO_AUTO_SCHEDULE_ROUTE_VERSION_REQUIRED = new ErrorCode(1_040_250_020, "排产工单缺少冻结工艺路线版本，scheduleOrderId={}");
    ErrorCode PRO_AUTO_SCHEDULE_REPLAN_SCOPE_LOCKED = new ErrorCode(1_040_250_021, "排产重排范围已有审批中申请：{}");

    // ========== MES 排程日历（1-040-260-000） ==========
    ErrorCode PRO_SCHEDULE_CALENDAR_INVALID_MONTH = new ErrorCode(1_040_260_000, "排程日历月份格式无效");
    ErrorCode PRO_SCHEDULE_CALENDAR_INVALID_DATE = new ErrorCode(1_040_260_001, "排程日历日期格式无效");
    ErrorCode PRO_SCHEDULE_CALENDAR_INVALID_WEEKEND_MODE = new ErrorCode(1_040_260_002, "排程日历周末模式无效");
    ErrorCode PRO_SCHEDULE_CALENDAR_INVALID_DATE_SHIFT_MODE = new ErrorCode(1_040_260_003, "夜班只能在工序排产配置中设置，排程日历仅支持白班或休息");
    ErrorCode PRO_SCHEDULE_CALENDAR_CURRENT_SCHEDULE_REQUIRED = new ErrorCode(1_040_260_004, "当前正式排程为空，无法加载排程日历");
    ErrorCode PRO_SCHEDULE_CALENDAR_PRODUCTION_MATERIAL_REQUIRED = new ErrorCode(1_040_260_005, "排程工单缺少生产用料清单");

    // ========== MES 排产员工作台（1-040-265-000） ==========
    ErrorCode PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_CONFIG_REQUIRED = new ErrorCode(1_040_265_000,
            "冒烟测试配置缺失或无效：{}");
    ErrorCode PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_RUNNING = new ErrorCode(1_040_265_001,
            "冒烟测试正在运行，请先结束当前测试");
    ErrorCode PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_START_FAILED = new ErrorCode(1_040_265_002,
            "冒烟测试启动失败：{}");
    ErrorCode PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_STOP_FAILED = new ErrorCode(1_040_265_003,
            "冒烟测试结束失败，进程仍在运行：PID {}");
    ErrorCode PRO_SCHEDULER_WORKBENCH_POLICY_SETTINGS_INVALID = new ErrorCode(1_040_265_004,
            "排产员工作台策略配置无效，请重新保存工作台默认值。");

    // ========== MES 排产工单（1-040-270-000） ==========
    ErrorCode PRO_SCHEDULE_ORDER_NOT_EXISTS = new ErrorCode(1_040_270_000, "排产工单不存在");
    ErrorCode PRO_SCHEDULE_ORDER_PROMISE_DATE_REQUIRED = new ErrorCode(1_040_270_001, "承诺交期不能为空");
    ErrorCode PRO_SCHEDULE_ORDER_WORK_ORDER_DUPLICATE = new ErrorCode(1_040_270_002, "该生产工单已存在排产工单");
    ErrorCode PRO_SCHEDULE_ORDER_WORK_ORDER_NOT_CONFIRMED = new ErrorCode(1_040_270_003, "生产工单已完成或已取消，不能生成排产工单");
    ErrorCode PRO_SCHEDULE_ORDER_WORK_ORDER_FROZEN = new ErrorCode(1_040_270_004, "生产工单已临时冻结，不能生成排产工单");
    ErrorCode PRO_SCHEDULE_ORDER_ROUTE_REQUIRED = new ErrorCode(1_040_270_005, "产品缺少启用工艺路线，不能生成排产工单");
    ErrorCode PRO_SCHEDULE_ORDER_ROUTE_PROCESS_REQUIRED = new ErrorCode(1_040_270_006, "工艺路线缺少工序，不能生成排产工单");
    ErrorCode PRO_SCHEDULE_ORDER_PRIORITY_INVALID = new ErrorCode(1_040_270_007, "排产工单优先级必须大于等于 1");
    ErrorCode PRO_SCHEDULE_ORDER_FROZEN = new ErrorCode(1_040_270_008, "排产工单已冻结，禁止写入操作");
    ErrorCode PRO_SCHEDULE_ORDER_REASON_REQUIRED = new ErrorCode(1_040_270_009, "操作原因不能为空");
    ErrorCode PRO_SCHEDULE_ORDER_BATCH_REQUIRED = new ErrorCode(1_040_270_010, "排产工单不能为空");
    ErrorCode PRO_SCHEDULE_ORDER_DELETE_BLOCKED = new ErrorCode(1_040_270_011, "排产工单存在已报工或已完成记录，不能删除");
    ErrorCode PRO_SCHEDULE_ORDER_ROUTE_SCHEDULE_CONFIG_REQUIRED = new ErrorCode(1_040_270_012, "排产工单缺少路线排产策略配置，processId={}");
    ErrorCode PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED = new ErrorCode(1_040_270_013, "排产资源缺少班次小时配置，routeProcessId={}, workstationId={}");
    ErrorCode PRO_SCHEDULE_ORDER_ROUTE_FLOW_CONFIG_REQUIRED = new ErrorCode(1_040_270_014, "排产工单缺少智能排产流程配置，processId={}");
    ErrorCode PRO_SCHEDULE_ORDER_ROUTE_FLOW_CONFIG_DISABLED = new ErrorCode(1_040_270_015, "排产工单工序已关闭智能排产用途，processId={}");
    ErrorCode PRO_SCHEDULE_ORDER_MANUAL_FINISH_STATUS_INVALID = new ErrorCode(1_040_270_016, "排产工单当前状态不允许人工完成");
    ErrorCode PRO_SCHEDULE_ORDER_MANUAL_FINISH_ALREADY = new ErrorCode(1_040_270_017, "排产工单已人工完成，不能重复操作");
    ErrorCode PRO_SCHEDULE_ORDER_MANUAL_FINISH_NOT_ACTIVE = new ErrorCode(1_040_270_018, "排产工单未处于人工完成状态，不能撤销");
    ErrorCode PRO_SCHEDULE_ORDER_WORKER_QUANTITY_REQUIRED = new ErrorCode(1_040_270_019, "排产资源缺少人员数量配置，routeProcessId={}, workstationId={}");
    ErrorCode PRO_SCHEDULE_ORDER_PROCESS_WIP_NOT_EXISTS = new ErrorCode(1_040_270_020, "当前没有可写入的工序在制记录，processId={}");
    ErrorCode PRO_SCHEDULE_ORDER_PROCESS_WIP_CALENDAR_RULE_REQUIRED = new ErrorCode(1_040_270_021, "工序启用夜班但缺少排程日历规则，processId={}");
    ErrorCode PRO_SCHEDULE_ORDER_RESOURCE_CAPACITY_REQUIRED = new ErrorCode(1_040_270_022, "资源计算排产工序缺少可用资源产能，routeProcessId={}");

    // ========== MES 第三方报工待归属（1-040-272-000） ==========
    ErrorCode PRO_FEEDBACK_IMPORT_RECORD_NOT_EXISTS = new ErrorCode(1_040_272_000, "待归属记录不存在");
    ErrorCode PRO_FEEDBACK_IMPORT_RECORD_NOT_PENDING = new ErrorCode(1_040_272_001, "待归属记录已完成归属");
    ErrorCode PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_NOT_EXISTS = new ErrorCode(1_040_272_002, "排产工单工序不存在或不属于当前排产工单");
    ErrorCode PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_REMAINING_NOT_ENOUGH = new ErrorCode(1_040_272_003, "排产工单工序剩余数量不足，不能归属");
    ErrorCode PRO_FEEDBACK_IMPORT_TARGET_TASK_NOT_EXISTS = new ErrorCode(1_040_272_004, "排产工单工序尚未生成活动任务，不能归属正式报工");
    ErrorCode PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_PROCESS_MISMATCH = new ErrorCode(1_040_272_005, "所选排产工序与导入行工序不匹配，不能归属");
    ErrorCode PRO_FEEDBACK_IMPORT_SCHEDULE_ORDER_ITEM_MISMATCH = new ErrorCode(1_040_272_006, "所选排产工单产品与导入行产品不匹配，不能归属");
    ErrorCode PRO_FEEDBACK_IMPORT_ATTRIBUTION_TARGET_INVALID = new ErrorCode(1_040_272_007, "归属目标类型或目标参数不正确");
    ErrorCode PRO_FEEDBACK_IMPORT_REATTRIBUTION_NOT_ALLOWED = new ErrorCode(1_040_272_008, "当前归属记录不满足修改条件");
    ErrorCode PRO_FEEDBACK_IMPORT_REATTRIBUTION_LINK_INCOMPLETE = new ErrorCode(1_040_272_009, "当前归属记录缺少完整正式报工关联链路，不能修改归属");
    ErrorCode PRO_FEEDBACK_IMPORT_REATTRIBUTION_FEEDBACK_NOT_PREPARE = new ErrorCode(1_040_272_010, "关联正式报工已离开草稿状态，不能修改归属");
    ErrorCode PRO_FEEDBACK_IMPORT_REATTRIBUTION_POOL_CONSUMED = new ErrorCode(1_040_272_011, "当前归属记录创建的缓存池已被其他记录消费，不能修改归属");
    ErrorCode PRO_FEEDBACK_IMPORT_CONFIRM_BATCH_PENDING_EXISTS = new ErrorCode(1_040_272_012, "当前批次仍有未归属记录：{}");
    ErrorCode PRO_FEEDBACK_IMPORT_CONFIRM_BATCH_REQUIRED_FIELD_MISSING = new ErrorCode(1_040_272_013, "当前批次存在漏填记录：{}");
    ErrorCode PRO_FEEDBACK_IMPORT_CONFIRM_BATCH_LINK_INCOMPLETE = new ErrorCode(1_040_272_014, "当前批次存在未生成正式报工草稿或关联链路不完整的记录：{}");
    ErrorCode PRO_FEEDBACK_IMPORT_CONFIRM_BATCH_FEEDBACK_NOT_PREPARE = new ErrorCode(1_040_272_015, "当前批次存在非草稿正式报工记录：{}");
    ErrorCode PRO_FEEDBACK_IMPORT_DIRECT_SUBMIT_FORBIDDEN = new ErrorCode(1_040_272_016, "导入来源的草稿正式报工请返回待归属页批量确认提交");
    ErrorCode PRO_FEEDBACK_IMPORT_DIRECT_MATCH_FAILED = new ErrorCode(1_040_272_017, "李萍报工单第{}行匹配失败：{}");
    ErrorCode PRO_FEEDBACK_IMPORT_DIRECT_MATCH_NOT_UNIQUE = new ErrorCode(1_040_272_018, "李萍报工单第{}行匹配到多条{}，请先清理数据");

    // ========== MES 工艺流程配置配置（1-040-271-000） ==========
    ErrorCode PRO_ROUTE_FLOW_TYPE_INVALID = new ErrorCode(1_040_271_000, "工艺流程配置类型不正确");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_ROUTE_REQUIRED = new ErrorCode(1_040_271_001, "工艺路线不存在");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED = new ErrorCode(1_040_271_002, "工艺路线工序不存在或不属于当前工艺路线");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_BATCH_REPORT_REQUIRED = new ErrorCode(1_040_271_003, "工艺流程批记录配置启用工序必须配置至少一张批记录表格");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_BATCH_REPORT_NOT_EXISTS = new ErrorCode(1_040_271_004, "工艺流程批记录配置引用的批记录报表不存在");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_BATCH_REPORT_DUPLICATE = new ErrorCode(1_040_271_005, "同一工序不能重复绑定同一张批记录表格");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_EXECUTION_MODE_INVALID = new ErrorCode(1_040_271_006, "工艺流程批记录配置执行模式必须为 SEQUENTIAL 或 PARALLEL");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_RECORD_CATEGORY_REQUIRED = new ErrorCode(1_040_271_007, "工艺流程批记录配置记录类型和校验策略不能为空");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_RECORD_CATEGORY_INVALID = new ErrorCode(1_040_271_008, "工艺流程批记录配置记录类型必须为 BATCH_RECORD 或 INTERNAL_RECORD");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_VALIDATION_PROFILE_MISMATCH = new ErrorCode(1_040_271_009, "工艺流程批记录配置记录类型与校验策略不匹配");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_FORM_SLOT_TYPE_REQUIRED = new ErrorCode(1_040_271_020, "工艺流程批记录配置表单槽位类型不能为空");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_FORM_SLOT_TYPE_INVALID = new ErrorCode(1_040_271_021, "工艺流程批记录配置表单槽位类型无效");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_MAIN_FORM_SLOT_REQUIRED = new ErrorCode(1_040_271_022, "工艺流程批记录配置每个启用工序必须至少配置一张 MAIN 主表");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_FORM_SLOT_PERMISSION_SCOPE_REQUIRED = new ErrorCode(1_040_271_023, "工艺流程批记录配置附属表单槽位必须配置对象级权限范围");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_FORM_SLOT_OWNER_ROLE_REQUIRED = new ErrorCode(1_040_271_024, "工艺流程批记录配置附属表单槽位必须配置责任角色");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_REQUIRED_POLICY_INVALID = new ErrorCode(1_040_271_025, "工艺流程批记录配置必填策略无效");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_CONDITION_CONFIG_MISSING = new ErrorCode(1_040_271_026, "工艺流程批记录配置条件必填槽位必须配置触发条件");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_ROUTE_DISABLED = new ErrorCode(1_040_271_027, "工艺流程已经禁用，请先启用工艺流程");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_PRODUCTION_QUANTITY_FACTOR_INVALID = new ErrorCode(1_040_271_028, "工艺流程排产配置生产系数必须大于 0，routeProcessId={}");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_REQUIRED = new ErrorCode(1_040_271_043, "工艺流程动态表单配置必须选择表单中心模板");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_DUPLICATE = new ErrorCode(1_040_271_044, "同一工序表单重复：同一个表单模板只能选择一次");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_PUBLISHED_VERSION_NOT_EXISTS = new ErrorCode(1_040_271_045, "表单中心模板最新已发布版本不存在，templateId={}");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_FILLER_REQUIRED = new ErrorCode(1_040_271_046, "工艺流程动态表单配置必须选择填写人，formBindingKey={}");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_FORM_TEMPLATE_FILLER_SOURCE_INVALID = new ErrorCode(1_040_271_047, "工艺流程动态表单配置填写人来源无效，formBindingKey={}，candidateSourceType={}");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_ROLE_CATEGORY_REQUIRED = new ErrorCode(1_040_271_048, "批记录附件默认角色分类 batch-record 不存在或未启用");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_ENABLED_USER_NOT_ENOUGH = new ErrorCode(1_040_271_049, "当前租户启用用户少于 2 人，无法初始化批记录附件上传角色");
    ErrorCode PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_OWNER_INVALID = new ErrorCode(1_040_271_050, "批记录附件负责人配置无效：{}");
    ErrorCode PRO_ROUTE_VERSION_STALE = new ErrorCode(1_040_271_029, "工艺路线版本已变更，请刷新后再操作，routeId={}，expectedRouteVersionId={}，activeRouteVersionId={}");
    ErrorCode PRO_ROUTE_VERSION_NOT_EXISTS = new ErrorCode(1_040_271_030, "工艺路线版本不存在，routeVersionId={}");
    ErrorCode PRO_ROUTE_VERSION_ACTIVE_NOT_EXISTS = new ErrorCode(1_040_271_031, "工艺路线缺少当前生效版本，routeId={}");
    ErrorCode PRO_ROUTE_VERSION_CONFLICT = new ErrorCode(1_040_271_032, "工艺路线当前生效版本已变化，routeId={}，expectedActiveVersionId={}，actualActiveVersionId={}");
    ErrorCode PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE = new ErrorCode(1_040_271_033, "工艺路线候选版本未满足发布条件，routeVersionId={}，status={}");
    ErrorCode PRO_ROUTE_VERSION_PRODUCTION_REFERENCE_FORBIDDEN = new ErrorCode(1_040_271_034, "未发布的工艺路线版本不能用于生产引用，routeVersionId={}，status={}");
    ErrorCode PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE = new ErrorCode(1_040_271_035, "工艺路线候选版本快照不完整，routeVersionId={}");
    ErrorCode PRO_ROUTE_VERSION_APPROVAL_NOT_EXISTS = new ErrorCode(1_040_271_039, "工艺路线版本审批实例不存在，approvalProcessInstanceId={}");
    ErrorCode PRO_ROUTE_VERSION_APPROVAL_RESULT_INVALID = new ErrorCode(1_040_271_040, "工艺路线版本审批结果无效，approvalResult={}");
    ErrorCode PRO_ROUTE_VERSION_APPROVAL_PROCESS_NOT_STARTED = new ErrorCode(1_040_271_041, "工艺路线版本审批流程未启动，processDefinitionKey={}");
    ErrorCode PRO_ROUTE_VERSION_APPROVAL_PROCESS_INSTANCE_INVALID = new ErrorCode(1_040_271_042, "工艺路线版本审批流程实例编号无效，processInstanceId={}");
    ErrorCode PRO_ROUTE_SCHEDULE_SHIFT_HOURS_REQUIRED = new ErrorCode(1_040_271_010, "工艺路线工序缺少排产班次小时配置，processId={}");
    ErrorCode PRO_ROUTE_SCHEDULE_CALENDAR_RULE_REQUIRED = new ErrorCode(1_040_271_011, "工艺路线排产配置引用的排程日历规则不存在或未配置");
    ErrorCode PRO_ROUTE_SCHEDULE_HOURLY_CAPACITY_REQUIRED = new ErrorCode(1_040_271_012, "工艺路线工序排产小时产能必须大于 0，routeProcessId={}");
    ErrorCode PRO_ROUTE_SCHEDULE_CONFIG_REQUIRED = new ErrorCode(1_040_271_013, "工艺路线工序缺少通用排产配置，routeVersionId={}，routeProcessId={}");
    ErrorCode PRO_ROUTE_SCHEDULE_CAPACITY_MODE_INVALID = new ErrorCode(1_040_271_036, "排产策略已升级，请选择资源计算、排产产能覆盖或无限公式，routeProcessId={}");
    ErrorCode PRO_ROUTE_SCHEDULE_MANUAL_CAPACITY_REQUIRED = new ErrorCode(1_040_271_037, "排产产能覆盖小时产能必须大于 0，routeProcessId={}");
    ErrorCode PRO_ROUTE_SCHEDULE_INFINITE_DURATION_FORMULA_REQUIRED = new ErrorCode(1_040_271_038, "无限公式必须配置数量系数和基础分钟，routeProcessId={}");
    ErrorCode PRO_ROUTE_SCHEDULE_NIGHT_SHIFT_REQUIRED = new ErrorCode(1_040_271_043, "排产策略必须显式传入夜班开关，routeProcessId={}");
    ErrorCode PRO_ROUTE_RESOURCE_READONLY = new ErrorCode(1_040_271_044, "工艺路线资源已改为只读，请在工作站维护设备资源、人力资源和班次产能");

    // ========== MES 设备管理-设备类型（1-040-300-000） ==========
    ErrorCode DV_MACHINERY_TYPE_NOT_EXISTS = new ErrorCode(1_040_300_000, "设备类型不存在");
    ErrorCode DV_MACHINERY_TYPE_EXITS_CHILDREN = new ErrorCode(1_040_300_001, "存在子类型，无法删除");
    ErrorCode DV_MACHINERY_TYPE_PARENT_NOT_EXITS = new ErrorCode(1_040_300_002, "父级类型不存在");
    ErrorCode DV_MACHINERY_TYPE_PARENT_ERROR = new ErrorCode(1_040_300_003, "不能设置自己为父类型");
    ErrorCode DV_MACHINERY_TYPE_NAME_DUPLICATE = new ErrorCode(1_040_300_004, "同一父类型下已存在该名称");
    ErrorCode DV_MACHINERY_TYPE_CODE_DUPLICATE = new ErrorCode(1_040_300_005, "设备类型编码已存在");
    ErrorCode DV_MACHINERY_TYPE_PARENT_IS_CHILD = new ErrorCode(1_040_300_006, "不能设置自己的子类型为父类型");
    ErrorCode DV_MACHINERY_TYPE_HAS_MACHINERY = new ErrorCode(1_040_300_007, "该类型下存在设备，无法删除");

    // ========== MES 设备管理-设备台账（1-040-301-000） ==========
    ErrorCode DV_MACHINERY_NOT_EXISTS = new ErrorCode(1_040_301_000, "设备不存在");
    ErrorCode DV_MACHINERY_CODE_DUPLICATE = new ErrorCode(1_040_301_001, "设备编码已存在");
    ErrorCode DV_MACHINERY_IS_DISABLE = new ErrorCode(1_040_301_007, "设备已禁用");
    ErrorCode DV_MACHINERY_IMPORT_LIST_IS_EMPTY = new ErrorCode(1_040_301_002, "导入设备数据不能为空");
    ErrorCode DV_MACHINERY_HAS_CHECK_PLAN = new ErrorCode(1_040_301_003, "设备已关联点检计划，无法删除");
    ErrorCode DV_MACHINERY_HAS_CHECK_RECORD = new ErrorCode(1_040_301_004, "设备已关联点检记录，无法删除");
    ErrorCode DV_MACHINERY_HAS_MAINTEN_RECORD = new ErrorCode(1_040_301_005, "设备已关联保养记录，无法删除");
    ErrorCode DV_MACHINERY_HAS_REPAIR = new ErrorCode(1_040_301_006, "设备已关联维修工单，无法删除");

    // ========== MES 设备管理-点检保养项目（1-040-304-000） ==========
    ErrorCode DV_SUBJECT_NOT_EXISTS = new ErrorCode(1_040_304_000, "点检保养项目不存在");
    ErrorCode DV_SUBJECT_CODE_DUPLICATE = new ErrorCode(1_040_304_001, "项目编码已存在");
    ErrorCode DV_SUBJECT_USED_BY_CHECK_PLAN = new ErrorCode(1_040_304_002, "点检保养项目已被点检保养方案使用，无法删除");
    ErrorCode DV_SUBJECT_IS_DISABLE = new ErrorCode(1_040_304_003, "点检保养项目已禁用");

    // ========== MES 设备管理-点检计划（1-040-302-000） ==========
    ErrorCode DV_CHECK_PLAN_NOT_EXISTS = new ErrorCode(1_040_302_000, "点检计划不存在");
    ErrorCode DV_CHECK_PLAN_CODE_DUPLICATE = new ErrorCode(1_040_302_001, "点检保养方案编码已存在");
    ErrorCode DV_CHECK_PLAN_NOT_PREPARE = new ErrorCode(1_040_302_002, "点检保养方案已启用，不允许修改或删除");
    ErrorCode DV_CHECK_PLAN_NO_MACHINERY = new ErrorCode(1_040_302_003, "启用方案时，至少需要关联一台设备");
    ErrorCode DV_CHECK_PLAN_NO_SUBJECT = new ErrorCode(1_040_302_004, "启用方案时，至少需要关联一个点检保养项目");
    ErrorCode DV_CHECK_PLAN_NOT_ENABLED = new ErrorCode(1_040_302_005, "点检保养方案未启用，不允许停用");
    ErrorCode DV_CHECK_PLAN_TYPE_MISMATCH = new ErrorCode(1_040_302_006, "点检保养方案类型与当前业务不匹配");
    ErrorCode DV_CHECK_PLAN_NOT_ENABLED_FOR_RECORD = new ErrorCode(1_040_302_007, "点检保养方案未启用，不允许创建记录");
    // ========== MES 设备管理-点检方案设备（1-040-302-100） ==========
    ErrorCode DV_CHECK_PLAN_MACHINERY_NOT_EXISTS = new ErrorCode(1_040_302_100, "点检保养方案设备不存在");
    ErrorCode DV_CHECK_PLAN_MACHINERY_DUPLICATE = new ErrorCode(1_040_302_101, "该设备已关联到当前方案，请勿重复添加");
    ErrorCode DV_CHECK_PLAN_MACHINERY_EXISTS_IN_SAME_TYPE = new ErrorCode(1_040_302_102, "该设备已存在于同类型的其他启用的或草稿的方案中，不允许同一设备添加多个同类型的方案");
    // ========== MES 设备管理-点检方案项目（1-040-302-200） ==========
    ErrorCode DV_CHECK_PLAN_SUBJECT_NOT_EXISTS = new ErrorCode(1_040_302_200, "点检保养方案项目不存在");
    ErrorCode DV_CHECK_PLAN_SUBJECT_DUPLICATE = new ErrorCode(1_040_302_201, "该项目已关联到当前方案，请勿重复添加");

    // ========== MES 设备管理-维修工单（1-040-303-000） ==========
    ErrorCode DV_REPAIR_NOT_EXISTS = new ErrorCode(1_040_303_000, "维修工单不存在");
    ErrorCode DV_REPAIR_NOT_PREPARE = new ErrorCode(1_040_303_001, "维修工单不是草稿状态，不允许修改或删除");
    ErrorCode DV_REPAIR_CODE_DUPLICATE = new ErrorCode(1_040_303_002, "维修工单编码已存在");
    ErrorCode DV_REPAIR_NOT_CONFIRMED = new ErrorCode(1_040_303_003, "只有维修中状态的维修工单才能完成维修");
    ErrorCode DV_REPAIR_NOT_APPROVING = new ErrorCode(1_040_303_004, "只有待验收状态的维修工单才能验收");
    // ========== MES 设备管理-维修工单行（1-040-303-100） ==========
    ErrorCode DV_REPAIR_LINE_NOT_EXISTS = new ErrorCode(1_040_303_100, "维修工单行不存在");

    // ========== MES 设备管理-保养记录（1-040-305-000） ==========
    ErrorCode MAINTEN_RECORD_NOT_EXISTS = new ErrorCode(1_040_305_000, "设备保养记录不存在");
    ErrorCode MAINTEN_RECORD_NOT_DRAFT = new ErrorCode(1_040_305_001, "设备保养记录已提交，不允许修改或删除");
    ErrorCode MAINTEN_RECORD_NO_LINE = new ErrorCode(1_040_305_002, "提交保养记录时，至少需要一条保养项目");
    // ========== MES 设备管理-保养记录明细（1-040-305-100） ==========
    ErrorCode MAINTEN_RECORD_LINE_NOT_EXISTS = new ErrorCode(1_040_305_100, "设备保养记录明细不存在");

    // ========== MES 设备管理-点检记录（1-040-306-000） ==========
    ErrorCode DV_CHECK_RECORD_NOT_EXISTS = new ErrorCode(1_040_306_000, "设备点检记录不存在");
    ErrorCode DV_CHECK_RECORD_NOT_DRAFT = new ErrorCode(1_040_306_001, "设备点检记录已完成，不允许修改或删除");
    ErrorCode DV_CHECK_RECORD_NO_LINE = new ErrorCode(1_040_306_002, "提交点检记录时，至少需要一条点检项目");
    // ========== MES 设备管理-点检记录明细（1-040-306-100） ==========
    ErrorCode DV_CHECK_RECORD_LINE_NOT_EXISTS = new ErrorCode(1_040_306_100, "设备点检记录明细不存在");

    // ========== MES 工具管理-工具类型（1-040-400-000） ==========
    ErrorCode TM_TOOL_TYPE_NOT_EXISTS = new ErrorCode(1_040_400_000, "工具类型不存在");
    ErrorCode TM_TOOL_TYPE_CODE_DUPLICATE = new ErrorCode(1_040_400_001, "工具类型编码已存在");
    ErrorCode TM_TOOL_TYPE_NAME_DUPLICATE = new ErrorCode(1_040_400_002, "工具类型名称已存在");
    ErrorCode TM_TOOL_TYPE_HAS_TOOL = new ErrorCode(1_040_400_003, "该工具类型下存在工具，无法删除");
    ErrorCode TM_TOOL_TYPE_HAS_WORKSTATION_TOOL = new ErrorCode(1_040_400_004, "该工具类型已被工作站工装资源引用，无法删除");

    // ========== MES 工具管理-工具台账（1-040-401-000） ==========
    ErrorCode TM_TOOL_NOT_EXISTS = new ErrorCode(1_040_401_000, "工具不存在");
    ErrorCode TM_TOOL_CODE_DUPLICATE = new ErrorCode(1_040_401_001, "工具编码已存在");
    ErrorCode TM_TOOL_HAS_BATCH = new ErrorCode(1_040_401_002, "该工具已被批次引用，无法删除");

    // ========== MES 生产管理-工序（1-040-500-000） ==========
    ErrorCode PRO_PROCESS_NOT_EXISTS = new ErrorCode(1_040_500_000, "工序不存在");
    ErrorCode PRO_PROCESS_CODE_EXISTS = new ErrorCode(1_040_500_001, "工序编码已存在");
    ErrorCode PRO_PROCESS_NAME_EXISTS = new ErrorCode(1_040_500_002, "工序名称已存在");
    ErrorCode PRO_PROCESS_USED_BY_ROUTE = new ErrorCode(1_040_500_003, "工序已被工艺路线引用，无法删除");
    ErrorCode PRO_PROCESS_IS_DISABLE = new ErrorCode(1_040_500_004, "工序已禁用");
    // ========== MES 生产管理-工序内容（1-040-500-100） ==========
    ErrorCode PRO_PROCESS_CONTENT_NOT_EXISTS = new ErrorCode(1_040_500_100, "工序内容不存在");

    // ========== MES 生产管理-工艺路线（1-040-501-000） ==========
    ErrorCode PRO_ROUTE_NOT_EXISTS = new ErrorCode(1_040_501_000, "工艺路线不存在");
    ErrorCode PRO_ROUTE_CODE_DUPLICATE = new ErrorCode(1_040_501_001, "工艺路线编码已存在");
    ErrorCode PRO_ROUTE_ENABLE_NO_PROCESS = new ErrorCode(1_040_501_002, "请先添加组成工序");
    ErrorCode PRO_ROUTE_ENABLE_NO_KEY_PROCESS = new ErrorCode(1_040_501_003, "工艺路线必须要有关键工序");
    ErrorCode PRO_ROUTE_ENABLE_PRODUCT_NO_BOM = new ErrorCode(1_040_501_004, "产品 {} 未配置工序的 BOM 消耗");
    ErrorCode PRO_ROUTE_IS_ENABLE = new ErrorCode(1_040_501_005, "工艺路线已启用，不允许操作");
    ErrorCode PRO_ROUTE_NAME_DUPLICATE = new ErrorCode(1_040_501_006, "工艺路线名称已存在");
    // ========== MES 生产管理-工艺路线工序（1-040-501-100） ==========
    ErrorCode PRO_ROUTE_PROCESS_NOT_EXISTS = new ErrorCode(1_040_501_100, "工艺路线工序不存在");
    ErrorCode PRO_ROUTE_PROCESS_SORT_DUPLICATE = new ErrorCode(1_040_501_101, "序号已存在");
    ErrorCode PRO_ROUTE_PROCESS_DUPLICATE = new ErrorCode(1_040_501_102, "不能重复添加工序");
    ErrorCode PRO_ROUTE_PROCESS_KEY_DUPLICATE = new ErrorCode(1_040_501_103, "当前工艺路线已经指定过关键工序");
    ErrorCode PRO_ROUTE_PROCESS_IDENTITY_NOT_FOUND = new ErrorCode(1_040_501_104,
            "无法解析当前工艺路线工序，routeId={}，sourceProcessId={}，routeProcessId={}，processCode={}");
    ErrorCode PRO_ROUTE_PROCESS_IDENTITY_AMBIGUOUS = new ErrorCode(1_040_501_105,
            "工艺路线工序身份不唯一，routeId={}，sourceProcessId={}，processCode={}，candidateRouteProcessIds={}");
    ErrorCode PRO_ROUTE_PROCESS_FLOW_INVALID = new ErrorCode(1_040_501_120, "工艺路线流转关系图无效，请先修正关系图");
    ErrorCode PRO_ROUTE_PROCESS_FLOW_VERSION_CONFLICT = new ErrorCode(1_040_501_121, "关系图已被其他用户修改，请刷新后重试");
    // ========== MES 生产管理-工艺路线产品（1-040-501-200） ==========
    ErrorCode PRO_ROUTE_PRODUCT_NOT_EXISTS = new ErrorCode(1_040_501_200, "工艺路线产品不存在");
    ErrorCode PRO_ROUTE_PRODUCT_ITEM_DUPLICATE = new ErrorCode(1_040_501_201, "此产品已配置了工艺路线");
    ErrorCode PRO_ROUTE_PRODUCT_WORK_ORDER_MATCH_EMPTY = new ErrorCode(1_040_501_202, "未找到产品名称等于当前工艺路线名称的生产订单产品：{}");
    ErrorCode PRO_ROUTE_PRODUCT_ITEM_BOUND_OTHER_ROUTE = new ErrorCode(1_040_501_203, "以下产品已配置了其它工艺路线：{}");
    ErrorCode PRO_ROUTE_PRODUCT_ROUTE_NAME_EMPTY = new ErrorCode(1_040_501_204, "工艺路线名称不能为空，无法从生产订单补齐产品");
    // ========== MES 生产管理-工艺路线产品BOM（1-040-501-300） ==========
    ErrorCode PRO_ROUTE_PRODUCT_BOM_NOT_EXISTS = new ErrorCode(1_040_501_300, "工艺路线产品 BOM 不存在");
    ErrorCode PRO_ROUTE_PRODUCT_BOM_DUPLICATE = new ErrorCode(1_040_501_301, "当前 BOM 物料在此工序已经配置过");
    // ========== MES 生产管理-工艺路线导入（1-040-501-400） ==========
    ErrorCode PRO_ROUTE_IMPORT_FILE_EMPTY = new ErrorCode(1_040_501_400, "工艺路线导入文件不能为空");
    ErrorCode PRO_ROUTE_IMPORT_INVALID_MARKDOWN = new ErrorCode(1_040_501_401, "工艺路线导入文件格式不正确：{}");
    ErrorCode PRO_ROUTE_IMPORT_ROUTE_DUPLICATE = new ErrorCode(1_040_501_402, "导入文件中工艺路线编码重复：{}");
    ErrorCode PRO_ROUTE_IMPORT_ROUTE_EXISTS = new ErrorCode(1_040_501_403, "工艺路线编码已存在：{}");
    ErrorCode PRO_ROUTE_IMPORT_ROUTE_NO_STEP = new ErrorCode(1_040_501_404, "导入路线没有工序：{}");
    ErrorCode PRO_ROUTE_IMPORT_SEQUENCE_DUPLICATE = new ErrorCode(1_040_501_405, "导入路线 {} 的工序序号重复：{}");
    ErrorCode PRO_ROUTE_IMPORT_FINAL_PROCESS_INVALID = new ErrorCode(1_040_501_406, "导入路线必须且只能有一个最终工序：{}");
    ErrorCode PRO_ROUTE_IMPORT_PROCESS_CONFLICT = new ErrorCode(1_040_501_407, "工序编码 {} 的名称不一致，本地：{}，导入：{}");
    ErrorCode PRO_ROUTE_IMPORT_PROCESS_NAME_EXISTS = new ErrorCode(1_040_501_408, "工序名称 {} 已存在但编码不一致");
    ErrorCode PRO_ROUTE_IMPORT_CHECK_PROCESS_INVALID = new ErrorCode(1_040_501_409, "检验工序映射无效：{}");
    ErrorCode PRO_ROUTE_IMPORT_PROCESS_STATUS_INVALID = new ErrorCode(1_040_501_410, "导入工序状态不正确：{}");
    ErrorCode PRO_ROUTE_IMPORT_INVALID_EXCEL = new ErrorCode(1_040_501_411, "工艺路线 Excel 文件无法解析");
    ErrorCode PRO_ROUTE_IMPORT_SHEET1_MISSING = new ErrorCode(1_040_501_412, "工艺路线 Excel 缺少 Sheet1");
    ErrorCode PRO_ROUTE_IMPORT_SHEET1_HEADERS_INVALID = new ErrorCode(1_040_501_413, "Sheet1 表头不符合导入要求");
    ErrorCode PRO_ROUTE_IMPORT_SHEET1_PRODUCT_DUPLICATE = new ErrorCode(1_040_501_414, "Sheet1 产品块名称重复：{}");
    ErrorCode PRO_ROUTE_IMPORT_WORKBOOK_SHEET_MISSING = new ErrorCode(1_040_501_415, "工艺路线导入导出 Excel 缺少 Sheet：{}");
    ErrorCode PRO_ROUTE_IMPORT_WORKBOOK_HEADERS_INVALID = new ErrorCode(1_040_501_416, "工艺路线导入导出 Excel 的 {} 表头不符合导入要求");
    ErrorCode PRO_ROUTE_IMPORT_WORKBOOK_CELL_REQUIRED = new ErrorCode(1_040_501_417, "工艺路线导入导出 Excel 的 {} 第 {} 行 {} 不能为空");
    ErrorCode PRO_ROUTE_IMPORT_WORKBOOK_DUPLICATE = new ErrorCode(1_040_501_418, "工艺路线导入导出 Excel 数据重复：{}");
    ErrorCode PRO_ROUTE_IMPORT_WORKBOOK_MASTER_MISSING = new ErrorCode(1_040_501_419, "工艺路线导入导出 Excel 引用的主数据不存在：{}");
    ErrorCode PRO_ROUTE_IMPORT_WORKBOOK_RESOURCE_INVALID = new ErrorCode(1_040_501_420, "工艺路线导入导出 Excel 工序资源无效：{}");
    ErrorCode PRO_ROUTE_IMPORT_WORKBOOK_STATUS_INVALID = new ErrorCode(1_040_501_421, "工艺路线导入导出 Excel 路线状态不正确：{}");
    ErrorCode PRO_ROUTE_EXPORT_WORKBOOK_FAILED = new ErrorCode(1_040_501_422, "工艺路线导入导出 Excel 生成失败：{}");

    // ========== MES 生产管理-生产工单（1-040-502-000） ==========
    ErrorCode PRO_WORK_ORDER_NOT_EXISTS = new ErrorCode(1_040_502_000, "生产工单不存在");
    ErrorCode PRO_WORK_ORDER_CODE_DUPLICATE = new ErrorCode(1_040_502_001, "生产工单编码已存在");
    ErrorCode PRO_WORK_ORDER_NOT_PREPARE = new ErrorCode(1_040_502_002, "只有草稿状态的工单才能执行此操作");
    ErrorCode PRO_WORK_ORDER_NOT_CONFIRMED = new ErrorCode(1_040_502_003, "只有已确认状态的工单才能执行此操作");
    ErrorCode PRO_WORK_ORDER_HAS_CHILDREN = new ErrorCode(1_040_502_004, "存在子工单，无法删除");
    ErrorCode PRO_WORK_ORDER_PRODUCT_MISMATCH = new ErrorCode(1_040_502_005, "当前产品物料与生产工单产品不一致");
    ErrorCode PRO_WORK_ORDER_ERP_BOM_SYNC_STATUS_INVALID = new ErrorCode(1_040_502_006, "当前工单状态不允许执行 ERP 同步 BOM");
    ErrorCode PRO_WORK_ORDER_ERP_BOM_SYNC_PRODUCT_CODE_MISSING = new ErrorCode(1_040_502_007, "当前工单产品编码缺失，无法执行 ERP 同步 BOM");
    ErrorCode PRO_WORK_ORDER_ERP_BOM_SYNC_NOT_FOUND = new ErrorCode(1_040_502_008, "ERP 中未找到产品编码 {} 的已审核 BOM");
    ErrorCode PRO_WORK_ORDER_ERP_BOM_SYNC_MULTI_VERSION = new ErrorCode(1_040_502_009, "ERP 中产品编码 {} 命中了多个已审核 BOM 版本：{}");
    ErrorCode PRO_WORK_ORDER_ERP_BOM_SYNC_DOWNSTREAM_EXISTS = new ErrorCode(1_040_502_010, "当前工单已存在领料、外协发料或物料消耗记录，禁止覆盖 BOM");
    ErrorCode PRO_WORK_ORDER_ERP_BOM_SYNC_ITEM_MISSING = new ErrorCode(1_040_502_011, "ERP BOM 子项物料未映射到本地 MES 物料：{}");
    ErrorCode PRO_WORK_ORDER_ERP_BOM_SYNC_RECURSIVE_ITEM = new ErrorCode(1_040_502_012, "ERP BOM 子项物料 {} 在本地仍配置了下级 BOM，禁止同步");
    ErrorCode PRO_WORK_ORDER_MANUAL_OPERATION_FORBIDDEN = new ErrorCode(1_040_502_013, "生产工单由金蝶同步生成，禁止本地操作");
    ErrorCode PRO_WORK_ORDER_CREATE_ERP_ALREADY_LINKED = new ErrorCode(1_040_502_014, "当前生产工单已关联 ERP 生产订单，禁止重复创建");
    ErrorCode PRO_WORK_ORDER_CREATE_ERP_STATUS_INVALID = new ErrorCode(1_040_502_015, "当前生产工单不允许创建 ERP 生产订单：{}");
    ErrorCode PRO_WORK_ORDER_CREATE_ERP_DATA_MISSING = new ErrorCode(1_040_502_016, "当前生产工单创建 ERP 生产订单缺少必要数据：{}");
    ErrorCode PRO_WORK_ORDER_CREATE_ERP_DUPLICATE = new ErrorCode(1_040_502_017, "ERP 中已存在同编码生产订单：{}");
    ErrorCode PRO_WORK_ORDER_BOM_NOT_EXISTS = new ErrorCode(1_040_502_100, "生产工单BOM不存在");

    // ========== MES 生产管理-生产任务（1-040-503-000） ==========
    ErrorCode PRO_TASK_NOT_EXISTS = new ErrorCode(1_040_503_000, "生产任务不存在");
    ErrorCode PRO_TASK_ALREADY_FINISHED = new ErrorCode(1_040_503_001, "生产任务已完成或已取消，不能继续操作");
    ErrorCode PRO_TASK_WORK_ORDER_MISMATCH = new ErrorCode(1_040_503_002, "生产任务不属于当前生产工单");
    ErrorCode PRO_TASK_WORKSTATION_MISMATCH = new ErrorCode(1_040_503_003, "生产任务不属于当前工作站");
    ErrorCode PRO_TASK_ROUTE_PROCESS_MISMATCH = new ErrorCode(1_040_503_004, "生产任务与当前工艺路线或工序不一致");
    ErrorCode PRO_TASK_ITEM_MISMATCH = new ErrorCode(1_040_503_005, "生产任务产品与当前产品物料不一致");
    ErrorCode PRO_WORKSTATION_PROCESS_MISMATCH = new ErrorCode(1_040_503_006, "工作站所属工序与当前工序不一致");
    // ========== MES 生产管理-生产任务投料（1-040-503-100） ==========
    ErrorCode PRO_TASK_ISSUE_NOT_EXISTS = new ErrorCode(1_040_503_100, "生产任务投料记录不存在");

    // ========== MES 生产管理-安灯呼叫配置（1-040-504-000） ==========
    ErrorCode PRO_ANDON_CONFIG_NOT_EXISTS = new ErrorCode(1_040_504_000, "安灯呼叫配置不存在");

    // ========== MES 生产管理-安灯呼叫记录（1-040-505-000） ==========
    ErrorCode PRO_ANDON_RECORD_NOT_EXISTS = new ErrorCode(1_040_505_000, "安灯呼叫记录不存在");
    ErrorCode PRO_ANDON_RECORD_ALREADY_HANDLED = new ErrorCode(1_040_505_001, "安灯记录已处置，不允许重复处置");
    ErrorCode PRO_ANDON_RECORD_HANDLE_TIME_REQUIRED = new ErrorCode(1_040_505_002, "标记已处置时，处置时间不能为空");
    ErrorCode PRO_ANDON_RECORD_HANDLER_USER_REQUIRED = new ErrorCode(1_040_505_003, "标记已处置时，处置人不能为空");

    // ========== MES 生产管理-生产报工（1-040-506-000） ==========
    ErrorCode PRO_FEEDBACK_NOT_EXISTS = new ErrorCode(1_040_506_000, "生产报工不存在");
    ErrorCode PRO_FEEDBACK_NOT_PREPARE = new ErrorCode(1_040_506_001, "只能修改或删除草稿状态的报工单");
    ErrorCode PRO_FEEDBACK_NOT_APPROVING = new ErrorCode(1_040_506_002, "只有审批中状态的报工单才能执行此操作");
    ErrorCode PRO_FEEDBACK_NOT_UNCHECK = new ErrorCode(1_040_506_003, "只有待检验状态的报工单才能完成检验");
    ErrorCode PRO_FEEDBACK_QUANTITY_EXCEED = new ErrorCode(1_040_506_004, "报工数量不能超过排产数量");
    ErrorCode PRO_FEEDBACK_STATUS_ERROR = new ErrorCode(1_040_506_005, "报工单状态不正确，无法执行此操作");
    ErrorCode PRO_FEEDBACK_WORK_ORDER_NOT_CONFIRMED = new ErrorCode(1_040_506_006, "关联的工单未确认，无法创建报工");
    ErrorCode PRO_FEEDBACK_QUALIFIED_UNQUALIFIED_MISMATCH = new ErrorCode(1_040_506_007, "合格品数量与不良品数量之和必须等于报工数量");
    ErrorCode PRO_FEEDBACK_ROUTE_PROCESS_INVALID = new ErrorCode(1_040_506_008, "未找到对应的工艺工序配置，请检查工艺路线与工序");
    ErrorCode PRO_FEEDBACK_TASK_OR_ORDER_FINISHED = new ErrorCode(1_040_506_009, "当前生产任务或工单已完成，不能继续报工");
    ErrorCode PRO_FEEDBACK_QUANTITY_MUST_POSITIVE = new ErrorCode(1_040_506_010, "报工数量必须大于 0");
    ErrorCode PRO_FEEDBACK_QUALIFIED_UNQUALIFIED_REQUIRED = new ErrorCode(1_040_506_011, "请输入合格品和不良品数量，且合计须大于 0");
    ErrorCode PRO_FEEDBACK_UNCHECK_QUANTITY_EXISTS = new ErrorCode(1_040_506_012, "当前报工单未完成检验（待检数量：{}），无法执行报工");
    ErrorCode PRO_FEEDBACK_IMPORT_FILE_EMPTY = new ErrorCode(1_040_506_013, "第三方报工导入文件不能为空");
    ErrorCode PRO_FEEDBACK_IMPORT_FILE_TYPE_INVALID = new ErrorCode(1_040_506_014, "第三方报工导入仅支持 .xlsx 文件");
    ErrorCode PRO_FEEDBACK_IMPORT_WORKBOOK_EMPTY = new ErrorCode(1_040_506_015, "第三方报工导入文件中没有可导入的非空工作表");
    ErrorCode PRO_FEEDBACK_IMPORT_HEADERS_INVALID = new ErrorCode(1_040_506_016, "第三方报工导入表头不匹配：{}");
    ErrorCode PRO_FEEDBACK_IMPORT_REQUIRED_CELL_EMPTY = new ErrorCode(1_040_506_017, "第三方报工导入 {} 第 {} 行字段 [{}] 不能为空");
    ErrorCode PRO_FEEDBACK_IMPORT_FEEDBACK_TIME_INVALID = new ErrorCode(1_040_506_018, "第三方报工导入 {} 第 {} 行报工日期格式不正确：{}");
    ErrorCode PRO_FEEDBACK_IMPORT_TASK_NOT_EXISTS = new ErrorCode(1_040_506_019, "第三方报工导入 {} 第 {} 行派工单号不存在：{}");
    ErrorCode PRO_FEEDBACK_IMPORT_TASK_NOT_UNIQUE = new ErrorCode(1_040_506_020, "第三方报工导入 {} 第 {} 行派工单号匹配到多条任务：{}");
    ErrorCode PRO_FEEDBACK_IMPORT_WORK_ORDER_MISMATCH = new ErrorCode(1_040_506_021, "第三方报工导入 {} 第 {} 行生产订单号与任务不一致，导入值：{}，任务值：{}");
    ErrorCode PRO_FEEDBACK_IMPORT_ITEM_MISMATCH = new ErrorCode(1_040_506_022, "第三方报工导入 {} 第 {} 行产品编码与任务不一致，导入值：{}，任务值：{}");
    ErrorCode PRO_FEEDBACK_IMPORT_PROCESS_MISMATCH = new ErrorCode(1_040_506_023, "第三方报工导入 {} 第 {} 行工序编码与任务不一致，导入值：{}，任务值：{}");
    ErrorCode PRO_FEEDBACK_IMPORT_FEEDBACK_USER_NOT_EXISTS = new ErrorCode(1_040_506_024, "第三方报工导入 {} 第 {} 行报工人编码不存在：{}");
    ErrorCode PRO_FEEDBACK_IMPORT_APPROVER_NOT_EXISTS = new ErrorCode(1_040_506_025, "第三方报工导入 {} 第 {} 行工段长不存在：{}");
    ErrorCode PRO_FEEDBACK_IMPORT_APPROVER_NOT_UNIQUE = new ErrorCode(1_040_506_026, "第三方报工导入 {} 第 {} 行工段长匹配到多名用户：{}");
    ErrorCode PRO_FEEDBACK_IMPORT_ROW_DUPLICATE = new ErrorCode(1_040_506_027, "第三方报工导入 {} 第 {} 行已导入，不能重复导入同一文件");
    ErrorCode PRO_FEEDBACK_SIMULATE_SOURCE_NOT_EXISTS = new ErrorCode(1_040_506_028,
            "当前没有可模拟报工的排产工序（需要启用、剩余数量大于 0，且工单、物料、工序、活动任务齐备）");
    ErrorCode PRO_FEEDBACK_SIMULATE_CURRENT_USER_NOT_EXISTS = new ErrorCode(1_040_506_029,
            "当前登录用户不存在或缺少用户名，不能模拟报工");
    ErrorCode PRO_FEEDBACK_SIMULATE_WORKBOOK_BUILD_FAILED = new ErrorCode(1_040_506_030,
            "模拟报工导入文件生成失败：{}");
    ErrorCode PRO_FEEDBACK_SIMULATE_PROCESS_COUNT_INVALID = new ErrorCode(1_040_506_031,
            "模拟报工工序数量必须在 1 到 20 之间");
    ErrorCode PRO_FEEDBACK_SIMULATE_SOURCE_NOT_ENOUGH = new ErrorCode(1_040_506_032,
            "当前可模拟报工的排产工序数量不足，请求数量：{}，可用数量：{}");

    // ========== MES 生产管理-生产流转卡（1-040-507-000） ==========
    ErrorCode PRO_CARD_NOT_EXISTS = new ErrorCode(1_040_507_000, "生产流转卡不存在");
    ErrorCode PRO_CARD_CODE_DUPLICATE = new ErrorCode(1_040_507_001, "流转卡编码已存在");
    ErrorCode PRO_CARD_STATUS_ERROR = new ErrorCode(1_040_507_002, "流转卡状态不正确");
    ErrorCode PRO_CARD_CANCEL_NOT_ALLOWED = new ErrorCode(1_040_507_003, "已完成或已取消的流转卡不允许取消");
    ErrorCode PRO_CARD_NOT_PREPARE = new ErrorCode(1_040_507_004, "流转卡不是草稿状态，不允许修改或删除");
    // ========== MES 生产管理-流转卡工序（1-040-507-100） ==========
    ErrorCode PRO_CARD_PROCESS_NOT_EXISTS = new ErrorCode(1_040_507_100, "流转卡工序记录不存在");

    // ========== MES 生产管理-工作记录（1-040-508-000） ==========
    ErrorCode WORK_RECORD_NOT_CLOCK_IN = new ErrorCode(1_040_508_001, "当前用户未上工，无法下工");
    ErrorCode WORK_RECORD_ALREADY_CLOCK_IN = new ErrorCode(1_040_508_002, "当前用户已上工，请先下工再操作");

    // ========== MES 质量管理-质检方案（1-040-600-000） ==========
    ErrorCode QC_TEMPLATE_NOT_EXISTS = new ErrorCode(1_040_600_000, "质检方案不存在");
    ErrorCode QC_TEMPLATE_CODE_DUPLICATE = new ErrorCode(1_040_600_001, "质检方案编号已存在");
    // ========== MES 质量管理-质检方案检测指标项（1-040-600-100） ==========
    ErrorCode QC_TEMPLATE_INDICATOR_NOT_EXISTS = new ErrorCode(1_040_600_100, "质检方案检测指标项不存在");
    // ========== MES 质量管理-质检方案产品关联（1-040-600-200） ==========
    ErrorCode QC_TEMPLATE_ITEM_NOT_EXISTS = new ErrorCode(1_040_600_200, "质检方案产品关联不存在");
    ErrorCode QC_TEMPLATE_ITEM_DUPLICATE = new ErrorCode(1_040_600_201, "该产品已关联此质检方案");

    // ========== MES 质量管理-质检指标（1-040-601-000） ==========
    ErrorCode QC_INDICATOR_NOT_EXISTS = new ErrorCode(1_040_601_000, "质检指标不存在");
    ErrorCode QC_INDICATOR_CODE_DUPLICATE = new ErrorCode(1_040_601_001, "质检指标编码已存在");
    ErrorCode QC_INDICATOR_NAME_DUPLICATE = new ErrorCode(1_040_601_002, "质检指标名称已存在");
    ErrorCode QC_INDICATOR_RESULT_SPECIFICATION_REQUIRED = new ErrorCode(1_040_601_003, "结果值属性不能为空");

    // ========== MES 质量管理-缺陷类型（1-040-602-000） ==========
    ErrorCode QC_DEFECT_NOT_EXISTS = new ErrorCode(1_040_602_000, "缺陷类型不存在");
    ErrorCode QC_DEFECT_CODE_DUPLICATE = new ErrorCode(1_040_602_001, "缺陷类型编码已存在");
    ErrorCode QC_DEFECT_NAME_DUPLICATE = new ErrorCode(1_040_602_002, "缺陷类型名称已存在");

    // ========== MES 质量管理-来料检验 IQC（1-040-603-000） ==========
    ErrorCode QC_IQC_NOT_EXISTS = new ErrorCode(1_040_603_000, "来料检验单不存在");
    ErrorCode QC_IQC_CODE_DUPLICATE = new ErrorCode(1_040_603_001, "来料检验单编号已存在");
    ErrorCode QC_IQC_NOT_PREPARE = new ErrorCode(1_040_603_002, "只有草稿状态的检验单才可操作");
    ErrorCode QC_IQC_QUANTITY_MISMATCH = new ErrorCode(1_040_603_004, "合格品与不合格品数量之和须等于检测数量");
    ErrorCode QC_NO_TEMPLATE = new ErrorCode(1_040_603_005, "当前产品未配置检测模板");
    ErrorCode QC_IQC_SOURCE_DOC_PARAMS_MISSING = new ErrorCode(1_040_603_006, "来源单据类型非空时，来源单据 ID 和来源单据行 ID 不能为空");
    ErrorCode QC_IQC_CHECK_RESULT_EMPTY = new ErrorCode(1_040_603_007, "完成检验单前，检测结果必须填写");
    // ========== MES 质量管理-来料检验行（1-040-603-100） ==========
    ErrorCode QC_IQC_LINE_NOT_EXISTS = new ErrorCode(1_040_603_100, "来料检验行不存在");

    // ========== MES 质量管理-过程检验 IPQC（1-040-604-000） ==========
    ErrorCode QC_IPQC_NOT_EXISTS = new ErrorCode(1_040_604_000, "过程检验单不存在");
    ErrorCode QC_IPQC_CODE_DUPLICATE = new ErrorCode(1_040_604_001, "过程检验单编号已存在");
    ErrorCode QC_IPQC_NOT_PREPARE = new ErrorCode(1_040_604_002, "只有草稿状态的检验单才可操作");
    ErrorCode QC_IPQC_QUANTITY_MISMATCH = new ErrorCode(1_040_604_004, "合格品与不合格品数量之和须等于检测数量");
    ErrorCode QC_IPQC_NO_TEMPLATE = new ErrorCode(1_040_604_005, "当前产品未配置 IPQC 检测模板");
    ErrorCode QC_IPQC_CHECK_RESULT_EMPTY = new ErrorCode(1_040_604_006, "完成检验单前，检测结果必须填写");
    ErrorCode QC_IPQC_SOURCE_DOC_TYPE_UNKNOWN = new ErrorCode(1_040_604_007, "未知的 IPQC 来源单据类型");
    ErrorCode QC_IPQC_SOURCE_DOC_NO_PENDING_LINE = new ErrorCode(1_040_604_008, "来源报工单不存在待检产出行，无法创建 IPQC");
    ErrorCode QC_IPQC_SOURCE_LINE_NOT_BELONG = new ErrorCode(1_040_604_009, "来源单据行不属于该报工单");
    ErrorCode QC_IPQC_SOURCE_LINE_REQUIRED = new ErrorCode(1_040_604_010, "来源单据类型为报工时，来源产出行 ID 不能为空");
    ErrorCode QC_IPQC_SOURCE_LINE_NOT_PENDING = new ErrorCode(1_040_604_011, "来源产出行不是待检验状态");
    // ========== MES 质量管理-过程检验行（1-040-604-100） ==========
    ErrorCode QC_IPQC_LINE_NOT_EXISTS = new ErrorCode(1_040_604_100, "过程检验行不存在");

    // ========== MES 质量管理-质检缺陷记录（通用）（1-040-605-000） ==========
    ErrorCode QC_DEFECT_RECORD_NOT_EXISTS = new ErrorCode(1_040_605_000, "缺陷记录不存在");
    ErrorCode QC_DEFECT_RECORD_LEVEL_UNKNOWN = new ErrorCode(1_040_605_001, "未知的缺陷等级");
    ErrorCode QC_DEFECT_RECORD_QC_TYPE_UNSUPPORTED = new ErrorCode(1_040_605_002, "不支持的检验类型");

    // ========== MES 质量管理-检验结果（1-040-606-000） ==========
    ErrorCode QC_RESULT_NOT_EXISTS = new ErrorCode(1_040_606_000, "检验结果不存在");
    ErrorCode QC_RESULT_VALUE_FORMAT_INVALID = new ErrorCode(1_040_606_001, "检测值格式不正确：{}");
    ErrorCode QC_FINISH_INDICATOR_RESULT_REQUIRED = new ErrorCode(1_040_606_002, "完成检验单前，至少需要录入一条检测结果");

    // ========== MES 质量管理-出货检验（1-040-607-000） ==========
    ErrorCode QC_OQC_NOT_EXISTS = new ErrorCode(1_040_607_000, "出货检验单不存在");
    ErrorCode QC_OQC_CODE_DUPLICATE = new ErrorCode(1_040_607_001, "出货检验单编号已存在");
    ErrorCode QC_OQC_NOT_PREPARE = new ErrorCode(1_040_607_002, "只有草稿状态的检验单才可操作");
    ErrorCode QC_OQC_QUANTITY_MISMATCH = new ErrorCode(1_040_607_004, "合格品与不合格品数量之和须等于检测数量");
    ErrorCode QC_OQC_NO_TEMPLATE = new ErrorCode(1_040_607_005, "当前产品未配置 OQC 检测模板");
    ErrorCode QC_OQC_CHECK_RESULT_EMPTY = new ErrorCode(1_040_607_006, "完成检验单前，检测结果必须填写");
    ErrorCode QC_OQC_SOURCE_DOC_TYPE_UNKNOWN = new ErrorCode(1_040_607_007, "未知的 OQC 来源单据类型");

    // ========== MES 质量管理-出货检验行（1-040-607-100） ==========
    ErrorCode QC_OQC_LINE_NOT_EXISTS = new ErrorCode(1_040_607_100, "出货检验行不存在");

    // ========== MES 质量管理-退货检验 RQC（1-040-608-000） ==========
    ErrorCode QC_RQC_NOT_EXISTS = new ErrorCode(1_040_608_000, "退货检验单不存在");
    ErrorCode QC_RQC_CODE_DUPLICATE = new ErrorCode(1_040_608_001, "退货检验单编号已存在");
    ErrorCode QC_RQC_NOT_PREPARE = new ErrorCode(1_040_608_002, "只有草稿状态的检验单才可操作");
    ErrorCode QC_RQC_QUANTITY_MISMATCH = new ErrorCode(1_040_608_004, "合格品与不合格品数量之和须等于检测数量");
    ErrorCode QC_RQC_NO_TEMPLATE = new ErrorCode(1_040_608_005, "当前产品未配置 RQC 检测模板");
    ErrorCode QC_RQC_CHECK_RESULT_EMPTY = new ErrorCode(1_040_608_006, "完成检验单前，检测结果必须填写");
    // ========== MES 质量管理-退货检验行（1-040-608-100） ==========
    ErrorCode QC_RQC_LINE_NOT_EXISTS = new ErrorCode(1_040_608_100, "退货检验行不存在");

    // ========== MES 仓库管理-仓库（1-040-700-000） ==========
    ErrorCode WM_WAREHOUSE_NOT_EXISTS = new ErrorCode(1_040_700_000, "仓库不存在");
    ErrorCode WM_WAREHOUSE_CODE_DUPLICATE = new ErrorCode(1_040_700_001, "仓库编码已存在");
    ErrorCode WM_WAREHOUSE_NAME_DUPLICATE = new ErrorCode(1_040_700_002, "仓库名称已存在");
    ErrorCode WM_WAREHOUSE_HAS_LOCATION = new ErrorCode(1_040_700_003, "仓库下存在库区，无法删除");
    ErrorCode WM_WAREHOUSE_HAS_WORKSTATION = new ErrorCode(1_040_700_004, "仓库已被工作站引用，无法删除");
    ErrorCode WM_WAREHOUSE_HAS_MATERIAL_STOCK = new ErrorCode(1_040_700_005, "仓库下有库存记录，无法删除");
    ErrorCode WM_WAREHOUSE_IS_VIRTUAL = new ErrorCode(1_040_700_006, "虚拟仓库不允许操作");

    // ========== MES 仓库管理-库区（1-040-701-000） ==========
    ErrorCode WM_WAREHOUSE_LOCATION_NOT_EXISTS = new ErrorCode(1_040_701_000, "库区不存在");
    ErrorCode WM_WAREHOUSE_LOCATION_CODE_DUPLICATE = new ErrorCode(1_040_701_001, "同一仓库下库区编码已存在");
    ErrorCode WM_WAREHOUSE_LOCATION_NAME_DUPLICATE = new ErrorCode(1_040_701_002, "同一仓库下库区名称已存在");
    ErrorCode WM_WAREHOUSE_LOCATION_HAS_AREA = new ErrorCode(1_040_701_003, "库区下存在库位，无法删除");
    ErrorCode WM_WAREHOUSE_LOCATION_HAS_WORKSTATION = new ErrorCode(1_040_701_004, "库区已被工作站引用，无法删除");
    ErrorCode WM_WAREHOUSE_REQUIRED = new ErrorCode(1_040_701_005, "选择库区时，仓库不能为空");
    ErrorCode WM_WAREHOUSE_LOCATION_RELATION_INVALID = new ErrorCode(1_040_701_006, "库区不属于所选仓库");
    ErrorCode WM_WAREHOUSE_LOCATION_HAS_MATERIAL_STOCK = new ErrorCode(1_040_701_007, "库区下有库存记录，无法删除");
    ErrorCode WM_WAREHOUSE_LOCATION_IS_VIRTUAL = new ErrorCode(1_040_701_008, "虚拟库区不允许操作");

    // ========== MES 仓库管理-库位（1-040-702-000） ==========
    ErrorCode WM_WAREHOUSE_AREA_NOT_EXISTS = new ErrorCode(1_040_702_000, "库位不存在");
    ErrorCode WM_WAREHOUSE_AREA_CODE_DUPLICATE = new ErrorCode(1_040_702_001, "同一库区下库位编码已存在");
    ErrorCode WM_WAREHOUSE_AREA_NAME_DUPLICATE = new ErrorCode(1_040_702_002, "同一库区下库位名称已存在");
    ErrorCode WM_WAREHOUSE_AREA_HAS_WORKSTATION = new ErrorCode(1_040_702_003, "库位已被工作站引用，无法删除");
    ErrorCode WM_WAREHOUSE_LOCATION_REQUIRED = new ErrorCode(1_040_702_004, "选择库位时，库区不能为空");
    ErrorCode WM_WAREHOUSE_AREA_RELATION_INVALID = new ErrorCode(1_040_702_005, "库位不属于所选库区");
    ErrorCode WM_WAREHOUSE_AREA_HAS_MATERIAL_STOCK = new ErrorCode(1_040_702_006, "库位下有库存记录，无法删除");
    ErrorCode WM_WAREHOUSE_AREA_WAREHOUSE_MISMATCH = new ErrorCode(1_040_702_007, "库位不属于所选仓库");
    ErrorCode WM_WAREHOUSE_AREA_IS_VIRTUAL = new ErrorCode(1_040_702_008, "虚拟库位不允许操作");

    // ========== MES 仓库管理-库存（1-040-703-000） ==========
    ErrorCode WM_MATERIAL_STOCK_NOT_EXISTS = new ErrorCode(1_040_703_000, "库存记录不存在");
    ErrorCode WM_MATERIAL_STOCK_INSUFFICIENT = new ErrorCode(1_040_703_001, "库存数量不足");
    ErrorCode WM_TRANSACTION_TYPE_NOT_EXISTS = new ErrorCode(1_040_703_002, "库存事务类型不存在");
    ErrorCode WM_TRANSACTION_WAREHOUSE_FROZEN = new ErrorCode(1_040_703_003, "仓库({})已被冻结");
    ErrorCode WM_TRANSACTION_LOCATION_FROZEN = new ErrorCode(1_040_703_004, "库区({})已被冻结");
    ErrorCode WM_TRANSACTION_AREA_FROZEN = new ErrorCode(1_040_703_005, "库位({})已被冻结");
    ErrorCode WM_TRANSACTION_STOCK_FROZEN = new ErrorCode(1_040_703_006, "存放于({}/{}/{})下的物料已被冻结");
    ErrorCode WM_TRANSACTION_BATCH_REQUIRED = new ErrorCode(1_040_703_007, "当前物料启用了批次管理，批次号不能为空");
    ErrorCode WM_MATERIAL_STOCK_AREA_ITEM_MIXING_NOT_ALLOWED = new ErrorCode(1_040_703_008, "库位({})不允许物料混放，请选择其他库位");
    ErrorCode WM_MATERIAL_STOCK_AREA_BATCH_MIXING_NOT_ALLOWED = new ErrorCode(1_040_703_009, "库位({})不允许批次混放，请选择其他库位");
    ErrorCode WM_TRANSACTION_RELATED_NOT_EXISTS = new ErrorCode(1_040_703_010, "关联的库存事务不存在");
    ErrorCode WM_TRANSACTION_LIST_EMPTY = new ErrorCode(1_040_703_011, "库存事务列表不能为空");
    ErrorCode WM_TRANSACTION_BATCH_NOT_EXISTS = new ErrorCode(1_040_703_012, "批次记录不存在");
    ErrorCode WM_MATERIAL_STOCK_REQUIRED = new ErrorCode(1_040_703_013, "库存记录不能为空");
    ErrorCode WM_MATERIAL_STOCK_SELECTION_MISMATCH = new ErrorCode(1_040_703_014, "库存记录与提交的物料、批次或库位信息不一致");

    // ========== MES 仓库管理-到货通知单（1-040-704-000） ==========
    ErrorCode WM_ARRIVAL_NOTICE_NOT_EXISTS = new ErrorCode(1_040_704_000, "到货通知单不存在");
    ErrorCode WM_ARRIVAL_NOTICE_CODE_DUPLICATE = new ErrorCode(1_040_704_001, "到货通知单编码已存在");
    ErrorCode WM_ARRIVAL_NOTICE_STATUS_NOT_PREPARE = new ErrorCode(1_040_704_002, "只有草稿状态才允许此操作");
    ErrorCode WM_ARRIVAL_NOTICE_STATUS_NOT_PENDING_QC = new ErrorCode(1_040_704_003, "只有待质检状态才允许审批");
    ErrorCode WM_ARRIVAL_NOTICE_STATUS_NOT_PENDING_RECEIPT = new ErrorCode(1_040_704_004, "只有待入库状态才允许完成");
    ErrorCode WM_ARRIVAL_NOTICE_IQC_PENDING = new ErrorCode(1_040_704_005, "存在待检验行，无法审批通过");
    ErrorCode WM_ARRIVAL_NOTICE_NO_LINE = new ErrorCode(1_040_704_006, "至少需要一条行项目");
    ErrorCode WM_ARRIVAL_NOTICE_VENDOR_MISMATCH = new ErrorCode(1_040_704_007, "到货通知单的供应商与当前单据不一致");
    ErrorCode WM_ARRIVAL_NOTICE_LINE_NOT_EXISTS = new ErrorCode(1_040_704_100, "到货通知单行不存在");
    ErrorCode WM_ARRIVAL_NOTICE_LINE_NOT_MATCH = new ErrorCode(1_040_704_101, "到货通知单行不属于指定的到货通知单");

    // ========== MES 仓库管理-采购入库单（1-040-705-000） ==========
    ErrorCode WM_ITEM_RECEIPT_NOT_EXISTS = new ErrorCode(1_040_705_000, "采购入库单不存在");
    ErrorCode WM_ITEM_RECEIPT_CODE_DUPLICATE = new ErrorCode(1_040_705_001, "采购入库单编码已存在");
    ErrorCode WM_ITEM_RECEIPT_STATUS_NOT_PREPARE = new ErrorCode(1_040_705_002, "只有草稿或待上架状态才允许此操作");
    ErrorCode WM_ITEM_RECEIPT_NO_LINE = new ErrorCode(1_040_705_003, "至少需要一条行项目");
    ErrorCode WM_ITEM_RECEIPT_DETAIL_QUANTITY_MISMATCH = new ErrorCode(1_040_705_004, "明细上架总数与行入库数量不匹配");
    ErrorCode WM_ITEM_RECEIPT_STATUS_ERROR = new ErrorCode(1_040_705_005, "入库单状态不正确");
    ErrorCode WM_ITEM_RECEIPT_CANCEL_NOT_ALLOWED = new ErrorCode(1_040_705_006, "已完成或已取消的入库单不允许取消");
    ErrorCode WM_ITEM_RECEIPT_LINE_NOT_EXISTS = new ErrorCode(1_040_705_100, "采购入库单行不存在");
    ErrorCode WM_ITEM_RECEIPT_LINE_ARRIVAL_NOTICE_LINE_REQUIRED = new ErrorCode(1_040_705_101,
            "入库单关联了到货通知单，必须选择到货通知单行");
    ErrorCode WM_ITEM_RECEIPT_LINE_ARRIVAL_NOTICE_LINE_NOT_ALLOWED = new ErrorCode(1_040_705_102,
            "入库单未关联到货通知单，不能选择到货通知单行");
    ErrorCode WM_ITEM_RECEIPT_DETAIL_NOT_EXISTS = new ErrorCode(1_040_705_200, "采购入库明细不存在");
    ErrorCode WM_ITEM_RECEIPT_DETAIL_QUANTITY_EXCEED = new ErrorCode(1_040_705_202, "上架明细总数量不能超过行入库数量");

    // ========== MES 仓库管理-领料申请单（1-040-706-000） ==========
    ErrorCode WM_MATERIAL_REQUEST_NOT_EXISTS = new ErrorCode(1_040_706_000, "领料申请单不存在");
    ErrorCode WM_MATERIAL_REQUEST_STATUS_INVALID = new ErrorCode(1_040_706_001, "领料申请单状态不正确，无法执行该操作");
    ErrorCode WM_MATERIAL_REQUEST_LINE_NOT_EXISTS = new ErrorCode(1_040_706_100, "领料申请单行不存在");

    // ========== MES 仓库管理-外协发料单（1-040-707-000） ==========
    ErrorCode WM_OUTSOURCE_ISSUE_NOT_EXISTS = new ErrorCode(1_040_707_000, "外协发料单不存在");
    ErrorCode WM_OUTSOURCE_ISSUE_CODE_DUPLICATE = new ErrorCode(1_040_707_001, "外协发料单编码已存在");
    ErrorCode WM_OUTSOURCE_ISSUE_STATUS_NOT_PREPARE = new ErrorCode(1_040_707_002, "只有草稿状态才允许此操作");
    ErrorCode WM_OUTSOURCE_ISSUE_NO_LINE = new ErrorCode(1_040_707_003, "至少需要一条发料行");
    ErrorCode WM_OUTSOURCE_ISSUE_QUANTITY_MISMATCH = new ErrorCode(1_040_707_004, "发料单行数量与明细数量不一致");
    ErrorCode WM_OUTSOURCE_ISSUE_STATUS_NOT_APPROVING = new ErrorCode(1_040_707_005, "只有待拣货状态才允许此操作");
    ErrorCode WM_OUTSOURCE_ISSUE_STATUS_NOT_APPROVED = new ErrorCode(1_040_707_006, "只有待执行出库状态才允许此操作");
    ErrorCode WM_OUTSOURCE_ISSUE_CANCEL_NOT_ALLOWED = new ErrorCode(1_040_707_007, "已完成或已取消的发料单不允许取消");
    ErrorCode WM_OUTSOURCE_ISSUE_LINE_NOT_EXISTS = new ErrorCode(1_040_707_100, "外协发料单行不存在");
    ErrorCode WM_OUTSOURCE_ISSUE_LINE_ITEM_NOT_IN_BOM = new ErrorCode(1_040_707_101, "发料单行对应的物料不在当前工单的 BOM 列表中");
    ErrorCode WM_OUTSOURCE_ISSUE_DETAIL_NOT_EXISTS = new ErrorCode(1_040_707_200, "外协发料单明细不存在");
    ErrorCode WM_OUTSOURCE_ISSUE_DETAIL_LINE_NOT_MATCH = new ErrorCode(1_040_707_201, "拣货明细不属于指定的外协发料单");
    ErrorCode WM_OUTSOURCE_ISSUE_DETAIL_ITEM_MISMATCH = new ErrorCode(1_040_707_202, "拣货明细的物料与外协发料单行的物料不一致");
    ErrorCode WM_OUTSOURCE_ISSUE_WORK_ORDER_TYPE_INVALID = new ErrorCode(1_040_707_008, "工单类型不是外协（代工）类型");

    // ========== MES 仓库管理-生产领料出库单（1-040-708-000） ==========
    ErrorCode WM_PRODUCT_ISSUE_NOT_EXISTS = new ErrorCode(1_040_708_000, "生产领料出库单不存在");
    ErrorCode WM_PRODUCT_ISSUE_STATUS_INVALID = new ErrorCode(1_040_708_001, "生产领料出库单状态不正确，无法执行该操作");
    ErrorCode WM_PRODUCT_ISSUE_NO_LINE = new ErrorCode(1_040_708_002, "生产领料出库单至少需要一条行数据");
    ErrorCode WM_PRODUCT_ISSUE_DETAIL_QUANTITY_MISMATCH = new ErrorCode(1_040_708_003, "领料出库单行数量与明细数量不一致");
    ErrorCode WM_PRODUCT_ISSUE_WORKORDER_NOT_EXISTS = new ErrorCode(1_040_708_004, "生产工单不存在");
    ErrorCode WM_PRODUCT_ISSUE_WORKSTATION_NOT_EXISTS = new ErrorCode(1_040_708_005, "工作站不存在");
    ErrorCode WM_PRODUCT_ISSUE_CANCEL_NOT_ALLOWED = new ErrorCode(1_040_708_006, "生产领料出库单已完成或已取消，无法取消");
    ErrorCode WM_PRODUCT_ISSUE_LINE_NOT_EXISTS = new ErrorCode(1_040_708_100, "生产领料出库单行不存在");
    ErrorCode WM_PRODUCT_ISSUE_LINE_ITEM_NOT_IN_BOM = new ErrorCode(1_040_708_101, "当前物料不在生产工单的 BOM 物料清单中");
    ErrorCode WM_PRODUCT_ISSUE_CODE_DUPLICATE = new ErrorCode(1_040_708_102, "领料出库单编码已存在");
    ErrorCode WM_PRODUCT_ISSUE_DETAIL_NOT_EXISTS = new ErrorCode(1_040_708_200, "生产领料出库单明细不存在");
    ErrorCode WM_PRODUCT_ISSUE_DETAIL_ITEM_MISMATCH = new ErrorCode(1_040_708_201, "拣货明细的物料与领料单行的物料不一致");
    ErrorCode WM_PRODUCT_ISSUE_DETAIL_LINE_NOT_MATCH = new ErrorCode(1_040_708_202, "拣货明细不属于指定的领料出库单");
    ErrorCode WM_PRODUCT_ISSUE_NO_DETAIL = new ErrorCode(1_040_708_203, "领料出库单没有拣货明细，无法执行领出");

    // ========== MES 仓库管理-生产入库单（1-040-709-000） ==========
    ErrorCode WM_PRODUCT_PRODUCE_NOT_EXISTS = new ErrorCode(1_040_709_000, "生产入库单不存在");
    ErrorCode WM_PRODUCT_PRODUCE_STATUS_INVALID = new ErrorCode(1_040_709_001, "生产入库单状态不正确，无法执行该操作");
    ErrorCode WM_PRODUCT_PRODUCE_NO_LINE = new ErrorCode(1_040_709_002, "生产入库单至少需要一条行数据");
    ErrorCode WM_PRODUCT_PRODUCE_DETAIL_QUANTITY_MISMATCH = new ErrorCode(1_040_709_003, "生产入库单行数量与明细数量不一致");
    ErrorCode WM_PRODUCT_PRODUCE_CANCEL_NOT_ALLOWED = new ErrorCode(1_040_709_004, "生产入库单已完成或已取消，无法取消");
    ErrorCode WM_PRODUCT_PRODUCE_BATCH_REQUIRED = new ErrorCode(1_040_709_005, "生产入库单批次号不能为空，无法回填生产工单");
    ErrorCode WM_PRODUCT_PRODUCE_BATCH_AMBIGUOUS = new ErrorCode(1_040_709_006, "生产入库单存在多个批次号，无法回填生产工单");
    ErrorCode WM_PRODUCT_PRODUCE_LINE_NOT_EXISTS = new ErrorCode(1_040_709_100, "生产入库单行不存在");
    ErrorCode WM_PRODUCT_PRODUCE_DETAIL_NOT_EXISTS = new ErrorCode(1_040_709_200, "生产入库单明细不存在");

    // ========== MES 仓库管理-转移调拨（1-040-710-000） ==========
    ErrorCode WM_TRANSFER_NOT_EXISTS = new ErrorCode(1_040_710_000, "转移单不存在");
    ErrorCode WM_TRANSFER_NOT_EDITABLE = new ErrorCode(1_040_710_001, "当前转移单状态不允许编辑");
    ErrorCode WM_TRANSFER_CODE_DUPLICATE = new ErrorCode(1_040_710_002, "转移单编号已存在");
    ErrorCode WM_TRANSFER_NOT_DRAFT = new ErrorCode(1_040_710_003, "只有草稿状态的转移单才可操作");
    ErrorCode WM_TRANSFER_NOT_CONFIRMED = new ErrorCode(1_040_710_004, "只有待确认状态的转移单才可执行确认");
    ErrorCode WM_TRANSFER_NOT_APPROVING = new ErrorCode(1_040_710_005, "只有待上架状态的转移单才可执行上架");
    ErrorCode WM_TRANSFER_NOT_APPROVED = new ErrorCode(1_040_710_006, "只有待执行状态的转移单才可完成");
    ErrorCode WM_TRANSFER_ALREADY_FINISHED = new ErrorCode(1_040_710_007, "转移单已完成或已取消，无法继续操作");
    ErrorCode WM_TRANSFER_NO_LINE = new ErrorCode(1_040_710_008, "转移单至少需要一条行数据");
    ErrorCode WM_TRANSFER_DETAIL_QUANTITY_MISMATCH = new ErrorCode(1_040_710_009, "转移单行数量与明细数量不一致");
    ErrorCode WM_TRANSFER_LINE_NOT_EXISTS = new ErrorCode(1_040_710_100, "转移单行不存在");
    ErrorCode WM_TRANSFER_LINE_QUANTITY_EXCEED_STOCK = new ErrorCode(1_040_710_101, "转移数量不能超过库存数量");
    ErrorCode WM_TRANSFER_DETAIL_NOT_EXISTS = new ErrorCode(1_040_710_200, "调拨明细不存在");
    ErrorCode WM_TRANSFER_DETAIL_QUANTITY_EXCEED = new ErrorCode(1_040_710_201, "调拨明细总数量不能超过调拨单行数量");
    ErrorCode WM_TRANSFER_DETAIL_MIXED_GOODS = new ErrorCode(1_040_710_202, "同一目标仓位下已存在其他物料的明细，不允许混货");

    // ========== MES 仓库管理-生产退料单（1-040-711-000） ==========
    ErrorCode WM_RETURN_ISSUE_NOT_EXISTS = new ErrorCode(1_040_710_000, "生产退料单不存在");
    ErrorCode WM_RETURN_ISSUE_STATUS_INVALID = new ErrorCode(1_040_710_001, "生产退料单状态不正确，无法执行该操作");
    ErrorCode WM_RETURN_ISSUE_NOT_PREPARE = new ErrorCode(1_040_710_002, "只有草稿状态的退料单才可操作");
    ErrorCode WM_RETURN_ISSUE_NOT_CONFIRMED = new ErrorCode(1_040_710_003, "只有待检验状态的退料单才可提交");
    ErrorCode WM_RETURN_ISSUE_NOT_APPROVING = new ErrorCode(1_040_710_004, "只有待上架状态的退料单才可入库上架");
    ErrorCode WM_RETURN_ISSUE_NOT_APPROVED = new ErrorCode(1_040_710_005, "只有待执行退料状态的退料单才可完成");
    ErrorCode WM_RETURN_ISSUE_NO_LINE = new ErrorCode(1_040_710_006, "生产退料单至少需要一条行数据");
    ErrorCode WM_RETURN_ISSUE_DETAIL_QUANTITY_MISMATCH = new ErrorCode(1_040_710_007, "退料单行数量与明细数量不一致");
    ErrorCode WM_RETURN_ISSUE_CANCEL_NOT_ALLOWED = new ErrorCode(1_040_710_008, "生产退料单已完成或已取消，无法取消");
    ErrorCode WM_RETURN_ISSUE_LINE_NOT_EXISTS = new ErrorCode(1_040_710_100, "生产退料单行不存在");
    ErrorCode WM_RETURN_ISSUE_DETAIL_NOT_EXISTS = new ErrorCode(1_040_710_200, "生产退料单明细不存在");
    ErrorCode WM_RETURN_ISSUE_DETAIL_QUANTITY_INVALID = new ErrorCode(1_040_710_201, "退料明细数量必须大于0");
    ErrorCode WM_RETURN_ISSUE_DETAIL_QUANTITY_EXCEED = new ErrorCode(1_040_710_202, "退料明细总数量不能超过退料单行数量");
    ErrorCode WM_RETURN_ISSUE_CODE_DUPLICATE = new ErrorCode(1_040_710_203, "退料单编码已存在");
    ErrorCode WM_RETURN_ISSUE_DETAIL_LINE_NOT_MATCH = new ErrorCode(1_040_710_204, "退料明细不属于指定的退料单");
    ErrorCode WM_RETURN_ISSUE_DETAIL_ITEM_MISMATCH = new ErrorCode(1_040_710_205, "退料明细的物料与退料单行的物料不一致");

    // ========== MES 仓库管理-供应商退货单（1-040-711-000） ==========
    ErrorCode WM_RETURN_VENDOR_NOT_EXISTS = new ErrorCode(1_040_711_000, "供应商退货单不存在");
    ErrorCode WM_RETURN_VENDOR_STATUS_INVALID = new ErrorCode(1_040_711_001, "供应商退货单状态不正确，无法执行该操作");
    ErrorCode WM_RETURN_VENDOR_NO_LINE = new ErrorCode(1_040_711_002, "供应商退货单至少需要一条行数据");
    ErrorCode WM_RETURN_VENDOR_DETAIL_QUANTITY_MISMATCH = new ErrorCode(1_040_711_003, "供应商退货单行数量与明细数量不一致");
    ErrorCode WM_RETURN_VENDOR_CANCEL_NOT_ALLOWED = new ErrorCode(1_040_711_004, "供应商退货单已完成或已取消，无法取消");
    ErrorCode WM_RETURN_VENDOR_CODE_DUPLICATE = new ErrorCode(1_040_711_005, "供应商退货单编号已存在");
    ErrorCode WM_RETURN_VENDOR_NO_DETAIL = new ErrorCode(1_040_711_006, "供应商退货单没有拣货明细，无法执行退货");
    ErrorCode WM_RETURN_VENDOR_DETAIL_ITEM_MISMATCH = new ErrorCode(1_040_711_007, "拣货明细的物料与退货单行的物料不一致");
    ErrorCode WM_RETURN_VENDOR_LINE_NOT_EXISTS = new ErrorCode(1_040_711_100, "供应商退货单行不存在");
    ErrorCode WM_RETURN_VENDOR_DETAIL_NOT_EXISTS = new ErrorCode(1_040_711_200, "供应商退货单明细不存在");
    ErrorCode WM_RETURN_VENDOR_DETAIL_QUANTITY_INVALID = new ErrorCode(1_040_711_201, "退货明细数量必须大于 0");
    ErrorCode WM_RETURN_VENDOR_DETAIL_LINE_NOT_MATCH = new ErrorCode(1_040_711_202, "拣货明细不属于指定的供应商退货单");

    // ========== MES 仓库管理-产品收货单（1-040-712-000） ==========
    ErrorCode WM_PRODUCT_RECPT_NOT_EXISTS = new ErrorCode(1_040_712_000, "产品收货单不存在");
    ErrorCode WM_PRODUCT_RECPT_CODE_DUPLICATE = new ErrorCode(1_040_712_001, "产品收货单编码已存在");
    ErrorCode WM_PRODUCT_RECPT_STATUS_NOT_PREPARE = new ErrorCode(1_040_712_002, "只有草稿或待上架状态才允许此操作");
    ErrorCode WM_PRODUCT_RECPT_NO_LINE = new ErrorCode(1_040_712_003, "至少需要一条行项目");
    ErrorCode WM_PRODUCT_RECPT_DETAIL_QUANTITY_MISMATCH = new ErrorCode(1_040_712_004, "明细上架总数与行收货数量不匹配");
    ErrorCode WM_PRODUCT_RECPT_STATUS_ERROR = new ErrorCode(1_040_712_005, "收货单状态不正确");
    ErrorCode WM_PRODUCT_RECPT_CANCEL_NOT_ALLOWED = new ErrorCode(1_040_712_006, "已完成或已取消的收货单不允许取消");
    ErrorCode WM_PRODUCT_RECPT_NO_DETAIL = new ErrorCode(1_040_712_007, "收货单没有上架明细，无法执行入库");
    ErrorCode WM_PRODUCT_RECPT_LINE_NOT_EXISTS = new ErrorCode(1_040_712_100, "产品收货单行不存在");
    ErrorCode WM_PRODUCT_RECPT_DETAIL_NOT_EXISTS = new ErrorCode(1_040_712_200, "产品收货明细不存在");

    // ========== MES 仓库管理-外协入库单（1-040-713-000） ==========
    ErrorCode WM_OUTSOURCE_RECEIPT_NOT_EXISTS = new ErrorCode(1_040_713_000, "外协入库单不存在");
    ErrorCode WM_OUTSOURCE_RECEIPT_CODE_DUPLICATE = new ErrorCode(1_040_713_001, "外协入库单编码已存在");
    ErrorCode WM_OUTSOURCE_RECEIPT_STATUS_NOT_PREPARE = new ErrorCode(1_040_713_002, "只有草稿状态才允许此操作");
    ErrorCode WM_OUTSOURCE_RECEIPT_NO_LINE = new ErrorCode(1_040_713_003, "至少需要一条行项目");
    ErrorCode WM_OUTSOURCE_RECEIPT_DETAIL_QUANTITY_MISMATCH = new ErrorCode(1_040_713_004, "明细上架总数与行入库数量不匹配");
    ErrorCode WM_OUTSOURCE_RECEIPT_STATUS_ERROR = new ErrorCode(1_040_713_005, "入库单状态不正确");
    ErrorCode WM_OUTSOURCE_RECEIPT_CANCEL_NOT_ALLOWED = new ErrorCode(1_040_713_006, "已完成或已取消的入库单不允许取消");
    ErrorCode WM_OUTSOURCE_RECEIPT_LINE_NOT_EXISTS = new ErrorCode(1_040_713_100, "外协入库单行不存在");
    ErrorCode WM_OUTSOURCE_RECEIPT_DETAIL_NOT_EXISTS = new ErrorCode(1_040_713_200, "外协入库明细不存在");

    // ========== MES 仓库管理-销售退货单（1-040-713-000） ==========
    ErrorCode WM_RETURN_SALES_NOT_EXISTS = new ErrorCode(1_040_713_000, "销售退货单不存在");
    ErrorCode WM_RETURN_SALES_CODE_DUPLICATE = new ErrorCode(1_040_713_001, "销售退货单编码已存在");
    ErrorCode WM_RETURN_SALES_STATUS_NOT_PREPARE = new ErrorCode(1_040_713_002, "只有草稿状态才允许此操作");
    ErrorCode WM_RETURN_SALES_STATUS_NOT_APPROVING = new ErrorCode(1_040_713_003, "只有待执行状态才允许执行退货");
    ErrorCode WM_RETURN_SALES_STATUS_NOT_APPROVED = new ErrorCode(1_040_713_004, "只有待上架状态才允许执行上架");
    ErrorCode WM_RETURN_SALES_NO_LINE = new ErrorCode(1_040_713_005, "销售退货单至少需要一条行数据");
    ErrorCode WM_RETURN_SALES_DETAIL_QUANTITY_MISMATCH = new ErrorCode(1_040_713_006, "销售退货单行数量与明细数量不一致");
    ErrorCode WM_RETURN_SALES_CANCEL_NOT_ALLOWED = new ErrorCode(1_040_713_007, "销售退货单已完成或已取消，无法取消");
    ErrorCode WM_RETURN_SALES_LINE_NOT_EXISTS = new ErrorCode(1_040_713_100, "销售退货单行不存在");
    ErrorCode WM_RETURN_SALES_DETAIL_NOT_EXISTS = new ErrorCode(1_040_713_200, "销售退货单明细不存在");
    ErrorCode WM_RETURN_SALES_DETAIL_QUANTITY_EXCEED = new ErrorCode(1_040_713_201, "上架明细总数量不能超过退货行数量");

    // ========== MES 仓库管理-盘点方案/任务/结果（1-040-714-100） ==========
    ErrorCode WM_STOCK_TAKING_PLAN_NOT_EXISTS = new ErrorCode(1_040_714_100, "盘点方案不存在");
    ErrorCode WM_STOCK_TAKING_PLAN_CODE_DUPLICATE = new ErrorCode(1_040_714_101, "盘点方案编码已存在");
    ErrorCode WM_STOCK_TAKING_PLAN_NOT_DISABLED = new ErrorCode(1_040_714_102, "只有禁用状态的盘点方案才允许修改、删除或维护参数");
    ErrorCode WM_STOCK_TAKING_PLAN_NOT_ENABLED = new ErrorCode(1_040_714_103, "只有启用状态的盘点方案才允许生成任务");
    ErrorCode WM_STOCK_TAKING_PLAN_PARAM_NOT_EXISTS = new ErrorCode(1_040_714_104, "盘点方案参数不存在");
    ErrorCode WM_STOCK_TAKING_PLAN_PARAM_EMPTY = new ErrorCode(1_040_714_105, "盘点方案参数不能为空，请先配置盘点参数");
    ErrorCode WM_STOCK_TAKING_PLAN_DYNAMIC_TIME_INVALID = new ErrorCode(1_040_714_106, "动态盘点方案必须设置开始时间和结束时间，且结束时间必须晚于开始时间");
    ErrorCode WM_STOCK_TAKING_TASK_NOT_EXISTS = new ErrorCode(1_040_714_110, "盘点任务不存在");
    ErrorCode WM_STOCK_TAKING_TASK_CODE_DUPLICATE = new ErrorCode(1_040_714_111, "盘点任务编码已存在");
    ErrorCode WM_STOCK_TAKING_TASK_NOT_PREPARE = new ErrorCode(1_040_714_112, "只有草稿状态的盘点任务才允许此操作");
    ErrorCode WM_STOCK_TAKING_TASK_NOT_APPROVING = new ErrorCode(1_040_714_113, "只有盘点中状态的任务才允许此操作");
    ErrorCode WM_STOCK_TAKING_TASK_CANNOT_CANCEL = new ErrorCode(1_040_714_114, "已完成或已取消的盘点任务不允许取消");
    ErrorCode WM_STOCK_TAKING_TASK_NO_STOCK = new ErrorCode(1_040_714_115, "未找到符合条件的库存数据");
    ErrorCode WM_STOCK_TAKING_TASK_NO_LINE = new ErrorCode(1_040_714_116, "盘点任务至少需要一条任务行");
    ErrorCode WM_STOCK_TAKING_TASK_LINE_NOT_EXISTS = new ErrorCode(1_040_714_117, "盘点任务行不存在");
    ErrorCode WM_STOCK_TAKING_TASK_DYNAMIC_TIME_REQUIRED = new ErrorCode(1_040_714_118, "动态盘点必须选择开始时间和结束时间");
    ErrorCode WM_STOCK_TAKING_TASK_RESULT_NOT_EXISTS = new ErrorCode(1_040_714_119, "盘点结果不存在");
    ErrorCode WM_STOCK_TAKING_TASK_LINE_ALREADY_TAKEN = new ErrorCode(1_040_714_120, "该盘点清单行已有盘点记录，不能重复盘点");

    // ========== MES 仓库管理-销售出库单（1-040-714-000） ==========
    ErrorCode WM_PRODUCT_SALES_NOT_EXISTS = new ErrorCode(1_040_714_000, "销售出库单不存在");
    ErrorCode WM_PRODUCT_SALES_CODE_DUPLICATE = new ErrorCode(1_040_714_001, "销售出库单号已存在");
    ErrorCode WM_PRODUCT_SALES_NOT_PREPARE = new ErrorCode(1_040_714_002, "只有草稿状态才可操作");
    ErrorCode WM_PRODUCT_SALES_LINES_EMPTY = new ErrorCode(1_040_714_003, "销售出库单行不能为空");
    ErrorCode WM_PRODUCT_SALES_CANNOT_SUBMIT = new ErrorCode(1_040_714_004, "当前状态不允许提交");
    ErrorCode WM_PRODUCT_SALES_CANNOT_PICK = new ErrorCode(1_040_714_005, "当前状态不允许拣货");
    ErrorCode WM_PRODUCT_SALES_CANNOT_SHIPPING = new ErrorCode(1_040_714_006, "当前状态不允许填写运单");
    ErrorCode WM_PRODUCT_SALES_CANNOT_FINISH = new ErrorCode(1_040_714_007, "当前状态不允许执行出库");
    ErrorCode WM_PRODUCT_SALES_CANNOT_CANCEL = new ErrorCode(1_040_714_008, "当前状态不允许取消");
    ErrorCode WM_PRODUCT_SALES_CANNOT_CONFIRM = new ErrorCode(1_040_714_009, "当前状态不允许确认检验通过");
    ErrorCode WM_PRODUCT_SALES_DETAILS_EMPTY = new ErrorCode(1_040_714_020, "拣货明细不能为空");
    ErrorCode WM_PRODUCT_SALES_DETAIL_QUANTITY_MISMATCH = new ErrorCode(1_040_714_010, "拣货数量与出库数量不匹配");
    ErrorCode WM_PRODUCT_SALES_LINE_NOT_EXISTS = new ErrorCode(1_040_714_011, "销售出库单行不存在");
    ErrorCode WM_PRODUCT_SALES_DETAIL_NOT_EXISTS = new ErrorCode(1_040_714_012, "销售出库明细不存在");
    ErrorCode WM_PRODUCT_SALES_STOCK_INSUFFICIENT = new ErrorCode(1_040_714_013, "库存不足，无法拣货");
    ErrorCode WM_PRODUCT_SALES_LINE_QUANTITY_INVALID = new ErrorCode(1_040_714_014, "出库数量必须大于 0");
    ErrorCode WM_PRODUCT_SALES_DETAIL_LINE_NOT_MATCH = new ErrorCode(1_040_714_015, "拣货明细不属于指定的销售出库单");
    ErrorCode WM_PRODUCT_SALES_DETAIL_ITEM_MISMATCH = new ErrorCode(1_040_714_016, "拣货明细的物料与销售出库单行的物料不一致");
    ErrorCode WM_PRODUCT_SALES_LINE_SALES_NOTICE_LINE_REQUIRED = new ErrorCode(1_040_714_017,
            "出库单关联了发货通知单，必须选择发货通知单行");
    ErrorCode WM_PRODUCT_SALES_LINE_SALES_NOTICE_LINE_NOT_ALLOWED = new ErrorCode(1_040_714_018,
            "出库单未关联发货通知单，不能选择发货通知单行");
    ErrorCode WM_PRODUCT_SALES_LINE_NOTICE_LINE_ITEM_MISMATCH = new ErrorCode(1_040_714_030,
            "出库行物料与发货通知单行物料不一致");
    ErrorCode WM_PRODUCT_SALES_LINE_NOTICE_LINE_QUANTITY_MISMATCH = new ErrorCode(1_040_714_031,
            "出库行数量与发货通知单行数量不一致");
    ErrorCode WM_PRODUCT_SALES_LINE_NOTICE_LINE_BATCH_MISMATCH = new ErrorCode(1_040_714_032,
            "出库行批次号与发货通知单行批次号不一致");
    ErrorCode WM_PRODUCT_SALES_LINE_NOTICE_LINE_OQC_MISMATCH = new ErrorCode(1_040_714_033,
            "出库行 OQC 检验标识与发货通知单行不一致");

    // ========== MES 仓库管理-杂项出库单（1-040-715-000） ==========
    ErrorCode WM_MISC_ISSUE_NOT_EXISTS = new ErrorCode(1_040_715_000, "杂项出库单不存在");
    ErrorCode WM_MISC_ISSUE_CODE_DUPLICATE = new ErrorCode(1_040_715_001, "杂项出库单编码已存在");
    ErrorCode WM_MISC_ISSUE_STATUS_INVALID = new ErrorCode(1_040_715_002, "杂项出库单状态不正确，无法执行该操作");
    ErrorCode WM_MISC_ISSUE_NO_LINE = new ErrorCode(1_040_715_003, "杂项出库单至少需要一条行数据");
    ErrorCode WM_MISC_ISSUE_CANCEL_NOT_ALLOWED = new ErrorCode(1_040_715_004, "杂项出库单已完成或已取消，无法取消");
    ErrorCode WM_MISC_ISSUE_DETAIL_QUANTITY_MISMATCH = new ErrorCode(1_040_715_005, "杂项出库单行数量与明细数量不一致");
    ErrorCode WM_MISC_ISSUE_LINE_NOT_EXISTS = new ErrorCode(1_040_715_100, "杂项出库单行不存在");
    ErrorCode WM_MISC_ISSUE_DETAIL_NOT_EXISTS = new ErrorCode(1_040_715_200, "杂项出库单明细不存在");

    // ========== MES 仓库管理-杂项入库单（1-040-716-000） ==========
    ErrorCode WM_MISC_RECEIPT_NOT_EXISTS = new ErrorCode(1_040_716_000, "杂项入库单不存在");
    ErrorCode WM_MISC_RECEIPT_CODE_DUPLICATE = new ErrorCode(1_040_716_001, "杂项入库单编码已存在");
    ErrorCode WM_MISC_RECEIPT_STATUS_NOT_PREPARE = new ErrorCode(1_040_716_002, "只有草稿状态才允许此操作");
    ErrorCode WM_MISC_RECEIPT_STATUS_NOT_APPROVED = new ErrorCode(1_040_716_003, "只有已审批状态才允许执行入库");
    ErrorCode WM_MISC_RECEIPT_NO_LINE = new ErrorCode(1_040_716_004, "至少需要一条行项目");
    ErrorCode WM_MISC_RECEIPT_CANCEL_NOT_ALLOWED = new ErrorCode(1_040_716_005, "已完成或已取消的入库单不允许取消");
    ErrorCode WM_MISC_RECEIPT_LINE_NOT_EXISTS = new ErrorCode(1_040_716_100, "杂项入库单行不存在");
    ErrorCode WM_MISC_RECEIPT_WAREHOUSE_REQUIRED = new ErrorCode(1_040_716_101, "仓库不能为空");
    ErrorCode WM_MISC_RECEIPT_QUANTITY_INVALID = new ErrorCode(1_040_716_102, "入库数量必须大于 0");
    ErrorCode WM_MISC_RECEIPT_DETAIL_NOT_EXISTS = new ErrorCode(1_040_716_200, "杂项入库单明细不存在");

    // ========== MES 仓库管理-发货通知单（1-040-720-000） ==========
    ErrorCode WM_SALES_NOTICE_NOT_EXISTS = new ErrorCode(1_040_720_000, "发货通知单不存在");
    ErrorCode WM_SALES_NOTICE_CODE_DUPLICATE = new ErrorCode(1_040_720_001, "通知单编号重复");
    ErrorCode WM_SALES_NOTICE_STATUS_NOT_ALLOW_DELETE = new ErrorCode(1_040_720_002, "单据状态不允许删除");
    ErrorCode WM_SALES_NOTICE_STATUS_NOT_ALLOW_UPDATE = new ErrorCode(1_040_720_003, "单据状态不允许修改");
    ErrorCode WM_SALES_NOTICE_STATUS_NOT_APPROVED = new ErrorCode(1_040_720_004, "发货通知单不是待出库状态");
    ErrorCode WM_SALES_NOTICE_CLIENT_MISMATCH = new ErrorCode(1_040_720_005, "发货通知单的客户与当前单据不一致");
    ErrorCode WM_SALES_NOTICE_LINE_NOT_EXISTS = new ErrorCode(1_040_720_010, "发货通知单行不存在");
    ErrorCode WM_SALES_NOTICE_LINE_EMPTY = new ErrorCode(1_040_720_011, "发货通知单行为空，不能提交");
    ErrorCode WM_SALES_NOTICE_LINE_NOT_MATCH = new ErrorCode(1_040_720_012, "发货通知单行不属于指定的发货通知单");

    // ========== MES 仓库管理-条码配置（1-040-730-000） ==========
    ErrorCode WM_BARCODE_CONFIG_NOT_EXISTS = new ErrorCode(1_040_730_000, "条码配置不存在");
    ErrorCode WM_BARCODE_CONFIG_BIZ_TYPE_DUPLICATE = new ErrorCode(1_040_730_001, "该业务类型的条码配置已存在");
    ErrorCode WM_BARCODE_CONFIG_HAS_BARCODE = new ErrorCode(1_040_730_002, "该条码配置已被条码记录关联，无法删除");

    // ========== MES 仓库管理-条码清单（1-040-731-000） ==========
    ErrorCode WM_BARCODE_NOT_EXISTS = new ErrorCode(1_040_731_000, "条码不存在");
    ErrorCode WM_BARCODE_ALREADY_EXISTS = new ErrorCode(1_040_731_001, "该业务对象的条码已存在");
    ErrorCode WM_BARCODE_CONTENT_DUPLICATE = new ErrorCode(1_040_731_002, "条码内容已存在");
    ErrorCode BARCODE_BIZ_TYPE_NOT_EXISTS = new ErrorCode(1_040_731_003, "业务类型不能为空");
    ErrorCode BARCODE_BIZ_CODE_NOT_EXISTS = new ErrorCode(1_040_731_004, "业务编码不能为空");
    ErrorCode BARCODE_CONFIG_NOT_EXISTS = new ErrorCode(1_040_731_005, "条码配置不存在");

    // ========== MES 仓库管理-装箱单（1-040-740-000） ==========
    ErrorCode WM_PACKAGE_NOT_EXISTS = new ErrorCode(1_040_740_000, "装箱单不存在");
    ErrorCode WM_PACKAGE_CODE_DUPLICATE = new ErrorCode(1_040_740_001, "装箱单编码已存在");
    ErrorCode WM_PACKAGE_STATUS_NOT_PREPARE = new ErrorCode(1_040_740_002, "只有草稿状态才允许此操作");
    ErrorCode WM_PACKAGE_PARENT_NOT_EXISTS = new ErrorCode(1_040_740_003, "父箱不存在");
    ErrorCode WM_PACKAGE_PARENT_SELF = new ErrorCode(1_040_740_004, "不能选择自己作为父箱");
    ErrorCode WM_PACKAGE_CHILD_HAS_PARENT = new ErrorCode(1_040_740_005, "该装箱单已有父箱，不能重复添加");
    ErrorCode WM_PACKAGE_PARENT_IS_CHILD = new ErrorCode(1_040_740_006, "不能选择子箱的后代作为父箱，会形成环路");
    ErrorCode WM_PACKAGE_CHILD_NOT_FINISHED = new ErrorCode(1_040_740_007, "子箱必须是已完成状态才能添加");
    ErrorCode WM_PACKAGE_HAS_CHILDREN = new ErrorCode(1_040_740_008, "存在子箱，不允许删除");
    ErrorCode WM_PACKAGE_LINE_NOT_EXISTS = new ErrorCode(1_040_740_100, "装箱明细不存在");

    ErrorCode MD_WORKSHOP_CHARGE_POST_NOT_READY = new ErrorCode(1_040_105_004, "车间负责人岗位 {} 不存在或未启用，请先创建并启用该岗位");
    ErrorCode MD_WORKSHOP_CHARGE_USER_EMPTY = new ErrorCode(1_040_105_005, "车间负责人岗位 {} 下没有启用用户，请先分配启用用户");

    // ========== MES 鎵瑰鐞嗘ā鏉匡紙1-040-750-000锛?==========
    ErrorCode PRO_BATCH_RECORD_IMPORT_NOT_EXISTS = new ErrorCode(1_040_750_000, "鎵瑰鐞嗗鍏ヨ褰曚笉瀛樺湪");
    ErrorCode PRO_BATCH_RECORD_IMPORT_FILE_REQUIRED = new ErrorCode(1_040_750_001, "鎵瑰鐞嗘枃浠朵笉鑳戒负绌?");
    ErrorCode PRO_BATCH_RECORD_IMPORT_FILE_EXTENSION_INVALID = new ErrorCode(1_040_750_002, "浠呮敮鎸?doc 鎴?docx 鎵瑰鐞嗘枃浠?");
    ErrorCode PRO_BATCH_RECORD_IMPORT_PARSE_FAILED = new ErrorCode(1_040_750_003, "鎵瑰鐞嗘枃浠惰В鏋愬け璐?");
    ErrorCode PRO_BATCH_RECORD_IMPORT_PARSE_TIMEOUT = new ErrorCode(1_040_750_004, "鎵瑰鐞嗘枃浠惰В鏋愯秴鏃?");
    ErrorCode PRO_BATCH_RECORD_IMPORT_STATUS_INVALID = new ErrorCode(1_040_750_005, "鎵瑰鐞嗗鍏ョ姸鎬佷笉鍏佽褰撳墠鎿嶄綔");
    ErrorCode PRO_BATCH_RECORD_IMPORT_CANDIDATE_EMPTY = new ErrorCode(1_040_750_006, "鎵瑰鐞嗘枃浠舵湭瑙ｆ瀽鍑轰换浣曟ā鏉垮€欓€夐」");
    ErrorCode PRO_BATCH_RECORD_IMPORT_COMMIT_EMPTY = new ErrorCode(1_040_750_007, "鑷冲皯闇€瑕侀€夋嫨涓€涓壒澶勭悊妯℃澘");
    ErrorCode PRO_BATCH_RECORD_IMPORT_TABLE_NOT_EXISTS = new ErrorCode(1_040_750_008, "鎸囧畾鐨勬潵婧愯〃鏍间笉瀛樺湪");
    ErrorCode PRO_BATCH_RECORD_TEMPLATE_NOT_EXISTS = new ErrorCode(1_040_750_100, "鎵瑰鐞嗘ā鏉夸笉瀛樺湪");
    ErrorCode PRO_BATCH_RECORD_TEMPLATE_CODE_DUPLICATE = new ErrorCode(1_040_750_101, "鎵瑰鐞嗘ā鏉跨紪鐮佸凡瀛樺湪");
    ErrorCode PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_CANDIDATE_EMPTY = new ErrorCode(1_040_750_200,
            "工序表单权限规则候选人为空，请检查候选来源配置");
    ErrorCode PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_SOURCE_INVALID = new ErrorCode(1_040_750_201,
            "工序表单权限规则候选来源不正确：{}");
    ErrorCode PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_COMPLETION_POLICY_INVALID = new ErrorCode(1_040_750_202,
            "工序表单权限规则完成策略不正确：{}");
    ErrorCode PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_SIGNATURE_ROLE_INVALID = new ErrorCode(1_040_750_203,
            "工序表单签名位角色不正确：{}");
    ErrorCode PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_ROUTE_BINDING_MISSING = new ErrorCode(1_040_750_204,
            "工序表单权限规则缺少工艺用途批记录表绑定，routeProcessId={}, batchRecordReportId={}");
    ErrorCode PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_VERSION_REQUIRED = new ErrorCode(1_040_750_205,
            "工序表单权限规则缺少批记录版本，routeProcessId={}, batchRecordReportId={}");

    // ========== MES 一线设备账号工序池（1-040-760-000） ==========
    ErrorCode PRO_FRONTLINE_DEVICE_ACCOUNT_BINDING_SOURCE_MISSING = new ErrorCode(1_040_760_000,
            "设备账号工艺路线绑定来源未接入，无法加载一线报工上下文");
    ErrorCode PRO_FRONTLINE_DEVICE_ACCOUNT_ROUTE_EMPTY = new ErrorCode(1_040_760_001,
            "设备账号 {} 未绑定启用工艺路线，无法切换工序");
    ErrorCode PRO_FRONTLINE_ROUTE_PROCESS_NOT_AUTHORIZED = new ErrorCode(1_040_760_002,
            "设备账号未授权当前工艺路线或工序，routeId={}, processId={}");
    ErrorCode PRO_FRONTLINE_DEVICE_ACCOUNT_CONTEXT_INVALID = new ErrorCode(1_040_760_003,
            "设备账号上下文不完整或不一致：{}");
    ErrorCode PRO_FRONTLINE_ROUTE_PROCESS_WORKSTATION_REQUIRED = new ErrorCode(1_040_760_004,
            "工艺路线工序缺少正式工作站绑定，routeId={}, processId={}");
    ErrorCode PRO_FRONTLINE_PROCESS_EMPLOYEE_EMPTY = new ErrorCode(1_040_760_005,
            "当前工序没有绑定可切换员工，workstationId={}, processId={}");
    ErrorCode PRO_FRONTLINE_ACTUAL_EMPLOYEE_NOT_BOUND = new ErrorCode(1_040_760_006,
            "实际填写员工 {} 不属于当前工序 {} 的绑定员工");
    ErrorCode PRO_FRONTLINE_TEMPLATE_BINDING_SOURCE_MISSING = new ErrorCode(1_040_760_007,
            "实际员工工序模板绑定来源未接入，无法重新加载模板");
    ErrorCode PRO_FRONTLINE_TEMPLATE_NOT_EXISTS = new ErrorCode(1_040_760_008,
            "实际员工 {} 在当前工序 {} 下没有正式模板绑定");
    ErrorCode PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED = new ErrorCode(1_040_760_009,
            "一线提交身份上下文缺少必填字段：{}");
    ErrorCode PRO_FRONTLINE_SIGNATURE_EMPLOYEE_MISMATCH = new ErrorCode(1_040_760_010,
            "电子签名员工必须等于实际填写员工，actualEmployeeId={}, signatureEmployeeId={}");
    ErrorCode PRO_FRONTLINE_SUBMIT_DEVICE_CONTEXT_MISMATCH = new ErrorCode(1_040_760_011,
            "提交设备/工作站上下文与授权工序不一致，submittedDeviceId={}, submittedWorkstationId={}, expectedDeviceId={}, expectedWorkstationId={}");
    ErrorCode PRO_FRONTLINE_TEMPLATE_MISMATCH = new ErrorCode(1_040_760_012,
            "提交模板编号与当前实际员工工序模板不一致：{}");

    ErrorCode MD_PRODUCT_BOM_ERP_SYNC_ITEM_CODE_MISSING = new ErrorCode(1_040_107_004, "褰撳墠鐗╂枡/浜у搧缂栫爜缂哄け锛屾棤娉曟墽琛?ERP 鍚屾 BOM");
    ErrorCode MD_PRODUCT_BOM_ERP_SYNC_NOT_FOUND = new ErrorCode(1_040_107_005, "ERP 涓湭鎵惧埌鐗╂枡/浜у搧缂栫爜 {} 鐨勫凡瀹℃牳 BOM");
    ErrorCode MD_PRODUCT_BOM_ERP_SYNC_MULTI_VERSION = new ErrorCode(1_040_107_006, "ERP 涓墿鏂?浜у搧缂栫爜 {} 鍛戒腑浜嗗涓凡瀹℃牳 BOM 鐗堟湰锛歿}");
    ErrorCode MD_PRODUCT_BOM_ERP_SYNC_ITEM_MISSING = new ErrorCode(1_040_107_007, "ERP BOM 瀛愰」鐗╂枡鏈槧灏勫埌鏈湴 MES 鐗╂枡锛歿}");
    ErrorCode MD_PRODUCT_BOM_ERP_SYNC_RECURSIVE_ITEM = new ErrorCode(1_040_107_008, "ERP BOM 瀛愰」鐗╂枡 {} 鍦ㄦ湰鍦颁粛閰嶇疆浜嗕笅绾?BOM锛岀姝㈠悓姝?");
}
