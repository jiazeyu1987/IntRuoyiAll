package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionArchiveDownloadRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionArchiveGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionArchiveRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
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
@RequestMapping("/mes/pro/edhr-batch-execution-archive")
@Validated
public class MesProEdhrBatchExecutionArchiveController {

    @Resource
    private MesProEdhrBatchExecutionService batchExecutionService;

    @PostMapping("/generate")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution-archive:create')")
    public CommonResult<EdhrBatchExecutionArchiveRespVO> generate(
            @Valid @RequestBody EdhrBatchExecutionArchiveGenerateReqVO reqVO) {
        return success(batchExecutionService.generateArchive(reqVO));
    }

    @GetMapping("/latest")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution-archive:query')")
    public CommonResult<EdhrBatchExecutionArchiveRespVO> latest(@RequestParam("batchExecutionId") Long batchExecutionId) {
        return success(batchExecutionService.getLatestArchive(batchExecutionId));
    }

    @GetMapping("/download")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-execution-archive:download')")
    public ResponseEntity<byte[]> download(@RequestParam("id") Long id) {
        EdhrBatchExecutionArchiveDownloadRespVO file = batchExecutionService.downloadArchive(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(file.getFileName(), StandardCharsets.UTF_8)
                .build());
        headers.setContentType(MediaType.parseMediaType(file.getContentType()));
        headers.setContentLength(file.getFileSize());
        return ResponseEntity.ok().headers(headers).body(file.getContent());
    }
}
