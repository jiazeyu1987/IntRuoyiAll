package cn.iocoder.yudao.module.dcc.service.file;

import com.fasterxml.jackson.annotation.JsonFormat;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSignatureSummaryRespVO;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes;
import java.util.List;

/** Authorized, immutable application read evidence; never a draft edit or current project defaults. */
public record DccControlledFileApplicationEvidence(
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long controlledFileId,
        String versionNo, String applicationType, String bpmRound, Integer attributeRound,
        boolean recorded, String unavailableReason,
        DccProjectAttributes defaultSource, DccProjectAttributes actualAttributes,
        List<DccControlledFileSignatureSummaryRespVO> signatures) {}
