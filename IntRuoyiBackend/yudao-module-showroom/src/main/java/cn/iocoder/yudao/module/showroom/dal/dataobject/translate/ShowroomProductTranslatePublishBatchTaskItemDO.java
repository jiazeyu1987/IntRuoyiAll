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

@TableName("showroom_product_translate_publish_batch_task_item")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomProductTranslatePublishBatchTaskItemDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long taskId;

    private Long productId;

    private Long sourceRevisionId;

    private String productCode;

    private String nameCn;

    private String nameEn;

    private String status;

    private Integer attemptCount;

    private String lastError;

    private Long publishedRevisionId;

    private LocalDateTime lastAttemptAt;

    private LocalDateTime completedAt;
}
