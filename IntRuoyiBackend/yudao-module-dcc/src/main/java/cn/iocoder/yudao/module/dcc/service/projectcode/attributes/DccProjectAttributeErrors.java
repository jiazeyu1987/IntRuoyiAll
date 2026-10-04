package cn.iocoder.yudao.module.dcc.service.projectcode.attributes;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

public final class DccProjectAttributeErrors {
    private DccProjectAttributeErrors() {}
    public static final ErrorCode INVALID = new ErrorCode(1_080_090_001, "项目属性缺失或组合不合法，请检查市场、身份与转移目标");
    public static final ErrorCode DEFAULTS_MISSING = new ErrorCode(1_080_090_002, "请先补齐项目默认属性，历史缺值不能推断");
    public static final ErrorCode LEADER_INVALID = new ErrorCode(1_080_090_003, "项目负责人必须是存在且启用的正式系统账号");
    public static final ErrorCode LEADER_REQUIRED = new ErrorCode(1_080_090_004, "仅当前项目正式负责人可办理此操作");
    public static final ErrorCode SNAPSHOT_INVALID = new ErrorCode(1_080_090_005, "申请属性身份、轮次或项目不匹配");
    public static final ErrorCode SNAPSHOT_FROZEN = new ErrorCode(1_080_090_006, "申请属性已冻结，不能覆盖历史轮次");
    public static final ErrorCode TEMPLATE_INVALID = new ErrorCode(1_080_090_007, "文件夹模板不存在、已停用或结构不合法");
    public static final ErrorCode TEMPLATE_USED = new ErrorCode(1_080_090_008, "文件夹模板已使用，请停用并保留历史");
    public static final ErrorCode TEMPLATE_FORBIDDEN = new ErrorCode(1_080_090_009, "没有文件夹模板编辑权限");
    public static final ErrorCode FOLDER_INVALID = new ErrorCode(1_080_090_010, "项目文件夹不存在或不属于当前项目");
    public static final ErrorCode FOLDER_USED = new ErrorCode(1_080_090_011, "项目目录仍有子目录、文件位置或引用，不能删除");
    public static final ErrorCode DEFAULTS_CHANGED = new ErrorCode(1_080_090_012, "项目默认属性已变化，请重新读取并确认恢复");
    public static final ErrorCode REJECTED_REQUEST_REWORK_INVALID = new ErrorCode(1_080_090_013, "仅原申请人可从未重提的驳回申请修改后重新提交");
    public static final ErrorCode WRITE_INCOMPLETE = new ErrorCode(1_080_090_014, "项目配置写入不完整：{}，请重试");
    public static ServiceException fail(ErrorCode code) { return exception(code); }
}
