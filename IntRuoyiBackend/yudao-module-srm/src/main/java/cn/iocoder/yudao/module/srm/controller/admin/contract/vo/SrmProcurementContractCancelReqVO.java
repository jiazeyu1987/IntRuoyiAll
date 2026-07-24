package cn.iocoder.yudao.module.srm.controller.admin.contract.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SrmProcurementContractCancelReqVO {

    @NotNull(message = "合同编号不能为空")
    private Long id;

    @NotBlank(message = "作废原因不能为空")
    private String cancelReason;
}
