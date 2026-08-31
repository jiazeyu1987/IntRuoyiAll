package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesPqcReleaseDossierReadiness {

    private boolean ready;
    private String blockerReason;
    private String blockerSuggestion;

    public static MesPqcReleaseDossierReadiness ready() {
        return new MesPqcReleaseDossierReadiness().setReady(true);
    }

    public static MesPqcReleaseDossierReadiness blocked(String reason, String suggestion) {
        return new MesPqcReleaseDossierReadiness()
                .setReady(false)
                .setBlockerReason(reason)
                .setBlockerSuggestion(suggestion);
    }
}
