package cn.iocoder.yudao.module.mes.service.pro.productionrelease.report;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProductionReleaseReportAttachmentPrepareResult {

    private String uploadToken;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;
    private String fileUrl;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long storageConfigId;
    private String storagePath;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String sha256;
    private String storageRetentionJson;
    private String storageRetentionHash;
    private Integer version;
}
