package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqCaseCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqCasePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqCaseRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationCloseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationRemediateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqDeviationRetestReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqRunCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqRunPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqRunRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqStepResultRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOqPqStepSubmitReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOqPqService;
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

@Tag(name = "管理后台 - MES eDHR OQ/PQ执行台")
@RestController
@RequestMapping("/mes/pro/edhr-oq-pq")
@Validated
public class MesProEdhrOqPqController {

    @Resource
    private MesProEdhrOqPqService oqPqService;

    @GetMapping("/case/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-oq-pq:query')")
    public CommonResult<PageResult<MesProEdhrOqPqCaseRespVO>> getCasePage(
            @Valid MesProEdhrOqPqCasePageReqVO reqVO) {
        return success(oqPqService.getCasePage(reqVO));
    }

    @PostMapping("/case/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-oq-pq:create')")
    public CommonResult<MesProEdhrOqPqCaseRespVO> createCase(
            @Valid @RequestBody MesProEdhrOqPqCaseCreateReqVO reqVO) {
        return success(oqPqService.createCase(reqVO));
    }

    @GetMapping("/run/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-oq-pq:query')")
    public CommonResult<PageResult<MesProEdhrOqPqRunRespVO>> getRunPage(
            @Valid MesProEdhrOqPqRunPageReqVO reqVO) {
        return success(oqPqService.getRunPage(reqVO));
    }

    @PostMapping("/run/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-oq-pq:create')")
    public CommonResult<MesProEdhrOqPqRunRespVO> createRun(
            @Valid @RequestBody MesProEdhrOqPqRunCreateReqVO reqVO) {
        return success(oqPqService.createRun(reqVO));
    }

    @PostMapping("/run/submit-step")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-oq-pq:execute')")
    public CommonResult<MesProEdhrOqPqStepResultRespVO> submitStepResult(
            @Valid @RequestBody MesProEdhrOqPqStepSubmitReqVO reqVO) {
        return success(oqPqService.submitStepResult(reqVO));
    }

    @PostMapping("/run/complete")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-oq-pq:execute')")
    public CommonResult<MesProEdhrOqPqRunRespVO> completeRun(@RequestParam("runId") Long runId) {
        return success(oqPqService.completeRun(runId));
    }

    @GetMapping("/deviation/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-oq-pq:query')")
    public CommonResult<PageResult<MesProEdhrOqPqDeviationRespVO>> getDeviationPage(
            @Valid MesProEdhrOqPqDeviationPageReqVO reqVO) {
        return success(oqPqService.getDeviationPage(reqVO));
    }

    @PostMapping("/deviation/remediate")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-oq-pq:retest')")
    public CommonResult<MesProEdhrOqPqDeviationRespVO> remediateDeviation(
            @Valid @RequestBody MesProEdhrOqPqDeviationRemediateReqVO reqVO) {
        return success(oqPqService.remediateDeviation(reqVO));
    }

    @PostMapping("/deviation/retest")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-oq-pq:retest')")
    public CommonResult<MesProEdhrOqPqDeviationRespVO> retestDeviation(
            @Valid @RequestBody MesProEdhrOqPqDeviationRetestReqVO reqVO) {
        return success(oqPqService.retestDeviation(reqVO));
    }

    @PostMapping("/deviation/close")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-oq-pq:close')")
    public CommonResult<MesProEdhrOqPqDeviationRespVO> closeDeviation(
            @Valid @RequestBody MesProEdhrOqPqDeviationCloseReqVO reqVO) {
        return success(oqPqService.closeDeviation(reqVO));
    }
}
