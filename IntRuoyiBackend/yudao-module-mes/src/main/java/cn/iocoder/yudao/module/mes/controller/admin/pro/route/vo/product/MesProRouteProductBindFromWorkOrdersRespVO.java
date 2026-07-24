package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES 从生产订单补齐工艺路线产品 Response VO")
@Data
@Builder
public class MesProRouteProductBindFromWorkOrdersRespVO {

    @Schema(description = "工艺路线编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long routeId;

    @Schema(description = "工艺路线名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "球囊扩张压力泵")
    private String routeName;

    @Schema(description = "匹配到的产品数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private Integer matchedCount;

    @Schema(description = "已存在关联数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer existingCount;

    @Schema(description = "新增关联数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer createdCount;

    @Schema(description = "冲突产品数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer conflictCount;

    @Schema(description = "匹配产品物料编码")
    private List<String> itemCodes;

    @Schema(description = "可新增产品物料编码")
    private List<String> creatableItemCodes;

    @Schema(description = "已存在当前路线关联的产品物料编码")
    private List<String> existingItemCodes;

    @Schema(description = "已关联其它路线的冲突产品物料编码")
    private List<String> conflictItemCodes;

}
