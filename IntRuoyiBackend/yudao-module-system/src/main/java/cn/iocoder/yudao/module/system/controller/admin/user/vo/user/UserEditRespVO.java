package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理后台 - 用户编辑详情 Response VO")
public class UserEditRespVO extends UserRespVO {

    @Schema(description = "正式已绑定岗位，包含未删除的停用岗位")
    private List<PostItem> assignedPosts;

    @Data
    public static class PostItem {
        private Long id;
        private String name;
        private Integer status;
    }
}
