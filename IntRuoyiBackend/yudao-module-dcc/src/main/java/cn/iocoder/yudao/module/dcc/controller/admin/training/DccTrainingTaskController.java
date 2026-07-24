package cn.iocoder.yudao.module.dcc.controller.admin.training;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingTaskPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingTaskRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingViewSessionHeartbeatReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingViewSessionStartReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingViewSessionStopReqVO;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.dcc.service.file.DccTrainingTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - DCC 培训任务")
@RestController
@RequestMapping("/dcc/training-tasks")
@Validated
public class DccTrainingTaskController {

    @Resource
    private DccTrainingTaskService trainingTaskService;

    @GetMapping("/my-page")
    @Operation(summary = "获取当前登录人的培训任务分页")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:training:mine')")
    public CommonResult<PageResult<DccTrainingTaskRespVO>> getMyPage(@Valid DccTrainingTaskPageReqVO reqVO) {
        return success(trainingTaskService.getMyTrainingTaskPage(getLoginUserId(), reqVO));
    }

    @GetMapping("/{progressId}")
    @Operation(summary = "获取培训任务详情")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:training:mine')")
    public CommonResult<DccTrainingTaskRespVO> getTask(@PathVariable("progressId") Long progressId) {
        return success(trainingTaskService.getTrainingTask(getLoginUserId(), progressId));
    }

    @GetMapping("/{progressId}/preview")
    @Operation(summary = "预览培训文件")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:training:mine')")
    public ResponseEntity<byte[]> preview(@PathVariable("progressId") Long progressId,
                                          HttpServletRequest request) {
        var binary = trainingTaskService.readTrainingPreviewFile(getLoginUserId(), progressId,
                DccRequestAuditContext.from(request, null));
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(binary.contentType()))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.inline().filename(binary.fileName()).build().toString())
                .header(org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        DccControlledFileController.PREVIEW_WATERMARK_HEADER)
                .header(DccControlledFileController.PREVIEW_WATERMARK_HEADER, Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(JsonUtils.toJsonString(binary.watermark()).getBytes(StandardCharsets.UTF_8)))
                .body(binary.bytes());
    }

    @PostMapping("/{progressId}/view-session/start")
    @Operation(summary = "开始培训查看会话")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:training:mine')")
    public CommonResult<DccTrainingTaskRespVO> start(@PathVariable("progressId") Long progressId,
                                                     @Valid @RequestBody DccTrainingViewSessionStartReqVO reqVO) {
        return success(trainingTaskService.startViewSession(getLoginUserId(), progressId, reqVO));
    }

    @PostMapping("/{progressId}/view-session/heartbeat")
    @Operation(summary = "上报培训查看心跳")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:training:mine')")
    public CommonResult<DccTrainingTaskRespVO> heartbeat(@PathVariable("progressId") Long progressId,
                                                         @Valid @RequestBody DccTrainingViewSessionHeartbeatReqVO reqVO) {
        return success(trainingTaskService.heartbeatViewSession(getLoginUserId(), progressId, reqVO));
    }

    @PostMapping("/{progressId}/view-session/stop")
    @Operation(summary = "结束培训查看会话")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:training:mine')")
    public CommonResult<DccTrainingTaskRespVO> stop(@PathVariable("progressId") Long progressId,
                                                    @Valid @RequestBody DccTrainingViewSessionStopReqVO reqVO) {
        return success(trainingTaskService.stopViewSession(getLoginUserId(), progressId, reqVO));
    }

    @PostMapping("/{progressId}/acknowledge")
    @Operation(summary = "确认培训完成")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:training:mine')")
    public CommonResult<Boolean> acknowledge(@PathVariable("progressId") Long progressId) {
        trainingTaskService.acknowledgeTraining(getLoginUserId(), progressId);
        return success(true);
    }
}
