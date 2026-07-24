package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DCC controlled file revision.
 */
@TableName("dcc_controlled_file")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    private Long masterId;
    private Long categoryId;
    private Long directoryId;
    private Long sourceFileId;
    private Long originalFileId;
    private Long drawingPdfFileId;
    private Long trainingRecordFileId;
    private Long publishedFileId;
    private Long stampedFileId;
    private String fileName;
    private String title;
    private String fileNumber;
    private Long productMasterId;
    private String productCode;
    private String productName;
    private Long dccProjectCodeId;
    private String projectCodeRecognitionType;
    private String projectCodeRecognitionText;
    private Long projectCodeRecognizedBy;
    private LocalDateTime projectCodeRecognizedTime;
    private Long fileTypeTaxonomyId;
    private String fileTypeLevel1;
    private String fileTypeLevel2;
    private String fileTypeLevel3;
    private String fileTypeLevel4;
    private String fileTypeLevel5;
    private Boolean needTraining;
    private String processType;
    private String changeType;
    private String versionNo;
    private LocalDate effectiveDate;
    private String remark;
    private String status;
    private Long submitterId;
    private Long requesterId;
    private String processInstanceId;
    private String processDefinitionKey;
    private LocalDateTime submittedTime;
    private LocalDateTime approvedTime;
    private LocalDateTime publishedTime;
    private LocalDateTime rejectedTime;
    private LocalDateTime stampedTime;
    private Long obsoletedBy;
    private LocalDateTime obsoletedTime;
    private String obsoleteReason;
    private Long supersededByFileId;
    private String rejectReason;
    private String finalizationError;

}
