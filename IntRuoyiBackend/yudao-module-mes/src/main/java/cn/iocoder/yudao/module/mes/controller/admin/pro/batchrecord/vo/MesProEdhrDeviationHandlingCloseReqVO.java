package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesProEdhrDeviationHandlingCloseReqVO {
    @NotNull
    private Long deviationId;
}
