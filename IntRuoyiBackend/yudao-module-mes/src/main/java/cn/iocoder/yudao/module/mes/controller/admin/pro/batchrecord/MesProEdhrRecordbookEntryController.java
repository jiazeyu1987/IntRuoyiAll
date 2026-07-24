package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEntryCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEntryPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEntryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEntrySaveDraftReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEntrySubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEventRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordbookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES eDHR 记录本条目")
@RestController
@RequestMapping("/mes/pro/edhr-recordbook-entry")
@Validated
public class MesProEdhrRecordbookEntryController {

    @Resource
    private MesProEdhrRecordbookService recordbookService;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-recordbook-entry:query')")
    public CommonResult<PageResult<MesProEdhrRecordbookEntryRespVO>> getPage(
            @Valid MesProEdhrRecordbookEntryPageReqVO reqVO) {
        return success(recordbookService.getEntryPage(reqVO));
    }

    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-recordbook-entry:query')")
    public CommonResult<MesProEdhrRecordbookEntryRespVO> get(@RequestParam("id") Long id) {
        return success(recordbookService.getEntry(id));
    }

    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-recordbook-entry:create')")
    public CommonResult<MesProEdhrRecordbookEntryRespVO> create(
            @Valid @RequestBody MesProEdhrRecordbookEntryCreateReqVO reqVO) {
        return success(recordbookService.createEntry(reqVO));
    }

    @PutMapping("/save-draft")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-recordbook-entry:save')")
    public CommonResult<MesProEdhrRecordbookEntryRespVO> saveDraft(
            @Valid @RequestBody MesProEdhrRecordbookEntrySaveDraftReqVO reqVO) {
        return success(recordbookService.saveDraft(reqVO));
    }

    @PutMapping("/submit")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-recordbook-entry:submit')")
    public CommonResult<MesProEdhrRecordbookEntryRespVO> submit(
            @Valid @RequestBody MesProEdhrRecordbookEntrySubmitReqVO reqVO) {
        return success(recordbookService.submit(reqVO));
    }

    @GetMapping("/event/page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-recordbook-entry:query')")
    public CommonResult<PageResult<MesProEdhrRecordbookEventRespVO>> getEventPage(
            @Valid MesProEdhrRecordbookEventPageReqVO reqVO) {
        return success(recordbookService.getEventPage(reqVO));
    }
}
