package cn.iocoder.yudao.module.dcc.controller.admin.log;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.log.vo.DccControlledFileLogPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.log.vo.DccControlledFileLogRespVO;
import cn.iocoder.yudao.module.dcc.service.log.DccControlledFileLogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - DCC 文控日志")
@RestController
@RequestMapping("/dcc/controlled-file-logs")
@Validated
public class DccControlledFileLogController {

    @Resource
    private DccControlledFileLogQueryService logQueryService;

    @GetMapping("/page")
    @Operation(summary = "分页查询 DCC 文控日志")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:log:query')")
    public CommonResult<PageResult<DccControlledFileLogRespVO>> getLogPage(
            @Valid DccControlledFileLogPageReqVO reqVO) {
        return success(logQueryService.getLogPage(reqVO));
    }

}
