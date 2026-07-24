package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrControlledTagCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrControlledTagPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrControlledTagRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrControlledTagStatusReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES eDHR 受控标签")
@RestController
@RequestMapping("/mes/pro/edhr-tag")
@Validated
public class MesProEdhrTagController {

    @Resource
    private MesProEdhrRecordbookService recordbookService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-tag:query')")
    public CommonResult<PageResult<MesProEdhrControlledTagRespVO>> getPage(
            @Valid MesProEdhrControlledTagPageReqVO reqVO) {
        return success(recordbookService.getTagPage(reqVO));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-tag:create')")
    public CommonResult<MesProEdhrControlledTagRespVO> create(
            @Valid @RequestBody MesProEdhrControlledTagCreateReqVO reqVO) {
        return success(recordbookService.createTag(reqVO));
    }

    @PostMapping("/activate")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-tag:activate')")
    public CommonResult<MesProEdhrControlledTagRespVO> activate(
            @Valid @RequestBody MesProEdhrControlledTagStatusReqVO reqVO) {
        return success(recordbookService.activateTag(reqVO));
    }

    @PostMapping("/disable")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-tag:disable')")
    public CommonResult<MesProEdhrControlledTagRespVO> disable(
            @Valid @RequestBody MesProEdhrControlledTagStatusReqVO reqVO) {
        return success(recordbookService.disableTag(reqVO));
    }
}
