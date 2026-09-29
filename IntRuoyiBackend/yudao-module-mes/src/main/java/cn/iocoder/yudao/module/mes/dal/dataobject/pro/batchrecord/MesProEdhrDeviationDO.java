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

@TableName("mes_pro_edhr_deviation")
@KeySequence("mes_pro_edhr_deviation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrDeviationDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;
    private Long tenantId;
    private String deviationCode;
    private Long batchExecutionId;
    private String batchExecutionCode;
    private Long workOrderId;
    private String workOrderCode;
    private String batchCode;
    private String level;
    private Long discoveryDepartmentId;
    private String discoveryDepartmentName;
    private Long discovererId;
    private String discovererName;
    private LocalDateTime discoveredAt;
    private String discoveryLocation;
    private String productName;
    private String productSpecification;
    private String equipmentOrSystem;
    private LocalDateTime reportedAt;
    private Long receiverId;
    private String receiverName;
    private String categoryCodesJson;
    private String description;
    private String emergencyAction;
    private String levelBasis;
    private Long initiatorUserId;
    private String initiatorName;
    private LocalDateTime initiatedAt;
    private Long initiatorSignatureId;
    private String initiatorContentHash;
    private String status;
    private String closeReason;
    private LocalDateTime closedAt;
    private Long nonconformanceReviewId;
    private String createIdempotencyKey;
    private String createPayloadHash;
    private Integer version;
}
