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

@TableName("mes_pro_edhr_release_transaction_event")
@KeySequence("mes_pro_edhr_release_transaction_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrReleaseTransactionEventDO extends BaseDO {

    @TableId
    private Long id;

    private Long releaseTransactionId;

    private String eventType;

    private String fromStatus;

    private String toStatus;

    private Long actorUserId;

    private String reason;

    private String opinion;

    private String idempotencyKey;

    private String signoffEvidenceHash;

    private String eventSnapshotJson;

    private String evidenceHash;

    private LocalDateTime occurredAt;
}
