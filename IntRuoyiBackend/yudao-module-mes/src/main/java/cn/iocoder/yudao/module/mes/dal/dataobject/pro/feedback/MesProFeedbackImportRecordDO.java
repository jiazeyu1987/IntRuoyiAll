package cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Third-party feedback import audit record.
 */
@TableName("mes_pro_feedback_import_record")
@KeySequence("mes_pro_feedback_import_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProFeedbackImportRecordDO extends BaseDO {

    public static final String ATTRIBUTION_STATUS_PENDING = "PENDING";
    public static final String ATTRIBUTION_STATUS_ATTRIBUTED = "ATTRIBUTED";
    public static final String ATTRIBUTION_TARGET_TYPE_CURRENT_ORDER = "CURRENT_ORDER";
    public static final String ATTRIBUTION_TARGET_TYPE_EXTERNAL_OTHER_ORDER = "EXTERNAL_OTHER_ORDER";
    public static final String PROGRESS_SOURCE_TYPE_DIRECT_WORK_REPORT = "DIRECT_WORK_REPORT";

    @TableId
    private Long id;

    private String sourceFileName;

    private String sourceFileSha256;

    private String sheetName;

    private Integer rowNo;

    private Long feedbackId;

    private String attributionStatus;

    private String taskCode;

    private String workOrderCode;

    private String itemCode;

    private String processCode;

    private String sourcePayloadJson;

    private Long scheduleOrderId;

    private Long scheduleOrderProcessId;

    private String attributionTargetType;

    private Integer candidateCount;

    private String progressSourceType;

    private BigDecimal progressQuantity;

    private LocalDateTime progressAppliedTime;

    private String progressWarningCode;

    private String progressWarningMessage;

    private String remark;
}
