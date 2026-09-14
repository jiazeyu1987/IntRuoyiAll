package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceGovernanceItemDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "Admin - DCC source governance blocker")
@Data
@Builder
public class DccControlledFileSourceGovernanceBlockerRespVO {

    private Long itemId;
    private Long controlledFileId;
    private String itemStatus;
    private String reasonCode;
    private String detail;
    private String lastError;

    public static DccControlledFileSourceGovernanceBlockerRespVO from(
            DccControlledFileSourceGovernanceItemDO item) {
        return DccControlledFileSourceGovernanceBlockerRespVO.builder()
                .itemId(item.getId())
                .controlledFileId(item.getControlledFileId())
                .itemStatus(item.getItemStatus())
                .reasonCode(item.getBlockerReasonCode())
                .detail(item.getBlockerDetail())
                .lastError(item.getLastError())
                .build();
    }
}
