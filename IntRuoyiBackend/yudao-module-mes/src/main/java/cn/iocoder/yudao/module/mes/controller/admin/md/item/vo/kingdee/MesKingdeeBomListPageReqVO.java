package cn.iocoder.yudao.module.mes.controller.admin.md.item.vo.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 物料清单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesKingdeeBomListPageReqVO extends PageParam {

    @Schema(description = "BOM编号")
    private String bomNumber;

    @Schema(description = "父项物料编码")
    private String parentMaterialCode;

    @Schema(description = "父项物料名称")
    private String parentMaterialName;

    @Schema(description = "子项物料编码")
    private String childMaterialCode;

    @Schema(description = "子项物料名称")
    private String childMaterialName;

}
