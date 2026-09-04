package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationProjectStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationResetRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationSaveRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationVersionOptionRespVO;

import java.util.Collection;
import java.util.List;

public interface MesQaInspectionRegulationService {

    MesQaInspectionRegulationSaveRespVO saveDraft(MesQaInspectionRegulationSaveReqVO reqVO);

    MesQaInspectionRegulationResetRespVO resetForTesting(Long dccProjectCodeId);

    MesQaInspectionRegulationPublishedVersionRespVO publish(MesQaInspectionRegulationSaveReqVO reqVO);

    MesQaInspectionRegulationPublishedVersionRespVO getPublishedVersion(Long dccProjectCodeId, Long versionId);

    List<MesQaInspectionRegulationVersionOptionRespVO> listVersions(Long dccProjectCodeId);

    MesQaInspectionRegulationPublishedVersionRespVO getCurrent(Long dccProjectCodeId);

    MesQaInspectionRegulationPublishedVersionRespVO getLockedVersionForOrder(
            Long dccProjectCodeId, Long qaRegulationId, Long qaRegulationVersionId);

    List<MesQaInspectionRegulationProjectStatusRespVO> getProjectStatuses(Collection<Long> dccProjectCodeIds);
}
