package cn.iocoder.yudao.module.srm.controller.admin.contract.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SrmProcurementContractSaveReqVO {

    @NotBlank(message = "合同来源类型不能为空")
    private String sourceType;

    @NotNull(message = "合同来源编号不能为空")
    private Long sourceId;

    @NotBlank(message = "合同标题不能为空")
    private String contractTitle;

    @NotNull(message = "合同金额不能为空")
    private BigDecimal contractAmount;

    @NotBlank(message = "币种不能为空")
    private String currency;

    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    @NotNull(message = "到期日期不能为空")
    private LocalDate expireDate;

    @Valid
    @NotEmpty(message = "合同至少需要一条付款约定")
    private List<Payment> payments;

    @Valid
    @NotEmpty(message = "合同至少需要一条签署信息")
    private List<Signing> signings;

    @Valid
    @NotEmpty(message = "合同至少需要一个附件")
    private List<Attachment> attachments;

    @Data
    public static class Payment {

        @NotBlank(message = "付款阶段不能为空")
        private String paymentStage;

        @NotNull(message = "付款比例不能为空")
        private BigDecimal paymentRatio;

        @NotNull(message = "付款金额不能为空")
        private BigDecimal paymentAmount;

        @NotNull(message = "付款到期日期不能为空")
        private LocalDate dueDate;

        private String paymentRemark;
    }

    @Data
    public static class Signing {

        @NotBlank(message = "签署方不能为空")
        private String signingParty;

        @NotBlank(message = "签署人不能为空")
        private String signerName;

        @NotNull(message = "签署日期不能为空")
        private LocalDate signingDate;

        private String signingRemark;
    }

    @Data
    public static class Attachment {

        @NotBlank(message = "附件名称不能为空")
        private String attachmentName;

        @NotBlank(message = "附件地址不能为空")
        private String attachmentUrl;

        @NotBlank(message = "附件类型不能为空")
        private String attachmentType;
    }
}
