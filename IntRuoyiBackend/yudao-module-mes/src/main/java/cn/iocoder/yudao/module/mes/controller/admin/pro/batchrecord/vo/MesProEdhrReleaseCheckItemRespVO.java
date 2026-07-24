package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrReleaseCheckItemRespVO {

    private Long id;

    private Long releaseTransactionId;

    private String checkCode;

    private String checkCategory;

    private String checkName;

    private String checkResult;

    private String itemStatus;

    private String severity;

    private String responsibilityModule;

    private String sourceObjectType;

    private String sourceObjectId;

    private String sourceObjectCode;

    private String sourceRecordUrl;

    private String failureReason;

    private String remediationSuggestion;

    private String impactScopeJson;

    private String evidenceHash;

    private LocalDateTime checkedAt;
}
