package cn.iocoder.yudao.module.mes.productionrelease.core;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Read-only evidence resolved from the authoritative flow 4/6/8 owners.
 * HTTP callers provide identifiers; they cannot provide the evidence payload used for release.
 */
@Data
@Accessors(chain = true)
public class MesReleaseFinalizationEvidence {

    private CompletionBackfillReceipt completionBackfillReceipt;
    private IndependentBatchPrerequisiteReceipt independentPrerequisiteReceipt;
    private MesReleaseMaterialGateReceipt materialGateReceipt;
}
