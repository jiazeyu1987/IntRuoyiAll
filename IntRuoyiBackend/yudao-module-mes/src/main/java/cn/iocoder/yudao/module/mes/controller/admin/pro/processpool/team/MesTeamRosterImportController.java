package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamRosterImportService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamRosterRow;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro/process-pool/team-leader/roster")
public class MesTeamRosterImportController {
    @Resource private MesTeamRosterImportService service;

    @PostMapping("/import-leader")
    @Operation(summary = "从车间名单创建生产组长并分配权限角色")
    @PreAuthorize("@ss.hasPermission('system:user:import') && @ss.hasPermission('system:user:create') && @ss.hasPermission('system:permission:assign-user-role')")
    public CommonResult<List<MesTeamRosterImportService.ImportedPerson>> importLeader(@RequestParam("file") MultipartFile file) throws IOException {
        return success(service.importLeader(ExcelUtils.read(file, MesTeamRosterRow.class)));
    }

    @PostMapping("/import-members")
    @Operation(summary = "当前生产组长从车间名单导入本人班组人员")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
    public CommonResult<List<MesTeamRosterImportService.ImportedPerson>> importMembers(@RequestParam("file") MultipartFile file) throws IOException {
        return success(service.importMembers(SecurityFrameworkUtils.getLoginUserId(), ExcelUtils.read(file, MesTeamRosterRow.class)));
    }
}
