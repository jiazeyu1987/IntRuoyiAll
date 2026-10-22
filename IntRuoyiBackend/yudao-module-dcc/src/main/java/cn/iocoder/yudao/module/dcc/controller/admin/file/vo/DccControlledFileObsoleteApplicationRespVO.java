package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Existing form response plus authorized obsolete draft facts, never client execution authority. */
@Data
@EqualsAndHashCode(callSuper=true)
public class DccControlledFileObsoleteApplicationRespVO extends FormInstanceRespVO {
    @JsonFormat(shape=JsonFormat.Shape.STRING)
    private Long controlledFileId;
    @JsonFormat(shape=JsonFormat.Shape.STRING)
    private Long dccProjectCodeId;
    private String versionNo;
    private Integer attributeRound;
    private String applicationContextKey;
    private DccProjectAttributes defaultSource;
    private DccProjectAttributes actualAttributes;
    private boolean attributesSubmitted;
    private DccApprovalMatrixBaselineVO approvalMatrixBefore;
    private DccApprovalMatrixBaselineVO approvalMatrixAfter;
    private String approvalMatrixBaselineToken;
    private String approvalMatrixConfirmationToken;
    private boolean approvalMatrixChanged;
    private boolean preparationReady;
    private String unavailableReason;
    private String reason;
    private String idempotencyKey;
    private String applicationEntryMode;
    @JsonFormat(shape=JsonFormat.Shape.STRING)
    private Long reworkSourceFormInstanceId;
    private String reworkSourceFormInstanceCode;
    private String reworkSourceBpmProcessInstanceId;
    private Integer reworkSourceAttributeRound;
    private String reworkSourceDigest;
    private String reworkSourceStatus;
    private String reworkSourceVersionNo;
    private java.util.List<DccControlledFileObsoleteReworkSourceVO> reworkSourceCandidates;
    private DccApprovalMatrixBaselineVO reworkSourceApprovalMatrix;
    private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationRoundEvidence reworkSourceRelations;
    private String reworkSourceRelationDigest;
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(contentUsing=com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private java.util.List<Long> selectedSignoffDepartmentIds;
    @Override
    @JsonFormat(shape=JsonFormat.Shape.STRING)
    public Long getId(){return super.getId();}
}
