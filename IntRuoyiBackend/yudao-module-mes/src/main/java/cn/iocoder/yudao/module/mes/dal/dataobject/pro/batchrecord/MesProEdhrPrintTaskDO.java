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

@TableName("mes_pro_edhr_print_task")
@KeySequence("mes_pro_edhr_print_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrPrintTaskDO extends BaseDO {

    @TableId
    private Long id;

    private String taskCode;

    private String sourceType;

    private Long sourceObjectId;

    private String sourceObjectCode;

    private String templateType;

    private Long templateId;

    private String templateCode;

    private Long labelInstanceId;

    private Long travelerId;

    private String status;

    private String printConfirmStatus;

    private Boolean isReprint;

    private Long originalPrintTaskId;

    private String reprintReason;

    private String watermarkText;

    private String failureReason;

    private String idempotencyKey;

    private Boolean printCountDeducted;

    private Long requestedBy;

    private LocalDateTime requestedAt;

    private Long confirmedBy;

    private LocalDateTime confirmedAt;

    private String confirmationEvidenceHash;
}
