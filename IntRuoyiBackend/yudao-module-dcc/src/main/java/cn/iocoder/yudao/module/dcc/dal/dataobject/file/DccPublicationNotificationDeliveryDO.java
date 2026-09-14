package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("dcc_publication_notification_delivery")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccPublicationNotificationDeliveryDO extends TenantBaseDO {
    @TableId private Long id;
    private Long batchId;
    private Long candidateId;
    private Long userId;
    private String businessKey;
    private String templateCode;
    private String status;
    private Integer attemptCount;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime sentAt;
    private Long systemMessageId;
    private String lastErrorSummary;
    private Integer rowVersion;
    private String creationToken;
}
