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

    @Schema(description = "产品状态")
    private String productStatus;

    @Schema(description = "数据来源")
    private String dataSource;

    @Schema(description = "排序字段，仅支持 projectName / projectCode")
    private String sortField;

    @Schema(description = "排序方向，asc / desc")
    private String sortOrder;
}
