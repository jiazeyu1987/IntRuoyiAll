package cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - DCC 产品目录树节点 Response VO")
@Data
public class DccProductCatalogTreeNodeRespVO extends DccProductCatalogRespVO {

    @Schema(description = "树节点 ID")
    private String treeNodeId;

    @Schema(description = "节点类型：categoryLevel1/categoryLevel2/product/detail")
    private String nodeType;

    @Schema(description = "树层级")
    private Integer treeLevel;

    @Schema(description = "显示名称")
    private String treeLabel;

    @Schema(description = "子节点")
    private List<DccProductCatalogTreeNodeRespVO> children = new ArrayList<>();
}
