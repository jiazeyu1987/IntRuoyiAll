package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 已作废活跃订单分页 Request VO")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesTeamLeaderVoidedActiveOrderPageReqVO extends PageParam {

    @Schema(description = "工单号", example = "MO20260922001")
    private String workOrderCode;

    @Schema(description = "产品名称", example = "球囊")
    private String productName;

    @Schema(description = "批次号", example = "B20260922001")
    private String batchCode;
}
