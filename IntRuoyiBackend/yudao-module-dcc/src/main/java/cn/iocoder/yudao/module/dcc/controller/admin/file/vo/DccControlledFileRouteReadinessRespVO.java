package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DccControlledFileRouteReadinessRespVO {
    private String approvalSourceMode;
    private cn.iocoder.yudao.module.dcc.service.file.DccApplicationCustomMatrix applicationCustomMatrix;
    private String applicationCustomMatrixCanonical;
    private String applicationCustomMatrixDigest;
    private String applicationCustomMatrixToken;
    private String applicationCustomMatrixPhase;

    private Boolean ready;
    private List<DccControlledFileRoutePreviewRespVO> nodes;
    private List<DccControlledFileRouteReadinessBlockerRespVO> blockers;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long routeId;
    private Integer routeVersionNo;
    private String matrixDigest;
    private String derivationVersion;
    private String applicationActionType;
    private DccApprovalMatrixBaselineVO approvalMatrix;
    private Boolean matrixChangeRequired;
    private DccApprovalMatrixBaselineVO matrixBefore;
    private DccApprovalMatrixBaselineVO matrixAfter;
    private String approvalMatrixBaselineToken;
    private String approvalMatrixConfirmationToken;
    private List<cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.RuleGroup> runtimeRuleGroups;
    private String executionContract;
    private String preparedProcessDefinitionId;
    private String preparedDefinitionDigest;
    private String supervisorSourceDigest;
    private String supervisorSourceToken;
    private List<String> runtimeCapabilityLimitations;
    private boolean newActionBaselineRequired;
    private boolean newActionBaselinePreparationAllowed;
}
