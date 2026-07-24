package cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode;

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

@TableName("dcc_project_code_assignment_file")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccProjectCodeAssignmentFileDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long assignmentId;
    private Long projectCodeId;
    private Long controlledFileId;
    private Long masterId;
    private String fileNumberSnapshot;
    private String fileNameSnapshot;
    private Long categoryIdSnapshot;
    private Long directoryIdSnapshot;
    private String initialFileTypeLevel1;
    private String initialFileTypeLevel2;
    private String initialFileTypeLevel3;
    private String initialFileTypeLevel4;
    private String initialFileTypeLevel5;
    private Boolean changed;
    private Integer changedFieldCount;
    private LocalDateTime lastChangedTime;

}
