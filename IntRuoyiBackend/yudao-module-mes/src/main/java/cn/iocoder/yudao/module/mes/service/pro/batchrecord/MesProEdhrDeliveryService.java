package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeliveryGateSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeliveryProjectCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeliveryProjectPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDeliveryProjectRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrEvidencePackagePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrEvidencePackageRespVO;

public interface MesProEdhrDeliveryService {

    PageResult<MesProEdhrDeliveryProjectRespVO> getProjectPage(MesProEdhrDeliveryProjectPageReqVO reqVO);

    MesProEdhrDeliveryProjectRespVO createProject(MesProEdhrDeliveryProjectCreateReqVO reqVO);

    MesProEdhrDeliveryProjectRespVO getProjectDetail(Long id);

    PageResult<MesProEdhrEvidencePackageRespVO> getEvidencePackagePage(MesProEdhrEvidencePackagePageReqVO reqVO);

    MesProEdhrDeliveryGateSummaryRespVO getGateSummary(Long projectId);
}
