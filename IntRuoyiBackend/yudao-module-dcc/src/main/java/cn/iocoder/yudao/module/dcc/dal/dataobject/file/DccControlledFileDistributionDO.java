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

@TableName("dcc_controlled_file_distribution")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileDistributionDO extends BaseDO {

    @TableId
    private Long id;
    private Long controlledFileId;
    private Long departmentId;
    private String distributionMedium;
    private String status;
    private Long acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private Long recoveredBy;
    private LocalDateTime recoveredAt;

}
