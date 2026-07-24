package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRequestReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrRecordChangeRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordChangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - eDHR记录变更")
@RestController
@RequestMapping("/mes/pro/edhr-change")
@Validated
public class MesProEdhrRecordChangeController {

    @Resource
    private MesProEdhrRecordChangeService edhrRecordChangeService;

    @PostMapping("/void-execution/request")
    @Operation(summary = "申请作废eDHR执行记录")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:void')")
    public CommonResult<EdhrRecordChangeRespVO> requestVoidExecution(@Valid @RequestBody EdhrRecordChangeRequestReqVO reqVO) {
        return success(edhrRecordChangeService.requestVoidExecution(reqVO));
    }

    @PostMapping("/void-execution/approve")
    @Operation(summary = "批准作废eDHR执行记录")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:approve')")
    public CommonResult<EdhrRecordChangeRespVO> approveVoidExecution(@Valid @RequestBody EdhrRecordChangeApproveReqVO reqVO) {
        return success(edhrRecordChangeService.approveVoidExecution(reqVO));
    }

    @PostMapping("/void-batch-execution/request")
    @Operation(summary = "申请作废eDHR批次执行")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:void')")
    public CommonResult<EdhrRecordChangeRespVO> requestVoidBatchExecution(@Valid @RequestBody EdhrRecordChangeRequestReqVO reqVO) {
        return success(edhrRecordChangeService.requestVoidBatchExecution(reqVO));
    }

    @PostMapping("/void-batch-execution/withdraw")
    @Operation(summary = "撤回eDHR批次执行作废申请")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:void')")
    public CommonResult<EdhrRecordChangeRespVO> withdrawVoidBatchExecution(@Valid @RequestBody EdhrRecordChangeApproveReqVO reqVO) {
        return success(edhrRecordChangeService.withdrawVoidBatchExecution(reqVO));
    }

    @PostMapping("/reopen-batch/request")
    @Operation(summary = "申请重开eDHR批次")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:reopen')")
    public CommonResult<EdhrRecordChangeRespVO> requestReopenBatch(@Valid @RequestBody EdhrRecordChangeRequestReqVO reqVO) {
        return success(edhrRecordChangeService.requestReopenBatch(reqVO));
    }

    @PostMapping("/reopen-batch/approve")
    @Operation(summary = "批准重开eDHR批次")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:approve')")
    public CommonResult<EdhrRecordChangeRespVO> approveReopenBatch(@Valid @RequestBody EdhrRecordChangeApproveReqVO reqVO) {
        return success(edhrRecordChangeService.approveReopenBatch(reqVO));
    }

    @PostMapping("/reopen-execution/request")
    @Operation(summary = "申请重开eDHR执行记录")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:reopen')")
    public CommonResult<EdhrRecordChangeRespVO> requestReopenExecution(@Valid @RequestBody EdhrRecordChangeRequestReqVO reqVO) {
        return success(edhrRecordChangeService.requestReopenExecution(reqVO));
    }

    @PostMapping("/reopen-execution/approve")
    @Operation(summary = "批准重开eDHR执行记录")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:approve')")
    public CommonResult<EdhrRecordChangeRespVO> approveReopenExecution(@Valid @RequestBody EdhrRecordChangeApproveReqVO reqVO) {
        return success(edhrRecordChangeService.approveReopenExecution(reqVO));
    }

    @PostMapping("/supplement/request")
    @Operation(summary = "申请补录eDHR记录")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:supplement')")
    public CommonResult<EdhrRecordChangeRespVO> requestSupplement(@Valid @RequestBody EdhrRecordChangeRequestReqVO reqVO) {
        return success(edhrRecordChangeService.requestSupplement(reqVO));
    }

    @PutMapping("/supplement/save-draft")
    @Operation(summary = "保存补录草稿")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:supplement')")
    public CommonResult<EdhrRecordChangeRespVO> saveSupplementDraft(@Valid @RequestBody EdhrRecordChangeRequestReqVO reqVO) {
        return success(edhrRecordChangeService.saveSupplementDraft(reqVO));
    }

    @PostMapping("/supplement/submit")
    @Operation(summary = "提交补录申请")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:supplement')")
    public CommonResult<EdhrRecordChangeRespVO> submitSupplement(@Valid @RequestBody EdhrRecordChangeApproveReqVO reqVO) {
        return success(edhrRecordChangeService.submitSupplement(reqVO));
    }

    @PostMapping("/supplement/approve")
    @Operation(summary = "批准补录eDHR记录")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:approve')")
    public CommonResult<EdhrRecordChangeRespVO> approveSupplement(@Valid @RequestBody EdhrRecordChangeApproveReqVO reqVO) {
        return success(edhrRecordChangeService.approveSupplement(reqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询eDHR记录变更")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:query')")
    public CommonResult<PageResult<EdhrRecordChangeRespVO>> getPage(@Valid EdhrRecordChangePageReqVO reqVO) {
        return success(edhrRecordChangeService.getPage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得eDHR记录变更")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-change:query')")
    public CommonResult<EdhrRecordChangeRespVO> get(@RequestParam("id") Long id) {
        return success(edhrRecordChangeService.get(id));
    }

}
