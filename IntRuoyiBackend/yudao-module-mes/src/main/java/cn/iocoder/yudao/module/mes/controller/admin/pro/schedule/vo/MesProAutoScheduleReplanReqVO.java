package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - MES 自动排产重排 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class MesProAutoScheduleReplanReqVO extends MesProAutoSchedulePreviewReqVO {

    @Schema(description = "重排应用幂等键")
    private String idempotencyKey;
}
