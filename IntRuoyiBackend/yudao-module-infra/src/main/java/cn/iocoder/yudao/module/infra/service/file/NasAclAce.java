package cn.iocoder.yudao.module.infra.service.file;

import java.util.List;

public record NasAclAce(
        int index,
        String aceType,
        List<String> aceFlags,
        long accessMask,
        String trusteeSid,
        boolean inherited
) {

    public NasAclAce {
        aceFlags = List.copyOf(aceFlags);
    }
}
