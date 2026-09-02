package cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - DCC 产品目录树 Request VO")
@Data
public class DccProductCatalogTreeReqVO {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "产品类别 I")
    private String categoryLevel1;

    @Schema(description = "产品类别 II")
    private String categoryLevel2;

    @Schema(description = "产品状态")
    private String productStatus;

    @Schema(description = "数据来源")
    private String dataSource;

    @Schema(description = "项目代码不为空")
    private Boolean projectCodeNotBlank;
}
