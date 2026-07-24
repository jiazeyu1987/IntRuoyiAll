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

import java.time.LocalDateTime;

@TableName("dcc_controlled_file_recognition_record")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileRecognitionRecordDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private Long controlledFileId;

    private String recognitionScope;

    private String recognitionMethod;

    private String recognitionVersion;

    private String status;

    private Long batchTaskId;

    private Long matchedProjectCodeId;

    private Long matchedProjectAliasId;

    private String matchedProjectAliasText;

    private String matchedProjectAliasSource;

    private String recognizedProductCode;

    private String recognizedProductName;

    private String matchType;

    private String matchText;

    private String failureStage;

    private String failureCode;

    private String failureMessage;

    private Long fileTypeTaxonomyId;

    private String fileTypeLevel1;

    private String fileTypeLevel2;

    private String fileTypeLevel3;

    private String fileTypeLevel4;

    private String fileTypeLevel5;

    private Long recognizedBy;

    private LocalDateTime recognizedTime;

    private Long sourceFileId;
}
