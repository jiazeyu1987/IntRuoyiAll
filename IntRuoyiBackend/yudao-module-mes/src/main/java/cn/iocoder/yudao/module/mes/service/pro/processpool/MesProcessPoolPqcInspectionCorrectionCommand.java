package cn.iocoder.yudao.module.mes.service.pro.processpool;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesProcessPoolPqcInspectionCorrectionCommand {

    private Long eventId;
    private Long actorUserId;
    private Integer actualInspectionQuantity;
    private Integer scrapQuantity;
    private List<ItemResultCommand> itemResults;
    private String changeReason;
    private String signaturePassword;

    @Data
    @Accessors(chain = true)
    public static class ItemResultCommand {

        private String itemCode;
        private Long selectedEquipmentId;
        private String selectedEquipmentNumber;
        private List<String> sampleValues;
    }
}
