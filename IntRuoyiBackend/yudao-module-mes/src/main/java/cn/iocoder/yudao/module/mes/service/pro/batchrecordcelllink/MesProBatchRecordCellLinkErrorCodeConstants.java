package cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProBatchRecordCellLinkErrorCodeConstants {

    ErrorCode PRO_BATCH_RECORD_CELL_LINK_SCOPE_REQUIRED =
            new ErrorCode(1_040_509_080, "批记录单元格链接缺少模板版本范围");
    ErrorCode PRO_BATCH_RECORD_CELL_LINK_FORM_LIST_EMPTY =
            new ErrorCode(1_040_509_081, "当前批记录版本没有可配置的表单");
    ErrorCode PRO_BATCH_RECORD_CELL_LINK_RULE_EMPTY =
            new ErrorCode(1_040_509_082, "至少需要配置一条跨表单单元格链接");
    ErrorCode PRO_BATCH_RECORD_CELL_LINK_REPORT_NOT_EXISTS =
            new ErrorCode(1_040_509_083, "批记录单元格链接引用的表单不存在：{}");
    ErrorCode PRO_BATCH_RECORD_CELL_LINK_CELL_MISSING =
            new ErrorCode(1_040_509_084, "批记录单元格链接引用的单元格不存在：{} / 第 {} 行第 {} 列");
    ErrorCode PRO_BATCH_RECORD_CELL_LINK_TARGET_NOT_WRITABLE =
            new ErrorCode(1_040_509_085, "目标单元格不是可填写单元格，不能自动带值：{} / {}");
    ErrorCode PRO_BATCH_RECORD_CELL_LINK_TARGET_DUPLICATE =
            new ErrorCode(1_040_509_086, "同一目标单元格只能配置一个来源：{} / {}");
    ErrorCode PRO_BATCH_RECORD_CELL_LINK_PAIR_DUPLICATE =
            new ErrorCode(1_040_509_087, "同一源单元格到目标单元格的链接重复：{} -> {}");
    ErrorCode PRO_BATCH_RECORD_CELL_LINK_EXECUTION_NOT_EXISTS =
            new ErrorCode(1_040_509_088, "批记录执行实例不存在，无法计算自动带值");
    ErrorCode PRO_BATCH_RECORD_CELL_LINK_LAYOUT_INVALID =
            new ErrorCode(1_040_509_089, "批记录表单布局 JSON 无效：{}");
    ErrorCode PRO_BATCH_RECORD_CELL_LINK_CELL_VALUES_INVALID =
            new ErrorCode(1_040_509_090, "批记录单元格值 JSON 无效：{}");
}
