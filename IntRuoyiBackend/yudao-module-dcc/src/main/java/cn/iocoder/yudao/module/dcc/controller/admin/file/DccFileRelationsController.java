package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileRelatedFileService;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.FileVersion;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.CurrentRelations;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.RelationChange;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRelatedFileRespVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/dcc/file-relations")
@PreAuthorize("isAuthenticated()")
public class DccFileRelationsController extends DccRelationsExceptionHandler {
    private final DccControlledFileRelatedFileService service;
    public DccFileRelationsController(DccControlledFileRelatedFileService service){this.service=service;}
    @GetMapping("/{sourceFileId}/current") public CommonResult<CurrentRelations> current(@PathVariable Long sourceFileId){
        return success(service.getCurrentRelationView(SecurityFrameworkUtils.getLoginUserId(),sourceFileId));
    }
    @GetMapping("/{sourceFileId}/history") public CommonResult<List<DccControlledFileRelatedFileRespVO>> history(@PathVariable Long sourceFileId){
        return success(service.listHistoricalRelatedFiles(SecurityFrameworkUtils.getLoginUserId(),sourceFileId));
    }
    @PutMapping("/{sourceFileId}") public CommonResult<RelationChange> replace(@PathVariable Long sourceFileId,@Valid @RequestBody ReplaceRequest request){
        return success(service.replaceCurrentRelations(SecurityFrameworkUtils.getLoginUserId(),sourceFileId,request.selectedFileIds(),request.expectedMasterIds(),request.expectedVersion(),request.idempotencyKey(),request.reason()));
    }
    public record ReplaceRequest(@NotNull List<@NotNull @Positive Long> selectedFileIds,@NotNull List<@NotNull @Positive Long> expectedMasterIds,@NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=128) String idempotencyKey,@NotBlank String reason){}
}
