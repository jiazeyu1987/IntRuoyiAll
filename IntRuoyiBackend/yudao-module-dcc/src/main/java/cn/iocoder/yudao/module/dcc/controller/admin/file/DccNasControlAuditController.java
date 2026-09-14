package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasControlAuditFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasControlAuditFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasControlAuditRecognizeRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasControlAuditTaskRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasOriginalPathSyncReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasUncontrolledImportSelectedReqVO;
import cn.iocoder.yudao.module.dcc.service.file.DccNasControlAuditReportFile;
import cn.iocoder.yudao.module.dcc.service.file.DccNasControlAuditService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileNasTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "Admin - DCC NAS Control Audit")
@RestController
@RequestMapping("/dcc/controlled-files/nas-control-audit")
@Validated
public class DccNasControlAuditController {

    @Resource
    private DccNasControlAuditService auditService;
    @Resource
    private DccControlledFileNasTransferService nasTransferService;

    @PostMapping("/start")
    @Operation(summary = "Start NAS controlled-file audit")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<DccNasControlAuditTaskRespVO> startAudit() {
        return success(auditService.startTask(getLoginUserId()));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get NAS controlled-file audit task state")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<DccNasControlAuditTaskRespVO> getAuditTask(@PathVariable("taskId") Long taskId) {
        return success(auditService.getTask(taskId));
    }

    @GetMapping("/{taskId}/files")
    @Operation(summary = "Page NAS uncontrolled audit files")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<PageResult<DccNasControlAuditFileRespVO>> getAuditFilePage(
            @PathVariable("taskId") Long taskId,
            @Valid DccNasControlAuditFilePageReqVO pageReqVO) {
        return success(auditService.getTaskFilePage(taskId, pageReqVO));
    }

    @PostMapping("/{taskId}/files/recognize")
    @Operation(summary = "Recognize NAS uncontrolled audit files")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<DccNasControlAuditRecognizeRespVO> recognizeAuditFiles(
            @PathVariable("taskId") Long taskId) {
        return success(auditService.recognizeTaskFiles(taskId));
    }

    @PostMapping("/{taskId}/import-selected")
    @Operation(summary = "Create NAS uncontrolled import task for selected audit files")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit') and @ss.hasPermission('dcc:controlled-file:directory:manage') and @ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<DccControlledFileNasTransferRespVO> importSelectedAuditFiles(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody DccNasUncontrolledImportSelectedReqVO reqVO) {
        return success(nasTransferService.createUncontrolledImportTask(getLoginUserId(), taskId, reqVO));
    }

    @PostMapping("/{taskId}/original-path-sync")
    @Operation(summary = "Sync NAS uncontrolled files into DCC by original NAS path")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<DccControlledFileNasTransferRespVO> syncOriginalPathAuditFiles(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody DccNasOriginalPathSyncReqVO reqVO) {
        return success(nasTransferService.createOriginalPathSyncTask(getLoginUserId(), taskId, reqVO));
    }

    @DeleteMapping("/original-path-sync/{syncFileId}")
    @Operation(summary = "Remove an active NAS original-path sync record")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<Boolean> deleteOriginalPathSyncFile(@PathVariable("syncFileId") Long syncFileId) {
        nasTransferService.deleteOriginalPathSyncFile(getLoginUserId(), syncFileId);
        return success(true);
    }

    @GetMapping("/{taskId}/download")
    @Operation(summary = "Download NAS controlled-file audit report")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public ResponseEntity<byte[]> downloadAuditReport(@PathVariable("taskId") Long taskId) {
        DccNasControlAuditReportFile report = auditService.downloadReport(taskId);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename(report.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(report.content());
    }
}
