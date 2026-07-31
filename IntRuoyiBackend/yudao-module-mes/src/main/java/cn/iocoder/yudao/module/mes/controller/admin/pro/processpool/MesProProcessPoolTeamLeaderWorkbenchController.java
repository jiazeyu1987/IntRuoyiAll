package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTeamLeaderWorkbenchRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTeamLeaderWorkbenchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 工序池班组长工作台")
@RestController
@RequestMapping("/mes/pro/process-pool/team-leader-workbench")
@Validated
public class MesProProcessPoolTeamLeaderWorkbenchController {

    private final ProcessPoolTeamLeaderWorkbenchService workbenchService;

    public MesProProcessPoolTeamLeaderWorkbenchController(ProcessPoolTeamLeaderWorkbenchService workbenchService) {
        this.workbenchService = workbenchService;
    }

    @GetMapping("/page")
    @Operation(summary = "查询工序池班组长工作台")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<ProcessPoolTeamLeaderWorkbenchRespVO> getWorkbenchPage(
            @Valid ProcessPoolTimelinePageReqVO pageReqVO) {
        return success(workbenchService.getWorkbench(pageReqVO));
    }

    @GetMapping("/detail")
    @Operation(summary = "查询工序池班组长工作台只读详情")
    @Parameter(name = "id", description = "工序池提交事件编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:query')")
    public CommonResult<ProcessPoolTimelineDetailRespVO> getWorkbenchDetail(@RequestParam("id") Long id) {
        return success(workbenchService.getDetail(id));
    }

}
