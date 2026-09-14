package cn.iocoder.yudao.module.mdm.controller.admin.enterprise;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo.MdmEnterprisePageReqVO;
import cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo.MdmEnterpriseRespVO;
import cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo.MdmEnterpriseSaveReqVO;
import cn.iocoder.yudao.module.mdm.controller.admin.enterprise.vo.MdmEnterpriseSimpleRespVO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.enterprise.MdmEnterpriseDO;
import cn.iocoder.yudao.module.mdm.service.enterprise.MdmEnterpriseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 关联公司")
@RestController
@RequestMapping("/mdm/enterprise")
@Validated
public class MdmEnterpriseController {

    @Resource
    private MdmEnterpriseService enterpriseService;

    @GetMapping("/page")
    @Operation(summary = "获得关联公司分页")
    @PreAuthorize("@ss.hasPermission('mdm:enterprise:query')")
    public CommonResult<PageResult<MdmEnterpriseRespVO>> getEnterprisePage(@Valid MdmEnterprisePageReqVO pageReqVO) {
        return success(BeanUtils.toBean(enterpriseService.getEnterprisePage(pageReqVO), MdmEnterpriseRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得关联公司详情")
    @Parameter(name = "id", description = "关联公司编号", required = true)
    @PreAuthorize("@ss.hasPermission('mdm:enterprise:query')")
    public CommonResult<MdmEnterpriseRespVO> getEnterprise(@RequestParam("id") Long id) {
        MdmEnterpriseDO enterprise = enterpriseService.getEnterprise(id);
        return success(enterprise == null ? null : BeanUtils.toBean(enterprise, MdmEnterpriseRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "新增关联公司")
    @PreAuthorize("@ss.hasPermission('mdm:enterprise:create')")
    public CommonResult<Long> createEnterprise(@Valid @RequestBody MdmEnterpriseSaveReqVO reqVO) {
        return success(enterpriseService.createEnterprise(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改关联公司")
    @PreAuthorize("@ss.hasPermission('mdm:enterprise:update')")
    public CommonResult<Boolean> updateEnterprise(@Valid @RequestBody MdmEnterpriseSaveReqVO reqVO) {
        enterpriseService.updateEnterprise(reqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "启用或停用关联公司")
    @PreAuthorize("@ss.hasPermission('mdm:enterprise:update')")
    public CommonResult<Boolean> updateEnterpriseStatus(@RequestParam("id") Long id,
                                                        @RequestParam("status") String status) {
        enterpriseService.updateEnterpriseStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除关联公司")
    @PreAuthorize("@ss.hasPermission('mdm:enterprise:delete')")
    public CommonResult<Boolean> deleteEnterprise(@RequestParam("id") Long id) {
        enterpriseService.deleteEnterprise(id);
        return success(true);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得关联公司精简列表")
    @PreAuthorize("@ss.hasPermission('mdm:enterprise:query')")
    public CommonResult<List<MdmEnterpriseSimpleRespVO>> getSimpleEnterpriseList(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(BeanUtils.toBean(enterpriseService.listSimpleEnterprises(type, status, keyword),
                MdmEnterpriseSimpleRespVO.class));
    }

}
