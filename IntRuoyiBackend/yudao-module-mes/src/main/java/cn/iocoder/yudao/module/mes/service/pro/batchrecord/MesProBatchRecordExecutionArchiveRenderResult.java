package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MesProBatchRecordExecutionArchiveRenderResult {

    private String fileName;
    private String contentType;
    private Long fileSize;
    private String sha256;
    private String renderSourceVersion;
    private byte[] content;
}
