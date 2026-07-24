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

import java.time.LocalDateTime;

@TableName("dcc_electronic_signature_authorization_audit")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccElectronicSignatureAuthorizationAuditDO extends BaseDO {

    @TableId
    private Long id;
    private Long targetUserId;
    private Long operatorId;
    private String beforeState;
    private String afterState;
    private Boolean beforeEnabled;
    private Boolean afterEnabled;
    private String reason;
    private LocalDateTime operatedAt;

}
