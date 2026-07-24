package cn.iocoder.yudao.module.dcc.dal.dataobject.protection;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
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
 * DCC controlled file watermark trace.
 */
@TableName("dcc_controlled_file_watermark_trace")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileWatermarkTraceDO extends BaseDO {

    @TableId
    private Long id;
    private String traceCode;
    private Long accessEventId;
    private String accessEventCode;
    private Long controlledFileId;
    private String fileNumber;
    private String fileVersionNo;
    private Long userId;
    private String userIdentifier;
    private String userDisplayName;
    private Long deptId;
    private String deptName;
    private String tenantName;
    private String privacyMode;
    private String watermarkPayloadJson;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;

}
