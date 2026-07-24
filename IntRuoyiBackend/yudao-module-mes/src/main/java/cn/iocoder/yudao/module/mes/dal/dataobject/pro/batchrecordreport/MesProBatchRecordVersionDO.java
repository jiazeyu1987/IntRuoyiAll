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

@TableName("mes_pro_batch_record_version")
@KeySequence("mes_pro_batch_record_version_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProBatchRecordVersionDO extends BaseDO {

    @TableId
    private Long id;

    private Long definitionId;

    private String versionNo;

    private String status;

    private Long sourceVersionId;

    private String sourceFileName;

    private String sourceFileSha256;

    private Long routeId;

    private Long sourceRouteId;

    private String approvalInstanceId;

    private Long submittedBy;

    private LocalDateTime submittedAt;

    private Long approvedBy;

    private LocalDateTime approvedAt;

    private String rejectReason;

    private String remark;
}
