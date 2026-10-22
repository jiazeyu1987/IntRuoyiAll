package cn.iocoder.yudao.module.dcc.controller.admin.projectcode;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectApplicationSnapshotService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/** 正式申请只读回读；写入仍由A真实申请事务处理。 */
@RestController
@RequestMapping("/dcc/project-codes")
public class DccProjectApplicationSnapshotController {
    @Resource private DccProjectApplicationSnapshotService snapshots;
    @GetMapping("/{projectId}/applications/{applicationId}/attributes")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:project-code:query','dcc:controlled-file:submit','dcc:controlled-file:query')")
    public CommonResult<DccProjectApplicationAttributesDO> read(@PathVariable Long projectId,@PathVariable Long applicationId,
                                                               @RequestParam String applicationType,@RequestParam String bpmRound) {
        return success(snapshots.read(getLoginUserId(),projectId,applicationType,applicationId,bpmRound));
    }
}
