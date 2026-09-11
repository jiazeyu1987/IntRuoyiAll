package cn.iocoder.yudao.module.dcc.controller.admin.projectcode;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectFileTemplateRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectFileTemplateSaveReqVO;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectFileTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - DCC 项目文件模板")
@RestController
@RequestMapping("/dcc/project-codes/{projectCodeId:\\d+}/file-template")
@Validated
public class DccProjectFileTemplateController {

    @Resource
    private DccProjectFileTemplateService templateService;
    @Resource
    private DccProjectCodeService projectCodeService;

    @GetMapping("")
    @Operation(summary = "获得 DCC 项目文件模板")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:query') or @ss.hasPermission('dcc:controlled-file:submit')")
    public CommonResult<DccProjectFileTemplateRespVO> getProjectTemplate(
            @PathVariable("projectCodeId") Long projectCodeId) {
        projectCodeService.getProjectCode(getLoginUserId(), projectCodeId);
        return success(templateService.getProjectTemplate(projectCodeId));
    }

    @PutMapping("")
    @Operation(summary = "替换 DCC 项目文件模板")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update')")
    public CommonResult<DccProjectFileTemplateRespVO> replaceProjectTemplate(
            @PathVariable("projectCodeId") Long projectCodeId,
            @Valid @RequestBody DccProjectFileTemplateSaveReqVO reqVO) {
        projectCodeService.getProjectCode(getLoginUserId(), projectCodeId);
        return success(templateService.replaceProjectTemplate(projectCodeId, reqVO));
    }
}
