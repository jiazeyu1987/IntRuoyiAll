package cn.iocoder.yudao.module.system.service.fenbeitongassistant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FenbeitongAssistantTicketPayload {

    private Long userId;
    private Set<String> permissions;
    private Long expiresAtEpochMilli;

}
