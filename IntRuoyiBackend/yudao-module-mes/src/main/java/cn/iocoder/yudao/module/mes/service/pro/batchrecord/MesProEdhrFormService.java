package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormCreateInstanceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormInstancePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormInstanceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormInstanceSaveDraftReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormInstanceSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormTemplateRespVO;

public interface MesProEdhrFormService {

    PageResult<MesProEdhrFormTemplateRespVO> getTemplatePage(MesProEdhrFormTemplatePageReqVO reqVO);

    MesProEdhrFormTemplateRespVO createTemplate(MesProEdhrFormTemplateCreateReqVO reqVO);

    MesProEdhrFormTemplateRespVO activateTemplate(MesProEdhrFormActivateReqVO reqVO);

    PageResult<MesProEdhrFormInstanceRespVO> getInstancePage(MesProEdhrFormInstancePageReqVO reqVO);

    MesProEdhrFormInstanceRespVO getInstance(Long id);

    MesProEdhrFormInstanceRespVO createInstance(MesProEdhrFormCreateInstanceReqVO reqVO);

    MesProEdhrFormInstanceRespVO saveDraft(MesProEdhrFormInstanceSaveDraftReqVO reqVO);

    MesProEdhrFormInstanceRespVO submit(MesProEdhrFormInstanceSubmitReqVO reqVO);

    PageResult<MesProEdhrFormEventRespVO> getEventPage(MesProEdhrFormEventPageReqVO reqVO);
}
