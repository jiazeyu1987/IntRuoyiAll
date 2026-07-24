package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.bom.MesProWorkOrderBomPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.bom.MesProWorkOrderBomRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.bom.MesProWorkOrderBomSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemTypeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderBomDO;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemTypeService;
import cn.iocoder.yudao.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderBomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - MES 生产工单 BOM")
@RestController
@RequestMapping("/mes/pro/work-order-bom")
@Validated
public class MesProWorkOrderBomController {

    @Resource
    private MesProWorkOrderBomService workOrderBomService;
    @Resource
    private MesMdItemService itemService;
    @Resource
    private MesMdUnitMeasureService unitMeasureService;
    @Resource
    private MesMdItemTypeService itemTypeService;

    @PostMapping("/create")
    @Operation(summary = "创建工单 BOM")
    @PreAuthorize("@ss.hasPermission('mes:pro-work-order:update')")
    public CommonResult<Long> createWorkOrderBom(@Valid @RequestBody MesProWorkOrderBomSaveReqVO createReqVO) {
        return success(workOrderBomService.createWorkOrderBom(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工单 BOM")
    @PreAuthorize("@ss.hasPermission('mes:pro-work-order:update')")
    public CommonResult<Boolean> updateWorkOrderBom(@Valid @RequestBody MesProWorkOrderBomSaveReqVO updateReqVO) {
        workOrderBomService.updateWorkOrderBom(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工单 BOM")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-work-order:update')")
    public CommonResult<Boolean> deleteWorkOrderBom(@RequestParam("id") Long id) {
        workOrderBomService.deleteWorkOrderBom(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得工单 BOM")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:pro-work-order:query')")
    public CommonResult<MesProWorkOrderBomRespVO> getWorkOrderBom(@RequestParam("id") Long id) {
        MesProWorkOrderBomDO workOrderBom = workOrderBomService.getWorkOrderBom(id);
        if (workOrderBom == null) {
            return success(null);
        }
        return success(buildWorkOrderBomRespVOList(ListUtil.of(workOrderBom)).get(0));
    }

    @GetMapping("/page")
    @Operation(summary = "获得工单 BOM 分页")
    @PreAuthorize("@ss.hasPermission('mes:pro-work-order:query')")
    public CommonResult<PageResult<MesProWorkOrderBomRespVO>> getWorkOrderBomPage(@Valid MesProWorkOrderBomPageReqVO pageReqVO) {
        PageResult<MesProWorkOrderBomDO> pageResult = workOrderBomService.getWorkOrderBomPage(pageReqVO);
        return success(new PageResult<>(buildWorkOrderBomRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/item-list-by-work-order-id")
    @Operation(summary = "获得工单物料需求列表")
    @Parameter(name = "workOrderId", description = "工单编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-work-order:query')")
    public CommonResult<List<MesProWorkOrderItemRespVO>> getWorkOrderBomItemListByWorkOrderId(
            @RequestParam("workOrderId") Long workOrderId) {
        Map<Long, BigDecimal> leafItems = workOrderBomService.getWorkOrderMaterialDemandByWorkOrderId(workOrderId);
        if (CollUtil.isEmpty(leafItems)) {
            return success(Collections.emptyList());
        }

        // 3. 拼接 VO 字段
        Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(leafItems.keySet());
        Map<Long, MesMdUnitMeasureDO> unitMeasureMap = unitMeasureService.getUnitMeasureMap(
                convertSet(itemMap.values(), MesMdItemDO::getUnitMeasureId));
        Map<Long, MesMdItemTypeDO> itemTypeMap = itemTypeService.getItemTypeMap(
                convertSet(itemMap.values(), MesMdItemDO::getItemTypeId));
        List<MesProWorkOrderItemRespVO> result = new ArrayList<>(leafItems.size());
        for (Map.Entry<Long, BigDecimal> entry : leafItems.entrySet()) {
            MesMdItemDO item = itemMap.get(entry.getKey());
            if (item == null) {
                continue;
            }
            MesProWorkOrderItemRespVO vo = new MesProWorkOrderItemRespVO()
                    .setItemId(item.getId()).setQuantity(entry.getValue())
                    .setItemCode(item.getCode()).setItemName(item.getName()).setItemSpecification(item.getSpecification());
            MapUtils.findAndThen(unitMeasureMap, item.getUnitMeasureId(),
                    unitMeasure -> vo.setUnitMeasureName(unitMeasure.getName()));
            MapUtils.findAndThen(itemTypeMap, item.getItemTypeId(),
                    itemType -> vo.setItemOrProduct(itemType.getItemOrProduct()));
            result.add(vo);
        }
        return success(result);
    }

    // ==================== 拼接 VO ====================

    private List<MesProWorkOrderBomRespVO> buildWorkOrderBomRespVOList(List<MesProWorkOrderBomDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 1. 批量获取关联数据
        Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(
                convertSet(list, MesProWorkOrderBomDO::getItemId));
        Map<Long, MesMdUnitMeasureDO> unitMeasureMap = unitMeasureService.getUnitMeasureMap(
                convertSet(itemMap.values(), MesMdItemDO::getUnitMeasureId));
        Map<Long, MesMdItemTypeDO> itemTypeMap = itemTypeService.getItemTypeMap(
                convertSet(itemMap.values(), MesMdItemDO::getItemTypeId));
        // 2. 拼接 VO
        return BeanUtils.toBean(list, MesProWorkOrderBomRespVO.class, vo -> {
            MapUtils.findAndThen(itemMap, vo.getItemId(), item -> {
                vo.setItemName(item.getName()).setItemCode(item.getCode())
                        .setItemSpecification(item.getSpecification());
                MapUtils.findAndThen(itemTypeMap, item.getItemTypeId(),
                        itemType -> vo.setItemOrProduct(itemType.getItemOrProduct()));
                MapUtils.findAndThen(unitMeasureMap, item.getUnitMeasureId(),
                        unitMeasure -> vo.setUnitMeasureName(unitMeasure.getName()));
            });
        });
    }

}
