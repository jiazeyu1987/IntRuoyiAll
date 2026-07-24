package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 钉钉组织用户导入 Response VO")
@Data
@Builder
public class UserDingTalkImportRespVO {

    @Schema(description = "创建成功的用户名数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> createUsernames;

    @Schema(description = "导入失败的用户集合，key 为用户名，value 为失败原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> failureUsernames;

    @Schema(description = "本次新建的部门路径数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> createdDeptPaths;

    @Schema(description = "本次自动启用的部门路径数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> enabledDeptPaths;

    @Schema(description = "部门负责人回填结果，key 为部门路径，value 为负责人用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> leaderAssignedDeptPaths;

    @Schema(description = "部门负责人跳过结果，key 为部门路径，value 为跳过原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> leaderSkippedDeptPaths;

}
