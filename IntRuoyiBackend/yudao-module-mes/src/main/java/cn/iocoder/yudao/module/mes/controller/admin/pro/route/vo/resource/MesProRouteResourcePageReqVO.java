package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resource;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - MES 产品工艺资源分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProRouteResourcePageReqVO extends PageParam {

    @Schema(description = "工艺路线编号", example = "1")
    private Long routeId;

    @Schema(description = "产品物料编号", example = "1")
    private Long productId;

    @Schema(description = "资源类型", example = "MACHINE")
    private String resourceType;

    @Schema(description = "关键词，匹配产品、路线、工序、工位或设备")
    private String keyword;
}
