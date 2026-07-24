package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "DCC current controlled file version by file number")
public class DccControlledFileCurrentVersionRespVO {

    @Schema(description = "Normalized file number used for lookup")
    private String fileNumber;

    @Schema(description = "Whether one current active version was found")
    private Boolean matched;

    @Schema(description = "Current controlled file id")
    private Long currentControlledFileId;

    @Schema(description = "Controlled file master id")
    private Long masterId;

    @Schema(description = "File name")
    private String fileName;

    @Schema(description = "Current version number")
    private String currentVersionNo;

    @Schema(description = "Current controlled file status")
    private String status;

    @Schema(description = "Category id")
    private Long categoryId;

    @Schema(description = "Directory id")
    private Long directoryId;

    @Schema(description = "Original editable file id from the current version")
    private Long originalFileId;

    @Schema(description = "Original editable file name from the current version")
    private String originalFileName;

    @Schema(description = "Original editable file storage path from the current version")
    private String originalFilePath;

    @Schema(description = "Current source file id")
    private Long sourceFileId;

    @Schema(description = "Current source file name")
    private String sourceFileName;

    @Schema(description = "Current source file storage path")
    private String sourceFilePath;

    @Schema(description = "Published PDF file id")
    private Long publishedFileId;

    @Schema(description = "Published PDF file name")
    private String publishedFileName;

    @Schema(description = "Published PDF file storage path")
    private String publishedFilePath;

    @Schema(description = "Stamped PDF file id")
    private Long stampedFileId;

    @Schema(description = "Stamped PDF file name")
    private String stampedFileName;

    @Schema(description = "Stamped PDF file storage path")
    private String stampedFilePath;

    @Schema(description = "Product master id")
    private Long productMasterId;

    @Schema(description = "DCC product code")
    private String productCode;

    @Schema(description = "Product name")
    private String productName;

    @Schema(description = "DCC project code id")
    private Long dccProjectCodeId;

    @Schema(description = "DCC file type taxonomy id")
    private Long fileTypeTaxonomyId;

    @Schema(description = "DCC file type level 1")
    private String fileTypeLevel1;

    @Schema(description = "DCC file type level 2")
    private String fileTypeLevel2;

    @Schema(description = "DCC file type level 3")
    private String fileTypeLevel3;

    @Schema(description = "DCC file type level 4")
    private String fileTypeLevel4;

    @Schema(description = "DCC file type level 5")
    private String fileTypeLevel5;

    @Schema(description = "Whether the same version chain has an unfinished workflow")
    private Boolean modifying;

    @Schema(description = "Backend controlled action projection for the current version")
    private DccControlledFileActionProjectionRespVO actionProjection;
}
