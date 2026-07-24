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

@TableName("mes_pro_edhr_traveler_event")
@KeySequence("mes_pro_edhr_traveler_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrTravelerEventDO extends BaseDO {

    @TableId
    private Long id;

    private Long travelerId;

    private String travelerCode;

    private String eventType;

    private String resultStatus;

    private String failureReason;

    private Long operatorUserId;

    private String operatorUsername;

    private LocalDateTime occurredAt;

    private String metadataJson;
}
