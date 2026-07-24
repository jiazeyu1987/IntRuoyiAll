package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasPermissionSnapshotItemRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasPermissionSnapshotSummaryRespVO;
import cn.iocoder.yudao.module.dcc.service.permission.DccNasPermissionSnapshotQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "Admin - DCC NAS Permission Snapshot")
@RestController
@RequestMapping("/dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-snapshot")
@Validated
public class DccNasPermissionSnapshotController {

    @Resource
    private DccNasPermissionSnapshotQueryService snapshotQueryService;

    @GetMapping("")
    @Operation(summary = "Get NAS permission snapshot summary for one transfer task")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:access-rule:manage')")
    public CommonResult<DccNasPermissionSnapshotSummaryRespVO> getPermissionSnapshotSummary(
            @PathVariable("taskId") Long taskId) {
        return success(DccNasPermissionSnapshotSummaryRespVO.of(snapshotQueryService.getSummary(taskId)));
    }

    @GetMapping("/items")
    @Operation(summary = "Get NAS permission snapshot directory items for one transfer task")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:access-rule:manage')")
    public CommonResult<PageResult<DccNasPermissionSnapshotItemRespVO>> getPermissionSnapshotItems(
            @PathVariable("taskId") Long taskId,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "status", required = false) String status) {
        PageResult<DccNasPermissionSnapshotQueryService.ItemResult> pageResult =
                snapshotQueryService.getItems(taskId, pageNo, pageSize, status);
        return success(new PageResult<>(
                pageResult.getList().stream().map(DccNasPermissionSnapshotItemRespVO::of).toList(),
                pageResult.getTotal()));
    }
}
