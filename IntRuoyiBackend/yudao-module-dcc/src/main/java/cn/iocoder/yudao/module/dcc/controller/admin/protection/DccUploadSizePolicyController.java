package cn.iocoder.yudao.module.dcc.controller.admin.protection;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.protection.vo.DccUploadSizePolicyEffectiveRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.protection.vo.DccUploadSizePolicyRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.protection.vo.DccUploadSizePolicySaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileUploadPolicyDO;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadSizePolicyMatch;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadSizePolicySaveCommand;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadSizePolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - DCC 上传大小策略")
@RestController
@RequestMapping("/dcc/protection/upload-size-policies")
@Validated
public class DccUploadSizePolicyController {

    @Resource
    private DccUploadSizePolicyService uploadSizePolicyService;

    @GetMapping
    @Operation(summary = "获取 DCC 上传大小策略列表")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<List<DccUploadSizePolicyRespVO>> getPolicyList() {
        return success(convertList(uploadSizePolicyService.getPolicyList(), this::toRespVO));
    }

    @PostMapping
    @Operation(summary = "创建 DCC 上传大小策略")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<Long> createPolicy(@Valid @RequestBody DccUploadSizePolicySaveReqVO reqVO) {
        return success(uploadSizePolicyService.createPolicy(toCommand(reqVO)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新 DCC 上传大小策略")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<Boolean> updatePolicy(@PathVariable("id") Long id,
                                              @Valid @RequestBody DccUploadSizePolicySaveReqVO reqVO) {
        uploadSizePolicyService.updatePolicy(id, toCommand(reqVO));
        return success(true);
    }

    @GetMapping("/effective")
    @Operation(summary = "解析 DCC 上传大小策略")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<DccUploadSizePolicyEffectiveRespVO> getEffectivePolicy(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "purpose", required = false) String purpose,
            @RequestParam(value = "fileSize", required = false) @PositiveOrZero Long fileSize) {
        DccUploadSizePolicyMatch match = fileSize == null
                ? uploadSizePolicyService.resolveEffectivePolicy(categoryId, purpose, null)
                : uploadSizePolicyService.validateUploadSize(categoryId, purpose, fileSize, null);
        return success(toEffectiveRespVO(match));
    }

    private DccUploadSizePolicyRespVO toRespVO(DccControlledFileUploadPolicyDO policy) {
        return BeanUtils.toBean(policy, DccUploadSizePolicyRespVO.class);
    }

    private DccUploadSizePolicyEffectiveRespVO toEffectiveRespVO(DccUploadSizePolicyMatch match) {
        DccUploadSizePolicyEffectiveRespVO respVO = new DccUploadSizePolicyEffectiveRespVO();
        respVO.setPolicyId(match.policyId());
        respVO.setPolicyCode(match.policyCode());
        respVO.setScopeType(match.scopeType().name());
        respVO.setCategoryId(match.categoryId());
        respVO.setPurpose(match.purpose());
        respVO.setMaxBytes(match.maxBytes());
        respVO.setPolicyVersion(match.policyVersion());
        respVO.setPolicyPriority(match.policyPriority());
        respVO.setScopePriority(match.scopePriority());
        return respVO;
    }

    private DccUploadSizePolicySaveCommand toCommand(DccUploadSizePolicySaveReqVO reqVO) {
        return DccUploadSizePolicySaveCommand.builder()
                .policyCode(reqVO.getPolicyCode())
                .scopeType(reqVO.getScopeType())
                .categoryId(reqVO.getCategoryId())
                .purpose(reqVO.getPurpose())
                .maxBytes(reqVO.getMaxBytes())
                .enabled(reqVO.getEnabled())
                .policyVersion(reqVO.getPolicyVersion())
                .effectiveFrom(reqVO.getEffectiveFrom())
                .effectiveTo(reqVO.getEffectiveTo())
                .changeReason(reqVO.getChangeReason())
                .build();
    }

}
