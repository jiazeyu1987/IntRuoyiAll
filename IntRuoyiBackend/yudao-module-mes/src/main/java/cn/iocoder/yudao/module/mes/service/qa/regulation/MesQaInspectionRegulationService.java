package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationProjectStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveRespVO;

import java.util.Collection;
import java.util.List;

public interface MesQaInspectionRegulationService {

    MesQaInspectionRegulationSaveRespVO saveDraft(MesQaInspectionRegulationSaveReqVO reqVO);

    MesQaInspectionRegulationPublishedVersionRespVO publish(MesQaInspectionRegulationSaveReqVO reqVO);

    MesQaInspectionRegulationPublishedVersionRespVO getPublishedVersion(Long dccProjectCodeId, Long versionId);

    MesQaInspectionRegulationPublishedVersionRespVO getCurrent(Long dccProjectCodeId);

    List<MesQaInspectionRegulationProjectStatusRespVO> getProjectStatuses(Collection<Long> dccProjectCodeIds);
}
