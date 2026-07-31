package cn.iocoder.yudao.module.system.controller.admin.profileworkbench;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.system.controller.admin.profileworkbench.vo.ProfileWorkbenchTaskVisibilitySaveReqVO;
import cn.iocoder.yudao.module.system.service.profileworkbench.ProfileWorkbenchTaskVisibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 个人工作台任务隐藏")
@RestController
@RequestMapping("/system/profile-workbench-task-visibility")
@Validated
public class ProfileWorkbenchTaskVisibilityController {

    @Resource
    private ProfileWorkbenchTaskVisibilityService profileWorkbenchTaskVisibilityService;

    @GetMapping("/hidden-keys")
    @Operation(summary = "获得当前用户隐藏的个人工作台任务 Key")
    public CommonResult<List<String>> getHiddenTaskKeys() {
        return success(profileWorkbenchTaskVisibilityService.getHiddenTaskKeys());
    }

    @PutMapping("/hide")
    @Operation(summary = "隐藏当前用户个人工作台任务")
    public CommonResult<Boolean> hideTask(@Valid @RequestBody ProfileWorkbenchTaskVisibilitySaveReqVO reqVO) {
        profileWorkbenchTaskVisibilityService.hideTask(reqVO);
        return success(true);
    }

    @DeleteMapping("/restore")
    @Operation(summary = "恢复当前用户个人工作台任务")
    public CommonResult<Boolean> restoreTask(
            @RequestParam("taskKey") @NotBlank(message = "taskKey 不能为空") String taskKey) {
        profileWorkbenchTaskVisibilityService.restoreTask(taskKey);
        return success(true);
    }
}
