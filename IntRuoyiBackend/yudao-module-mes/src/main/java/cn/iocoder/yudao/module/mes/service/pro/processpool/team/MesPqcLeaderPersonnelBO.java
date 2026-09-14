package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesPqcLeaderPersonnelBO {

    private Long scopeId;
    private Long systemUserId;
    private String displayName;
    private String username;
    private Boolean enabled;
}
