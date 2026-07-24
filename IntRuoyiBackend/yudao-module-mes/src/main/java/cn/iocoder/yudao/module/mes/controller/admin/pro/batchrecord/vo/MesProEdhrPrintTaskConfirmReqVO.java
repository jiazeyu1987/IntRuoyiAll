package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesProEdhrPrintTaskConfirmReqVO {

    @NotNull(message = "打印任务ID不能为空")
    private Long id;

    @NotBlank(message = "打印确认凭证hash不能为空")
    private String confirmationEvidenceHash;
}
