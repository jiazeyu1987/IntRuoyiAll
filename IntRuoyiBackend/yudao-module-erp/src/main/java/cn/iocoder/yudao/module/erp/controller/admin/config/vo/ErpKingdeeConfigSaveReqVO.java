package cn.iocoder.yudao.module.erp.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Schema(description = "管理后台 - ERP 金蝶配置保存 Request VO")
@Data
public class ErpKingdeeConfigSaveReqVO {

    @Schema(description = "金蝶基础地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "http://172.30.30.8/K3Cloud")
    @NotBlank(message = "金蝶基础地址不能为空")
    private String baseUrl;

    @Schema(description = "账套 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "6977227150362f")
    @NotBlank(message = "账套 ID 不能为空")
    private String acctId;

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "kingdee-user")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "password")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "金蝶应用 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "invoice-print-app")
    @NotBlank(message = "金蝶应用 ID 不能为空")
    private String appId;

    @Schema(description = "SimPas 签名数据", requiredMode = Schema.RequiredMode.REQUIRED, example = "signed-data")
    @NotBlank(message = "SimPas 签名数据不能为空")
    private String signedData;

    @Schema(description = "SimPas 签名时间戳", requiredMode = Schema.RequiredMode.REQUIRED, example = "1787795088")
    @NotBlank(message = "SimPas 签名时间戳不能为空")
    private String timestamp;

    @Schema(description = "语言 LCID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2052")
    @NotNull(message = "语言 LCID 不能为空")
    private Integer lcid;

    @Schema(description = "产品同步配置", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "产品同步配置不能为空")
    private ProductConfig product;

    @Schema(description = "生产工单同步配置", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "生产工单同步配置不能为空")
    private ProductionOrderConfig productionOrder;

    @Schema(description = "BOM 同步配置", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "BOM 同步配置不能为空")
    private BomConfig bom;

    @Schema(description = "采购订单同步配置", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "采购订单同步配置不能为空")
    private PurchaseOrderConfig purchaseOrder;

    @Schema(description = "销售订单同步配置", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "销售订单同步配置不能为空")
    private SaleOrderConfig saleOrder;

    @Schema(description = "产品同步配置")
    @Data
    public static class ProductConfig {

        @Schema(description = "产品同步查询上限", requiredMode = Schema.RequiredMode.REQUIRED, example = "5000")
        @NotNull(message = "产品同步查询上限不能为空")
        @Positive(message = "产品同步查询上限必须大于 0")
        private Integer queryLimit;

    }

    @Schema(description = "生产工单同步配置")
    @Data
    public static class ProductionOrderConfig {

        @Schema(description = "生产工单同步查询上限", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
        @NotNull(message = "生产工单同步查询上限不能为空")
        @Positive(message = "生产工单同步查询上限必须大于 0")
        private Integer queryLimit;

        @Schema(description = "生产订单模板单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "MO-TEMPLATE-001")
        @NotBlank(message = "生产订单模板单号不能为空")
        private String templateBillNo;

    }

    @Schema(description = "BOM 同步配置")
    @Data
    public static class BomConfig {

        @Schema(description = "BOM 同步查询上限", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
        @NotNull(message = "BOM 同步查询上限不能为空")
        @Positive(message = "BOM 同步查询上限必须大于 0")
        private Integer queryLimit;

    }

    @Schema(description = "采购订单同步配置")
    @Data
    public static class PurchaseOrderConfig {

        @Schema(description = "采购组织编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "881")
        @NotBlank(message = "采购组织编码不能为空")
        private String purchaseOrgNumber;

        @Schema(description = "采购订单查询天数", requiredMode = Schema.RequiredMode.REQUIRED, example = "365")
        @NotNull(message = "采购订单查询天数不能为空")
        @Positive(message = "采购订单查询天数必须大于 0")
        private Integer queryDays;

        @Schema(description = "采购订单查询上限", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
        @NotNull(message = "采购订单查询上限不能为空")
        @Positive(message = "采购订单查询上限必须大于 0")
        private Integer queryLimit;

    }

    @Schema(description = "销售订单同步配置")
    @Data
    public static class SaleOrderConfig {

        @Schema(description = "销售订单查询天数", requiredMode = Schema.RequiredMode.REQUIRED, example = "365")
        @NotNull(message = "销售订单查询天数不能为空")
        @Positive(message = "销售订单查询天数必须大于 0")
        private Integer queryDays;

        @Schema(description = "销售订单查询上限", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
        @NotNull(message = "销售订单查询上限不能为空")
        @Positive(message = "销售订单查询上限必须大于 0")
        private Integer queryLimit;

    }

}
