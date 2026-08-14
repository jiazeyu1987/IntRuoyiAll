package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductBindFromWorkOrdersReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductBindFromWorkOrdersRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductByItemSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductCopyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product.MesProRouteProductSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

@Tag(name = "管理后台 - MES 工艺路线产品")
@RestController
@RequestMapping("/mes/pro/route-product")
@Validated
public class MesProRouteProductController {

    @Resource
    private MesProRouteProductService routeProductService;
    @Resource
    private MesMdItemService itemService;
    @Resource
    private MesMdUnitMeasureService unitMeasureService;

    @PostMapping("/create")
    @Operation(summary = "创建工艺路线产品")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<Long> createRouteProduct(@Valid @RequestBody MesProRouteProductSaveReqVO createReqVO) {
        return success(routeProductService.createRouteProduct(createReqVO));
    }

    @PostMapping("/copy")
    @Operation(summary = "复制工艺路线产品")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<Long> copyRouteProduct(@Valid @RequestBody MesProRouteProductCopyReqVO copyReqVO) {
        return success(routeProductService.copyRouteProduct(copyReqVO));
    }

    @PostMapping("/bind-from-work-orders")
    @Operation(summary = "从生产订单补齐工艺路线产品")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<MesProRouteProductBindFromWorkOrdersRespVO> bindFromWorkOrders(
            @Valid @RequestBody MesProRouteProductBindFromWorkOrdersReqVO reqVO) {
        return success(routeProductService.bindFromWorkOrders(reqVO.getRouteId(), reqVO.getRouteVersionId()));
    }

    @PostMapping("/preview-bind-from-work-orders")
    @Operation(summary = "预览从生产订单补齐工艺路线产品")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<MesProRouteProductBindFromWorkOrdersRespVO> previewBindFromWorkOrders(
            @Valid @RequestBody MesProRouteProductBindFromWorkOrdersReqVO reqVO) {
        return success(routeProductService.previewBindFromWorkOrders(reqVO.getRouteId(), reqVO.getRouteVersionId()));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工艺路线产品")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<Boolean> updateRouteProduct(@Valid @RequestBody MesProRouteProductSaveReqVO updateReqVO) {
        routeProductService.updateRouteProduct(updateReqVO);
        return success(true);
    }

    @PostMapping("/save-by-item")
    @Operation(summary = "按产品保存工艺路线绑定")
    @PreAuthorize("@ss.hasPermission('mes:md-item:update')")
    public CommonResult<Long> saveRouteProductByItem(@Valid @RequestBody MesProRouteProductByItemSaveReqVO reqVO) {
        return success(routeProductService.saveRouteProductByItem(reqVO.getItemId(), reqVO.getRouteId()));
    }

    @PostMapping("/save-qa-regulation-route-by-item")
    @Operation(summary = "QA 规程按产品绑定已发布工艺路线")
    @PreAuthorize("@ss.hasPermission('mes:qc-template:update')")
    public CommonResult<Long> saveQaRegulationRouteProductByItem(
            @Valid @RequestBody MesProRouteProductByItemSaveReqVO reqVO) {
        return success(routeProductService.saveQaRegulationRouteProductByItem(reqVO.getItemId(), reqVO.getRouteId()));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工艺路线产品")
    @Parameters({
            @Parameter(name = "id", description = "编号", required = true),
            @Parameter(name = "routeVersionId", description = "路线版本编号", required = true)
    })
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<Boolean> deleteRouteProduct(@RequestParam("id") Long id,
                                                    @RequestParam("routeVersionId") Long routeVersionId) {
        routeProductService.deleteRouteProduct(id, routeVersionId);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得工艺路线产品")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:query')")
    public CommonResult<MesProRouteProductRespVO> getRouteProduct(@RequestParam("id") Long id) {
        MesProRouteProductDO routeProduct = routeProductService.getRouteProduct(id);
        return success(buildRouteProductRespVO(routeProduct));
    }

    @GetMapping("/get-by-item")
    @Operation(summary = "按产品获得工艺路线绑定")
    @Parameter(name = "itemId", description = "产品物料编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('mes:md-item:query')")
    public CommonResult<MesProRouteProductRespVO> getRouteProductByItem(@RequestParam("itemId") Long itemId) {
        MesProRouteProductDO routeProduct = routeProductService.getRouteProductByItemId(itemId);
        return success(buildRouteProductRespVO(routeProduct));
    }

    @GetMapping("/list-by-route")
    @Operation(summary = "按工艺路线获得产品列表")
    @Parameter(name = "routeId", description = "工艺路线编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:query')")
    public CommonResult<List<MesProRouteProductRespVO>> getRouteProductListByRoute(@RequestParam("routeId") Long routeId) {
        List<MesProRouteProductDO> list = routeProductService.getRouteProductListByRouteId(routeId);
        return success(buildRouteProductRespVOList(list));
    }

    // ==================== 拼接 VO ====================

    private List<MesProRouteProductRespVO> buildRouteProductRespVOList(List<MesProRouteProductDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 1. 批量获取关联数据
        Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(
                convertSet(list, MesProRouteProductDO::getItemId));
        Map<Long, MesMdUnitMeasureDO> unitMap = unitMeasureService.getUnitMeasureMap(
                convertSet(itemMap.values(), MesMdItemDO::getUnitMeasureId));
        // 2. 拼接 VO
        return BeanUtils.toBean(list, MesProRouteProductRespVO.class, vo -> {
            MapUtils.findAndThen(itemMap, vo.getItemId(), item -> {
                vo.setItemCode(item.getCode()).setItemName(item.getName())
                        .setSpecification(item.getSpecification());
                MapUtils.findAndThen(unitMap, item.getUnitMeasureId(),
                        unit -> vo.setUnitName(unit.getName()));
            });
        });
    }

    private MesProRouteProductRespVO buildRouteProductRespVO(MesProRouteProductDO routeProduct) {
        if (routeProduct == null) {
            return null;
        }
        return buildRouteProductRespVOList(ListUtil.of(routeProduct)).get(0);
    }

}
