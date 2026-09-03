package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;

import java.math.BigDecimal;
import java.util.List;

public record MesProFrontlineFeedbackMaterialSubmission(BigDecimal progressQuantity,
                                                        BigDecimal totalLossQuantity,
                                                        List<Material> materials) {

    public record Material(Long materialId,
                           String materialCode,
                           String materialName,
                           String materialSpecification,
                           BigDecimal bomQuantity,
                           BigDecimal outputQuantity,
                           BigDecimal lossQuantity,
                           List<MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO> lossDetails,
                           List<MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO> selectedDevices,
                           List<MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO>
                                   deviceParameterReadings) {
    }
}
