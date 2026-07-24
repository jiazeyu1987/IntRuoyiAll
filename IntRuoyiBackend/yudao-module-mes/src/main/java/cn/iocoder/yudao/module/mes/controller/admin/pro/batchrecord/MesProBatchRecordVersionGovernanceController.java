package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionDraftReuploadRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceImpactRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceMetricsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceRollbackReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionMigrationConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionMigrationConfirmRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionMigrationDiffRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordVersionGovernanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - eDHR批记录版本治理")
@RestController
@RequestMapping("/mes/pro/batch-record-version/governance")
@Validated
public class MesProBatchRecordVersionGovernanceController {

    @Resource
    private MesProBatchRecordVersionGovernanceService governanceService;

    @GetMapping("/summary")
    @Operation(summary = "查询批记录版本治理摘要")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-version:governance-query')")
    public CommonResult<MesProBatchRecordVersionGovernanceSummaryRespVO> getSummary(
            @NotNull(message = "批记录定义不能为空") @RequestParam("definitionId") Long definitionId) {
        return success(governanceService.getSummary(definitionId));
    }

    @GetMapping("/impact")
    @Operation(summary = "查询批记录版本影响面")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-version:governance-query')")
    public CommonResult<MesProBatchRecordVersionGovernanceImpactRespVO> getImpact(
            @NotNull(message = "批记录版本不能为空") @RequestParam("versionId") Long versionId) {
        return success(governanceService.getImpact(versionId));
    }

    @GetMapping("/inspection")
    @Operation(summary = "查询批记录版本巡检结果")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-version:governance-query')")
    public CommonResult<MesProBatchRecordVersionGovernanceInspectionRespVO> getInspection(
            @NotNull(message = "批记录版本不能为空") @RequestParam("versionId") Long versionId) {
        return success(governanceService.getInspection(versionId));
    }

    @GetMapping("/metrics")
    @Operation(summary = "查询批记录版本运营指标")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-version:governance-query')")
    public CommonResult<MesProBatchRecordVersionGovernanceMetricsRespVO> getMetrics(
            @NotNull(message = "批记录版本不能为空") @RequestParam("versionId") Long versionId) {
        return success(governanceService.getMetrics(versionId));
    }

    @PostMapping("/rollback/request")
    @Operation(summary = "申请批记录版本受控回滚")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-version:rollback-request')")
    public CommonResult<MesProEdhrUnifiedChangeRespVO> requestRollback(
            @Valid @RequestBody MesProBatchRecordVersionGovernanceRollbackReqVO reqVO) {
        return success(governanceService.requestRollback(reqVO));
    }

    @GetMapping("/migration-diff")
    @Operation(summary = "查询批记录版本迁移差异")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-version:query')")
    public CommonResult<MesProBatchRecordVersionMigrationDiffRespVO> getMigrationDiff(
            @NotNull(message = "批记录版本不能为空") @RequestParam("versionId") Long versionId) {
        return success(governanceService.getMigrationDiff(versionId));
    }

    @PostMapping("/migration-confirm")
    @Operation(summary = "确认批记录版本迁移项")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-version:confirm')")
    public CommonResult<MesProBatchRecordVersionMigrationConfirmRespVO> confirmMigrationItems(
            @NotNull(message = "批记录版本不能为空") @RequestParam("versionId") Long versionId,
            @Valid @RequestBody MesProBatchRecordVersionMigrationConfirmReqVO reqVO) {
        return success(governanceService.confirmMigrationItems(versionId, reqVO));
    }

    @PostMapping("/draft-reupload")
    @Operation(summary = "重新上传批记录版本草稿")
    @PreAuthorize("@ss.hasPermission('mes:pro-batch-record-version:import')")
    public CommonResult<MesProBatchRecordVersionDraftReuploadRespVO> reuploadDraft(
            @NotNull(message = "批记录版本不能为空") @RequestParam("versionId") Long versionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("productNames") List<String> productNames,
            @RequestParam(value = "remark", required = false) String remark) {
        return success(governanceService.reuploadDraft(versionId, file, productNames, remark));
    }
}
