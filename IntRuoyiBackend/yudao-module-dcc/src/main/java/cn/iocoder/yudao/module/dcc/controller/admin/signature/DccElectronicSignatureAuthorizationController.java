package cn.iocoder.yudao.module.dcc.controller.admin.signature;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationAuditPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationAuditRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureImageRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationUnlockReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationUpdateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureAuthorizationRespVO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - DCC电子签名授权")
@RestController
@RequestMapping("/dcc/electronic-signature-authorizations")
@Validated
public class DccElectronicSignatureAuthorizationController {

    @Resource
    private DccElectronicSignatureManagementService managementService;

    @GetMapping("/page")
    @Operation(summary = "获取DCC电子签名授权分页")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:signature:manage') and @ss.hasRole('electronic_signature_admin')")
    public CommonResult<PageResult<DccElectronicSignatureAuthorizationRespVO>> getAuthorizationPage(
            @Valid DccElectronicSignatureAuthorizationPageReqVO reqVO) {
        return success(managementService.getAuthorizationPage(reqVO));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "更新DCC电子签名授权")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:signature:manage') and @ss.hasRole('electronic_signature_admin')")
    public CommonResult<DccSignatureAuthorizationRespVO> updateAuthorization(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody DccElectronicSignatureAuthorizationUpdateReqVO reqVO) {
        return success(managementService.updateAuthorization(userId, reqVO.getElectronicSignatureEnabled(),
                getLoginUserId(), reqVO.getReason()));
    }

    @GetMapping("/{userId}/audits/page")
    @Operation(summary = "获取DCC电子签名授权审计分页")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:signature:manage') and @ss.hasRole('electronic_signature_admin')")
    public CommonResult<PageResult<DccElectronicSignatureAuthorizationAuditRespVO>> getAuthorizationAuditPage(
            @PathVariable("userId") Long userId,
            @Valid DccElectronicSignatureAuthorizationAuditPageReqVO reqVO) {
        return success(managementService.getAuthorizationAuditPage(userId, reqVO));
    }

    @GetMapping("/my-image")
    @Operation(summary = "获取当前用户DCC电子签名图片")
    public CommonResult<DccElectronicSignatureImageRespVO> getMySignatureImage() {
        return success(managementService.getMySignatureImage(getLoginUserId()));
    }

    @PostMapping("/my-image/upload")
    @Operation(summary = "上传当前用户DCC电子签名图片")
    public CommonResult<DccElectronicSignatureImageRespVO> uploadMySignatureImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "reason", required = false) String reason) {
        Long loginUserId = getLoginUserId();
        return success(managementService.uploadMySignatureImage(loginUserId, file, loginUserId, reason));
    }

    @PostMapping("/my-image/{imageId}/enable")
    @Operation(summary = "启用当前用户DCC电子签名图片")
    public CommonResult<DccElectronicSignatureImageRespVO> enableMySignatureImage(
            @PathVariable("imageId") Long imageId,
            @RequestParam(value = "reason", required = false) String reason) {
        Long loginUserId = getLoginUserId();
        return success(managementService.enableMySignatureImage(loginUserId, imageId, loginUserId, reason));
    }

    @PostMapping("/my-image/disable")
    @Operation(summary = "停用当前用户DCC电子签名图片")
    public CommonResult<DccElectronicSignatureImageRespVO> disableMySignatureImage(
            @RequestParam(value = "reason", required = false) String reason) {
        Long loginUserId = getLoginUserId();
        return success(managementService.disableMySignatureImage(loginUserId, loginUserId, reason));
    }

    @PostMapping("/{userId}/unlock")
    @Operation(summary = "解锁DCC电子签名授权")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:signature:manage') and @ss.hasRole('electronic_signature_admin')")
    public CommonResult<DccSignatureAuthorizationRespVO> unlockAuthorization(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody DccElectronicSignatureAuthorizationUnlockReqVO reqVO) {
        return success(managementService.unlockAuthorization(userId, getLoginUserId(), reqVO.getReason()));
    }
}
