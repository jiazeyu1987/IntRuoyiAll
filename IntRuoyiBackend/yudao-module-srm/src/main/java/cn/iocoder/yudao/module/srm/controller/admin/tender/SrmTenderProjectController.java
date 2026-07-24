package cn.iocoder.yudao.module.srm.controller.admin.tender;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.srm.controller.admin.tender.vo.*;
import cn.iocoder.yudao.module.srm.service.tender.SrmTenderProcurementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SRM 招标项目")
@RestController
@RequestMapping("/srm/tender-project")
@Validated
public class SrmTenderProjectController {

    @Resource
    private SrmTenderProcurementService tenderProcurementService;

    @GetMapping("/page")
    @Operation(summary = "获得招标项目分页")
    @PreAuthorize("@ss.hasPermission('srm:tender-project:query')")
    public CommonResult<PageResult<SrmTenderProjectRespVO>> getProjectPage(@Valid SrmTenderProjectPageReqVO pageReqVO) {
        return success(tenderProcurementService.getProjectPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得招标项目详情")
    @Parameter(name = "id", description = "招标项目编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('srm:tender-project:query')")
    public CommonResult<SrmTenderProjectRespVO> getProject(@RequestParam("id") Long id) {
        return success(tenderProcurementService.getProject(id));
    }

    @PostMapping("/publish")
    @Operation(summary = "发布招标项目")
    @PreAuthorize("@ss.hasPermission('srm:tender-project:publish')")
    public CommonResult<SrmTenderProjectRespVO> publishProject(@Valid @RequestBody SrmTenderPublishReqVO publishReqVO) {
        return success(tenderProcurementService.publishProject(publishReqVO));
    }

    @PostMapping("/submit-bid")
    @Operation(summary = "提交供应商投标")
    @PreAuthorize("@ss.hasPermission('srm:tender-project:submit-bid')")
    public CommonResult<SrmTenderProjectRespVO> submitBid(@Valid @RequestBody SrmTenderSubmissionReqVO submissionReqVO) {
        return success(tenderProcurementService.submitBid(submissionReqVO));
    }

    @PostMapping("/expert/create")
    @Operation(summary = "创建招标专家")
    @PreAuthorize("@ss.hasPermission('srm:tender-project:expert')")
    public CommonResult<Long> createExpert(@Valid @RequestBody SrmTenderExpertSaveReqVO createReqVO) {
        return success(tenderProcurementService.createExpert(createReqVO));
    }

    @PutMapping("/expert/approve")
    @Operation(summary = "通过招标专家")
    @PreAuthorize("@ss.hasPermission('srm:tender-project:expert')")
    public CommonResult<Boolean> approveExpert(@Valid @RequestBody SrmTenderExpertAuditReqVO auditReqVO) {
        tenderProcurementService.approveExpert(auditReqVO);
        return success(true);
    }

    @PostMapping("/committee")
    @Operation(summary = "组建评标委员会")
    @PreAuthorize("@ss.hasPermission('srm:tender-project:committee')")
    public CommonResult<SrmTenderProjectRespVO> formCommittee(@Valid @RequestBody SrmTenderCommitteeReqVO committeeReqVO) {
        return success(tenderProcurementService.formCommittee(committeeReqVO));
    }

    @PostMapping("/candidate")
    @Operation(summary = "生成中标候选")
    @PreAuthorize("@ss.hasPermission('srm:tender-project:candidate')")
    public CommonResult<SrmTenderProjectRespVO> createCandidates(@Valid @RequestBody SrmTenderCandidateReqVO candidateReqVO) {
        return success(tenderProcurementService.createCandidates(candidateReqVO));
    }

    @PostMapping("/winning")
    @Operation(summary = "确认中标结果")
    @PreAuthorize("@ss.hasPermission('srm:tender-project:winning')")
    public CommonResult<SrmTenderProjectRespVO> confirmWinning(@Valid @RequestBody SrmTenderWinningReqVO winningReqVO) {
        return success(tenderProcurementService.confirmWinning(winningReqVO));
    }
}
