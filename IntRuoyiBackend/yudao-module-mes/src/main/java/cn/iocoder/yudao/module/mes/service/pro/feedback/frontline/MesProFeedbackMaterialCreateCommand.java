package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import java.math.BigDecimal;
import java.util.List;

public record MesProFeedbackMaterialCreateCommand(Long feedbackId,
                                                  Long activeOrderId,
                                                  Long workOrderId,
                                                  Long routeId,
                                                  Long routeVersionId,
                                                  Long routeProcessId,
                                                  Long processId,
                                                  List<Entry> entries) {

    public record Entry(Long materialId,
                        String materialCode,
                        String materialName,
                        String materialSpecification,
                        BigDecimal bomQuantity,
                        BigDecimal outputQuantity,
                        BigDecimal lossQuantity,
                        String lossDetailsJson,
                        String selectedDeviceJson,
                        String deviceParameterReadingsJson) {
    }
}
