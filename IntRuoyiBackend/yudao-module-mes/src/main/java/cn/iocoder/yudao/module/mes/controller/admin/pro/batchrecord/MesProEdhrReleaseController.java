package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.method.HandlerMethod;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import java.util.Map;
import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.ATTRIBUTE_HANDLER_METHOD;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.*;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseCheckItemPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseCheckItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleasePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleasePrecheckReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseWithdrawReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseService;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFinalizationCommand;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - eDHR放行管理")
@RestController
@RequestMapping("/mes/pro/edhr-release")
@Validated
public class MesProEdhrReleaseController {

    @Resource
    private MesProEdhrReleaseService releaseService;
    @Resource
    private GlobalExceptionHandler globalExceptionHandler;

    // 仅公开固定业务提示；不得使用异常原文，其中可能带有签名密码或请求内容。
    private static final Map<Integer, String> SAFE_DECISION_MESSAGES = Map.ofEntries(
            Map.entry(PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS.getCode(), PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS.getMsg()),
            Map.entry(PRO_EDHR_RELEASE_PRECHECK_REQUIRED.getCode(), PRO_EDHR_RELEASE_PRECHECK_REQUIRED.getMsg()),
            Map.entry(PRO_EDHR_RELEASE_STATUS_INVALID.getCode(), PRO_EDHR_RELEASE_STATUS_INVALID.getMsg()),
            Map.entry(PRO_EDHR_RELEASE_REASON_REQUIRED.getCode(), PRO_EDHR_RELEASE_REASON_REQUIRED.getMsg()),
            Map.entry(PRO_EDHR_RELEASE_SIGNOFF_REQUIRED.getCode(), PRO_EDHR_RELEASE_SIGNOFF_REQUIRED.getMsg()),
            Map.entry(PRO_EDHR_RELEASE_IDEMPOTENCY_KEY_REQUIRED.getCode(), PRO_EDHR_RELEASE_IDEMPOTENCY_KEY_REQUIRED.getMsg()),
            Map.entry(PRO_EDHR_RELEASE_SIGNATURE_PASSWORD_REQUIRED.getCode(), PRO_EDHR_RELEASE_SIGNATURE_PASSWORD_REQUIRED.getMsg()),
            Map.entry(PRO_EDHR_RELEASE_OWNER_INVALID.getCode(), "放行责任人未配置或当前用户不是放行责任人，请核对后重试"));

    @ExceptionHandler(ServiceException.class)
    public CommonResult<?> handleServiceException(ServiceException exception, HttpServletRequest request) {
        CommonResult<?> result = globalExceptionHandler.serviceExceptionHandler(exception);
        if (request.getAttribute(ATTRIBUTE_HANDLER_METHOD) instanceof HandlerMethod handler) {
            ApiAccessLog privacy = handler.getMethodAnnotation(ApiAccessLog.class);
            String safeMessage = SAFE_DECISION_MESSAGES.get(exception.getCode());
            if (privacy != null && !privacy.requestEnable() && safeMessage != null) {
                result.setMsg(safeMessage);
            }
        }
        return result;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询 eDHR 放行候选")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:query')")
    public CommonResult<PageResult<MesProEdhrReleaseRespVO>> getPage(@Valid MesProEdhrReleasePageReqVO reqVO) {
        return success(releaseService.getPage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得 eDHR 放行预检事务")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:query')")
    public CommonResult<MesProEdhrReleaseRespVO> get(@RequestParam("id") Long id) {
        return success(releaseService.get(id));
    }

    @PostMapping("/precheck")
    @Operation(summary = "执行 eDHR 放行前检查")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:precheck')")
    public CommonResult<MesProEdhrReleaseRespVO> precheck(@Valid @RequestBody MesProEdhrReleasePrecheckReqVO reqVO) {
        return success(releaseService.precheck(reqVO));
    }

    @PostMapping("/submit")
    @Operation(summary = "负责人电子签名放行 eDHR 批次")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:submit')")
    public CommonResult<MesProEdhrReleaseRespVO> submit(@Valid @RequestBody MesProEdhrReleaseSubmitReqVO reqVO) {
        return success(releaseService.submit(reqVO));
    }

    @PostMapping("/approve")
    @Operation(summary = "批准 eDHR 放行")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:approve')")
    public CommonResult<MesProEdhrReleaseRespVO> approve(@Valid @RequestBody MesProEdhrReleaseApproveReqVO reqVO) {
        return success(releaseService.approve(reqVO));
    }

    @PostMapping("/finalize")
    @Operation(summary = "统一完成 eDHR 放行最终化")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:approve')")
    public CommonResult<MesProEdhrReleaseRespVO> finalizeRelease(
            @Valid @RequestBody MesReleaseFinalizationCommand command) {
        return success(releaseService.finalizeRelease(command));
    }

    @PostMapping("/reject")
    @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable = false)
    @Operation(summary = "驳回 eDHR 放行")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:reject')")
    public CommonResult<MesProEdhrReleaseRespVO> reject(@Valid @RequestBody MesProEdhrReleaseRejectReqVO reqVO) {
        return success(releaseService.reject(reqVO));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "撤回 eDHR 放行")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:withdraw')")
    public CommonResult<MesProEdhrReleaseRespVO> withdraw(@Valid @RequestBody MesProEdhrReleaseWithdrawReqVO reqVO) {
        return success(releaseService.withdraw(reqVO));
    }

    @GetMapping("/check-item/page")
    @Operation(summary = "分页查询 eDHR 放行前检查项")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:query')")
    public CommonResult<PageResult<MesProEdhrReleaseCheckItemRespVO>> getCheckItemPage(
            @Valid MesProEdhrReleaseCheckItemPageReqVO reqVO) {
        return success(releaseService.getCheckItemPage(reqVO));
    }

    @GetMapping("/event/page")
    @Operation(summary = "分页查询 eDHR 放行事务事件")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-release:event-query')")
    public CommonResult<PageResult<MesProEdhrReleaseEventRespVO>> getEventPage(
            @Valid MesProEdhrReleaseEventPageReqVO reqVO) {
        return success(releaseService.getEventPage(reqVO));
    }
}
