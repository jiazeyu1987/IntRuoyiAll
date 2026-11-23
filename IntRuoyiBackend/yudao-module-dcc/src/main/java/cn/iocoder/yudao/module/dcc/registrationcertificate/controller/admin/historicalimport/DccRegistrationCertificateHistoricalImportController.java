package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.historicalimport;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.historicalimport.vo.DccRegistrationCertificateHistoricalImportPageReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.historicalimport.vo.DccRegistrationCertificateHistoricalImportRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.historicalimport.DccRegistrationCertificateHistoricalImportService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.historicalimport.RegistrationCertificateWorkbookImportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 注册证历史导入")
@RestController
@RequestMapping("/dcc/registration-certificates/historical-import")
@Validated
public class DccRegistrationCertificateHistoricalImportController {

    @Resource
    private DccRegistrationCertificateHistoricalImportService historicalImportService;

    @Resource
    private RegistrationCertificateWorkbookImportService workbookImport;

    @PostMapping("/workbook")
    @Operation(summary = "批量导入注册证 Excel")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:historical-import') and @ss.hasPermission('dcc:registration-certificate:upload:approve')")
    public CommonResult<RegistrationCertificateWorkbookImportService.ImportResult> importWorkbook(
            @RequestParam("file") MultipartFile file) throws IOException {
        return success(workbookImport.importFile(file,
                SecurityFrameworkUtils.getLoginUserId()));
    }

    @GetMapping("/page")
    @Operation(summary = "获得注册证历史导入分页")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:historical-import')")
    public CommonResult<PageResult<DccRegistrationCertificateHistoricalImportRespVO>> getHistoricalImportPage(
            @Valid DccRegistrationCertificateHistoricalImportPageReqVO reqVO) {
        return success(historicalImportService.getHistoricalImportPage(reqVO));
    }
}
