package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.service.file.DccLifecycleProjectionRepairService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/** Explicit administrative command; no query-side repair or raw SQL entry. */
@RestController
@RequestMapping("/dcc/controlled-file/workflow-lifecycle")
public class DccLifecycleProjectionRepairController {
    private final DccLifecycleProjectionRepairService repair;
    public DccLifecycleProjectionRepairController(DccLifecycleProjectionRepairService repair) { this.repair=repair; }

    @GetMapping("/{id}/lifecycle-projection-repair-preview")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage') and @ss.hasPermission('dcc:controlled-file:query') and @ss.hasPermission('dcc:controlled-file:update')")
    public CommonResult<DccLifecycleProjectionRepairPreviewRespVO> preview(@PathVariable Long id) {
        return CommonResult.success(repair.preview(getLoginUserId(),id));
    }

    @PostMapping("/{id}/repair-lifecycle-projection")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage') and @ss.hasPermission('dcc:controlled-file:query') and @ss.hasPermission('dcc:controlled-file:update')")
    public CommonResult<DccLifecycleProjectionRepairRespVO> repair(@PathVariable Long id,
            @Valid @RequestBody DccLifecycleProjectionRepairReqVO request) {
        return CommonResult.success(repair.repair(getLoginUserId(),id,request));
    }
}
