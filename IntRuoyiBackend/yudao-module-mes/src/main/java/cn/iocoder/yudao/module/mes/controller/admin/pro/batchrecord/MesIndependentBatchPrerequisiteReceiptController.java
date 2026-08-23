package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesIndependentBatchPrerequisiteReceiptIssueReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesIndependentBatchPrerequisiteReceiptRevokeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesIndependentBatchPrerequisiteReceiptRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesIndependentBatchPrerequisiteReceiptVerifyReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptIssueCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptRevokeCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptVerifyCommand;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro/edhr-batch-entry-receipt")
@Validated
public class MesIndependentBatchPrerequisiteReceiptController {

    @Resource
    private MesIndependentBatchPrerequisiteReceiptService receiptService;

    @PostMapping("/issue")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-entry-receipt:issue')")
    public CommonResult<MesIndependentBatchPrerequisiteReceiptRespVO> issue(
            @Valid @RequestBody MesIndependentBatchPrerequisiteReceiptIssueReqVO req) {
        Long tenantId = TenantContextHolder.getTenantId();
        Long actorId = SecurityFrameworkUtils.getLoginUserId();
        MesIndependentBatchPrerequisiteReceiptIssueCommand command = new MesIndependentBatchPrerequisiteReceiptIssueCommand()
                .setEntryType(req.getEntryType()).setWorkOrderId(req.getWorkOrderId()).setWorkOrderCode(req.getWorkOrderCode())
                .setRouteId(req.getRouteId()).setRouteVersionId(req.getRouteVersionId()).setRouteVersion(req.getRouteVersion())
                .setBatchCode(req.getBatchCode()).setSourceRelationId(req.getSourceRelationId())
                .setSourceRelationVersion(req.getSourceRelationVersion()).setSourceRelationSnapshotHash(req.getSourceRelationSnapshotHash())
                .setSourceObjectType(req.getSourceObjectType()).setSourceObjectId(req.getSourceObjectId())
                .setMaterialSourceType(req.getMaterialSourceType()).setMaterialSourceId(req.getMaterialSourceId())
                .setSourceContextHash(req.getSourceContextHash()).setSourceSnapshotHash(req.getSourceSnapshotHash())
                .setBusinessReason(req.getBusinessReason()).setIdempotencyKey(req.getIdempotencyKey())
                .setSourceEvidence(req.getSourceEvidence());
        return success(MesIndependentBatchPrerequisiteReceiptRespVO.from(receiptService.issue(command, tenantId, actorId)));
    }

    @PostMapping("/verify")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-entry-receipt:verify')")
    public CommonResult<MesIndependentBatchPrerequisiteReceiptRespVO> verify(
            @Valid @RequestBody MesIndependentBatchPrerequisiteReceiptVerifyReqVO req) {
        MesIndependentBatchPrerequisiteReceiptVerifyCommand command = new MesIndependentBatchPrerequisiteReceiptVerifyCommand()
                .setReceiptId(req.getReceiptId()).setEntryType(req.getEntryType())
                .setSourceSnapshotHash(req.getSourceSnapshotHash());
        return success(MesIndependentBatchPrerequisiteReceiptRespVO.from(
                receiptService.verify(command, TenantContextHolder.getTenantId())));
    }

    @PostMapping("/revoke")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-batch-entry-receipt:revoke')")
    public CommonResult<MesIndependentBatchPrerequisiteReceiptRespVO> revoke(
            @Valid @RequestBody MesIndependentBatchPrerequisiteReceiptRevokeReqVO req) {
        MesIndependentBatchPrerequisiteReceiptRevokeCommand command = new MesIndependentBatchPrerequisiteReceiptRevokeCommand()
                .setReceiptId(req.getReceiptId()).setReason(req.getReason());
        return success(MesIndependentBatchPrerequisiteReceiptRespVO.from(receiptService.revoke(
                command, TenantContextHolder.getTenantId(), SecurityFrameworkUtils.getLoginUserId())));
    }
}
