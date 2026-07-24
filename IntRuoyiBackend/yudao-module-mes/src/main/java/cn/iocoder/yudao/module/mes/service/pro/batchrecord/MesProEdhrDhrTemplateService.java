package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrCatalogCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrCatalogPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrCatalogRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateImpactPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateImpactReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateImpactRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateLifecycleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateSignoffReqVO;

public interface MesProEdhrDhrTemplateService {

    PageResult<MesProEdhrDhrCatalogRespVO> getCatalogPage(MesProEdhrDhrCatalogPageReqVO reqVO);

    MesProEdhrDhrCatalogRespVO createCatalog(MesProEdhrDhrCatalogCreateReqVO reqVO);

    PageResult<MesProEdhrDhrTemplateRespVO> getTemplatePage(MesProEdhrDhrTemplatePageReqVO reqVO);

    MesProEdhrDhrTemplateRespVO createTemplate(MesProEdhrDhrTemplateCreateReqVO reqVO);

    MesProEdhrDhrTemplateRespVO runIntegrityCheck(MesProEdhrDhrTemplateLifecycleReqVO reqVO);

    MesProEdhrDhrTemplateRespVO approveTemplate(MesProEdhrDhrTemplateLifecycleReqVO reqVO);

    MesProEdhrDhrTemplateRespVO signoffTemplate(MesProEdhrDhrTemplateSignoffReqVO reqVO);

    MesProEdhrDhrTemplateRespVO activateTemplate(MesProEdhrDhrTemplateLifecycleReqVO reqVO);

    MesProEdhrDhrTemplateRespVO retireTemplate(MesProEdhrDhrTemplateImpactReqVO reqVO);

    MesProEdhrDhrTemplateRespVO voidTemplate(MesProEdhrDhrTemplateImpactReqVO reqVO);

    PageResult<MesProEdhrDhrTemplateImpactRespVO> getImpactPage(MesProEdhrDhrTemplateImpactPageReqVO reqVO);
}
