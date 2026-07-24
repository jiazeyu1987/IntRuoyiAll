package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDistributionRecipientAckReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDistributionRecipientSignReqVO;
import cn.iocoder.yudao.module.dcc.service.file.DccDistributionReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "Admin - DCC Distribution Receipt")
@RestController
@RequestMapping("/dcc/controlled-files")
@Validated
public class DccDistributionReceiptController {

    @Resource
    private DccDistributionReceiptService distributionReceiptService;

    @PostMapping("/{id}/distributions/{distributionId}/recipients/{recipientId}/acknowledge")
    @Operation(summary = "Acknowledge one electronic distribution recipient row with password signature")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<Boolean> acknowledgeElectronicDistribution(@PathVariable("id") Long id,
                                                                   @PathVariable("distributionId") Long distributionId,
                                                                   @PathVariable("recipientId") Long recipientId,
                                                                   @Valid @RequestBody DccControlledFileDistributionRecipientAckReqVO reqVO) {
        distributionReceiptService.acknowledgeElectronicDistribution(getLoginUserId(), id, distributionId,
                recipientId, reqVO);
        return success(true);
    }

    @PostMapping("/{id}/distributions/{distributionId}/recipients/{recipientId}/sign")
    @Operation(summary = "Create additional electronic distribution recipients with password signature")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<Boolean> createDistributionRecipientSign(@PathVariable("id") Long id,
                                                                 @PathVariable("distributionId") Long distributionId,
                                                                 @PathVariable("recipientId") Long recipientId,
                                                                 @Valid @RequestBody DccControlledFileDistributionRecipientSignReqVO reqVO) {
        distributionReceiptService.createDistributionRecipientSign(getLoginUserId(), id, distributionId,
                recipientId, reqVO);
        return success(true);
    }
}
