package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("dcc_electronic_signature_authorization")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccElectronicSignatureAuthorizationDO extends BaseDO {

    @TableId
    private Long id;

    private Long userId;

    private Boolean electronicSignatureEnabled;

    private String authorizationState;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime lockedUntil;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String lockReason;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime lastFailureAt;

    private Integer failureCount;
}
