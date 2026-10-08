package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.module.system.api.profileworkbench.ProfileWorkbenchTodoSourceId;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
@Data
public class ProfileWorkbenchTodoCountReqVO {
    private List<@NotNull ProfileWorkbenchTodoSourceId> enabledSources = List.of();
}
