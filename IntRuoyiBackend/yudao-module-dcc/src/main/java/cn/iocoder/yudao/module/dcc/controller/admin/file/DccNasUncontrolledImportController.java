package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasUncontrolledImportLocalWriteResultReqVO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBinary;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileNasTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "Admin - DCC NAS Uncontrolled Import")
@RestController
@RequestMapping("/dcc/controlled-files/nas-uncontrolled-import")
@Validated
public class DccNasUncontrolledImportController {

    private static final String SOURCE_SIGNATURE_HEADER = "X-Source-Signature";

    @Resource
    private DccControlledFileNasTransferService nasTransferService;

    @GetMapping("/tasks/{importTaskId}/files/{auditFileId}/content")
    @Operation(summary = "Download selected NAS uncontrolled import content")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit') and @ss.hasPermission('dcc:controlled-file:directory:manage') and @ss.hasPermission('dcc:controlled-file:category:manage')")
    public ResponseEntity<byte[]> downloadImportContent(
            @PathVariable("importTaskId") Long importTaskId,
            @PathVariable("auditFileId") Long auditFileId,
            @RequestParam("sourceSignature") String sourceSignature,
            @RequestParam("localRelativePath") String localRelativePath) {
        DccControlledFileBinary binary = nasTransferService.readUncontrolledImportContent(
                getLoginUserId(), importTaskId, auditFileId, sourceSignature, localRelativePath);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(binary.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(binary.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        HttpHeaders.CONTENT_DISPOSITION + "," + SOURCE_SIGNATURE_HEADER)
                .header(SOURCE_SIGNATURE_HEADER, sourceSignature)
                .body(binary.bytes());
    }

    @PostMapping("/tasks/{importTaskId}/files/{auditFileId}/local-write-result")
    @Operation(summary = "Record selected NAS uncontrolled import local write result")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit') and @ss.hasPermission('dcc:controlled-file:directory:manage') and @ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<DccControlledFileNasTransferRespVO> recordLocalWriteResult(
            @PathVariable("importTaskId") Long importTaskId,
            @PathVariable("auditFileId") Long auditFileId,
            @Valid @RequestBody DccNasUncontrolledImportLocalWriteResultReqVO reqVO) {
        return success(nasTransferService.recordUncontrolledImportLocalWriteResult(
                getLoginUserId(), importTaskId, auditFileId, reqVO));
    }
}
