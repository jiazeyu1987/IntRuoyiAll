package cn.iocoder.yudao.module.srm.dal.dataobject.naslocator;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("srm_nas_locator_refresh_task")
@KeySequence("srm_nas_locator_refresh_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmNasLocatorRefreshTaskDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String status;

    private String scopeShare;

    private String rootPath;

    private LocalDateTime startedTime;

    private LocalDateTime finishedTime;

    private Long directoryCount;

    private Long fileCount;

    private String errorMessage;
}
