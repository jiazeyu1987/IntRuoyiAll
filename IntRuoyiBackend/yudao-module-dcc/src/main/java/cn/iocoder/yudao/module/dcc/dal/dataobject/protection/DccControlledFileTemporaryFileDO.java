package cn.iocoder.yudao.module.dcc.dal.dataobject.protection;

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

/**
 * DCC controlled file temporary upload file.
 */
@TableName("dcc_controlled_file_temporary_file")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileTemporaryFileDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String uploadTicket;
    private String sessionId;
    private String purpose;
    private Long uploaderId;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private String fileSha256;
    private Long storageFileId;
    private String status;
    private LocalDateTime expireTime;
    private Long boundControlledFileId;
    private LocalDateTime boundTime;
    private String cleanupStatus;
    private String cleanupReason;
    private LocalDateTime cleanupTime;
    private String requestId;

}
