package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionArchiveDownloadRespVO {

    private String fileName;

    private String contentType;

    private Long fileSize;

    private byte[] content;
}
