package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelInstancePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelInstanceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelPreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelPreviewRespVO;
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

@Tag(name = "管理后台 - MES eDHR 标签")
@RestController
@RequestMapping("/mes/pro/edhr-label")
@Validated
public class MesProEdhrLabelController {

    @Resource
    private MesProEdhrLabelPrintService labelPrintService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-label:query')")
    public CommonResult<PageResult<MesProEdhrLabelInstanceRespVO>> getPage(
            @Valid MesProEdhrLabelInstancePageReqVO reqVO) {
        return success(labelPrintService.getLabelPage(reqVO));
    }

    @PostMapping("/preview")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-label:preview')")
    public CommonResult<MesProEdhrLabelPreviewRespVO> preview(
            @Valid @RequestBody MesProEdhrLabelPreviewReqVO reqVO) {
        return success(labelPrintService.previewLabel(reqVO));
    }
}
