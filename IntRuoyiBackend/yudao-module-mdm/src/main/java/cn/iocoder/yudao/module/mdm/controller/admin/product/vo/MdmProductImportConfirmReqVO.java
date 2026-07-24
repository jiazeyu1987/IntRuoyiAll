package cn.iocoder.yudao.module.mdm.controller.admin.product.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MdmProductImportConfirmReqVO {

    @NotNull(message = "batchId is required")
    private Long batchId;

}
