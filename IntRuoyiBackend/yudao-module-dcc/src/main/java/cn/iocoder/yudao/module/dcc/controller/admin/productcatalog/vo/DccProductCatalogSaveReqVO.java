package cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - DCC 产品目录新增 Request VO")
@Data
public class DccProductCatalogSaveReqVO {

    @Schema(description = "数据来源", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "数据来源不能为空")
    private String dataSource;

    @Schema(description = "产品类别 I")
    private String categoryLevel1;

    @Schema(description = "产品类别 II")
    private String categoryLevel2;

    @Schema(description = "产品序号")
    private String productSequence;

    @Schema(description = "产品", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "产品不能为空")
    private String product;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "注册证名称")
    private String registrationCertificateName;

    @Schema(description = "注册证号")
    private String registrationCertificateNumber;

    @Schema(description = "持证人")
    private String certificateHolder;

    @Schema(description = "注册地")
    private String registrationPlace;

    @Schema(description = "生效日期")
    private String effectiveDate;

    @Schema(description = "有效期至")
    private String expiryDate;

    @Schema(description = "分类")
    private String classification;

    @Schema(description = "注册证信息链接")
    private String registrationInfoLink;

    @Schema(description = "产品状态")
    private String productStatus;

    @Schema(description = "备注")
    private String remark;
}
