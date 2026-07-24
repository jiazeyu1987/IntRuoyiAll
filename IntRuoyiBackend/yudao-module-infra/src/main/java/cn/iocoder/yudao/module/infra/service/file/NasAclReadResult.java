package cn.iocoder.yudao.module.infra.service.file;

import java.util.List;

public record NasAclReadResult(
        String path,
        String ownerSid,
        String groupSid,
        List<String> controlFlags,
        boolean daclPresent,
        boolean daclProtected,
        List<NasAclAce> aces
) {

    public NasAclReadResult {
        controlFlags = List.copyOf(controlFlags);
        aces = List.copyOf(aces);
    }
}
