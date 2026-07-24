package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrInitIssueRespVO {

    private Long id;

    private Long initBatchId;

    private Long initManifestId;

    private String issueCode;

    private String issueLevel;

    private String issueStatus;

    private String packageType;

    private String sourceFileName;

    private Integer sourceRowNo;

    private String sourceFieldName;

    private String objectType;

    private String objectKey;

    private Long responsibleUserId;

    private String responsibleName;

    private String issueMessage;

    private String remediationSuggestion;

    private String impactScopeJson;
}
