package cn.iocoder.yudao.module.system.controller.admin.permission;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.module.system.service.permission.EdhrPermissionTransferService;
import cn.iocoder.yudao.module.system.service.permission.EdhrPermissionWorkbook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;

@RestController
@RequestMapping("/system/role/edhr-transfer")
@Tag(name = "管理后台 - eDHR权限Excel迁移")
public class EdhrPermissionTransferController {
    @Resource private EdhrPermissionTransferService service;

    @GetMapping("/export")
    @Operation(summary = "导出当前芋道源码租户的eDHR显式账号角色权限及用途")
    @PreAuthorize("@ss.hasPermission('system:role:export') and @ss.hasPermission('system:user:query')")
    @ApiAccessLog(operateType = EXPORT)
    public void export(HttpServletResponse response) throws IOException {
        byte[] bytes = service.exportWorkbook();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8("eDHR账号角色权限.xlsx"));
        response.getOutputStream().write(bytes);
    }

    @PostMapping("/preview")
    @Operation(summary = "预览当前瑛泰集团租户的eDHR授权导入，不写入授权")
    @PreAuthorize("@ss.hasPermission('system:role:create') and @ss.hasPermission('system:permission:assign-role-menu') and @ss.hasPermission('system:permission:assign-user-role') and @ss.hasPermission('system:permission:assign-role-data-scope') and @ss.hasPermission('system:user:query')")
    public CommonResult<EdhrPermissionTransferService.Preview> preview(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "mappings", required = false) String mappings) throws IOException {
        return success(service.preview(read(file), mappings));
    }

    @PostMapping("/confirm")
    @Operation(summary = "确认当前瑛泰集团租户的eDHR授权导入")
    @PreAuthorize("@ss.hasPermission('system:role:create') and @ss.hasPermission('system:permission:assign-role-menu') and @ss.hasPermission('system:permission:assign-user-role') and @ss.hasPermission('system:permission:assign-role-data-scope') and @ss.hasPermission('system:user:query')")
    public CommonResult<EdhrPermissionTransferService.ImportResult> confirm(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "mappings", required = false) String mappings,
            @RequestParam("token") String token, @RequestParam("reason") String reason) throws IOException {
        return success(service.confirm(read(file), mappings, token, reason));
    }

    private byte[] read(MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > EdhrPermissionWorkbook.MAX_BYTES || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx"))
            throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0(400, "请选择非空且不超过5MB的.xlsx文件");
        return file.getBytes();
    }
}
