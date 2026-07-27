package cn.iocoder.yudao.module.bpm.controller.admin.formcenter;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.*;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 表单中心")
@RestController
@RequestMapping("/form-center")
@Validated
public class FormCenterController {

    @Resource
    private FormCenterRuntimeService formCenterRuntimeService;

    @GetMapping("/template-pool")
    @Operation(summary = "查询表单模板发布池")
    @PreAuthorize("@ss.hasPermission('form:template:query')")
    public CommonResult<PageResult<FormCenterTemplateRespVO>> getTemplatePool(
            @Valid FormCenterTemplatePoolPageReqVO reqVO) {
        return success(formCenterRuntimeService.getTemplatePool(reqVO));
    }

    @GetMapping("/templates/{templateId}/versions/{versionNo}")
    @Operation(summary = "查询指定表单模板版本")
    @PreAuthorize("@ss.hasPermission('form:template:query')")
    public CommonResult<FormCenterTemplateRespVO> getTemplateVersion(
            @PathVariable("templateId") Long templateId,
            @PathVariable("versionNo") String versionNo) {
        return success(formCenterRuntimeService.getTemplateVersion(templateId, versionNo));
    }

    @GetMapping("/policies")
    @Operation(summary = "查询表单策略")
    @PreAuthorize("@ss.hasPermission('form:policy:query')")
    public CommonResult<PageResult<FormPolicyRespVO>> getPolicyPage(@Valid FormPolicyPageReqVO reqVO) {
        return success(formCenterRuntimeService.getPolicyPage(reqVO));
    }

    @PostMapping("/policies")
    @Operation(summary = "保存表单策略")
    @PreAuthorize("@ss.hasPermission('form:policy:create')")
    public CommonResult<FormPolicyRespVO> savePolicy(@Valid @RequestBody FormPolicySaveReqVO reqVO) {
        return success(formCenterRuntimeService.savePolicy(reqVO));
    }

    @PostMapping("/policies/{policyId}/publish")
    @Operation(summary = "发布表单策略")
    @PreAuthorize("@ss.hasPermission('form:policy:publish')")
    public CommonResult<Boolean> publishPolicy(@PathVariable("policyId") Long policyId) {
        formCenterRuntimeService.publishPolicy(policyId);
        return success(true);
    }

    @PostMapping("/policies/{policyId}/switch-approval-mode")
    @Operation(summary = "切换表单策略审批模式")
    @PreAuthorize("@ss.hasPermission('form:policy:publish')")
    public CommonResult<FormPolicyRespVO> switchPolicyApprovalMode(@PathVariable("policyId") Long policyId,
            @Valid @RequestBody FormPolicySwitchApprovalModeReqVO reqVO) {
        return success(formCenterRuntimeService.switchPolicyApprovalMode(policyId, reqVO));
    }

    @PostMapping("/templates/import-doc")
    @Operation(summary = "导入 doc/docx 表单模板")
    @PreAuthorize("@ss.hasPermission('form:template:create')")
    public CommonResult<FormCenterTemplateImportRespVO> importDoc(@Valid FormCenterTemplateImportReqVO reqVO) {
        return success(formCenterRuntimeService.importDoc(reqVO, WebFrameworkUtils.getLoginUserId()));
    }

    @PutMapping("/templates/{templateId}/versions/{versionNo}/jimu-schema")
    @Operation(summary = "保存模板 Jimu 调整结果")
    @PreAuthorize("@ss.hasPermission('form:template:update')")
    public CommonResult<Boolean> saveJimuSchema(@PathVariable("templateId") Long templateId,
            @PathVariable("versionNo") String versionNo, @Valid @RequestBody FormCenterTemplateJimuSchemaReqVO reqVO) {
        formCenterRuntimeService.saveJimuSchema(templateId, versionNo, reqVO);
        return success(true);
    }

    @PostMapping("/templates/{templateId}/versions/{versionNo}/publish")
    @Operation(summary = "发布模板版本")
    @PreAuthorize("@ss.hasPermission('form:template:publish')")
    public CommonResult<Boolean> publishTemplate(@PathVariable("templateId") Long templateId,
            @PathVariable("versionNo") String versionNo) {
        formCenterRuntimeService.publishTemplate(templateId, versionNo);
        return success(true);
    }

    @PostMapping("/templates/{templateId}/versions/{versionNo}/disable")
    @Operation(summary = "停用模板版本")
    @PreAuthorize("@ss.hasPermission('form:template:disable')")
    public CommonResult<Boolean> disableTemplate(@PathVariable("templateId") Long templateId,
            @PathVariable("versionNo") String versionNo) {
        formCenterRuntimeService.disableTemplate(templateId, versionNo);
        return success(true);
    }

    @PostMapping("/templates/{templateId}/versions/{versionNo}/enable")
    @Operation(summary = "启用模板版本")
    @PreAuthorize("@ss.hasPermission('form:template:disable')")
    public CommonResult<Boolean> enableTemplate(@PathVariable("templateId") Long templateId,
            @PathVariable("versionNo") String versionNo) {
        formCenterRuntimeService.enableTemplate(templateId, versionNo);
        return success(true);
    }

