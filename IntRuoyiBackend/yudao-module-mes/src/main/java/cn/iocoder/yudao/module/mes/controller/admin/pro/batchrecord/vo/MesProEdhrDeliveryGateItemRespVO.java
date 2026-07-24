package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - eDHR 交付门禁项 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrDeliveryGateItemRespVO {

    private Long id;
    private Long projectId;
    private Long packageId;
    private String gateCode;
    private String gateName;
    private String gateStatus;
    private String missingEvidence;
    private String ownerName;
    private String nextAction;
    private String signoffImpact;
    private Boolean blockingFlag;
    private Integer sort;
}
