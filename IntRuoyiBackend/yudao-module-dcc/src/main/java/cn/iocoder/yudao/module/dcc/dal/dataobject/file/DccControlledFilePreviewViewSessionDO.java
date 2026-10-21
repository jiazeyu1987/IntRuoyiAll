package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import java.time.LocalDateTime;

/** Viewer lifecycle evidence. No raw preview token or client-supplied duration is stored. */
@TableName("dcc_controlled_file_preview_view_session")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFilePreviewViewSessionDO extends BaseDO {
    @TableId private Long id;
    private Long tenantId;
    private String resourceType;
    private Long controlledFileId;
    private String fileVersionNo;
    private Long accessEventId;
    private String accessEventCode;
    private String watermarkTraceCode;
    private Long userId;
    private String viewerTokenId;
    private String viewerTokenNonce;
    private String clientSessionId;
    private String bodyReadContext;
    private LocalDateTime openedAt;
    private LocalDateTime startedAt;
    private LocalDateTime lastHeartbeatAt;
    private LocalDateTime endedAt;
    private Integer accumulatedSeconds;
    private String endedReason;
    private String requestId;
    private String sourceIp;
    private String userAgent;
}
