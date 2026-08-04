package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;

public interface MesQaInspectionRegulationService {

    MesQaInspectionRegulationPublishedVersionRespVO getPublishedVersion(Long versionId);
}
