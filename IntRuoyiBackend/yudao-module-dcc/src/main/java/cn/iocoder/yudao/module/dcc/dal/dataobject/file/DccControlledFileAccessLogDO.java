package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DCC controlled file access log.
 */
@TableName("dcc_controlled_file_access_log")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileAccessLogDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    private Long controlledFileId;
    private Long accessEventId;
    private String accessEventCode;
    private String watermarkTraceCode;
    private String fileVersionNo;
    private Long userId;
    private String actionType;
    private String purpose;
    private String result;
    private String failureCode;
    private String reason;
    private String sourceIp;
    private String requestId;
    private String userAgent;

}
