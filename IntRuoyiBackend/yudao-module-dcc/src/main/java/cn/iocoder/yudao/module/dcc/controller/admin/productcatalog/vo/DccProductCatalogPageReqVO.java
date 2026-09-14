package cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - DCC 产品目录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DccProductCatalogPageReqVO extends PageParam {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "产品类别 I")
    private String categoryLevel1;

    @Schema(description = "产品类别 II")
    private String categoryLevel2;

    @Schema(description = "产品序号")
    private String productSequence;

    @Schema(description = "产品")
    private String product;

    @Schema(description = "产品状态")
    private String productStatus;

    @Schema(description = "数据来源")
    private String dataSource;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目代码")
    private String projectCode;

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

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "项目代码不为空")
    private Boolean projectCodeNotBlank;

    @Schema(description = "排序字段，仅支持 projectName / projectCode")
    private String sortField;

    @Schema(description = "排序方向，asc / desc")
    private String sortOrder;
}