    @PostMapping("/templates/{templateId}/versions/{versionNo}/obsolete")
    @Operation(summary = "作废模板版本")
    @PreAuthorize("@ss.hasPermission('form:template:obsolete')")
    public CommonResult<Boolean> obsoleteTemplate(@PathVariable("templateId") Long templateId,
            @PathVariable("versionNo") String versionNo) {
        formCenterRuntimeService.obsoleteTemplate(templateId, versionNo);
        return success(true);
    }

    @PostMapping("/templates/{templateId}/versions/{versionNo}/obsolete-request")
    @Operation(summary = "提交模板作废申请")
    @PreAuthorize("@ss.hasPermission('form:template:obsolete')")
    public CommonResult<FormTemplateObsoleteRespVO> submitTemplateObsoleteRequest(
            @PathVariable("templateId") Long templateId,
            @PathVariable("versionNo") String versionNo,
            @Valid @RequestBody FormTemplateObsoleteReqVO reqVO) {
        return success(formCenterRuntimeService.submitTemplateObsoleteRequest(templateId, versionNo, reqVO,
                WebFrameworkUtils.getLoginUserId()));
    }

    @GetMapping("/templates/{templateId}/versions/{versionNo}/obsolete-request/pending")
    @Operation(summary = "查询模板作废审批中申请")
    @PreAuthorize("@ss.hasPermission('form:template:query')")
    public CommonResult<FormTemplateObsoletePendingRespVO> findTemplateObsoletePendingRequest(
            @PathVariable("templateId") Long templateId,
            @PathVariable("versionNo") String versionNo) {
        return success(formCenterRuntimeService.findTemplateObsoletePendingRequest(templateId, versionNo,
                WebFrameworkUtils.getLoginUserId()));
    }

    @PostMapping("/templates/{templateId}/versions/{versionNo}/obsolete-request/withdraw")
    @Operation(summary = "撤回模板作废申请")
    @PreAuthorize("@ss.hasPermission('form:template:obsolete')")
    public CommonResult<Boolean> withdrawTemplateObsoleteRequest(@PathVariable("templateId") Long templateId,
            @PathVariable("versionNo") String versionNo,
            @RequestBody(required = false) FormTemplateObsoleteWithdrawReqVO reqVO) {
        formCenterRuntimeService.withdrawTemplateObsoleteRequest(templateId, versionNo,
                reqVO == null ? null : reqVO.getReason(), WebFrameworkUtils.getLoginUserId());
        return success(true);
    }

