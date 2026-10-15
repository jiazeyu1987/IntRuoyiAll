package cn.iocoder.yudao.module.dcc.controller.admin.distribution;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionTaskPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionTaskRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionRecipientReceiptRespVO;
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import cn.iocoder.yudao.module.dcc.service.file.DccDistributionTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - DCC 分发待办")
@RestController
@RequestMapping("/dcc/distribution-tasks")
@Validated
public class DccDistributionTaskController {

    @Resource
    private DccDistributionTaskService distributionTaskService;

    @PostMapping("/automatic-retry")
    @Operation(summary="按精确版本批次重试服务端自动下发，不接受客户端名册")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:distribute')")
    public CommonResult<cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccAutomaticDistributionResultRespVO> retryAutomaticDistribution(
            @Valid @RequestBody cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccAutomaticDistributionRetryReqVO request) {
        return success(distributionTaskService.retryAutomaticDistribution(getLoginUserId(),request));
    }
    @GetMapping("/automatic-result")
    @Operation(summary="文控读取准确版本下发批次或真实失败状态")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:distribute')")
    public CommonResult<cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccAutomaticDistributionResultRespVO> getAutomaticDistributionResult(@RequestParam("controlledFileId") Long fileId) {
        return success(distributionTaskService.getAutomaticDistributionResult(getLoginUserId(),fileId));
    }

    @GetMapping("/recipient-receipts")
    @Operation(summary = "文控查看精确版本逐人发送与签收事实")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:distribute')")
    public CommonResult<List<DccDistributionRecipientReceiptRespVO>> getRecipientReceipts(@RequestParam("controlledFileId") Long controlledFileId) {
        return success(distributionTaskService.getDistributionRecipientReceipts(getLoginUserId(), controlledFileId));
    }

    @GetMapping("/my-page")
    @Operation(summary = "获取当前登录人的分发签收待办分页")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<PageResult<DccDistributionTaskRespVO>> getMyPage(
            @Valid DccDistributionTaskPageReqVO reqVO) {
        return success(distributionTaskService.getMyDistributionTaskPage(getLoginUserId(), reqVO));
    }
}
