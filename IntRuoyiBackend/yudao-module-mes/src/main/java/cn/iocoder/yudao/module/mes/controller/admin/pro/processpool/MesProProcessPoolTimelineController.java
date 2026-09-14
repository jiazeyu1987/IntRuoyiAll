package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineService;
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

@Tag(name = "管理后台 - MES 工序池时间轴")
@RestController
@RequestMapping("/mes/pro/process-pool/timeline")
@Validated
public class MesProProcessPoolTimelineController {

    private final ProcessPoolTimelineService processPoolTimelineService;

    public MesProProcessPoolTimelineController(ProcessPoolTimelineService processPoolTimelineService) {
        this.processPoolTimelineService = processPoolTimelineService;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询工序池提交事件时间轴")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool:query')")
    public CommonResult<PageResult<ProcessPoolTimelineEventRespVO>> getTimelinePage(
            @Valid ProcessPoolTimelinePageReqVO pageReqVO) {
        return success(processPoolTimelineService.getTimelinePage(pageReqVO));
    }

    @GetMapping("/detail")
    @Operation(summary = "查询工序池提交事件只读详情")
    @Parameter(name = "id", description = "工序池提交事件编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool:query')")
    public CommonResult<ProcessPoolTimelineDetailRespVO> getTimelineDetail(@RequestParam("id") Long id) {
        return success(processPoolTimelineService.getTimelineDetail(id));
    }

}
