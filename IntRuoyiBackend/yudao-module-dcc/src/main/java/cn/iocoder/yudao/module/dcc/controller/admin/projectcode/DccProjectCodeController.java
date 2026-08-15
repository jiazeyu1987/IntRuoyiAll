package cn.iocoder.yudao.module.dcc.controller.admin.projectcode;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeAssociatedFileAiCategoryRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeExportExcelVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeImportConfirmReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeImportExcelVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeImportPreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeUpdateReqVO;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - DCC 项目代码基础数据")
@RestController
@RequestMapping("/dcc/project-codes")
@Validated
public class DccProjectCodeController {

    @Resource
    private DccProjectCodeService projectCodeService;

    @PostMapping("/create")
    @Operation(summary = "创建 DCC 项目代码")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:create')")
    public CommonResult<Long> createProjectCode(@Valid @RequestBody DccProjectCodeSaveReqVO reqVO) {
        return success(projectCodeService.createProjectCode(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 DCC 项目代码")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update')")
    public CommonResult<Boolean> updateProjectCode(@Valid @RequestBody DccProjectCodeUpdateReqVO reqVO) {
        projectCodeService.updateProjectCode(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 DCC 项目代码")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:delete')")
    public CommonResult<Boolean> deleteProjectCode(@RequestParam("id") Long id) {
        projectCodeService.deleteProjectCode(id);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得 DCC 项目代码分页")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:query')")
    public CommonResult<PageResult<DccProjectCodeRespVO>> getProjectCodePage(
            @Valid DccProjectCodePageReqVO pageReqVO) {
        return success(BeanUtils.toBean(projectCodeService.getProjectCodePage(getLoginUserId(), pageReqVO),
                DccProjectCodeRespVO.class));
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "获得 DCC 项目代码详情")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:query')")
    public CommonResult<DccProjectCodeRespVO> getProjectCode(@PathVariable("id") Long id) {
        return success(BeanUtils.toBean(projectCodeService.getProjectCode(getLoginUserId(), id),
                DccProjectCodeRespVO.class));
    }

    @GetMapping("/{id:\\d+}/controlled-files/page")
    @Operation(summary = "获得 DCC 项目代码关联受控文件分页")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:query') and @ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<PageResult<DccControlledFileRespVO>> getProjectCodeControlledFilePage(
            @PathVariable("id") Long id,
            @Valid DccProjectCodeControlledFilePageReqVO pageReqVO) {
        return success(projectCodeService.getControlledFilePage(getLoginUserId(), id, pageReqVO));
    }

    @GetMapping("/{id:\\d+}/associated-files/ai-category-candidates")
    @Operation(summary = "获得 DCC 项目代码关联文件 AI 分类候选")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update') and @ss.hasPermission('dcc:controlled-file:update')")
    public CommonResult<List<DccProjectCodeAssociatedFileAiCategoryRespVO>> getAssociatedFileAiCategoryCandidates(
            @PathVariable("id") Long id) {
        return success(projectCodeService.getAssociatedFileAiCategoryCandidates(getLoginUserId(), id));
    }

    @PostMapping("/{id:\\d+}/associated-files/{fileId:\\d+}/ai-category")
    @Operation(summary = "按文件名称 AI 分类 DCC 项目代码关联文件")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update') and @ss.hasPermission('dcc:controlled-file:update')")
    public CommonResult<DccProjectCodeAssociatedFileAiCategoryRespVO> classifyAssociatedFileByName(
            @PathVariable("id") Long id,
            @PathVariable("fileId") Long fileId) {
        return success(projectCodeService.classifyAssociatedFileByName(getLoginUserId(), id, fileId));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出 DCC 项目代码")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportProjectCodeExcel(@Valid DccProjectCodePageReqVO exportReqVO,
                                       HttpServletResponse response) throws IOException {
        List<DccProjectCodeExportExcelVO> list = projectCodeService.getExportList(exportReqVO);
        ExcelUtils.write(response, "项目代码.xlsx", "DCC基础数据", DccProjectCodeExportExcelVO.class, list);
    }

    @GetMapping("/import-template")
    @Operation(summary = "获得 DCC 项目代码导入模板")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        List<DccProjectCodeImportExcelVO> list = List.of(DccProjectCodeImportExcelVO.builder()
                .docControlNo("DCC-001")
                .projectName("示例项目")
                .projectCode("CODE-001")
                .category("类别")
                .commissionedProduction("√")
                .projectLeader("负责人")
                .projectEngineer("工程师")
                .storageLocation("新N")
                .priority("高")
                .build());
        ExcelUtils.write(response, "项目代码导入模板.xlsx", "DCC基础数据", DccProjectCodeImportExcelVO.class, list);
    }

    @PostMapping("/import-preview")
    @Operation(summary = "预览 DCC 项目代码全量导入")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:import')")
    public CommonResult<DccProjectCodeImportPreviewRespVO> previewImport(@RequestParam("file") MultipartFile file)
            throws Exception {
        return success(projectCodeService.previewImport(file));
    }

    @PostMapping("/import-confirm")
    @Operation(summary = "确认 DCC 项目代码全量导入")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:import')")
    public CommonResult<DccProjectCodeImportPreviewRespVO> confirmImport(
            @Valid @RequestBody DccProjectCodeImportConfirmReqVO reqVO) {
        return success(projectCodeService.confirmImport(reqVO.getBatchId()));
    }
}
