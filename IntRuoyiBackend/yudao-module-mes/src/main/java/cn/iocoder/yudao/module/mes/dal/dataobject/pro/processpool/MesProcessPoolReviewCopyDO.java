package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
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

@TableName("mes_pro_process_pool_review_copy")
@KeySequence("mes_pro_process_pool_review_copy_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolReviewCopyDO extends TenantBaseDO {

    public static final String STATUS_SUBMITTED = "SUBMITTED";

    @TableId
    private Long id;

    private Long eventId;
    private Long processPoolId;
    private Long workOrderId;
    private Long routeId;
    private Long routeProcessId;
    private Long processId;
    private String feedbackSourceType;
    private Long feedbackSourceId;
    private String recordbookSourceType;
    private Long recordbookSourceId;
    private String rawPayloadSnapshot;
    private String reviewStatus;
    private Long reviewerUserId;
    private Long reviewerSignatureId;
    private Long reviewerSignatureUserId;
    private String reviewerSignatureSnapshot;
    private LocalDateTime reviewedAt;
}
