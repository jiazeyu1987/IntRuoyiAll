package cn.iocoder.yudao.module.dcc.controller.admin.signature;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignaturePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureEvidenceRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureVerifyRespVO;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - DCC电子签名")
@RestController
@RequestMapping("/dcc/electronic-signatures")
@Validated
public class DccElectronicSignatureController {

    @Resource
    private DccElectronicSignatureManagementService managementService;

    @GetMapping("/page")
    @Operation(summary = "获取DCC电子签名记录分页")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:signature:manage')")
    public CommonResult<PageResult<DccElectronicSignatureRespVO>> getSignaturePage(
            @Valid DccElectronicSignaturePageReqVO reqVO) {
        return success(managementService.getSignaturePage(reqVO));
    }

    @GetMapping("/{id}/evidence")
    @Operation(summary = "获取DCC电子签名证据详情")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:signature:manage')")
    public CommonResult<DccSignatureEvidenceRespVO> getSignatureEvidence(@PathVariable("id") Long id) {
        return success(managementService.getSignatureEvidenceDetail(id));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "校验DCC电子签名证据")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:signature:manage')")
    public CommonResult<DccSignatureVerifyRespVO> verifySignatureEvidence(@PathVariable("id") Long id) {
        return success(managementService.verifySignatureEvidence(id));
    }
}
