package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@TableName("dcc_distribution_dispatch_attempt")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccDistributionDispatchAttemptDO extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long batchId;
    private Long messageJobId;
    private Integer attemptNo;
    private String businessKey;
    private Long recipientUserId;
    private String templateCode;
    private String status;
    private Long platformMessageId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorCode;
    private String errorMessage;
}
