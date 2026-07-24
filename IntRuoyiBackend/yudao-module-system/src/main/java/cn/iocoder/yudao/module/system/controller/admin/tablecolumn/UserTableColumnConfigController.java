package cn.iocoder.yudao.module.system.controller.admin.tablecolumn;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.system.controller.admin.tablecolumn.vo.UserTableColumnConfigRespVO;
import cn.iocoder.yudao.module.system.controller.admin.tablecolumn.vo.UserTableColumnConfigSaveReqVO;
import cn.iocoder.yudao.module.system.service.tablecolumn.UserTableColumnConfigService;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 用户列表列配置")
@RestController
@RequestMapping("/system/user-table-column-config")
@Validated
public class UserTableColumnConfigController {

    @Resource
    private UserTableColumnConfigService userTableColumnConfigService;

    @GetMapping("/get")
    @Operation(summary = "获得当前用户的列表列配置")
    public CommonResult<UserTableColumnConfigRespVO> getConfig(
            @RequestParam("tableKey") @NotBlank(message = "tableKey 不能为空") String tableKey) {
        return success(userTableColumnConfigService.getConfig(tableKey));
    }

    @PutMapping("/save")
    @Operation(summary = "保存当前用户的列表列配置")
    public CommonResult<Boolean> saveConfig(@Valid @RequestBody UserTableColumnConfigSaveReqVO reqVO) {
        userTableColumnConfigService.saveConfig(reqVO);
        return success(true);
    }

    @DeleteMapping("/reset")
    @Operation(summary = "重置当前用户的列表列配置")
    public CommonResult<Boolean> resetConfig(
            @RequestParam("tableKey") @NotBlank(message = "tableKey 不能为空") String tableKey) {
        userTableColumnConfigService.resetConfig(tableKey);
        return success(true);
    }

}
