package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRoutePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteCopyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.route.importer.IntGyRouteMarkdownImportResult;
import cn.iocoder.yudao.module.mes.service.pro.route.importer.IntGyRouteMarkdownImportService;
import cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookExportService;
import cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookImportResult;
import cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookImportService;
import cn.iocoder.yudao.module.mes.service.pro.route.importer.Sheet1RouteExcelImportResult;
import cn.iocoder.yudao.module.mes.service.pro.route.importer.Sheet1RouteExcelImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - MES 工艺路线")
@RestController
@RequestMapping("/mes/pro/route")
@Validated
public class MesProRouteController {

    @Resource
    private MesProRouteService routeService;
    @Resource
    private IntGyRouteMarkdownImportService routeMarkdownImportService;
    @Resource
    private Sheet1RouteExcelImportService sheet1RouteExcelImportService;
    @Resource
    private MesProRouteWorkbookExportService routeWorkbookExportService;
    @Resource
    private MesProRouteWorkbookImportService routeWorkbookImportService;

    @PostMapping("/create")
    @Operation(summary = "创建工艺路线")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:create')")
    public CommonResult<Long> createRoute(@Valid @RequestBody MesProRouteSaveReqVO createReqVO) {
        return success(routeService.createRoute(createReqVO));
    }

    @PostMapping("/copy")
    @Operation(summary = "复制工艺路线并继承排产/批记录子配置")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:create')")
    public CommonResult<Long> copyRoute(@Valid @RequestBody MesProRouteCopyReqVO reqVO) {
        return success(routeService.copyRoute(reqVO.getSourceRouteId(), reqVO.getTargetCode(), reqVO.getTargetName()));
    }

    @PostMapping("/import-intgy-md")
    @Operation(summary = "导入 IntGY Markdown 工艺路线")
    @Parameters({
            @Parameter(name = "file", description = "IntGY Markdown 文件", required = true),
            @Parameter(name = "processStatus", description = "新建工序状态", required = true),
            @Parameter(name = "checkProcessCodesByRouteCodeJson", description = "按路线编码配置质检工序 JSON")
    })
    @PreAuthorize("@ss.hasPermission('mes:pro-route:create')")
    public CommonResult<IntGyRouteMarkdownImportResult> importIntGyMarkdown(
            @RequestParam("file") MultipartFile file,
            @RequestParam("processStatus") Integer processStatus,
            @RequestParam(value = "checkProcessCodesByRouteCodeJson", required = false)
                    String checkProcessCodesByRouteCodeJson) throws IOException {
        String markdown = new String(file.getBytes(), StandardCharsets.UTF_8);
        return success(routeMarkdownImportService.importMarkdown(markdown, processStatus,
                checkProcessCodesByRouteCodeJson));
    }

    @PostMapping("/import-sheet1-xlsx")
    @Operation(summary = "导入 Sheet1 Excel 工艺路线")
    @Parameters({
            @Parameter(name = "file", description = "Sheet1 Excel 文件", required = true),
            @Parameter(name = "processStatus", description = "新建工序状态", required = true)
    })
    @PreAuthorize("@ss.hasPermission('mes:pro-route:create')")
    public CommonResult<Sheet1RouteExcelImportResult> importSheet1Xlsx(
            @RequestParam("file") MultipartFile file,
            @RequestParam("processStatus") Integer processStatus) {
        return success(sheet1RouteExcelImportService.importExcel(file, processStatus));
    }

    @PostMapping("/import-workbook-xlsx")
    @Operation(summary = "导入工艺路线导入导出 Excel")
    @Parameter(name = "file", description = "工艺路线导入导出 Excel 文件", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-route:create')")
    public CommonResult<MesProRouteWorkbookImportResult> importRouteWorkbookXlsx(
            @RequestParam("file") MultipartFile file) {
        return success(routeWorkbookImportService.importWorkbook(file));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工艺路线")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<Boolean> updateRoute(@Valid @RequestBody MesProRouteSaveReqVO updateReqVO) {
        routeService.updateRoute(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新工艺路线状态")
    @Parameters({
            @Parameter(name = "id", description = "编号", required = true),
            @Parameter(name = "status", description = "状态", required = true)
    })
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<Boolean> updateRouteStatus(@RequestParam("id") Long id,
                                                    @RequestParam("status") Integer status) {
        routeService.updateRouteStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工艺路线")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-route:delete')")
    public CommonResult<Boolean> deleteRoute(@RequestParam("id") Long id) {
        routeService.deleteRoute(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得工艺路线")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-route:query', 'mes:pro-route:schedule-config:query', 'mes:pro-route:batch-record-config:query')")
    public CommonResult<MesProRouteRespVO> getRoute(@RequestParam("id") Long id) {
        return success(routeService.getRouteRespVO(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得工艺路线分页")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-route:query', 'mes:pro-route:schedule-config:query', 'mes:pro-route:batch-record-config:query')")
    public CommonResult<PageResult<MesProRouteRespVO>> getRoutePage(@Valid MesProRoutePageReqVO pageReqVO) {
        return success(routeService.getRoutePageRespVO(pageReqVO));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得工艺路线精简列表", description = "只包含被开启的工艺路线，主要用于前端的下拉选项")
    public CommonResult<List<MesProRouteRespVO>> getRouteSimpleList() {
        List<MesProRouteDO> list = routeService.getRouteListByStatus(CommonStatusEnum.ENABLE.getStatus());
        return success(convertList(list, route -> new MesProRouteRespVO()
                .setId(route.getId()).setName(route.getName()).setCode(route.getCode())));
    }

    @GetMapping("/item-binding-list")
    @Operation(summary = "获得产品侧工艺路线绑定选择列表", description = "用于 MES 物料产品选择工艺路线；包含状态，保存时仍按路线是否启用做正式校验")
    @PreAuthorize("@ss.hasPermission('mes:md-item:query')")
    public CommonResult<List<MesProRouteRespVO>> getRouteItemBindingList() {
        List<MesProRouteDO> list = routeService.getRouteList();
        return success(convertList(list, route -> new MesProRouteRespVO()
                .setId(route.getId()).setName(route.getName()).setCode(route.getCode()).setStatus(route.getStatus())));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出工艺路线 Excel")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportRouteExcel(@Valid MesProRoutePageReqVO pageReqVO,
                                  HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MesProRouteRespVO> data = routeService.getRoutePageRespVO(pageReqVO).getList();
        ExcelUtils.write(response, "工艺路线.xls", "数据", MesProRouteRespVO.class, data);
    }

    @GetMapping("/export-import-xlsx")
    @Operation(summary = "导出可导入的工艺路线 Excel")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportRouteImportWorkbook(@Valid MesProRoutePageReqVO pageReqVO,
                                          HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        byte[] data = routeWorkbookExportService.exportWorkbook(pageReqVO);
        response.addHeader("Content-Disposition",
                "attachment;filename=" + HttpUtils.encodeUtf8("工艺路线导入导出.xlsx"));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        response.getOutputStream().write(data);
    }

}
