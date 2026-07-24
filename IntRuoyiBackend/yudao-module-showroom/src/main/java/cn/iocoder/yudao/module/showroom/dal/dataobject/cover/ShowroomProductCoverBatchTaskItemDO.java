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

@TableName("showroom_product_cover_batch_task_item")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomProductCoverBatchTaskItemDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long taskId;

    private Long productId;

    private Long sourceRevisionId;

    private String productCode;

    private String nameCn;

    private String nameEn;

    private String promptFieldsJson;

    private String status;

    private Integer attemptCount;

    private String lastError;

    private String generatedCoverImage;

    private LocalDateTime lastAttemptAt;

    private LocalDateTime completedAt;
}
