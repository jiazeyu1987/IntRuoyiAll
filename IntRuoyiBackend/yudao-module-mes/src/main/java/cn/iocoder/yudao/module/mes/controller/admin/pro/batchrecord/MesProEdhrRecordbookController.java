package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookRespVO;
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

@Tag(name = "管理后台 - MES eDHR 记录本")
@RestController
@RequestMapping("/mes/pro/edhr-recordbook")
@Validated
public class MesProEdhrRecordbookController {

    @Resource
    private MesProEdhrRecordbookService recordbookService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-recordbook:query')")
    public CommonResult<PageResult<MesProEdhrRecordbookRespVO>> getPage(
            @Valid MesProEdhrRecordbookPageReqVO reqVO) {
        return success(recordbookService.getRecordbookPage(reqVO));
    }

    @GetMapping("/my-page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-recordbook:query')")
    public CommonResult<PageResult<MesProEdhrRecordbookRespVO>> getMyPage(
            @Valid MesProEdhrRecordbookPageReqVO reqVO) {
        return success(recordbookService.getMyRecordbookPage(reqVO));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-recordbook:create')")
    public CommonResult<MesProEdhrRecordbookRespVO> create(
            @Valid @RequestBody MesProEdhrRecordbookCreateReqVO reqVO) {
        return success(recordbookService.createRecordbook(reqVO));
    }
}
