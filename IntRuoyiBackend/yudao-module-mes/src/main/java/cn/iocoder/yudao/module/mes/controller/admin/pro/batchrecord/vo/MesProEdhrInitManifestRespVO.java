package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrInitManifestRespVO {

    private Long id;

    private Long initBatchId;

    private String packageType;

    private String manifestHash;

    private String sourceFileName;

    private String sourceFileUrl;

    private Long fileSize;

    private String checksumJson;

    private String manifestJson;

    private String uploadStatus;

    private Long uploadedBy;

    private LocalDateTime uploadedAt;
}
