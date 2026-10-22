package cn.iocoder.yudao.module.system.dal.dataobject.permission;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("system_approval_authority_revision")
public class ApprovalAuthorityRevisionDO {
    @TableId private Long tenantId;
    private Long revision;
    private LocalDateTime updatedAt;
}
