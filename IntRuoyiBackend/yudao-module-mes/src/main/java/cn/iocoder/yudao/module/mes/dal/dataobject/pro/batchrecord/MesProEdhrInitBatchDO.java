package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("mes_pro_edhr_init_batch")
@KeySequence("mes_pro_edhr_init_batch_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrInitBatchDO extends BaseDO {

    @TableId
    private Long id;

    private String projectCode;

    private String projectName;

    private String targetEnvironment;

    private Long targetTenantId;

    private String dataVersion;

    private Long ownerUserId;

    private Long approvalOwnerUserId;

    private LocalDateTime plannedStartTime;

    private LocalDateTime plannedEndTime;

    private String initScopeJson;

    private String status;

    private Integer manifestCount;

    private Integer blockingIssueCount;

    private LocalDateTime lastPrecheckAt;

    private Integer version;

    private String remark;
}
