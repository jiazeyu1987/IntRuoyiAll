package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalPrintTemplateRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalPrintTemplateSaveReqVO;
import cn.iocoder.yudao.module.dcc.service.file.DccApprovalPrintTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "Admin - DCC Approval Print Template")
@RestController
@RequestMapping("/dcc/approval-print-template")
@Validated
public class DccApprovalPrintTemplateController {

    @Resource
    private DccApprovalPrintTemplateService approvalPrintTemplateService;

    @GetMapping("/active")
    @Operation(summary = "Get active DCC approval print template")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<DccApprovalPrintTemplateRespVO> getActiveTemplate() {
        return success(approvalPrintTemplateService.getActiveTemplate());
    }

    @PostMapping("/save")
    @Operation(summary = "Save active DCC approval print template")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:print-template:manage')")
    public CommonResult<DccApprovalPrintTemplateRespVO> saveActiveTemplate(
            @Valid @RequestBody DccApprovalPrintTemplateSaveReqVO reqVO) {
        return success(approvalPrintTemplateService.saveActiveTemplate(getLoginUserId(), reqVO));
    }
}
