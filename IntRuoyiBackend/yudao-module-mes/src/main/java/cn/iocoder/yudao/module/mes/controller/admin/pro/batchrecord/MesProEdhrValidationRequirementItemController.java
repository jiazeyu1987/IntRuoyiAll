package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationRequirementItemCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationRequirementItemPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationRequirementItemRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrValidationService;
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

@Tag(name = "管理后台 - MES eDHR 验证条目")
@RestController
@RequestMapping("/mes/pro/edhr-validation-requirement-item")
@Validated
public class MesProEdhrValidationRequirementItemController {

    @Resource
    private MesProEdhrValidationService validationService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-validation:query')")
    public CommonResult<PageResult<MesProEdhrValidationRequirementItemRespVO>> getRequirementItemPage(
            @Valid MesProEdhrValidationRequirementItemPageReqVO reqVO) {
        return success(validationService.getRequirementItemPage(reqVO));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-validation:create')")
    public CommonResult<MesProEdhrValidationRequirementItemRespVO> createRequirementItem(
            @Valid @RequestBody MesProEdhrValidationRequirementItemCreateReqVO reqVO) {
        return success(validationService.createRequirementItem(reqVO));
    }
}
