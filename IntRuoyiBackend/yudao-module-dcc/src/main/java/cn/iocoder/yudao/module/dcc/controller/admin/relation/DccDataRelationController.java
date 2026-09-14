package cn.iocoder.yudao.module.dcc.controller.admin.relation;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.relation.vo.DccDataRelationCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.relation.vo.DccDataRelationRespVO;
import cn.iocoder.yudao.module.dcc.service.relation.DccDataRelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - DCC 三方明确关联")
@RestController
@RequestMapping("/dcc/data-relations")
@Validated
public class DccDataRelationController {

    @Resource
    private DccDataRelationService relationService;

    @PostMapping("/create")
    @Operation(summary = "确认 DCC 产品目录、项目代码、注册证关联")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update')")
    public CommonResult<DccDataRelationRespVO> create(@Valid @RequestBody DccDataRelationCreateReqVO reqVO) {
        return success(BeanUtils.toBean(relationService.createRelation(getLoginUserId(), reqVO),
                DccDataRelationRespVO.class));
    }

    @GetMapping("/by-product-catalog/{id:\\d+}")
    @Operation(summary = "按 DCC 产品目录查询明确关联")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:query')")
    public CommonResult<List<DccDataRelationRespVO>> getByProductCatalogId(@PathVariable("id") @NotNull Long id) {
        return success(BeanUtils.toBean(relationService.getByProductCatalogId(id), DccDataRelationRespVO.class));
    }

    @GetMapping("/by-project-code/{id:\\d+}")
    @Operation(summary = "按 DCC 项目代码查询明确关联")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:query')")
    public CommonResult<List<DccDataRelationRespVO>> getByProjectCodeId(@PathVariable("id") @NotNull Long id) {
        return success(BeanUtils.toBean(relationService.getByProjectCodeId(id), DccDataRelationRespVO.class));
    }

    @GetMapping("/by-registration-certificate/{id:\\d+}")
    @Operation(summary = "按注册证查询明确关联")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:query')")
    public CommonResult<List<DccDataRelationRespVO>> getByRegistrationCertificateId(
            @PathVariable("id") @NotNull Long id) {
        return success(BeanUtils.toBean(relationService.getByRegistrationCertificateId(id),
                DccDataRelationRespVO.class));
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "解除 DCC 三方明确关联")
    @PreAuthorize("@ss.hasPermission('dcc:project-code:update')")
    public CommonResult<Boolean> delete(@PathVariable("id") @NotNull Long id) {
        relationService.deleteRelation(id);
        return success(true);
    }
}
