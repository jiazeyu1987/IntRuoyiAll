package cn.iocoder.yudao.module.erp.service.kingdee.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpKingdeeEventCallbackResult {

    private Long id;
    private String eventKey;
    private String eventId;
    private String status;
    private boolean duplicate;

    public static ErpKingdeeEventCallbackResult accepted(Long id, String eventKey, String eventId) {
        return new ErpKingdeeEventCallbackResult(id, eventKey, eventId, "accepted", false);
    }

    public static ErpKingdeeEventCallbackResult duplicate(Long id, String eventKey, String eventId) {
        return new ErpKingdeeEventCallbackResult(id, eventKey, eventId, "duplicate", true);
    }

}
