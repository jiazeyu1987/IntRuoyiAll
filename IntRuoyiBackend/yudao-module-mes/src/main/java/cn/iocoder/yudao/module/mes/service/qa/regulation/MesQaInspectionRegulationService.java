package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaCommonRegulationBindReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaCommonRegulationBindingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaCommonRegulationSetRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaCommonRegulationSetSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaCommonRegulationSetVersionOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaCommonRegulationSetVersionSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaCommonRegulationVersionOptionRespVO;
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

    MesQaCommonRegulationBindingRespVO getCurrentCommonRegulationBinding(Long dccProjectCodeId);

    List<MesQaCommonRegulationVersionOptionRespVO> listCommonRegulationPublishedVersions();

    List<MesQaCommonRegulationSetRespVO> listCommonRegulationSets();

    MesQaCommonRegulationSetRespVO getCommonRegulationSet(Long setId);

    MesQaCommonRegulationSetRespVO saveCommonRegulationSet(MesQaCommonRegulationSetSaveReqVO reqVO);

    void deleteCommonRegulationSet(Long setId);

    MesQaCommonRegulationSetRespVO.Version saveCommonRegulationSetVersion(
            MesQaCommonRegulationSetVersionSaveReqVO reqVO);

    void deleteCommonRegulationSetVersion(Long setVersionId);

    List<MesQaCommonRegulationSetVersionOptionRespVO> listCommonRegulationPublishedSetVersions();

    MesQaCommonRegulationBindingRespVO bindCommonRegulationVersion(MesQaCommonRegulationBindReqVO reqVO);

    MesQaCommonRegulationBindingRespVO unbindCommonRegulation(Long dccProjectCodeId);

    MesQaInspectionRegulationPublishedVersionRespVO getLockedVersionForOrder(
            Long dccProjectCodeId, Long qaRegulationId, Long qaRegulationVersionId);

    MesQaInspectionRegulationPublishedVersionRespVO getLockedCommonVersionForOrder(
            Long qaRegulationId, Long qaRegulationVersionId);

    List<MesQaInspectionRegulationProjectStatusRespVO> getProjectStatuses(Collection<Long> dccProjectCodeIds);
}
