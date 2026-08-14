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

/**
 * DCC controlled file print record.
 */
@TableName("dcc_controlled_file_print_record")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFilePrintRecordDO extends BaseDO {

    @TableId
    private Long id;
    private Long tenantId;
    private Long controlledFileId;
    private String fileNumber;
    private String versionNo;
    private String printNo;
    private String purpose;
    private Integer copies;
    private String receivingDepartment;
    private String useLocation;
    private Long printUserId;
    private String printUserName;
    private LocalDateTime printTime;
    private String approvalStatus;
    private Long approvalUserId;
    private String approvalUserName;
    private LocalDateTime approvalTime;

}
