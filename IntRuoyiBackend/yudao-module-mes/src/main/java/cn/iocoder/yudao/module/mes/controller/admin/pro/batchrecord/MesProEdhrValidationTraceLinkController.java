package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationTraceLinkCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationTraceLinkRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES eDHR 追溯关系")
@RestController
@RequestMapping("/mes/pro/edhr-validation-trace-link")
@Validated
public class MesProEdhrValidationTraceLinkController {

    @Resource
    private MesProEdhrValidationService validationService;

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-validation:create')")
    public CommonResult<MesProEdhrValidationTraceLinkRespVO> createTraceLink(
            @Valid @RequestBody MesProEdhrValidationTraceLinkCreateReqVO reqVO) {
        return success(validationService.createTraceLink(reqVO));
    }
}
