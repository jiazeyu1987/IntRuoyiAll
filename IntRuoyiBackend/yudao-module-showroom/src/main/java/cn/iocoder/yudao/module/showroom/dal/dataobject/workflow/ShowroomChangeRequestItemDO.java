package cn.iocoder.yudao.module.showroom.dal.dataobject.workflow;

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

@TableName("showroom_change_request_item")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomChangeRequestItemDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long changeRequestId;

    private String fieldCode;

    private String oldValueJson;

    private String newValueJson;

    private String approvalStatus;

    private Long approvedBy;

    private LocalDateTime approvedAt;

    private String comment;

}
