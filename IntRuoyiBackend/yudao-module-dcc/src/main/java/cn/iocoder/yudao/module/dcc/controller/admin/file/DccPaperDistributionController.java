package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPaperDistributionIssueReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPaperDistributionRecordRespVO;
import cn.iocoder.yudao.module.dcc.service.file.DccPaperDistributionAckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "Admin - DCC Paper Distribution")
@RestController
@RequestMapping("/dcc/controlled-files")
@Validated
public class DccPaperDistributionController {

    @Resource
    private DccPaperDistributionAckService paperDistributionAckService;

    @GetMapping("/{id}/paper-distributions/records")
    @Operation(summary = "List paper distribution and recovery records")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<List<DccPaperDistributionRecordRespVO>> getPaperDistributionRecords(
            @PathVariable("id") Long id) {
        return success(paperDistributionAckService.getPaperDistributionRecords(id));
    }

    @PostMapping("/{id}/paper-distributions/{distributionId}/acknowledge")
    @Operation(summary = "Register one paper distribution issue row")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<Boolean> acknowledgePaperDistribution(@PathVariable("id") Long id,
                                                              @PathVariable("distributionId") Long distributionId,
                                                              @Valid @RequestBody DccPaperDistributionIssueReqVO reqVO) {
        paperDistributionAckService.acknowledgePaperDistribution(getLoginUserId(), id, distributionId,
                reqVO.getRecipientUserIds());
        return success(true);
    }

    @PostMapping("/{id}/paper-distributions/{distributionId}/recover")
    @Operation(summary = "Recover one paper distribution row")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<Boolean> recoverPaperDistribution(@PathVariable("id") Long id,
                                                          @PathVariable("distributionId") Long distributionId) {
        paperDistributionAckService.recoverPaperDistribution(getLoginUserId(), id, distributionId);
        return success(true);
    }
}
