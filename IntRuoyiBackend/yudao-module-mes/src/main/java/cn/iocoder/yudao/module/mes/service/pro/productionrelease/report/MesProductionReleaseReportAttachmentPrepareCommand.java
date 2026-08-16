package cn.iocoder.yudao.module.mes.service.pro.productionrelease.report;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProductionReleaseReportAttachmentPrepareCommand {

    private Long batchTaskId;
    private Integer expectedVersion;
    private String idempotencyKey;
    private String fileName;
    private String contentType;
    private byte[] content;
}
