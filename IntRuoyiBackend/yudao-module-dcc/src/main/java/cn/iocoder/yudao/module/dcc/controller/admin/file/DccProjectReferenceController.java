package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccProjectReferenceService;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.ReferenceView;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.ReferenceUsage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/dcc/project-file-references")
@PreAuthorize("isAuthenticated()")
public class DccProjectReferenceController extends DccRelationsExceptionHandler {
    private final DccProjectReferenceService service;
    public DccProjectReferenceController(DccProjectReferenceService service){this.service=service;}
    @GetMapping public CommonResult<List<ReferenceView>> list(@RequestParam Long projectId,@RequestParam Long folderId){
        return success(service.list(SecurityFrameworkUtils.getLoginUserId(),projectId,folderId));
    }
    @GetMapping("/usage") public CommonResult<ReferenceUsage> usage(@RequestParam Long selectedFileId){
        return success(service.getUsage(SecurityFrameworkUtils.getLoginUserId(),selectedFileId));
    }
    @GetMapping("/usage-page") public CommonResult<cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.ReferenceUsagePage> usagePage(
            @RequestParam Long selectedFileId,@RequestParam(defaultValue="1") Integer pageNo,
            @RequestParam(defaultValue="20") Integer pageSize) {
        return success(service.getUsagePage(SecurityFrameworkUtils.getLoginUserId(),selectedFileId,pageNo,pageSize));
    }
    @PostMapping public CommonResult<ReferenceView> create(@Valid @RequestBody CreateRequest request){
        return success(service.create(SecurityFrameworkUtils.getLoginUserId(),request.projectId(),request.folderId(),request.selectedFileId(),request.reason()));
    }
    @PostMapping("/batch") public CommonResult<List<ReferenceView>> createBatch(@Valid @RequestBody BatchCreateRequest request){
        return success(service.createBatch(SecurityFrameworkUtils.getLoginUserId(),request.projectId(),request.folderId(),request.selectedFileIds(),request.reason()));
    }
    @PostMapping("/cancel") public CommonResult<Long> cancel(@Valid @RequestBody CancelRequest request){
        return success(service.cancel(SecurityFrameworkUtils.getLoginUserId(),request.projectId(),request.folderId(),request.masterId(),request.referenceId(),Boolean.TRUE.equals(request.confirmed()),request.reason()));
    }
    public record CreateRequest(@NotNull @Positive Long projectId,@NotNull @Positive Long folderId,@NotNull @Positive Long selectedFileId,@NotBlank String reason){}
    public record BatchCreateRequest(@NotNull @Positive Long projectId,@NotNull @Positive Long folderId,@NotEmpty List<@NotNull @Positive Long> selectedFileIds,@NotBlank String reason){}
    public record CancelRequest(@NotNull @Positive Long projectId,@NotNull @Positive Long folderId,@NotNull @Positive Long masterId,@NotNull @Positive Long referenceId,@NotNull Boolean confirmed,@NotBlank String reason){}
}
