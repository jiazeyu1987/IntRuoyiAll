package cn.iocoder.yudao.module.showroom.dal.dataobject.cover;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("showroom_product_cover_batch_task")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomProductCoverBatchTaskDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long operatorUserId;

    private String status;

    private String keyword;

    private String lifecycleStage;

    private String incompleteStatus;

    private String approvalStatus;

    private String coverGenerationMode;

    private Long promptVersionId;

    private Integer matchedCount;

    private Integer publishedCount;

    private Integer skippedUnpublishedCount;

    private Integer skippedExistingCount;

    private Integer succeededCount;

    private Integer failedCount;

    private Integer remainingPendingCount;

    private LocalDateTime nextCheckAt;

    private LocalDateTime lastRunAt;

    private LocalDateTime completedAt;

    private String lastFailureMessage;
}
