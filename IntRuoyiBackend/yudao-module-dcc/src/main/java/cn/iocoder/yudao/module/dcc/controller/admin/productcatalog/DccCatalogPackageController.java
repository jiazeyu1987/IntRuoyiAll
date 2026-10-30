package cn.iocoder.yudao.module.dcc.controller.admin.productcatalog;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.service.productcatalog.packageio.DccCatalogPackageService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;

@RestController
@RequestMapping("/dcc/product-catalog/workbook")
public class DccCatalogPackageController {
    @Resource private DccCatalogPackageService service;
    @ExceptionHandler(IllegalArgumentException.class)
    public CommonResult<Void> invalidWorkbook(IllegalArgumentException exception) {
        return CommonResult.error(BAD_REQUEST.getCode(), exception.getMessage());
    }
    @GetMapping("/export")
    @PreAuthorize("@ss.hasAnyRoles('super_admin', 'tenant_admin') or (@ss.hasPermission('mdm:product:query') and @ss.hasPermission('dcc:project-code:query') and @ss.hasPermission('mdm:enterprise:query') and @ss.hasPermission('mdm:company-scope:query') and @ss.hasPermission('dcc:registration-certificate:query-current'))")
    public void exportWorkbook(HttpServletResponse response) throws IOException {
        byte[] bytes = service.exportWorkbook();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=dcc-product-catalog.xlsx");
        response.getOutputStream().write(bytes);
    }
    @PostMapping("/import")
    @PreAuthorize("@ss.hasAnyRoles('super_admin', 'tenant_admin') or (@ss.hasPermission('mdm:product:create') and @ss.hasPermission('mdm:product:update') and @ss.hasPermission('dcc:project-code:create') and @ss.hasPermission('dcc:project-code:update') and @ss.hasPermission('mdm:enterprise:create') and @ss.hasPermission('mdm:enterprise:update') and @ss.hasPermission('mdm:company-scope:create') and @ss.hasPermission('mdm:company-scope:update') and @ss.hasPermission('dcc:registration-certificate:config:update'))")
    public CommonResult<Map<String, Integer>> importWorkbook(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 20L * 1024 * 1024) throw new IllegalArgumentException("请选择 20MB 以内的 Excel 工作簿");
        return success(service.importWorkbook(file.getBytes()));
    }
}
