package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchiveDownloadRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchiveGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchivePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchiveRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro/batch-record-execution-archive")
@Validated
public class MesProBatchRecordExecutionArchiveController {

    @Resource
    private MesProBatchRecordExecutionArchiveService archiveService;

    @PostMapping("/generate")
    @Operation(summary = "生成批记录执行归档")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution-archive:create')")
    public CommonResult<MesProBatchRecordExecutionArchiveRespVO> generate(
            @Valid @RequestBody MesProBatchRecordExecutionArchiveGenerateReqVO reqVO) {
        return success(archiveService.generateExecutionArchive(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询批记录执行归档")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution-archive:query')")
    public CommonResult<PageResult<MesProBatchRecordExecutionArchiveRespVO>> page(
            @Valid MesProBatchRecordExecutionArchivePageReqVO pageReqVO) {
        return success(archiveService.getExecutionArchivePage(pageReqVO));
    }

    @GetMapping("/latest")
    @Operation(summary = "查询最新批记录执行归档")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution-archive:query')")
    public CommonResult<MesProBatchRecordExecutionArchiveRespVO> latest(@RequestParam("executionId") Long executionId,
                                                                        @RequestParam("artifactType") String artifactType) {
        return success(archiveService.getLatestExecutionArchive(executionId, artifactType));
    }

    @GetMapping("/download")
    @Operation(summary = "下载批记录执行归档")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution-archive:download')")
    public ResponseEntity<byte[]> download(@RequestParam("id") Long id) {
        MesProBatchRecordExecutionArchiveDownloadRespVO file = archiveService.downloadExecutionArchive(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(file.getFileName(), StandardCharsets.UTF_8)
                .build());
        headers.setContentType(MediaType.parseMediaType(file.getContentType()));
        headers.setContentLength(file.getFileSize());
        return ResponseEntity.ok().headers(headers).body(file.getContent());
    }
}
