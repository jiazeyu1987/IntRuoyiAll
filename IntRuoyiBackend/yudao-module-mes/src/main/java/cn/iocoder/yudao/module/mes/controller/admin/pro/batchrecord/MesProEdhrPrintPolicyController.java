package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintPolicyActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintPolicyCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintPolicyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintPolicyRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrLabelPrintService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES eDHR 打印策略")
@RestController
@RequestMapping("/mes/pro/edhr-print-policy")
@Validated
public class MesProEdhrPrintPolicyController {

    @Resource
    private MesProEdhrLabelPrintService labelPrintService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-print-policy:query')")
    public CommonResult<PageResult<MesProEdhrPrintPolicyRespVO>> getPage(
            @Valid MesProEdhrPrintPolicyPageReqVO reqVO) {
        return success(labelPrintService.getPrintPolicyPage(reqVO));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-print-policy:create')")
    public CommonResult<MesProEdhrPrintPolicyRespVO> create(
            @Valid @RequestBody MesProEdhrPrintPolicyCreateReqVO reqVO) {
        return success(labelPrintService.createPrintPolicy(reqVO));
    }

    @PostMapping("/activate")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-print-policy:activate')")
    public CommonResult<MesProEdhrPrintPolicyRespVO> activate(
            @Valid @RequestBody MesProEdhrPrintPolicyActivateReqVO reqVO) {
        return success(labelPrintService.activatePrintPolicy(reqVO));
    }
}
