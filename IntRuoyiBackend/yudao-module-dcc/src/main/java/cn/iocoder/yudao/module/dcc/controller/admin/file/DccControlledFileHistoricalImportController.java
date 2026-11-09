package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileHistoricalImportReqVO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileHistoricalImportService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/dcc/controlled-file-imports")
public class DccControlledFileHistoricalImportController {
    @Resource private DccControlledFileHistoricalImportService imports;

    @GetMapping("/options")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:import')")
    public CommonResult<DccControlledFileHistoricalImportService.Options> options(
            @RequestParam(required=false) Long dccProjectCodeId) {
        return success(imports.options(getLoginUserId(),dccProjectCodeId));
    }

    @PostMapping(consumes="multipart/form-data")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:import')")
    public CommonResult<DccControlledFileHistoricalImportService.Result> create(
            @Valid @ModelAttribute DccControlledFileHistoricalImportReqVO request) throws java.io.IOException {
        return success(imports.create(getLoginUserId(),request));
    }
}
