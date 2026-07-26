package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteBatchRecordAttachmentOwnerInitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteBatchRecordAttachmentOwnerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteBatchRecordAttachmentOwnerSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowProcessConfigRespVO;
import jakarta.validation.Valid;

import java.util.List;

public interface MesProRouteFlowConfigService {

    List<MesProRouteFlowProcessConfigRespVO> getRouteFlowProcessConfigList(Long routeId, String useType);

    List<MesProRouteFlowProcessConfigRespVO> getRouteFlowProcessConfigList(Long routeId, String useType,
                                                                           Long routeVersionId);

    void saveRouteFlowConfig(@Valid MesProRouteFlowConfigSaveReqVO saveReqVO);

    void saveRouteFlowConfigForConfigPackageImport(@Valid MesProRouteFlowConfigSaveReqVO saveReqVO);

    List<MesProRouteBatchRecordAttachmentOwnerRespVO> getBatchRecordAttachmentOwners(Long routeId,
                                                                                     Long routeVersionId);

    List<MesProRouteBatchRecordAttachmentOwnerRespVO> initializeBatchRecordAttachmentOwners(
            @Valid MesProRouteBatchRecordAttachmentOwnerInitReqVO initReqVO);

    void saveBatchRecordAttachmentOwners(@Valid MesProRouteBatchRecordAttachmentOwnerSaveReqVO saveReqVO);

}
