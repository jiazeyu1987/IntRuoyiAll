package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrReleaseDossierRequirementSettingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrReleaseDossierRequirementSettingUpdateReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseDossierRequirementSettingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES eDHR 放行配置")
@RestController
@RequestMapping("/mes/pro/edhr-release-setting")
@Validated
public class MesProEdhrReleaseSettingController {

    @Resource
    private MesProEdhrReleaseDossierRequirementSettingService dossierRequirementSettingService;

    @GetMapping("/dossier-requirements")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:golden-finger')")
    public CommonResult<EdhrReleaseDossierRequirementSettingRespVO> getDossierRequirements() {
        return success(dossierRequirementSettingService.getRequirementSetting());
    }

    @PutMapping("/dossier-requirements")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:golden-finger')")
    public CommonResult<EdhrReleaseDossierRequirementSettingRespVO> updateDossierRequirements(
            @Valid @RequestBody EdhrReleaseDossierRequirementSettingUpdateReqVO reqVO) {
        return success(dossierRequirementSettingService.updateRequirementSetting(reqVO));
    }
}
