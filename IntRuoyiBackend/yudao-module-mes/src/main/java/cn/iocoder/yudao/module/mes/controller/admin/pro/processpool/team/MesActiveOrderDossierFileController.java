package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesActiveOrderDossierFileService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro/process-pool/team-leader/active-order/dossier-files")
@RequiredArgsConstructor
public class MesActiveOrderDossierFileController {

    private final MesActiveOrderDossierFileService dossierFileService;

    @GetMapping
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-process-pool-team-leader:query', 'mes:pro-production-release:query')")
    public CommonResult<MesActiveOrderDossierFileService.Result> list(
            @RequestParam("activeOrderId") Long activeOrderId,
            @RequestParam(value = "applicationId", required = false) Long applicationId) {
        return success(dossierFileService.list(SecurityFrameworkUtils.getLoginUserId(),
                new MesActiveOrderDossierFileService.Query(activeOrderId, applicationId)));
    }

    @PostMapping("/upload")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-process-pool-team-leader:maintain', 'mes:pro-production-release:pqc-approve')")
    public CommonResult<MesActiveOrderDossierFileService.FileItem> upload(
            @RequestParam("activeOrderId") Long activeOrderId,
            @RequestParam(value = "applicationId", required = false) Long applicationId,
            @RequestParam("categoryKey") String categoryKey,
            @RequestParam("file") MultipartFile file) throws IOException {
        return success(dossierFileService.upload(SecurityFrameworkUtils.getLoginUserId(),
                new MesActiveOrderDossierFileService.UploadCommand(activeOrderId, applicationId, categoryKey,
                        file.getOriginalFilename(), file.getContentType(), file.getBytes())));
    }

    @PostMapping("/delete")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-process-pool-team-leader:maintain', 'mes:pro-production-release:pqc-approve')")
    public CommonResult<Boolean> delete(@RequestBody DeleteReqVO reqVO) {
        dossierFileService.delete(SecurityFrameworkUtils.getLoginUserId(),
                new MesActiveOrderDossierFileService.DeleteCommand(reqVO.getActiveOrderId(),
                        reqVO.getApplicationId(), reqVO.getCategoryKey(), reqVO.getAttachmentId()));
        return success(true);
    }

    @Data
    public static class DeleteReqVO {
        private Long activeOrderId;
        private Long applicationId;
        private String categoryKey;
        private Long attachmentId;
    }
}
