package cn.iocoder.yudao.module.system.service.fenbeitongassistant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Configuration for the assistant's existing ValidateUser password login. */
public interface FenbeitongKingdeeConfigProvider {

    PasswordLoginConfig getCurrentPasswordLoginConfig();

    @Getter
    @RequiredArgsConstructor
    class PasswordLoginConfig {
        private final String baseUrl;
        private final String acctId;
        private final String username;
        private final String password;
        private final Integer lcid;
    }
}
