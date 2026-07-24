package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormCreateInstanceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormInstancePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormInstanceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormInstanceSaveDraftReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormInstanceSubmitReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES eDHR 独立表单实例")
@RestController
@RequestMapping("/mes/pro/edhr-form-instance")
@Validated
public class MesProEdhrFormInstanceController {

    @Resource
    private MesProEdhrFormService formService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-form-instance:query')")
    public CommonResult<PageResult<MesProEdhrFormInstanceRespVO>> getPage(
            @Valid MesProEdhrFormInstancePageReqVO reqVO) {
        return success(formService.getInstancePage(reqVO));
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-form-instance:query')")
    public CommonResult<MesProEdhrFormInstanceRespVO> get(@RequestParam("id") Long id) {
        return success(formService.getInstance(id));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-form-instance:create')")
    public CommonResult<MesProEdhrFormInstanceRespVO> create(
            @Valid @RequestBody MesProEdhrFormCreateInstanceReqVO reqVO) {
        return success(formService.createInstance(reqVO));
    }

    @PutMapping("/save-draft")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-form-instance:save')")
    public CommonResult<MesProEdhrFormInstanceRespVO> saveDraft(
            @Valid @RequestBody MesProEdhrFormInstanceSaveDraftReqVO reqVO) {
        return success(formService.saveDraft(reqVO));
    }

    @PutMapping("/submit")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-form-instance:submit')")
    public CommonResult<MesProEdhrFormInstanceRespVO> submit(
            @Valid @RequestBody MesProEdhrFormInstanceSubmitReqVO reqVO) {
        return success(formService.submit(reqVO));
    }

    @GetMapping("/event/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-form-instance:query')")
    public CommonResult<PageResult<MesProEdhrFormEventRespVO>> getEventPage(
            @Valid MesProEdhrFormEventPageReqVO reqVO) {
        return success(formService.getEventPage(reqVO));
    }
}
