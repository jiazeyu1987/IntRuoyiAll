package cn.iocoder.yudao.module.mes.service.pro.processpool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolEventRevisionUpdateReqBO {

    private Long eventId;
    private String afterPayload;
    private String changeReason;
    private Long revisionSignatureId;
    private Long revisionSignatureUserId;
    private String revisionSignatureSnapshot;
    private Long modifiedByUserId;
    private List<MesProcessPoolEventRevisionFieldChangeBO> changedFields;
}
