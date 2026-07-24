package cn.iocoder.yudao.module.dcc.controller.admin.training;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingExecutionPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingExecutionRespVO;
import cn.iocoder.yudao.module.dcc.service.file.DccTrainingTaskService;
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
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - DCC 培训执行")
@RestController
@RequestMapping("/dcc/training-executions")
@Validated
public class DccTrainingExecutionController {

    @Resource
    private DccTrainingTaskService trainingTaskService;

    @GetMapping("/page")
    @Operation(summary = "获取培训执行分页")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<PageResult<DccTrainingExecutionRespVO>> getPage(@Valid DccTrainingExecutionPageReqVO reqVO) {
        return success(trainingTaskService.getTrainingExecutionPage(getLoginUserId(), reqVO));
    }
}
