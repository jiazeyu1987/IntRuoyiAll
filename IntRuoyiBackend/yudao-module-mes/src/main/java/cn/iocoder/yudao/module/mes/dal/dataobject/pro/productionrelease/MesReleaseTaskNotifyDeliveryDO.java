package cn.iocoder.yudao.module.mes.dal.dataobject.pro.productionrelease;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** One frozen recipient of one formal release task's ASSIGNED event. */
@TableName("mes_pro_edhr_release_task_notify_delivery")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class MesReleaseTaskNotifyDeliveryDO extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long workTaskId;
    private String eventType;
    private Long userId;
    private String businessKey;
    private String templateCode;
    private String templateParamsJson;
    private Long initiatedBy;
    private String status;
    private Integer attemptCount;
    private Integer rowVersion;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime sentAt;
    private Long systemMessageId;
    private String lastErrorSummary;
}
