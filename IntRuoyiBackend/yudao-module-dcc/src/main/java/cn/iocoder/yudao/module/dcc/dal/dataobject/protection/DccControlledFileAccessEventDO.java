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
 * DCC controlled file access event.
 */
@TableName("dcc_controlled_file_access_event")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileAccessEventDO extends BaseDO {

    @TableId
    private Long id;
    private String accessEventCode;
    private Long controlledFileId;
    private String fileVersionNo;
    private Long userId;
    private String accessType;
    private String purpose;
    private String result;
    private String failureCode;
    private String failureReason;
    private String sourceIp;
    private String userAgent;
    private String requestId;
    private LocalDateTime occurredAt;

}
