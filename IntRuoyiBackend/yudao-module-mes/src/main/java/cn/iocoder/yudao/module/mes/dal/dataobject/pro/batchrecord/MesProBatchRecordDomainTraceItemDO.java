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

@TableName("mes_pro_batch_record_domain_trace_item")
@KeySequence("mes_pro_batch_record_domain_trace_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProBatchRecordDomainTraceItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long snapshotId;

    private Long executionId;

    private String itemType;

    private String itemKey;

    private String itemName;

    private String sourceTable;

    private Long sourceId;

    private String sourceCode;

    private String sourceVersion;

    private String snapshotJson;

    private String snapshotHash;

    private Boolean requiredFlag;

    private String status;

    private String blockerCode;

    private String blockerMessage;

    private String blockerReason;
}
