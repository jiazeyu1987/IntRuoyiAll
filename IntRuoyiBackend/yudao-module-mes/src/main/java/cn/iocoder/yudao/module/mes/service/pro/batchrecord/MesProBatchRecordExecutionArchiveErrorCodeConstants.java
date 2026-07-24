package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface MesProBatchRecordExecutionArchiveErrorCodeConstants {

    ErrorCode PRO_BATCH_RECORD_ARCHIVE_EXECUTION_NOT_EXISTS =
            new ErrorCode(1_040_750_300, "批记录执行归档对应的执行记录不存在");
    ErrorCode PRO_BATCH_RECORD_ARCHIVE_EXECUTION_NOT_SUBMITTED =
            new ErrorCode(1_040_750_301, "只有已提交的批记录执行记录才允许归档");
    ErrorCode PRO_BATCH_RECORD_ARCHIVE_SNAPSHOT_MISSING =
            new ErrorCode(1_040_750_302, "批记录执行快照缺失，无法生成受控归档");
    ErrorCode PRO_BATCH_RECORD_ARCHIVE_SNAPSHOT_INVALID =
            new ErrorCode(1_040_750_303, "批记录执行快照或填写数据格式无效，无法生成受控归档");
    ErrorCode PRO_BATCH_RECORD_ARCHIVE_TYPE_UNSUPPORTED =
            new ErrorCode(1_040_750_304, "不支持的批记录归档类型");
    ErrorCode PRO_BATCH_RECORD_ARCHIVE_RENDERER_UNAVAILABLE =
            new ErrorCode(1_040_750_305, "批记录归档渲染器不可用");
    ErrorCode PRO_BATCH_RECORD_ARCHIVE_RENDER_FAILED =
            new ErrorCode(1_040_750_306, "批记录归档渲染失败");
    ErrorCode PRO_BATCH_RECORD_ARCHIVE_FILE_STORAGE_UNAVAILABLE =
            new ErrorCode(1_040_750_307, "归档文件服务不可用");
    ErrorCode PRO_BATCH_RECORD_ARCHIVE_FILE_PERSIST_FAILED =
            new ErrorCode(1_040_750_308, "归档文件保存失败");
    ErrorCode PRO_BATCH_RECORD_ARCHIVE_NOT_EXISTS =
            new ErrorCode(1_040_750_309, "批记录执行归档不存在");
    ErrorCode PRO_BATCH_RECORD_ARCHIVE_CHECKSUM_MISMATCH =
            new ErrorCode(1_040_750_310, "归档文件摘要校验失败，拒绝下载");
    ErrorCode PRO_BATCH_RECORD_ARCHIVE_SOURCE_CHANGED_REGENERATE_REQUIRED =
            new ErrorCode(1_040_750_311, "归档源数据已变化，需要显式重新生成");
    ErrorCode PRO_BATCH_RECORD_ARCHIVE_SEAL_SIGNATURE_FAILED =
            new ErrorCode(1_040_750_312, "归档封存电子签名失败");
    ErrorCode PRO_BATCH_RECORD_ARCHIVE_EXECUTION_NOT_CLOSED =
            new ErrorCode(1_040_750_313, "只有审批关闭后的批记录执行记录才允许归档");
    ErrorCode PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED =
            new ErrorCode(1_040_750_314, "归档文件存储侧 Retention/Object Lock/legal hold 证据校验失败，拒绝封存或下载");
    ErrorCode PRO_BATCH_RECORD_ARCHIVE_ATTACHMENT_CHAIN_INVALID =
            new ErrorCode(1_040_750_315, "批记录附件审计链校验失败，拒绝生成受控归档");
    ErrorCode PRO_BATCH_RECORD_ARCHIVE_ATTACHMENT_METADATA_INCOMPLETE =
            new ErrorCode(1_040_750_316, "批记录附件归档元数据不完整，拒绝生成受控归档");
}
