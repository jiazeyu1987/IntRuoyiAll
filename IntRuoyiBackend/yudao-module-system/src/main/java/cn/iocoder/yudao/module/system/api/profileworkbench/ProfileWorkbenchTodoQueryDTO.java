package cn.iocoder.yudao.module.system.api.profileworkbench;

import lombok.Data;
/** Internal query: identity is injected from the authenticated request, never HTTP input. */
@Data
public class ProfileWorkbenchTodoQueryDTO {
    private Long userId;
    private Long tenantId;
    private String visibility;
    private String taskType;
    private String filterField;
    private String filterOperator;
    private String filterValue;
    private String sortKey;
    private String sortOrder;
}