    @GetMapping("/templates/{templateId}/versions/{versionNo}/source-file")
    @Operation(summary = "下载模板源文件")
    @PreAuthorize("@ss.hasPermission('form:template-source:download')")
    public void downloadSourceFile(@PathVariable("templateId") Long templateId,
            @PathVariable("versionNo") String versionNo, HttpServletResponse response) throws IOException {
        byte[] bytes = formCenterRuntimeService.getTemplateSourceFile(templateId, versionNo);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"form-template-" + templateId
                + "-" + versionNo + ".docx\"");
        response.getOutputStream().write(bytes);
    }

    @PostMapping("/actions/resolve")
    @Operation(summary = "解析业务动作表单策略")
    @PreAuthorize("@ss.hasPermission('form:instance:create')")
    public CommonResult<FormActionResolutionRespVO> resolveAction(@Valid @RequestBody BusinessActionContextReqVO reqVO) {
        return success(formCenterRuntimeService.resolveAction(reqVO));
    }

    @PostMapping("/actions/active-instance")
    @Operation(summary = "查询业务对象当前活动表单实例")
    @PreAuthorize("@ss.hasPermission('form:instance:create')")
    public CommonResult<FormInstanceRespVO> findActiveBusinessAction(
            @Valid @RequestBody BusinessActionContextReqVO reqVO) {
        return success(formCenterRuntimeService.findActiveBusinessAction(reqVO));
    }

    @PostMapping("/instances")
    @Operation(summary = "创建表单实例草稿")
    @PreAuthorize("@ss.hasPermission('form:instance:create')")
    public CommonResult<FormInstanceRespVO> createInstance(@Valid @RequestBody FormInstanceCreateReqVO reqVO) {
        return success(formCenterRuntimeService.createInstance(reqVO, WebFrameworkUtils.getLoginUserId()));
    }

    @PutMapping("/instances/{instanceId}/draft")
    @Operation(summary = "保存表单实例草稿")
    @PreAuthorize("@ss.hasPermission('form:instance:update')")
    public CommonResult<Boolean> saveDraft(@PathVariable("instanceId") Long instanceId,
            @Valid @RequestBody FormInstanceDraftReqVO reqVO) {
        formCenterRuntimeService.saveDraft(instanceId, reqVO, WebFrameworkUtils.getLoginUserId());
        return success(true);
    }

    @PostMapping("/instances/{instanceId}/submit")
    @Operation(summary = "提交表单实例")
    @PreAuthorize("@ss.hasPermission('form:instance:submit')")
    public CommonResult<FormInstanceRespVO> submitInstance(@PathVariable("instanceId") Long instanceId,
            @Valid @RequestBody FormInstanceSubmitReqVO reqVO) {
        return success(formCenterRuntimeService.submitInstance(instanceId, reqVO, WebFrameworkUtils.getLoginUserId()));
    }

    @PostMapping("/instances/{instanceId}/rework-submit")
    @Operation(summary = "返工重提表单实例")
    @PreAuthorize("@ss.hasPermission('form:instance:submit')")
    public CommonResult<Boolean> reworkSubmitInstance(@PathVariable("instanceId") Long instanceId,
            @Valid @RequestBody FormInstanceSubmitReqVO reqVO) {
        formCenterRuntimeService.reworkSubmitInstance(instanceId, reqVO, WebFrameworkUtils.getLoginUserId());
        return success(true);
    }

    @PostMapping("/instances/{instanceId}/abandon")
    @Operation(summary = "放弃表单实例")
    @PreAuthorize("@ss.hasPermission('form:instance:abandon')")
    public CommonResult<Boolean> abandonInstance(@PathVariable("instanceId") Long instanceId) {
        formCenterRuntimeService.abandonInstance(instanceId, WebFrameworkUtils.getLoginUserId());
        return success(true);
    }

    @PostMapping("/bpm/task-created")
    @Operation(summary = "BPM 当前任务创建回调")
    @PreAuthorize("@ss.hasPermission('form:bpm-callback:handle')")
    public CommonResult<Boolean> onBpmTaskCreated(@Valid @RequestBody FormBpmTaskCreatedReqVO reqVO) {
        formCenterRuntimeService.onBpmTaskCreated(reqVO);
        return success(true);
    }

    @PostMapping("/bpm/task-completed")
    @Operation(summary = "BPM 当前任务完成回调")
    @PreAuthorize("@ss.hasPermission('form:bpm-callback:handle')")
    public CommonResult<Boolean> onBpmTaskCompleted(@Valid @RequestBody FormBpmTaskCompletedReqVO reqVO) {
        formCenterRuntimeService.onBpmTaskCompleted(reqVO);
        return success(true);
    }

    @PostMapping("/bpm/rework-required")
    @Operation(summary = "BPM 返工回调")
    @PreAuthorize("@ss.hasPermission('form:bpm-callback:handle')")
    public CommonResult<Boolean> onBpmReworkRequired(@Valid @RequestBody FormBpmReworkRequiredReqVO reqVO) {
        formCenterRuntimeService.onBpmReworkRequired(reqVO);
        return success(true);
    }

    @PostMapping("/bpm/process-rejected")
    @Operation(summary = "BPM 流程驳回回调")
    @PreAuthorize("@ss.hasPermission('form:bpm-callback:handle')")
    public CommonResult<Boolean> onBpmProcessRejected(@Valid @RequestBody FormBpmProcessRejectedReqVO reqVO) {
        formCenterRuntimeService.onBpmProcessRejected(reqVO);
        return success(true);
    }

    @PostMapping("/bpm/process-approved")
    @Operation(summary = "BPM 流程全部通过回调")
    @PreAuthorize("@ss.hasPermission('form:bpm-callback:handle')")
    public CommonResult<FormEffectExecutionRespVO> onBpmProcessApproved(
            @Valid @RequestBody FormBpmProcessApprovedReqVO reqVO) {
        return success(formCenterRuntimeService.onBpmProcessApproved(reqVO));
    }

    @GetMapping("/instances/{instanceId}/snapshots")
    @Operation(summary = "查询表单实例快照")
    @PreAuthorize("@ss.hasPermission('form:instance:snapshot:query')")
    public CommonResult<java.util.List<FormInstanceSnapshotRespVO>> getInstanceSnapshots(
            @PathVariable("instanceId") Long instanceId) {
        return success(formCenterRuntimeService.getInstanceSnapshots(instanceId));
    }

    @GetMapping("/effects/pending")
    @Operation(summary = "查询生效失败待处理")
    @PreAuthorize("@ss.hasPermission('form:effect:query')")
    public CommonResult<PageResult<FormEffectExecutionRespVO>> getPendingEffects(
            @Valid FormEffectPendingPageReqVO reqVO) {
        return success(formCenterRuntimeService.getPendingEffects(reqVO));
    }

    @PostMapping("/effects/{instanceId}/retry")
    @Operation(summary = "重试表单生效")
    @PreAuthorize("@ss.hasPermission('form:effect:retry')")
    public CommonResult<FormEffectExecutionRespVO> retryEffect(@PathVariable("instanceId") Long instanceId) {
        return success(formCenterRuntimeService.retryEffect(instanceId));
    }

    @ExceptionHandler(FormCenterException.class)
    public CommonResult<?> handleFormCenterException(FormCenterException ex) {
        return CommonResult.error(ex.getErrorCode().getCode(), ex.getMessage());
    }

}
