package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionArchiveRespVO {

    private Long id;

    private Long batchExecutionId;

    private String artifactType;

    private Integer archiveVersion;

    private String archiveStatus;

    private String fileName;

    private Long fileSize;

    private String contentHash;

    private String pdfaProfile;

    private String pdfaValidationStatus;

    private LocalDateTime pdfaValidatedAt;

    private String sourceManifestJson;

    private LocalDateTime generatedAt;
}
