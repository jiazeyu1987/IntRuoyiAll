package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrReleaseDossierRequirementSettingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrReleaseDossierRequirementSettingUpdateReqVO;
import jakarta.validation.Valid;

public interface MesProEdhrReleaseDossierRequirementSettingService {

    String CONFIG_KEY = "mes.edhr.release.dossier.requirements";

    EdhrReleaseDossierRequirementSettingRespVO getRequirementSetting();

    EdhrReleaseDossierRequirementSettingRespVO updateRequirementSetting(
            @Valid EdhrReleaseDossierRequirementSettingUpdateReqVO reqVO);

    MesProEdhrReleaseDossierRequirementState getRequirementState();

    void requireCurrentConfigHash(String precheckConfigHash);
}
