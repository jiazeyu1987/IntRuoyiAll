package cn.iocoder.yudao.module.dcc.controller.admin.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRouteNodeRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRoutePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRoutePreviewReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRoutePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRouteRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRouteSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper;
import cn.iocoder.yudao.module.dcc.service.route.DccApprovalRouteAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - DCC 审批路线")
@RestController
@RequestMapping("/dcc/approval-routes")
@Validated
public class DccApprovalRouteController {

    @Resource
    private DccApprovalRouteAdminService routeAdminService;
    @Resource
    private DccCategoryApprovalRouteNodeMapper routeNodeMapper;

    @GetMapping("/page")
    @Operation(summary = "分页获取审批路线")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:route:manage')")
    public CommonResult<PageResult<DccApprovalRouteRespVO>> getRoutePage(@Valid DccApprovalRoutePageReqVO reqVO) {
        return success(routeAdminService.getRoutePage(reqVO));
    }

    @GetMapping
    @Operation(summary = "获取类别审批路线")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<List<DccApprovalRouteRespVO>> getRoutes(@RequestParam("categoryId") Long categoryId) {
        List<DccCategoryApprovalRouteDO> routes = routeAdminService.getRoutes(categoryId);
        return success(convertList(routes, this::toRouteResp));
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "保存类别审批路线")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:route:manage')")
    public CommonResult<Long> saveRoute(@PathVariable("categoryId") Long categoryId,
                                        @Valid @RequestBody DccApprovalRouteSaveReqVO reqVO) {
        return success(routeAdminService.saveRoute(categoryId, reqVO).getId());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除审批路线版本")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:route:manage')")
    public CommonResult<Boolean> deleteRoute(@PathVariable("id") Long id) {
        routeAdminService.deleteRoute(id);
        return success(true);
    }

    @PostMapping("/preview")
    @Operation(summary = "预览审批路线")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:route:manage')")
    public CommonResult<List<DccApprovalRoutePreviewRespVO>> previewRoute(@Valid @RequestBody DccApprovalRoutePreviewReqVO reqVO) {
        return success(routeAdminService.previewRoute(reqVO));
    }

    private DccApprovalRouteRespVO toRouteResp(DccCategoryApprovalRouteDO route) {
        DccApprovalRouteRespVO respVO = BeanUtils.toBean(route, DccApprovalRouteRespVO.class);
        List<DccCategoryApprovalRouteNodeDO> nodes = routeNodeMapper.selectList(DccCategoryApprovalRouteNodeDO::getRouteId, route.getId()).stream()
                .sorted(Comparator.comparing(DccCategoryApprovalRouteNodeDO::getSort).thenComparing(DccCategoryApprovalRouteNodeDO::getStageNo))
                .toList();
        respVO.setNodes(CollectionUtils.convertList(nodes, node -> {
            DccApprovalRouteNodeRespVO nodeRespVO = BeanUtils.toBean(node, DccApprovalRouteNodeRespVO.class);
            nodeRespVO.setCandidateSourceIds(parseIds(node.getCandidateSourceIds(), node.getCandidateSourceId()));
            return nodeRespVO;
        }));
        return respVO;
    }

    private List<Long> parseIds(String csv, Long fallbackId) {
        if (csv != null && !csv.isBlank()) {
            return List.of(csv.split(",")).stream()
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        }
        return fallbackId == null ? List.of() : List.of(fallbackId);
    }
}
