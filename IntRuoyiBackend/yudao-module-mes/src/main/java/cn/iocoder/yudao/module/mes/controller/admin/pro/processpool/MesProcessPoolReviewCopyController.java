package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolReviewCopyGenerateSubmitReqVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolReviewCopyService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolReviewCopyFieldMappingDTO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolReviewCopyGenerateReqDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 工序池审核副本")
@RestController
@RequestMapping("/mes/pro/process-pool/review-copy")
@Validated
public class MesProcessPoolReviewCopyController {

    @Resource
    private MesProcessPoolReviewCopyService reviewCopyService;

    @PostMapping("/generate-submit")
    @Operation(summary = "生成并提交工序池审核副本")
    @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-review-copy:generate-submit')")
    public CommonResult<Long> generateAndSubmit(
            @RequestBody @Valid ProcessPoolReviewCopyGenerateSubmitReqVO reqVO) {
        return success(reviewCopyService.generateAndSubmitReviewCopy(toDTO(reqVO)));
    }

    private MesProcessPoolReviewCopyGenerateReqDTO toDTO(ProcessPoolReviewCopyGenerateSubmitReqVO reqVO) {
        return MesProcessPoolReviewCopyGenerateReqDTO.builder()
                .eventId(reqVO.getEventId())
                .reviewerUserId(reqVO.getReviewerUserId())
                .reviewerSignatureId(reqVO.getReviewerSignatureId())
                .reviewerSignatureUserId(reqVO.getReviewerSignatureUserId())
                .reviewerSignatureSnapshot(reqVO.getReviewerSignatureSnapshot())
                .fieldMappings(toMappingDTOs(reqVO.getFieldMappings()))
                .build();
    }

    private List<MesProcessPoolReviewCopyFieldMappingDTO> toMappingDTOs(
            List<ProcessPoolReviewCopyGenerateSubmitReqVO.FieldMapping> fieldMappings) {
        return fieldMappings.stream()
                .map(mapping -> MesProcessPoolReviewCopyFieldMappingDTO.builder()
                        .fieldCode(mapping.getFieldCode())
                        .fieldName(mapping.getFieldName())
                        .lowerLimit(mapping.getLowerLimit())
                        .upperLimit(mapping.getUpperLimit())
                        .valueType(mapping.getValueType())
                        .affectsAllocation(mapping.getAffectsAllocation())
                        .allocationField(mapping.getAllocationField())
                        .sourceQuantityFragmentId(mapping.getSourceQuantityFragmentId())
                        .templateFieldMetadataJson(mapping.getTemplateFieldMetadataJson())
                        .build())
                .toList();
    }
}
