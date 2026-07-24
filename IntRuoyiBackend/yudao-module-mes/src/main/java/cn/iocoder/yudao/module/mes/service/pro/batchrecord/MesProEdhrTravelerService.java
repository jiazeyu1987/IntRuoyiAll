package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerTemplateRespVO;

public interface MesProEdhrTravelerService {

    PageResult<MesProEdhrTravelerTemplateRespVO> getTemplatePage(MesProEdhrTravelerTemplatePageReqVO reqVO);

    MesProEdhrTravelerTemplateRespVO createTemplate(MesProEdhrTravelerTemplateCreateReqVO reqVO);

    MesProEdhrTravelerTemplateRespVO activateTemplate(MesProEdhrTravelerActivateReqVO reqVO);

    PageResult<MesProEdhrTravelerRespVO> getPage(MesProEdhrTravelerPageReqVO reqVO);

    MesProEdhrTravelerRespVO get(Long id);

    MesProEdhrTravelerRespVO generate(MesProEdhrTravelerGenerateReqVO reqVO);

    PageResult<MesProEdhrTravelerEventRespVO> getEventPage(MesProEdhrTravelerEventPageReqVO reqVO);
}
