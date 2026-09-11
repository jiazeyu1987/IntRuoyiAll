package cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName(value = "gxp_audit_ledger_sequence", autoResultMap = true)
@Data
public class GxpAuditLedgerSequenceDO {

    @TableId
    private Long tenantId;
    private Long nextLedgerSequence;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
