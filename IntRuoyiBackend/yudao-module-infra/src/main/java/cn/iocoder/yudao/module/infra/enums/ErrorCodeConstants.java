package cn.iocoder.yudao.module.infra.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * Infra 错误码枚举类
 *
 * infra 系统，使用 1-001-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 参数配置 1-001-000-000 ==========
    ErrorCode CONFIG_NOT_EXISTS = new ErrorCode(1_001_000_001, "参数配置不存在");
    ErrorCode CONFIG_KEY_DUPLICATE = new ErrorCode(1_001_000_002, "参数配置 key 重复");
    ErrorCode CONFIG_CAN_NOT_DELETE_SYSTEM_TYPE = new ErrorCode(1_001_000_003, "不能删除类型为系统内置的参数配置");
    ErrorCode CONFIG_GET_VALUE_ERROR_IF_VISIBLE = new ErrorCode(1_001_000_004, "获取参数配置失败，原因：不允许获取不可见配置");

    // ========== 定时任务 1-001-001-000 ==========
    ErrorCode JOB_NOT_EXISTS = new ErrorCode(1_001_001_000, "定时任务不存在");
    ErrorCode JOB_HANDLER_EXISTS = new ErrorCode(1_001_001_001, "定时任务的处理器已经存在");
    ErrorCode JOB_CHANGE_STATUS_INVALID = new ErrorCode(1_001_001_002, "只允许修改为开启或者关闭状态");
    ErrorCode JOB_CHANGE_STATUS_EQUALS = new ErrorCode(1_001_001_003, "定时任务已经处于该状态，无需修改");
    ErrorCode JOB_UPDATE_ONLY_NORMAL_STATUS = new ErrorCode(1_001_001_004, "只有开启状态的任务，才可以修改");
    ErrorCode JOB_CRON_EXPRESSION_VALID = new ErrorCode(1_001_001_005, "CRON 表达式不正确");
    ErrorCode JOB_HANDLER_BEAN_NOT_EXISTS = new ErrorCode(1_001_001_006, "定时任务的处理器 Bean 不存在，注意 Bean 默认首字母小写");
    ErrorCode JOB_HANDLER_BEAN_TYPE_ERROR = new ErrorCode(1_001_001_007, "定时任务的处理器 Bean 类型不正确，未实现 JobHandler 接口");

    // ========== API 错误日志 1-001-002-000 ==========
    ErrorCode API_ERROR_LOG_NOT_FOUND = new ErrorCode(1_001_002_000, "API 错误日志不存在");
    ErrorCode API_ERROR_LOG_PROCESSED = new ErrorCode(1_001_002_001, "API 错误日志已处理");

    // ========= 文件相关 1-001-003-000 =================
    ErrorCode FILE_PATH_EXISTS = new ErrorCode(1_001_003_000, "文件路径已存在");
    ErrorCode FILE_NOT_EXISTS = new ErrorCode(1_001_003_001, "文件不存在");
    ErrorCode FILE_IS_EMPTY = new ErrorCode(1_001_003_002, "文件为空");
    ErrorCode FILE_NAS_DIRECTORY_PATH_BLANK = new ErrorCode(1_001_003_003, "NAS 目录路径不能为空");
    ErrorCode FILE_NAS_DIRECTORY_NOT_EXISTS = new ErrorCode(1_001_003_004, "NAS 目录不存在");
    ErrorCode FILE_NAS_DIRECTORY_NOT_DIRECTORY = new ErrorCode(1_001_003_005, "NAS 路径不是目录");
    ErrorCode FILE_NAS_DIRECTORY_READ_FAILED = new ErrorCode(1_001_003_006, "NAS 目录读取失败：{}");
    ErrorCode FILE_NAS_DIRECTORY_PATH_INVALID = new ErrorCode(1_001_003_007, "NAS 目录路径不合法");
    ErrorCode FILE_NAS_DEPENDENCY_MISSING = new ErrorCode(1_001_003_008, "NAS 依赖缺失：{}");
    ErrorCode FILE_NAS_AUTH_FAILED = new ErrorCode(1_001_003_009, "NAS 认证失败");
    ErrorCode FILE_NAS_SHARE_NOT_EXISTS = new ErrorCode(1_001_003_010, "NAS 共享不存在：{}");
    ErrorCode FILE_NAS_CONNECT_FAILED = new ErrorCode(1_001_003_011, "NAS 连接失败：{}");
    ErrorCode FILE_NAS_PATH_NOT_EXISTS = new ErrorCode(1_001_003_012, "NAS 路径不存在：{}");
    ErrorCode FILE_NAS_PATH_NOT_DIRECTORY = new ErrorCode(1_001_003_013, "NAS 路径不是目录：{}");
    ErrorCode FILE_NAS_READ_FAILED = new ErrorCode(1_001_003_014, "NAS 读取失败：{}");
    ErrorCode FILE_NAS_CONFIG_MISSING = new ErrorCode(1_001_003_015, "NAS 配置未完成，请先保存服务器、共享名、用户名和密码");
    ErrorCode FILE_NAS_ACL_READ_FAILED = new ErrorCode(1_001_003_016, "NAS 权限读取失败：{}");
    ErrorCode FILE_DIRECT_LINK_BLOCKED_BY_DCC = new ErrorCode(1_001_003_017, "DCC 受控文件禁止通过通用文件直链访问：fileId={}");
    ErrorCode FILE_PROTECTED_SHOWROOM_MEDIA = new ErrorCode(1_001_003_018, "展厅受保护媒体禁止通过基础设施文件管理删除：fileId={}");
    ErrorCode FILE_NAS_WRITE_FAILED = new ErrorCode(1_001_003_019, "NAS 写入失败：{}");

    // ========== 代码生成器 1-001-004-000 ==========
    ErrorCode CODEGEN_TABLE_EXISTS = new ErrorCode(1_001_004_002, "表定义已经存在");
    ErrorCode CODEGEN_IMPORT_TABLE_NULL = new ErrorCode(1_001_004_001, "导入的表不存在");
    ErrorCode CODEGEN_IMPORT_COLUMNS_NULL = new ErrorCode(1_001_004_002, "导入的字段不存在");
    ErrorCode CODEGEN_TABLE_NOT_EXISTS = new ErrorCode(1_001_004_004, "表定义不存在");
    ErrorCode CODEGEN_COLUMN_NOT_EXISTS = new ErrorCode(1_001_004_005, "字段义不存在");
    ErrorCode CODEGEN_SYNC_COLUMNS_NULL = new ErrorCode(1_001_004_006, "同步的字段不存在");
    ErrorCode CODEGEN_SYNC_NONE_CHANGE = new ErrorCode(1_001_004_007, "同步失败，不存在改变");
    ErrorCode CODEGEN_TABLE_INFO_TABLE_COMMENT_IS_NULL = new ErrorCode(1_001_004_008, "数据库的表注释未填写");
    ErrorCode CODEGEN_TABLE_INFO_COLUMN_COMMENT_IS_NULL = new ErrorCode(1_001_004_009, "数据库的表字段({})注释未填写");
    ErrorCode CODEGEN_MASTER_TABLE_NOT_EXISTS = new ErrorCode(1_001_004_010, "主表(id={})定义不存在，请检查");
    ErrorCode CODEGEN_SUB_COLUMN_NOT_EXISTS = new ErrorCode(1_001_004_011, "子表的字段(id={})不存在，请检查");
    ErrorCode CODEGEN_MASTER_GENERATION_FAIL_NO_SUB_TABLE = new ErrorCode(1_001_004_012, "主表生成代码失败，原因：它没有子表");

    // ========== 文件配置 1-001-006-000 ==========
    ErrorCode FILE_CONFIG_NOT_EXISTS = new ErrorCode(1_001_006_000, "文件配置不存在");
    ErrorCode FILE_CONFIG_DELETE_FAIL_MASTER = new ErrorCode(1_001_006_001, "该文件配置不允许删除，原因：它是主配置，删除会导致无法上传文件");
    ErrorCode FILE_CONFIG_PROTECTED = new ErrorCode(1_001_006_002, "展厅固定文件配置 28 受保护，禁止修改");

    // ========== 数据源配置 1-001-007-000 ==========
    ErrorCode DATA_SOURCE_CONFIG_NOT_EXISTS = new ErrorCode(1_001_007_000, "数据源配置不存在");
    ErrorCode DATA_SOURCE_CONFIG_NOT_OK = new ErrorCode(1_001_007_001, "数据源配置不正确，无法进行连接");

    // ========== 运行控制台 1-001-008-000 ==========
    ErrorCode RUNTIME_CONTROL_INVALID_TARGET = new ErrorCode(1_001_008_000, "运行控制台目标不存在：环境({}) 组件({})");
    ErrorCode RUNTIME_CONTROL_PROD_GUARD_REQUIRED = new ErrorCode(1_001_008_001, "运行控制台重启必须填写原因；生产环境还必须输入 PROD 确认");
    ErrorCode RUNTIME_CONTROL_SCRIPT_NOT_EXISTS = new ErrorCode(1_001_008_002, "运行控制台脚本不存在：{}");
    ErrorCode RUNTIME_CONTROL_COMMAND_FAILED = new ErrorCode(1_001_008_003, "运行控制台命令执行失败：{}");
    ErrorCode RUNTIME_CONTROL_OPERATION_STORE_FAILED = new ErrorCode(1_001_008_004, "运行控制台操作记录读写失败：{}");
    ErrorCode RUNTIME_CONTROL_INVALID_ACTION = new ErrorCode(1_001_008_005, "运行控制台动作不存在：{}");
    ErrorCode RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED = new ErrorCode(1_001_008_006, "运行控制台动作缺少必填参数：{}");
    ErrorCode RUNTIME_CONTROL_LOG_NOT_EXISTS = new ErrorCode(1_001_008_007, "运行控制台日志不存在：{}");
    ErrorCode RUNTIME_CONTROL_LOG_PATH_INVALID = new ErrorCode(1_001_008_008, "运行控制台日志路径不合法：{}");
    ErrorCode RUNTIME_CONTROL_ACTION_PARAMETER_INVALID = new ErrorCode(1_001_008_009, "运行控制台动作参数不合法：{}");

    // ========== 学生 1-001-201-000 ==========
    ErrorCode DEMO01_CONTACT_NOT_EXISTS = new ErrorCode(1_001_201_000, "示例联系人不存在");
    ErrorCode DEMO02_CATEGORY_NOT_EXISTS = new ErrorCode(1_001_201_001, "示例分类不存在");
    ErrorCode DEMO02_CATEGORY_EXITS_CHILDREN = new ErrorCode(1_001_201_002, "存在存在子示例分类，无法删除");
    ErrorCode DEMO02_CATEGORY_PARENT_NOT_EXITS = new ErrorCode(1_001_201_003,"父级示例分类不存在");
    ErrorCode DEMO02_CATEGORY_PARENT_ERROR = new ErrorCode(1_001_201_004, "不能设置自己为父示例分类");
    ErrorCode DEMO02_CATEGORY_NAME_DUPLICATE = new ErrorCode(1_001_201_005, "已经存在该名字的示例分类");
    ErrorCode DEMO02_CATEGORY_PARENT_IS_CHILD = new ErrorCode(1_001_201_006, "不能设置自己的子示例分类为父示例分类");
    ErrorCode DEMO03_STUDENT_NOT_EXISTS = new ErrorCode(1_001_201_007, "学生不存在");
    ErrorCode DEMO03_COURSE_NOT_EXISTS = new ErrorCode(1_001_201_008, "学生课程不存在");
    ErrorCode DEMO03_GRADE_NOT_EXISTS = new ErrorCode(1_001_201_009, "学生班级不存在");
    ErrorCode DEMO03_GRADE_EXISTS = new ErrorCode(1_001_201_010, "学生班级已存在");

}
