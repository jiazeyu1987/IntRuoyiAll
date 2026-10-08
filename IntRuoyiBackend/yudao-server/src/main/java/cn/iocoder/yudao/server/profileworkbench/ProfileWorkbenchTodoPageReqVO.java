package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.module.system.api.profileworkbench.ProfileWorkbenchTodoSourceId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;
@Data
public class ProfileWorkbenchTodoPageReqVO {
    @NotNull @Min(1) private Integer pageNo = 1;
    @NotNull @Min(10) @Max(100) private Integer pageSize = 10;
    @NotNull @Pattern(regexp = "visible|hidden") private String visibility = "visible";
    private List<@NotNull ProfileWorkbenchTodoSourceId> enabledSources = List.of();
    @Pattern(regexp = "文控|批记录|排产|展厅|行政") private String taskType;
    @Valid private QuickFilter quickFilter;
    @Valid private Sort sort;
    @Data public static class QuickFilter {
        @NotNull @Pattern(regexp = "source|detail|statusLabel") private String fieldKey;
        @NotNull @Pattern(regexp = "contains|eq") private String operator;
        @NotNull @Size(max = 1000) private String value;
    }
    @Data public static class Sort {
        @NotNull @Pattern(regexp = "taskType|source|detail|statusLabel") private String key;
        @NotNull @Pattern(regexp = "asc|desc") private String order;
    }
}
