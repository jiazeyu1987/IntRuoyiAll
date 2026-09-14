package cn.iocoder.yudao.module.mes.service.pro.productionrelease.report;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProductionReleaseReportAttachmentPreparePortCommand {

    private Long actorUserId;
    private Long batchTaskId;
    private String idempotencyKey;
    private String fileName;
    private String contentType;
    private byte[] content;
}
