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

@TableName("showroom_change_request_signature")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomChangeRequestSignatureDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long changeRequestId;

    private String approvalStage;

    private String actionType;

    private Long actorId;

    private String signatureMode;

    private Boolean passwordVerified;

    private String comment;

    private LocalDateTime signedAt;

}
