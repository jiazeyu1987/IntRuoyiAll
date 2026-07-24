package cn.iocoder.yudao.module.showroom.dal.dataobject.content;

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

@TableName("showroom_product_comment")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomProductCommentDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long productId;

    private Long targetRevisionId;

    private Long changeRequestId;

    private Long parentCommentId;

    private String anchorType;

    private String anchorKey;

    private String content;

    private String status;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long resolvedBy;

    private LocalDateTime resolvedAt;

}
