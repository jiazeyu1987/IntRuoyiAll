package cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("erp_kingdee_production_pick_list")
@KeySequence("erp_kingdee_production_pick_list_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpKingdeeProductionPickListDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String sourceFormId;
    private String sourceFid;
    private String sourceBillNo;
    private LocalDateTime billDate;
    private String documentStatus;
    private String stockOrgNumber;
    private String stockOrgName;
    private String productionOrgNumber;
    private String productionOrgName;
    private String ownerNumber;
    private String ownerName;
    private String departmentNumber;
    private String departmentName;
    private String description;
    private LocalDateTime sourceModifyTime;
    private LocalDateTime lastSyncTime;
    private String rawPayload;

}
