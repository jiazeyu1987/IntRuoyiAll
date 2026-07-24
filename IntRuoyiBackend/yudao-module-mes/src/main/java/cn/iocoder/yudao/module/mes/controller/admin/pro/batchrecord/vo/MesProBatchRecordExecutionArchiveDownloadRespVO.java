package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 批记录执行归档下载 Response VO")
@Data
public class MesProBatchRecordExecutionArchiveDownloadRespVO {

    private String fileName;
    private String contentType;
    private Long fileSize;
    private String sha256;
    private Long approvalSnapshotId;
    private String approvalSnapshotHash;
    private byte[] content;
}
