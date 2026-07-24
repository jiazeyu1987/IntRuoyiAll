package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrUnifiedChangeImpactRespVO {

    private Long id;

    private Long changeRequestId;

    private String impactType;

    private String impactObjectType;

    private String impactObjectId;

    private String impactObjectCode;

    private String riskLevel;

    private String responsibilityModule;

    private Boolean requiresTraining;

    private Boolean requiresRevalidation;

    private Boolean requiresReleaseRecheck;

    private String impactDetail;

    private String nextAction;

    private String evidenceHash;
}
