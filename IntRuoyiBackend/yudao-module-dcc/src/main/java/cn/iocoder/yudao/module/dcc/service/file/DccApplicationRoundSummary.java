package cn.iocoder.yudao.module.dcc.service.file;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/** A persisted file/type/BPM mapping; no inferred or reserved draft rounds. */
@Data
public class DccApplicationRoundSummary {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long controlledFileId;
    private String applicationType;
    private String bpmRound;
    private Integer attributeRound;
}
