package cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("bpm_form_template_version")
@KeySequence("bpm_form_template_version_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormTemplateVersionDO extends BaseDO {

    @TableId
    private Long id;

    private Long templateId;

    private Long tenantId;

    private String templateName;

    private String versionNo;

    private String status;

    private String sourceFileName;

    private String sourceFileContent;

    private String recognizedSchemaJson;

    private String jimuSchemaJson;

    private String batchRecordReportId;

    private String batchRecordReportName;

    private String batchRecordName;

    private String batchRecordVersionNo;

    private String batchRecordFormSlotType;

    private String batchRecordBindingStatus;

    private String batchRecordBindingError;

    private String remark;

}
