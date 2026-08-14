package cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("dcc_project_code")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccProjectCodeDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long productMasterId;
    private String docControlNo;
    private String projectName;
    private String projectCode;
    private String category;
    private String commissionedProduction;
    private String projectLeader;
    private String projectEngineer;
    private String storageLocation;
    private String priority;
    private String status;
    private Long lastImportBatchId;
    @TableField(exist = false)
    private Long associatedFileCount;
}
