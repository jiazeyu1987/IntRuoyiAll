package cn.iocoder.yudao.module.srm.controller.admin.coderule;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRuleEnableReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRulePageReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRuleRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRuleSaveReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.coderule.SrmCodeRuleDO;
import cn.iocoder.yudao.module.srm.service.coderule.SrmCodeRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SRM 编码规则")
@RestController
@RequestMapping("/srm/code-rule")
@Validated
public class SrmCodeRuleController {

    @Resource
    private SrmCodeRuleService codeRuleService;

    @PostMapping("/create")
    @Operation(summary = "创建 SRM 编码规则")
    @PreAuthorize("@ss.hasPermission('srm:code-rule:create')")
    public CommonResult<Long> createCodeRule(@Valid @RequestBody SrmCodeRuleSaveReqVO createReqVO) {
        return success(codeRuleService.createCodeRule(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 SRM 编码规则")
    @PreAuthorize("@ss.hasPermission('srm:code-rule:update')")
    public CommonResult<Boolean> updateCodeRule(@Valid @RequestBody SrmCodeRuleSaveReqVO updateReqVO) {
        codeRuleService.updateCodeRule(updateReqVO);
        return success(true);
    }

    @PutMapping("/enable")
    @Operation(summary = "启停 SRM 编码规则")
    @PreAuthorize("@ss.hasPermission('srm:code-rule:enable')")
    public CommonResult<Boolean> enableCodeRule(@Valid @RequestBody SrmCodeRuleEnableReqVO enableReqVO) {
        codeRuleService.enableCodeRule(enableReqVO.getId(), enableReqVO.getEnabled());
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 SRM 编码规则")
    @Parameter(name = "id", description = "规则编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('srm:code-rule:query')")
    public CommonResult<SrmCodeRuleRespVO> getCodeRule(@RequestParam("id") Long id) {
        SrmCodeRuleDO codeRule = codeRuleService.getCodeRule(id);
        return success(BeanUtils.toBean(codeRule, SrmCodeRuleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得 SRM 编码规则分页")
    @PreAuthorize("@ss.hasPermission('srm:code-rule:query')")
    public CommonResult<PageResult<SrmCodeRuleRespVO>> getCodeRulePage(@Valid SrmCodeRulePageReqVO pageReqVO) {
        PageResult<SrmCodeRuleDO> pageResult = codeRuleService.getCodeRulePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, SrmCodeRuleRespVO.class));
    }

}
