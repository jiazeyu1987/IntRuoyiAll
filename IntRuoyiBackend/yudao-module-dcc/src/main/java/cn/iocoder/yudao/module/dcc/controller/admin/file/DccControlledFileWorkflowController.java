package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTrainingRecordReqVO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "Admin - DCC Controlled File Workflow")
@RestController
@RequestMapping("/dcc/controlled-files")
@Validated
public class DccControlledFileWorkflowController {

    @Resource
    private DccControlledFileWorkflowService workflowService;

    @PostMapping("/{id}/training-record")
    @Operation(summary = "Upload applicant training record before document-control approval")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<Boolean> uploadTrainingRecord(@PathVariable("id") Long id,
                                                      @Valid @RequestBody DccControlledFileTrainingRecordReqVO reqVO) {
        workflowService.uploadTrainingRecord(getLoginUserId(), id, reqVO);
        return success(true);
    }

}
