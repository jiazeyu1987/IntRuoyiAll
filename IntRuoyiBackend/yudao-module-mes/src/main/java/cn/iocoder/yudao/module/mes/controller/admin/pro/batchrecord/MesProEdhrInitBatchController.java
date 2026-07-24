package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitBatchCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitBatchPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitBatchPrecheckRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitBatchRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitIssuePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitIssueRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitManifestRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitManifestUploadReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrInitBatchService;
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

@Tag(name = "管理后台 - MES eDHR 初始化批次")
@RestController
@RequestMapping("/mes/pro/edhr-init-batch")
@Validated
public class MesProEdhrInitBatchController {

    @Resource
    private MesProEdhrInitBatchService initBatchService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-init-batch:query')")
    public CommonResult<PageResult<MesProEdhrInitBatchRespVO>> getPage(
            @Valid MesProEdhrInitBatchPageReqVO reqVO) {
        return success(initBatchService.getPage(reqVO));
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-init-batch:query')")
    public CommonResult<MesProEdhrInitBatchRespVO> get(@RequestParam("id") Long id) {
        return success(initBatchService.get(id));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-init-batch:create')")
    public CommonResult<MesProEdhrInitBatchRespVO> create(
            @Valid @RequestBody MesProEdhrInitBatchCreateReqVO reqVO) {
        return success(initBatchService.create(reqVO));
    }

    @PostMapping("/upload")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-init-batch:create')")
    public CommonResult<MesProEdhrInitManifestRespVO> upload(
            @Valid @RequestBody MesProEdhrInitManifestUploadReqVO reqVO) {
        return success(initBatchService.uploadManifest(reqVO));
    }

    @PostMapping("/precheck")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-init-batch:precheck')")
    public CommonResult<MesProEdhrInitBatchPrecheckRespVO> precheck(@RequestParam("id") Long id) {
        return success(initBatchService.runPrecheck(id));
    }

    @GetMapping("/issue/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-init-batch:query')")
    public CommonResult<PageResult<MesProEdhrInitIssueRespVO>> getIssuePage(
            @Valid MesProEdhrInitIssuePageReqVO reqVO) {
        return success(initBatchService.getIssuePage(reqVO));
    }
}
