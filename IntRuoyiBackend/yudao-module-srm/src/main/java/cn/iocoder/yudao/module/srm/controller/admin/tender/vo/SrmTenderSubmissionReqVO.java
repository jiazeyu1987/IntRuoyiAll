package cn.iocoder.yudao.module.srm.controller.admin.tender.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SrmTenderSubmissionReqVO {

    @NotNull(message = "招标项目编号不能为空")
    private Long projectId;

    @NotNull(message = "供应商编号不能为空")
    private Long supplierId;

    @NotNull(message = "投标金额不能为空")
    private BigDecimal bidAmount;

    private String attachmentUrl;
}
