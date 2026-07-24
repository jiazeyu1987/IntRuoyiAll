package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - eDHR 交付门禁摘要 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrDeliveryGateSummaryRespVO {

    private Long projectId;
    private String projectCode;
    private String projectStatus;
    private Boolean signoffAllowed;
    private Integer packageCount;
    private Integer gateCount;
    private Integer blockedCount;
    private String gateStatus;
    private String summary;
    private List<MesProEdhrDeliveryGateItemRespVO> gateItems;
}
