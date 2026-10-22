package cn.iocoder.yudao.module.dcc.dal.dataobject.file;
import lombok.Data;
@Data
public class DccSourceNameReservationDO {
    private Long id;
    private Long tenantId;
    private String sourceOriginalFileName;
    private String reservationKind;
    private Long verificationScopeId;
    private Long modernClaimId;
    private Long modernMasterId;
    private Long generation;
    private Integer active;
}
