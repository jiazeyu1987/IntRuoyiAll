package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.historicalimport;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.historicalimport.vo.DccRegistrationCertificateHistoricalImportPageReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.historicalimport.vo.DccRegistrationCertificateHistoricalImportRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.historicalimport.DccRegistrationCertificateHistoricalImportService;
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

    @GetMapping("/page")
    @Operation(summary = "获得注册证历史导入分页")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:historical-import')")
    public CommonResult<PageResult<DccRegistrationCertificateHistoricalImportRespVO>> getHistoricalImportPage(
            @Valid DccRegistrationCertificateHistoricalImportPageReqVO reqVO) {
        return success(historicalImportService.getHistoricalImportPage(reqVO));
    }
}
