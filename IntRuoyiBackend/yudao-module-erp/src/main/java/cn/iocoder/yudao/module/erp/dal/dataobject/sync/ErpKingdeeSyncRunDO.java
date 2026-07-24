package cn.iocoder.yudao.module.erp.dal.dataobject.sync;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName("erp_kingdee_sync_run")
@KeySequence("erp_kingdee_sync_run_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpKingdeeSyncRunDO extends BaseDO {

    @TableId
    private Long id;

    private String syncType;
    private String triggerType;
    private Integer status;
    private LocalDateTime windowStartTime;
    private LocalDateTime windowEndTime;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer createdCount;
    private Integer updatedCount;
    private Integer skippedCount;
    private Integer failedCount;
    private String failureMessage;

}
