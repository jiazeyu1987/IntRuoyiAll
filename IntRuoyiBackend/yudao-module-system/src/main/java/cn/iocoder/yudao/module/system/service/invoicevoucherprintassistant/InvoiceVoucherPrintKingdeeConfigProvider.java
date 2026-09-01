package cn.iocoder.yudao.module.system.service.invoicevoucherprintassistant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public interface InvoiceVoucherPrintKingdeeConfigProvider {

    KingdeeConfigSnapshot getCurrentConfigSnapshot();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    class KingdeeConfigSnapshot {

        private String baseUrl;
        private String acctId;
        private String username;
        private String password;
        private String appId;
        private String signedData;
        private String timestamp;
        private Integer lcid;

    }

}
