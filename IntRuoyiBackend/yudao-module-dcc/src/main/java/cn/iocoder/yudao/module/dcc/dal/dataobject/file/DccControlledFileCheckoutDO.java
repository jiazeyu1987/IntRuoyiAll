package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

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

/** Immutable checkout history and the current edit lock for one logical file. */
@TableName("dcc_controlled_file_checkout")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileCheckoutDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long masterId;
    private Long baseIterationId;
    private Long actorId;
    private String reason;
    private String baseSourceSha256;
    private String status;
    private String checkinUploadTicket;
    private Long checkinIterationId;
    private Long checkinSourceFileId;
    private String checkinSourceSha256;
    private LocalDateTime checkedInTime;
    private String cancelReason;
    private LocalDateTime cancelledTime;
}
