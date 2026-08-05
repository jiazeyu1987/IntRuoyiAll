package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationProjectStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;

import java.util.Collection;
import java.util.List;

public interface MesQaInspectionRegulationService {

    MesQaInspectionRegulationPublishedVersionRespVO getPublishedVersion(Long versionId);

    List<MesQaInspectionRegulationProjectStatusRespVO> getProjectStatuses(Collection<Long> productIds);
}
