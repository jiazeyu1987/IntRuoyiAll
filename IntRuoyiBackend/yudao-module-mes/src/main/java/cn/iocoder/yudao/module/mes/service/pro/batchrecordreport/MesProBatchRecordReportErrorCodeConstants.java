package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProBatchRecordReportErrorCodeConstants {

    ErrorCode PRO_BATCH_RECORD_REPORT_NOT_EXISTS = new ErrorCode(1_040_509_000, "电子批记录生成报表不存在");
    ErrorCode PRO_BATCH_RECORD_REPORT_FILE_EMPTY = new ErrorCode(1_040_509_001, "电子批记录导入文件不能为空");
    ErrorCode PRO_BATCH_RECORD_REPORT_FILE_EXTENSION_INVALID = new ErrorCode(1_040_509_002, "电子批记录仅支持导入 .doc 文件");
    ErrorCode PRO_BATCH_RECORD_REPORT_FILE_NAME_INVALID = new ErrorCode(1_040_509_003, "电子批记录导入文件名不符合当前试点样本要求：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_PARSE_FAILED = new ErrorCode(1_040_509_004, "电子批记录 Word 解析失败：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_TABLE_COUNT_INVALID = new ErrorCode(1_040_509_005, "电子批记录 Word 未解析出可用表格，当前为：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_LINKED_REPORT_MISSING = new ErrorCode(1_040_509_006, "电子批记录绑定的积木报表不存在：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_IMAGE_FILE_EXTENSION_INVALID = new ErrorCode(1_040_509_007, "电子批记录图片仅支持 .png/.jpg/.jpeg/.bmp");
    ErrorCode PRO_BATCH_RECORD_REPORT_IMAGE_PARSE_FAILED = new ErrorCode(1_040_509_008, "电子批记录图片解析失败：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_IMAGE_CODEX_TIMEOUT = new ErrorCode(1_040_509_009, "Codex CLI 图片识别超时");
    ErrorCode PRO_BATCH_RECORD_REPORT_IMAGE_OUTPUT_INVALID = new ErrorCode(1_040_509_010, "Codex CLI 图片识别返回格式无效");
    ErrorCode PRO_BATCH_RECORD_REPORT_IMAGE_CONFIDENCE_LOW = new ErrorCode(1_040_509_011, "Codex CLI 图片识别置信度过低：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_ROUTE_INVALID = new ErrorCode(1_040_509_012, "电子批记录识别路线无效：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_FIXED_SAMPLE_MISSING = new ErrorCode(1_040_509_013, "电子批记录固定样本文件不存在：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_FIXED_SAMPLE_READ_FAILED = new ErrorCode(1_040_509_014, "电子批记录固定样本文件读取失败：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_ROUTE_RECOGNIZER_MISSING = new ErrorCode(1_040_509_015, "电子批记录识别路线未配置实现：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_ROUTE_RESULT_COUNT_INVALID = new ErrorCode(1_040_509_016, "电子批记录识别路线 {} 生成报表数量与源 Word 表格数量不一致，源表格数：{}，当前为：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_CATEGORY_NOT_EXISTS = new ErrorCode(1_040_509_017, "电子批记录报表目录不存在");
    ErrorCode PRO_BATCH_RECORD_REPORT_BOUND_BY_ROUTE_PROCESS = new ErrorCode(1_040_509_018, "电子批记录报表已被工艺路线工序绑定，不能删除：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_NAME_EMPTY = new ErrorCode(1_040_509_020, "电子批记录报表名称不能为空");
    ErrorCode PRO_BATCH_RECORD_REPORT_NAME_TOO_LONG = new ErrorCode(1_040_509_021, "电子批记录报表名称长度不能超过 50 个字符");
    ErrorCode PRO_BATCH_RECORD_REPORT_JSON_INVALID = new ErrorCode(1_040_509_022, "电子批记录报表 JSON 无效：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_SIGNATURE_ACTION_INVALID = new ErrorCode(1_040_509_023, "电子批记录签名动作无效：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_SIGNATURE_CELL_MISSING = new ErrorCode(1_040_509_024, "电子批记录签名单元格不存在：第 {} 行第 {} 列");
    ErrorCode PRO_BATCH_RECORD_REPORT_CELL_RULE_INVALID = new ErrorCode(1_040_509_025, "电子批记录单元格规则无效：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_CELL_RULE_CELL_MISSING = new ErrorCode(1_040_509_026, "电子批记录单元格规则对应单元格不存在：第 {} 行第 {} 列");
    ErrorCode PRO_BATCH_RECORD_REPORT_LEGACY_LAYOUT_MIGRATION_REQUIRED =
            new ErrorCode(1_040_509_069, "电子批记录报表仍为旧布局，请先通过导入、升版或显式迁移入口完成迁移：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_ROUTE_CANDIDATE_TARGET_CHANGED =
            new ErrorCode(1_040_509_070, "工艺路线候选版本已变化，请重新预检后再导入：预期 {}，当前 {}");
    ErrorCode PRO_BATCH_RECORD_REPORT_ROUTE_CANDIDATE_STATUS_BLOCKED =
            new ErrorCode(1_040_509_071, "工艺路线候选版本 {} 当前状态为 {}，请先撤回、取消或完成发布后再导入");
    ErrorCode PRO_BATCH_RECORD_REPORT_ROUTE_CANDIDATE_SOURCE_CHANGED =
            new ErrorCode(1_040_509_072, "工艺路线草稿 {} 的来源版本已变化：草稿来源 {}，当前生效版本 {}，请先取消该草稿后重新导入");
    ErrorCode PRO_BATCH_RECORD_REPORT_SIGNATURE_CELL_DUPLICATE =
            new ErrorCode(1_040_509_027, "电子批记录签名单元格重复：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_SIGNATURE_REVIEW_SOURCE_REQUIRED =
            new ErrorCode(1_040_509_028, "审批签名单元格必须选择岗位或角色来源：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_SIGNATURE_REVIEW_SOURCE_INVALID =
            new ErrorCode(1_040_509_029, "审批签名单元格来源类型无效：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_DELETE_CONFIRM_INVALID =
            new ErrorCode(1_040_509_030, "删除全部批记录模板必须输入 PROD 确认");
    ErrorCode PRO_BATCH_RECORD_REPORT_BATCH_NAME_EMPTY =
            new ErrorCode(1_040_509_031, "批记录名称不能为空");
    ErrorCode PRO_BATCH_RECORD_REPORT_BATCH_NAME_TOO_LONG =
            new ErrorCode(1_040_509_032, "批记录名称长度不能超过 100 个字符");
    ErrorCode PRO_BATCH_RECORD_REPORT_BATCH_NAME_EXISTS =
            new ErrorCode(1_040_509_033, "批记录名称已存在，请确认是否升级：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_BATCH_NAME_DUPLICATE =
            new ErrorCode(1_040_509_034, "批记录名称在同一路线同一表序号下存在重复数据，无法自动升级：{} / {} / 表{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_PRODUCT_INFO_MISSING =
            new ErrorCode(1_040_509_035, "未识别到产品信息固定工序，已中止导入并回滚");
    ErrorCode PRO_BATCH_RECORD_REPORT_PRODUCT_INFO_NOT_FIRST =
            new ErrorCode(1_040_509_036, "产品信息固定工序必须位于首位，已中止导入并回滚");
    ErrorCode PRO_BATCH_RECORD_REPORT_PROCESS_EMPTY =
            new ErrorCode(1_040_509_037, "产品信息后没有可生成工艺路线的工序，已中止导入并回滚");
    ErrorCode PRO_BATCH_RECORD_REPORT_ROUTE_GENERATION_FAILED =
            new ErrorCode(1_040_509_038, "工艺路线生成失败，已中止导入并回滚：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_FORM_SLOT_INVALID =
            new ErrorCode(1_040_509_039, "批记录附加表单槽位无效：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_FORM_SLOT_EXISTS =
            new ErrorCode(1_040_509_040, "批记录「{}」的「{}」已上传，请先删除后重新上传");
    ErrorCode PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_NAME_REQUIRED =
            new ErrorCode(1_040_509_041, "工艺路线对应产品名称不能为空，已中止导入并回滚");
    ErrorCode PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_EMPTY =
            new ErrorCode(1_040_509_042, "所选产品名称未找到可绑定的产品编码，已中止导入并回滚：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_BIND_FAILED =
            new ErrorCode(1_040_509_043, "工艺路线对应产品绑定失败，已中止导入并回滚：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_VERSION_NOT_EXISTS =
            new ErrorCode(1_040_509_044, "批记录版本不存在：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_VERSION_STATUS_INVALID =
            new ErrorCode(1_040_509_045, "批记录版本状态不允许当前操作：{} / {}");
    ErrorCode PRO_BATCH_RECORD_REPORT_VERSION_MIGRATION_BLOCKED =
            new ErrorCode(1_040_509_046, "批记录版本存在阻断迁移项，不能提交审批：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_VERSION_APPROVAL_NOT_EXISTS =
            new ErrorCode(1_040_509_047, "批记录版本审批实例不存在：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_VERSION_APPROVAL_RESULT_INVALID =
            new ErrorCode(1_040_509_049, "批记录版本审批结果无效：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_VERSION_CURRENT_CHANGED =
            new ErrorCode(1_040_509_050, "批记录当前版本已变化，请重新预检后再审批：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_VERSION_CONFIRM_BLOCKER =
            new ErrorCode(1_040_509_051, "阻断迁移项不能人工确认：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_VERSION_CONFIRM_SCOPE_INVALID =
            new ErrorCode(1_040_509_052, "迁移项不属于当前批记录版本：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_VERSION_DRAFT_REUPLOAD_INVALID =
            new ErrorCode(1_040_509_053, "仅草稿、预检失败或驳回版本允许重新上传：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_VERSION_RESET_BLOCKED =
            new ErrorCode(1_040_509_054, "批记录已无表单但仍有历史业务引用，不能按 V1.0 重新导入：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_IMPORT_SCOPE_EMPTY =
            new ErrorCode(1_040_509_055, "导入 Word 至少需要选择重建批记录或一个产线项");
    ErrorCode PRO_BATCH_RECORD_REPORT_ROUTE_REBUILD_VERSION_REQUIRED =
            new ErrorCode(1_040_509_056, "仅重建产线需要已有批记录版本：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_ROUTE_PRODUCT_SCOPE_INVALID =
            new ErrorCode(1_040_509_057, "所选产线项不属于当前批记录路线：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_IMPORT_ACTION_INVALID =
            new ErrorCode(1_040_509_058, "Word 导入动作无效：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_IMPORT_ACTION_NOT_ALLOWED =
            new ErrorCode(1_040_509_059, "当前批记录状态不允许执行 Word 导入动作：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_VERSION_UPGRADE_SOURCE_REQUIRED =
            new ErrorCode(1_040_509_060, "升版导入必须存在当前源版本：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_VERSION_NO_INVALID =
            new ErrorCode(1_040_509_061, "批记录版本号无效：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_VERSION_TARGET_CHANGED =
            new ErrorCode(1_040_509_062, "批记录目标版本已变化，请重新预检后再导入：预期 {}，当前 {}");
    ErrorCode PRO_BATCH_RECORD_REPORT_VERSION_APPROVAL_PROCESS_NOT_STARTED =
            new ErrorCode(1_040_509_063, "批记录升版 BPM 流程未成功启动，请确认流程设计器已部署并启用流程定义：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_VERSION_PENDING_APPROVAL_EXISTS =
            new ErrorCode(1_040_509_068, "批记录「{}」已有申请中的升版版本，请等待审批完成或撤回后再提交");
    ErrorCode PRO_BATCH_RECORD_REPORT_ROUTE_DUPLICATE =
            new ErrorCode(1_040_509_064, "DCC项目「{}」存在多条候选工艺路线，请先人工保留唯一路线：{}");
    ErrorCode PRO_BATCH_RECORD_REPORT_ROUTE_UPGRADE_CONFIRM_REQUIRED =
            new ErrorCode(1_040_509_065, "工艺路线「{}」已存在，请确认是否升版本");
    ErrorCode PRO_BATCH_RECORD_REPORT_ROUTE_UPGRADE_TARGET_CHANGED =
            new ErrorCode(1_040_509_066, "工艺路线已变化，请重新预检后再导入：预期路线 {}/版本 {}，当前路线 {}/版本 {}");
    ErrorCode PRO_BATCH_RECORD_REPORT_DCC_PROJECT_NAME_REQUIRED =
            new ErrorCode(1_040_509_067, "一次只能导入一个 DCC 项目，且批记录名称必须等于 DCC 项目名称：{} / {}");
    ErrorCode PRO_BATCH_RECORD_REPORT_DCC_PROJECT_CODE_REQUIRED =
            new ErrorCode(1_040_509_069, "请选择有效的 DCC 项目代码后再导入 Word");
    ErrorCode PRO_BATCH_RECORD_REPORT_DCC_PROJECT_CODE_MISMATCH =
            new ErrorCode(1_040_509_070, "所选 DCC 项目代码与导入产品名称不一致：{} / {}");
    ErrorCode PRO_BATCH_RECORD_REPORT_DCC_PROJECT_PRODUCT_MISSING =
            new ErrorCode(1_040_509_071, "DCC 项目代码未绑定有效产品主数据，不能用于导入 Word：{}");
}
