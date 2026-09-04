package cn.iocoder.yudao.module.mes.service.pro.processpool;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesProcessPoolProductionReportCorrectionCommand {

    private Long eventId;
    private Long actorUserId;
    private BigDecimal outputQuantity;
    private List<MaterialDetailCommand> materialDetails;
    private List<LossDetailCommand> lossDetails;
    private List<DeviceParameterReadingCommand> deviceParameterReadings;
    private String changeReason;
    private String signaturePassword;

    @Data
    @Accessors(chain = true)
    public static class LossDetailCommand {
        private Long reasonId;
        private BigDecimal quantity;
    }

    @Data
    @Accessors(chain = true)
    public static class MaterialDetailCommand {
        private Long materialId;
        private BigDecimal outputQuantity;
        private BigDecimal lossQuantity;
        private List<LossDetailCommand> lossDetails;
        private SelectedDeviceCommand selectedDevice;
        private List<DeviceParameterReadingCommand> deviceParameterReadings;
    }

    @Data
    @Accessors(chain = true)
    public static class SelectedDeviceCommand {
        private Long deviceId;
    }

    @Data
    @Accessors(chain = true)
    public static class DeviceParameterReadingCommand {
        private Long deviceId;
        private String parameterCode;
        private BigDecimal value;
        private String textValue;
    }
}
