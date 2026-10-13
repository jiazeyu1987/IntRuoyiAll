package cn.iocoder.yudao.module.showroom.workbench;
import lombok.Data;
import lombok.EqualsAndHashCode;
import cn.iocoder.yudao.module.system.api.workbench.TaskSummary;
/** Module SQL row; never serialized as the public neutral response. */
@Data @EqualsAndHashCode(callSuper=true)
public class ShowroomAssignmentWorkbenchRow extends TaskSummary {

}
