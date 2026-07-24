package cn.iocoder.yudao.module.mes.controller.admin.dv.machinery;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryFinalSyncRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachinerySaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryTypeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryFinalSheetSyncService;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryTypeService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkshopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "\u7BA1\u7406\u540E\u53F0 - MES \u8BBE\u5907\u53F0\u8D26")
@RestController
@RequestMapping("/mes/dv/machinery")
@Validated
public class MesDvMachineryController {

    @Resource
    private MesDvMachineryService machineryService;
    @Resource
    private MesDvMachineryFinalSheetSyncService machineryFinalSheetSyncService;
    @Resource
    private MesDvMachineryTypeService machineryTypeService;
    @Resource
    private MesMdWorkshopService workshopService;

    @PostMapping("/create")
    @Operation(summary = "\u521B\u5EFA\u8BBE\u5907")
    @PreAuthorize("@ss.hasPermission('mes:dv-machinery:create')")
    public CommonResult<Long> createMachinery(@Valid @RequestBody MesDvMachinerySaveReqVO createReqVO) {
        return success(machineryService.createMachinery(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "\u66F4\u65B0\u8BBE\u5907")
    @PreAuthorize("@ss.hasPermission('mes:dv-machinery:update')")
    public CommonResult<Boolean> updateMachinery(@Valid @RequestBody MesDvMachinerySaveReqVO updateReqVO) {
        machineryService.updateMachinery(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "\u5220\u9664\u8BBE\u5907")
    @Parameter(name = "id", description = "\u7F16\u53F7", required = true)
    @PreAuthorize("@ss.hasPermission('mes:dv-machinery:delete')")
    public CommonResult<Boolean> deleteMachinery(@RequestParam("id") Long id) {
        machineryService.deleteMachinery(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "\u83B7\u5F97\u8BBE\u5907")
    @Parameter(name = "id", description = "\u7F16\u53F7", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:dv-machinery:query')")
    public CommonResult<MesDvMachineryRespVO> getMachinery(@RequestParam("id") Long id) {
        MesDvMachineryDO machinery = machineryService.getMachinery(id);
        if (machinery == null) {
            return success(null);
        }
        return success(buildMachineryRespVOList(Collections.singletonList(machinery)).get(0));
    }

    @GetMapping("/page")
    @Operation(summary = "\u83B7\u5F97\u8BBE\u5907\u5206\u9875")
    @PreAuthorize("@ss.hasPermission('mes:dv-machinery:query')")
    public CommonResult<PageResult<MesDvMachineryRespVO>> getMachineryPage(@Valid MesDvMachineryPageReqVO pageReqVO) {
        PageResult<MesDvMachineryDO> pageResult = machineryService.getMachineryPage(pageReqVO);
        return success(new PageResult<>(buildMachineryRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "\u5BFC\u51FA\u8BBE\u5907 Excel")
    @PreAuthorize("@ss.hasPermission('mes:dv-machinery:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMachineryExcel(@Valid MesDvMachineryPageReqVO pageReqVO,
                                     HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MesDvMachineryDO> list = machineryService.getMachineryPage(pageReqVO).getList();
        ExcelUtils.write(response, "\u8BBE\u5907\u53F0\u8D26.xls", "\u6570\u636E", MesDvMachineryRespVO.class,
                buildMachineryRespVOList(list));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "\u83B7\u5F97\u8BBE\u5907\u7CBE\u7B80\u5217\u8868", description = "\u4E3B\u8981\u7528\u4E8E\u524D\u7AEF\u4E0B\u62C9\u9009\u9879")
    @PreAuthorize("@ss.hasPermission('mes:dv-machinery:query')")
    public CommonResult<List<MesDvMachineryRespVO>> getMachinerySimpleList() {
        List<MesDvMachineryDO> list = machineryService.getMachineryList();
        return success(BeanUtils.toBean(list, MesDvMachineryRespVO.class));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "\u83B7\u5F97\u8BBE\u5907\u5BFC\u5165\u6A21\u677F")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<MesDvMachineryImportExcelVO> list = Collections.singletonList(
                MesDvMachineryImportExcelVO.builder()
                        .code("EQ-001")
                        .name("\u793A\u4F8B\u8BBE\u5907")
                        .brand("\u793A\u4F8B\u54C1\u724C")
                        .specification("\u578B\u53F7A")
                        .machineryTypeCode("MT-001")
                        .workshopCode("WS-001")
                        .processName("\u793A\u4F8B\u5DE5\u5E8F")
                        .standardHourlyCapacity(new BigDecimal("180"))
                        .status(0)
                        .build()
        );
        ExcelUtils.write(response, "\u8BBE\u5907\u5BFC\u5165\u6A21\u677F.xls", "\u8BBE\u5907\u5217\u8868",
                MesDvMachineryImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "\u5BFC\u5165\u8BBE\u5907")
    @Parameters({
            @Parameter(name = "file", description = "Excel \u6587\u4EF6", required = true),
            @Parameter(name = "updateSupport", description = "\u662F\u5426\u652F\u6301\u66F4\u65B0", example = "true")
    })
    @PreAuthorize("@ss.hasPermission('mes:dv-machinery:import')")
    public CommonResult<MesDvMachineryImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
                                                                @RequestParam(value = "updateSupport", required = false,
                                                                        defaultValue = "false") Boolean updateSupport) throws Exception {
        List<MesDvMachineryImportExcelVO> list = ExcelUtils.read(file, MesDvMachineryImportExcelVO.class);
        return success(machineryService.importMachineryList(list, updateSupport));
    }

    @PostMapping("/sync-final-sheet")
    @Operation(summary = "\u540C\u6B65\u6700\u7EC8\u7248\u8BBE\u5907 Excel")
    @Parameter(name = "file", description = "\u6700\u7EC8\u7248 Excel \u6587\u4EF6", required = true)
    @PreAuthorize("@ss.hasPermission('mes:dv-machinery:import')")
    public CommonResult<MesDvMachineryFinalSyncRespVO> syncFinalSheet(@RequestParam("file") MultipartFile file) {
        return success(machineryFinalSheetSyncService.syncFinalSheet(file));
    }

    private List<MesDvMachineryRespVO> buildMachineryRespVOList(List<MesDvMachineryDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, MesDvMachineryTypeDO> machineryTypeMap = machineryTypeService.getMachineryTypeMap(
                convertSet(list, MesDvMachineryDO::getMachineryTypeId));
        Map<Long, MesMdWorkshopDO> workshopMap = workshopService.getWorkshopMap(
                convertSet(list, MesDvMachineryDO::getWorkshopId));
        return BeanUtils.toBean(list, MesDvMachineryRespVO.class, vo -> {
            MapUtils.findAndThen(machineryTypeMap, vo.getMachineryTypeId(),
                    machineryType -> vo.setMachineryTypeName(machineryType.getName()));
            MapUtils.findAndThen(workshopMap, vo.getWorkshopId(),
                    workshop -> vo.setWorkshopName(workshop.getName()));
        });
    }
}
