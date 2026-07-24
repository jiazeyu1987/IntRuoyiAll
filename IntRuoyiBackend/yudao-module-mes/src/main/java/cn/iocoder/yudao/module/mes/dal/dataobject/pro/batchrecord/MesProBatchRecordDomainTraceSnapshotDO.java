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

@TableName("mes_pro_batch_record_domain_trace_snapshot")
@KeySequence("mes_pro_batch_record_domain_trace_snapshot_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProBatchRecordDomainTraceSnapshotDO extends BaseDO {

    @TableId
    private Long id;

    private Long executionId;

    private String snapshotVersion;

    private String snapshotJson;

    private String snapshotHash;

    private String completenessStatus;

    private Integer blockerCount;

    private Long verifiedBy;

    private LocalDateTime verifiedAt;
}
