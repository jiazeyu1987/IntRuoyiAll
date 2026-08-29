package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkFormCellsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkPrefillRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkRulesSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkRulesSaveRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkWorkbenchContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordRepeatRowGroupSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordRepeatRowGroupSaveRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink.MesProBatchRecordCellLinkService;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "管理后台 - MES 批记录跨表单单元格链接")
@RestController
@RequestMapping("/mes/pro/batch-record-cell-link")
@Validated
public class MesProBatchRecordCellLinkController {

    @Resource
    private MesProBatchRecordCellLinkService cellLinkService;

    @GetMapping("/workbench-context")
    @Operation(summary = "获取批记录跨表单单元格链接工作台上下文")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-cell-link:query')")
    public CommonResult<BatchRecordCellLinkWorkbenchContextRespVO> getWorkbenchContext(
            @RequestParam(value = "routeId", required = false) Long routeId,
            @RequestParam(value = "definitionId", required = false) Long definitionId,
            @RequestParam(value = "versionId", required = false) Long versionId,
            @RequestParam(value = "sourceReportId", required = false) String sourceReportId,
            @RequestParam(value = "templateId", required = false) Long templateId,
            @RequestParam(value = "versionNo", required = false) String versionNo,
            @RequestParam(value = "routeProcessId", required = false) Long routeProcessId,
            @RequestParam(value = "qaProcessId", required = false) Long qaProcessId,
            @RequestParam(value = "dccProjectCodeId", required = false) Long dccProjectCodeId) {
        return success(cellLinkService.getWorkbenchContext(routeId, definitionId, versionId, sourceReportId,
                templateId, versionNo, routeProcessId, qaProcessId, dccProjectCodeId));
    }

    @GetMapping("/form-cells")
    @Operation(summary = "获取可参与跨表单链接的真实表单单元格")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-cell-link:query')")
    public CommonResult<BatchRecordCellLinkFormCellsRespVO> getFormCells(
            @RequestParam("reportId") String reportId,
            @RequestParam(value = "versionId", required = false) Long versionId) {
        return success(cellLinkService.getFormCells(reportId, versionId));
    }

    @PostMapping("/rules/save")
    @Operation(summary = "保存批记录跨表单单元格链接规则")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-cell-link:update')")
    public CommonResult<BatchRecordCellLinkRulesSaveRespVO> saveRules(
            @Valid @RequestBody BatchRecordCellLinkRulesSaveReqVO reqVO) {
        return success(cellLinkService.saveRules(reqVO));
    }


    @PostMapping("/repeat-row-group/save")
    @Operation(summary = "保存批记录重复行组对应关系")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-cell-link:update')")
    public CommonResult<BatchRecordRepeatRowGroupSaveRespVO> saveRepeatRowGroup(
            @Valid @RequestBody BatchRecordRepeatRowGroupSaveReqVO reqVO) {
        return success(cellLinkService.saveRepeatRowGroup(reqVO));
    }
    @GetMapping("/prefill")
    @Operation(summary = "预览目标执行表单自动带值")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-cell-link:query') "
            + "or #workTaskId != null")
    public CommonResult<BatchRecordCellLinkPrefillRespVO> getPrefill(
            @RequestParam("targetExecutionId") Long targetExecutionId,
            @RequestParam(value = "workTaskId", required = false) Long workTaskId) {
        return success(cellLinkService.getPrefill(targetExecutionId, workTaskId));
    }
}
