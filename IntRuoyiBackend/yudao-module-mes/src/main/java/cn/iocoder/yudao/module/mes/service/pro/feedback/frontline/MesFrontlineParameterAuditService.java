package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;

import java.util.List;

public interface MesFrontlineParameterAuditService {

    MesFrontlineParameterAuditResult resolveAndApply(MesProFrontlineFeedbackSubmitReqVO reqVO);

    MesFrontlineParameterAuditResult resolveAndApplyMaterial(
            MesProFrontlineFeedbackSubmitReqVO reqVO,
            Long materialId,
            String materialName,
            List<MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO> selectedDevices,
            List<MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO> readings);
}
