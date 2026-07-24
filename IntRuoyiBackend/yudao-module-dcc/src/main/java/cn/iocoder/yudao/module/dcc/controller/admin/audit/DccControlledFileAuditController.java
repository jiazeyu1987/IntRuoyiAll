package cn.iocoder.yudao.module.dcc.controller.admin.audit;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.audit.vo.DccControlledFileAuditPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.audit.vo.DccControlledFileAuditRespVO;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAuditQuery;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAuditQueryService;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAuditRecord;
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
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - DCC 受控文件审计")
@RestController
@RequestMapping("/dcc/controlled-file-audits")
@Validated
public class DccControlledFileAuditController {

    @Resource
    private DccControlledFileAuditQueryService auditQueryService;

    @GetMapping("/page")
    @Operation(summary = "分页查询 DCC 受控文件审计")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:audit:query')")
    public CommonResult<PageResult<DccControlledFileAuditRespVO>> getAuditPage(
            @Valid DccControlledFileAuditPageReqVO reqVO) {
        PageResult<DccControlledFileAuditRecord> pageResult = auditQueryService.getAuditPage(toQuery(reqVO));
        return success(new PageResult<>(convertList(pageResult.getList(), this::toRespVO), pageResult.getTotal()));
    }

    private DccControlledFileAuditQuery toQuery(DccControlledFileAuditPageReqVO reqVO) {
        DccControlledFileAuditQuery query = new DccControlledFileAuditQuery();
        query.setPageNo(reqVO.getPageNo());
        query.setPageSize(reqVO.getPageSize());
        query.setAccessEventCode(reqVO.getAccessEventCode());
        query.setWatermarkTraceCode(reqVO.getWatermarkTraceCode());
        query.setControlledFileId(reqVO.getControlledFileId());
        query.setUserId(reqVO.getUserId());
        query.setActionType(reqVO.getActionType());
        query.setResult(reqVO.getResult());
        query.setFailureCode(reqVO.getFailureCode());
        query.setRequestId(reqVO.getRequestId());
        query.setOccurredAt(reqVO.getOccurredAt());
        return query;
    }

    private DccControlledFileAuditRespVO toRespVO(DccControlledFileAuditRecord record) {
        DccControlledFileAuditRespVO respVO = new DccControlledFileAuditRespVO();
        respVO.setId(record.getId());
        respVO.setAccessEventId(record.getAccessEventId());
        respVO.setAccessEventCode(record.getAccessEventCode());
        respVO.setWatermarkTraceCode(record.getWatermarkTraceCode());
        respVO.setControlledFileId(record.getControlledFileId());
        respVO.setFileNumber(record.getFileNumber());
        respVO.setFileVersionNo(record.getFileVersionNo());
        respVO.setUserId(record.getUserId());
        respVO.setUserIdentifier(record.getUserIdentifier());
        respVO.setUserDisplayName(record.getUserDisplayName());
        respVO.setDeptId(record.getDeptId());
        respVO.setDeptName(record.getDeptName());
        respVO.setTenantName(record.getTenantName());
        respVO.setActionType(record.getActionType());
        respVO.setPurpose(record.getPurpose());
        respVO.setResult(record.getResult());
        respVO.setFailureCode(record.getFailureCode());
        respVO.setReason(record.getReason());
        respVO.setSourceIp(record.getSourceIp());
        respVO.setRequestId(record.getRequestId());
        respVO.setUserAgent(record.getUserAgent());
        respVO.setPrivacyMode(record.getPrivacyMode());
        respVO.setWatermarkPayloadJson(record.getWatermarkPayloadJson());
        respVO.setOccurredAt(record.getOccurredAt());
        respVO.setIssuedAt(record.getIssuedAt());
        respVO.setExpiresAt(record.getExpiresAt());
        respVO.setCreateTime(record.getCreateTime());
        return respVO;
    }

}
