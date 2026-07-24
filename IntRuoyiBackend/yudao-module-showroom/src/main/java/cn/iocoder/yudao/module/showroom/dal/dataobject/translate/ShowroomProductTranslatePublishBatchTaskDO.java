package cn.iocoder.yudao.module.showroom.dal.dataobject.translate;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("showroom_product_translate_publish_batch_task")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomProductTranslatePublishBatchTaskDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long operatorUserId;

    private String status;

    private String keyword;

    private String lifecycleStage;

    private String incompleteStatus;

    private String approvalStatus;

    private Integer matchedCount;

    private Integer succeededCount;

    private Integer failedCount;

    private Integer remainingCount;

    private Long currentProductId;

    private String currentProductCode;

    private String currentProductNameCn;

    private LocalDateTime lastRunAt;

    private LocalDateTime completedAt;

    private String lastFailureMessage;
}
