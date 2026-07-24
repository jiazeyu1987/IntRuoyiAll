package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileRecognitionMigrationImportRowRespVO {

    private Integer rowNo;
    private String directoryPath;
    private String fileName;
    private String fileNumber;
    private Long testControlledFileId;
    private Long targetControlledFileId;
    private String targetFileName;
    private String targetFileNumber;
    private String recognitionStatus;
    private String importAction;
    private String failureReason;
    private String productName;
    private String productCode;
    private Long productMasterId;
    private String projectName;
    private String projectCode;
    private Long dccProjectCodeId;
    private String fileTypeLevel1;
    private String fileTypeLevel2;
    private String fileTypeLevel3;
    private String fileTypeLevel4;
    private String fileTypeLevel5;
}
