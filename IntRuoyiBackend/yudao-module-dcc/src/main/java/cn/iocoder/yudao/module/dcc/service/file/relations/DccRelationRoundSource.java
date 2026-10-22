package cn.iocoder.yudao.module.dcc.service.file.relations;

import com.fasterxml.jackson.annotation.JsonFormat;

/** Formal allocated destination and exact bound source, never inferred from file/latest BPM. */
public record DccRelationRoundSource(
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long tenantId,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long projectId,
        String targetApplicationType,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long targetFileId,Integer targetAttributeRound,
        String sourceApplicationType,
        @JsonFormat(shape=JsonFormat.Shape.STRING) Long sourceFileId,
        String sourceBpmRound,String sourceRelationDigest) {}
