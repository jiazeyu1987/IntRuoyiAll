package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrTravelerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES eDHR 流转单")
@RestController
@RequestMapping("/mes/pro/edhr-traveler")
@Validated
public class MesProEdhrTravelerController {

    @Resource
    private MesProEdhrTravelerService travelerService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-traveler:query')")
    public CommonResult<PageResult<MesProEdhrTravelerRespVO>> getPage(@Valid MesProEdhrTravelerPageReqVO reqVO) {
        return success(travelerService.getPage(reqVO));
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-traveler:query')")
    public CommonResult<MesProEdhrTravelerRespVO> get(@RequestParam("id") Long id) {
        return success(travelerService.get(id));
    }

    @PostMapping("/generate")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-traveler:generate')")
    public CommonResult<MesProEdhrTravelerRespVO> generate(@Valid @RequestBody MesProEdhrTravelerGenerateReqVO reqVO) {
        return success(travelerService.generate(reqVO));
    }

    @GetMapping("/event/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-traveler:query')")
    public CommonResult<PageResult<MesProEdhrTravelerEventRespVO>> getEventPage(
            @Valid MesProEdhrTravelerEventPageReqVO reqVO) {
        return success(travelerService.getEventPage(reqVO));
    }
}
