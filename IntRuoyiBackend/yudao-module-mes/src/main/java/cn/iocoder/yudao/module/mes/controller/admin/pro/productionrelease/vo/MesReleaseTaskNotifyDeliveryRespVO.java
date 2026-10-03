package cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.productionrelease.MesReleaseTaskNotifyDeliveryDO;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "正式放行工作任务站内信投递回执")
public class MesReleaseTaskNotifyDeliveryRespVO {
    private String id;
    private String workTaskId;
    private String userId;
    @Schema(allowableValues = {"PENDING", "FAILED", "SENT"})
    private String status;
    private Integer rowVersion;
    private Integer attemptCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAttemptAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentAt;
    private String systemMessageId;
    private String lastErrorSummary;

    public static MesReleaseTaskNotifyDeliveryRespVO from(MesReleaseTaskNotifyDeliveryDO row) {
        MesReleaseTaskNotifyDeliveryRespVO value = new MesReleaseTaskNotifyDeliveryRespVO();
        value.setId(row.getId().toString()); value.setWorkTaskId(row.getWorkTaskId().toString());
        value.setUserId(row.getUserId().toString()); value.setStatus(row.getStatus());
        value.setRowVersion(row.getRowVersion()); value.setAttemptCount(row.getAttemptCount());
        value.setLastAttemptAt(row.getLastAttemptAt()); value.setSentAt(row.getSentAt());
        value.setSystemMessageId(row.getSystemMessageId() == null ? null : row.getSystemMessageId().toString());
        value.setLastErrorSummary(row.getLastErrorSummary());
        return value;
    }
}
