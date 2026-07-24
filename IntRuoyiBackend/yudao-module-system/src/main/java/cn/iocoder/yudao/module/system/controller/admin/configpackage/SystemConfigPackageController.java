package cn.iocoder.yudao.module.system.controller.admin.configpackage;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.module.system.controller.admin.configpackage.vo.SystemConfigPackageImportRespVO;
import cn.iocoder.yudao.module.system.controller.admin.configpackage.vo.SystemConfigPackagePrecheckRespVO;
import cn.iocoder.yudao.module.system.service.configpackage.SystemConfigPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 系统配置包")
@RestController
@RequestMapping("/system/config-package")
@Validated
public class SystemConfigPackageController {

    @Resource
    private SystemConfigPackageService configPackageService;

    @GetMapping("/export-excel")
    @Operation(summary = "导出系统配置包")
    @PreAuthorize("@ss.hasPermission('system:config-package:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(HttpServletResponse response) throws IOException {
        byte[] content = configPackageService.exportPackage();
        response.addHeader("Content-Disposition",
                "attachment;filename=" + HttpUtils.encodeUtf8("系统配置包.xlsx"));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        response.getOutputStream().write(content);
    }

    @PostMapping("/precheck")
    @Operation(summary = "预检系统配置包")
    @Parameters({
            @Parameter(name = "file", description = "系统配置包 Excel", required = true),
            @Parameter(name = "availableComponents", description = "当前前端构建组件路径清单", required = true)
    })
    @PreAuthorize("@ss.hasPermission('system:config-package:import')")
    public CommonResult<SystemConfigPackagePrecheckRespVO> precheck(
            @RequestParam("file") MultipartFile file,
            @RequestParam("availableComponents") String availableComponents) throws IOException {
        return success(configPackageService.precheck(file.getBytes(), parseComponents(availableComponents)));
    }

    @PostMapping("/import")
    @Operation(summary = "确认覆盖导入系统配置包")
    @Parameters({
            @Parameter(name = "file", description = "系统配置包 Excel", required = true),
            @Parameter(name = "availableComponents", description = "当前前端构建组件路径清单", required = true),
            @Parameter(name = "confirmed", description = "是否确认覆盖", required = true),
            @Parameter(name = "targetSnapshotSha256", description = "预检返回的目标快照摘要", required = true)
    })
    @PreAuthorize("@ss.hasPermission('system:config-package:import')")
    public CommonResult<SystemConfigPackageImportRespVO> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("availableComponents") String availableComponents,
            @RequestParam("confirmed") Boolean confirmed,
            @RequestParam("targetSnapshotSha256") String targetSnapshotSha256) throws IOException {
        return success(configPackageService.importPackage(file.getBytes(), confirmed, targetSnapshotSha256,
                parseComponents(availableComponents)));
    }

    private Collection<String> parseComponents(String availableComponents) {
        return StrUtil.splitTrim(availableComponents, ",");
    }

}
