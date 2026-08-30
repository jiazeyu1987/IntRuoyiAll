package cn.iocoder.yudao.module.mdm.controller.admin.companyscope;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mdm.controller.admin.companyscope.vo.MdmCompanyScopePageReqVO;
import cn.iocoder.yudao.module.mdm.controller.admin.companyscope.vo.MdmCompanyScopeRespVO;
import cn.iocoder.yudao.module.mdm.controller.admin.companyscope.vo.MdmCompanyScopeSaveReqVO;
import cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 企业公司范围")
@RestController
@RequestMapping("/mdm/company-scope")
@Validated
public class MdmCompanyScopeController {

    @Resource
    private MdmCompanyScopeService companyScopeService;

    @GetMapping("/page")
    @Operation(summary = "获得企业公司范围分页")
    @PreAuthorize("@ss.hasPermission('mdm:company-scope:query')")
    public CommonResult<PageResult<MdmCompanyScopeRespVO>> getCompanyScopePage(
            @Valid MdmCompanyScopePageReqVO reqVO) {
        return success(companyScopeService.getCompanyScopePage(reqVO));
    }

    @PostMapping("/create")
    @Operation(summary = "创建企业公司范围")
    @PreAuthorize("@ss.hasPermission('mdm:company-scope:create')")
    public CommonResult<Long> createCompanyScope(@Valid @RequestBody MdmCompanyScopeSaveReqVO reqVO) {
        return success(companyScopeService.createCompanyScope(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新企业公司范围")
    @PreAuthorize("@ss.hasPermission('mdm:company-scope:update')")
    public CommonResult<Boolean> updateCompanyScope(@Valid @RequestBody MdmCompanyScopeSaveReqVO reqVO) {
        companyScopeService.updateCompanyScope(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除企业公司范围")
    @PreAuthorize("@ss.hasPermission('mdm:company-scope:delete')")
    public CommonResult<Boolean> deleteCompanyScope(@RequestParam("scopeType") String scopeType,
                                                    @RequestParam("id") Long id) {
        companyScopeService.deleteCompanyScope(scopeType, id);
        return success(true);
    }
}
