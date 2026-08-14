package cn.iocoder.yudao.module.erp.dal.dataobject.nastablesync;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalTime;

@TableName("erp_nas_table_sync_plan")
@KeySequence("erp_nas_table_sync_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErpNasTableSyncPlanDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    private Boolean enabled;
    private LocalTime dailyStartTime;
    private String cronExpression;
    private String nasDirectory;
    private String fileNamePattern;
    private Long jobId;
    private Long lastRunId;
    private String lastStatus;
}
