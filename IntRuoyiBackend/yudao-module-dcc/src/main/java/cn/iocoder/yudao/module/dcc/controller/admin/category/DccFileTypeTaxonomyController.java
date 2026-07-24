package cn.iocoder.yudao.module.dcc.controller.admin.category;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileTypeTaxonomyRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileTypeTaxonomySaveReqVO;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - DCC 文件类型五级分类")
@RestController
@RequestMapping("/dcc/file-type-taxonomies")
@Validated
public class DccFileTypeTaxonomyController {

    @Resource
    private DccFileTypeTaxonomyAdminService taxonomyAdminService;

    @GetMapping
    @Operation(summary = "获取文件类型五级分类列表")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<List<DccFileTypeTaxonomyRespVO>> getTaxonomyList() {
        return success(convertList(taxonomyAdminService.getTaxonomyList(),
                item -> BeanUtils.toBean(item, DccFileTypeTaxonomyRespVO.class)));
    }

    @PostMapping
    @Operation(summary = "创建文件类型分类节点")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<Long> createTaxonomy(@Valid @RequestBody DccFileTypeTaxonomySaveReqVO reqVO) {
        return success(taxonomyAdminService.createTaxonomy(reqVO));
    }

    @PutMapping("/{id:\\d+}")
    @Operation(summary = "更新文件类型分类节点")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<Boolean> updateTaxonomy(@PathVariable("id") Long id,
                                                @Valid @RequestBody DccFileTypeTaxonomySaveReqVO reqVO) {
        reqVO.setId(id);
        taxonomyAdminService.updateTaxonomy(reqVO);
        return success(true);
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "删除文件类型分类节点")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:category:manage')")
    public CommonResult<Boolean> deleteTaxonomy(@PathVariable("id") Long id) {
        taxonomyAdminService.deleteTaxonomy(id);
        return success(true);
    }
}
