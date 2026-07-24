package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionRecordDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileRecognitionMigrationExportExcelVO {

    private String directoryPath;
    private String fileName;
    private String fileNumber;
    private Long testControlledFileId;
    private String recognitionStatus;
    private String productName;
    private String productCode;
    private String projectName;
    private String projectCode;
    private Long testProjectCodeId;
    private Long matchedProjectAliasId;
    private String matchedProjectAliasText;
    private String matchedProjectAliasSource;
    private String matchType;
    private String matchText;
    private String failureMessage;
    private String fileTypeLevel1;
    private String fileTypeLevel2;
    private String fileTypeLevel3;
    private String fileTypeLevel4;
    private String fileTypeLevel5;
    private String recognitionVersion;
    private Long batchTaskId;
    private Long recognizedBy;
    private LocalDateTime recognizedTime;

    public static DccControlledFileRecognitionMigrationExportExcelVO from(
            DccControlledFileDO file,
            String directoryPath,
            DccControlledFileRecognitionRecordDO record,
            DccProjectCodeDO projectCode,
            MdmProductRespDTO product) {
        return DccControlledFileRecognitionMigrationExportExcelVO.builder()
                .directoryPath(directoryPath)
                .fileName(StrUtil.trim(file.getFileName()))
                .fileNumber(StrUtil.trim(file.getFileNumber()))
                .testControlledFileId(file.getId())
                .recognitionStatus(record.getStatus())
                .productName(product == null ? StrUtil.trim(record.getRecognizedProductName()) : StrUtil.trim(product.getNameCn()))
                .productCode(product == null ? StrUtil.trim(record.getRecognizedProductCode()) : StrUtil.trim(product.getDccProductCode()))
                .projectName(projectCode == null ? null : StrUtil.trim(projectCode.getProjectName()))
                .projectCode(projectCode == null ? null : StrUtil.trim(projectCode.getProjectCode()))
                .testProjectCodeId(record.getMatchedProjectCodeId())
                .matchedProjectAliasId(record.getMatchedProjectAliasId())
                .matchedProjectAliasText(record.getMatchedProjectAliasText())
                .matchedProjectAliasSource(record.getMatchedProjectAliasSource())
                .matchType(record.getMatchType())
                .matchText(record.getMatchText())
                .failureMessage(record.getFailureMessage())
                .fileTypeLevel1(record.getFileTypeLevel1())
                .fileTypeLevel2(record.getFileTypeLevel2())
                .fileTypeLevel3(record.getFileTypeLevel3())
                .fileTypeLevel4(record.getFileTypeLevel4())
                .fileTypeLevel5(record.getFileTypeLevel5())
                .recognitionVersion(record.getRecognitionVersion())
                .batchTaskId(record.getBatchTaskId())
                .recognizedBy(record.getRecognizedBy())
                .recognizedTime(record.getRecognizedTime())
                .build();
    }
}
