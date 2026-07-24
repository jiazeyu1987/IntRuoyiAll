package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport;

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

@TableName("mes_pro_batch_record_version_approval_event")
@KeySequence("mes_pro_batch_record_version_approval_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProBatchRecordVersionApprovalEventDO extends BaseDO {

    @TableId
    private Long id;

    private Long definitionId;

    private Long versionId;

    private String approvalInstanceId;

    private String approvalEventId;

    private String approvalResult;

    private String processedResult;

    private Long actorUserId;

    private LocalDateTime processedAt;

    private String remark;
}
