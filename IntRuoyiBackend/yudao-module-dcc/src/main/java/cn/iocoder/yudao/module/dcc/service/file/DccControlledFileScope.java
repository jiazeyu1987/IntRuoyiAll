package cn.iocoder.yudao.module.dcc.service.file;

import java.util.List;
import java.util.Objects;

public record DccControlledFileScope(Long infraFileId, List<DccControlledFileArtifactReference> references) {

    public DccControlledFileScope {
        references = List.copyOf(Objects.requireNonNull(references, "references"));
    }

    public boolean controlled() {
        return !references.isEmpty();
    }
}
