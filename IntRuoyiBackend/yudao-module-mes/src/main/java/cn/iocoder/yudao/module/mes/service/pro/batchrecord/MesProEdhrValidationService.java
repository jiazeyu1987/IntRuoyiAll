package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationPackageCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationPackagePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationPackageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationRequirementItemCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationRequirementItemPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationRequirementItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationTraceEvaluateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationTraceLinkCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationTraceLinkRespVO;

public interface MesProEdhrValidationService {

    PageResult<MesProEdhrValidationPackageRespVO> getPackagePage(MesProEdhrValidationPackagePageReqVO reqVO);

    MesProEdhrValidationPackageRespVO createPackage(MesProEdhrValidationPackageCreateReqVO reqVO);

    MesProEdhrValidationPackageRespVO getPackageDetail(Long id);

    PageResult<MesProEdhrValidationRequirementItemRespVO> getRequirementItemPage(MesProEdhrValidationRequirementItemPageReqVO reqVO);

    MesProEdhrValidationRequirementItemRespVO createRequirementItem(MesProEdhrValidationRequirementItemCreateReqVO reqVO);

    MesProEdhrValidationTraceLinkRespVO createTraceLink(MesProEdhrValidationTraceLinkCreateReqVO reqVO);

    MesProEdhrValidationTraceEvaluateRespVO evaluateTrace(Long packageId);
}
