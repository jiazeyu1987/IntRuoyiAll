package cn.iocoder.yudao.module.erp.controller.admin.kingdee.vo;

import cn.iocoder.yudao.module.erp.service.kingdee.event.ErpKingdeeEventCallbackResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ERP Kingdee 事件回调响应 VO")
@Data
public class ErpKingdeeEventCallbackRespVO {

    @Schema(description = "事件记录编号")
    private Long id;
    @Schema(description = "幂等事件键")
    private String eventKey;
    @Schema(description = "Kingdee 事件编号")
    private String eventId;
    @Schema(description = "接收状态，accepted 或 duplicate")
    private String status;
    @Schema(description = "是否重复事件")
    private Boolean duplicate;

    public static ErpKingdeeEventCallbackRespVO of(ErpKingdeeEventCallbackResult result) {
        ErpKingdeeEventCallbackRespVO respVO = new ErpKingdeeEventCallbackRespVO();
        respVO.setId(result.getId());
        respVO.setEventKey(result.getEventKey());
        respVO.setEventId(result.getEventId());
        respVO.setStatus(result.getStatus());
        respVO.setDuplicate(result.isDuplicate());
        return respVO;
    }

}
