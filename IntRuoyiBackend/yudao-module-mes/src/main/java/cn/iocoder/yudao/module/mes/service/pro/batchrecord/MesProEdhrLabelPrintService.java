package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelInstancePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelInstanceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelPreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelTemplateActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrLabelTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskMarkFailedReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintExportAuditRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintHistoryCopyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintHistoryCopyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintHistoryExportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintPolicyActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintPolicyCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintPolicyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintPolicyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReprintApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReprintRequestRespVO;

public interface MesProEdhrLabelPrintService {

    PageResult<MesProEdhrLabelTemplateRespVO> getLabelTemplatePage(MesProEdhrLabelTemplatePageReqVO reqVO);

    MesProEdhrLabelTemplateRespVO createLabelTemplate(MesProEdhrLabelTemplateCreateReqVO reqVO);

    MesProEdhrLabelTemplateRespVO activateLabelTemplate(MesProEdhrLabelTemplateActivateReqVO reqVO);

    PageResult<MesProEdhrLabelInstanceRespVO> getLabelPage(MesProEdhrLabelInstancePageReqVO reqVO);

    MesProEdhrLabelPreviewRespVO previewLabel(MesProEdhrLabelPreviewReqVO reqVO);

    PageResult<MesProEdhrPrintTaskRespVO> getPrintTaskPage(MesProEdhrPrintTaskPageReqVO reqVO);

    MesProEdhrPrintTaskRespVO createPrintTask(MesProEdhrPrintTaskCreateReqVO reqVO);

    MesProEdhrPrintTaskRespVO markPrintTaskFailed(MesProEdhrPrintTaskMarkFailedReqVO reqVO);

    MesProEdhrPrintTaskRespVO confirmPrintTask(MesProEdhrPrintTaskConfirmReqVO reqVO);

    PageResult<MesProEdhrPrintPolicyRespVO> getPrintPolicyPage(MesProEdhrPrintPolicyPageReqVO reqVO);

    MesProEdhrPrintPolicyRespVO createPrintPolicy(MesProEdhrPrintPolicyCreateReqVO reqVO);

    MesProEdhrPrintPolicyRespVO activatePrintPolicy(MesProEdhrPrintPolicyActivateReqVO reqVO);

    MesProEdhrReprintRequestRespVO applyReprint(MesProEdhrReprintApplyReqVO reqVO);

    MesProEdhrPrintHistoryCopyRespVO createVoidHistoryCopy(MesProEdhrPrintHistoryCopyReqVO reqVO);

    MesProEdhrPrintExportAuditRespVO exportPrintHistory(MesProEdhrPrintHistoryExportReqVO reqVO);
}
