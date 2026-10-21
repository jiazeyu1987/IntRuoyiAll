package cn.iocoder.yudao.module.dcc.workbench;
import lombok.Data;
import lombok.EqualsAndHashCode;
import cn.iocoder.yudao.module.system.api.workbench.TaskSummary;
/** Module SQL row; never serialized as the public neutral response. */
@Data @EqualsAndHashCode(callSuper=true)
public class DccTrainingWorkbenchRow extends TaskSummary {
 private Long ownerUserId;
 private String controlledFileId;
}
