package cn.iocoder.yudao.module.mes.productionrelease.core;

import java.util.List;

/**
 * Explicit integration blocker used until flow 4/6/8 publish their persistent context adapters.
 * It never accepts request payloads as a substitute for authoritative receipts.
 */
public class MesReleaseAuthoritativeContextUnavailablePort implements MesReleaseAuthoritativeContextPort {

    @Override
    public MesReleaseFinalizationEvidence require(MesReleaseFinalizationCommand command) {
        throw new MesReleaseFlowBlockerException(
                "authoritative flow 4/6/8 receipt context is not wired",
                new MesReleaseFlowFailureRespVO()
                        .setStage(MesReleaseFlowStage.SP_4)
                        .setBlockers(List.of(new MesReleaseFlowBlocker()
                                .setBlockerType(MesReleaseFlowBlockerType.AUTHORITATIVE_RECEIPT_CONTEXT_REQUIRED)
                                .setObjectType("RELEASE_FINALIZATION")
                                .setObjectId(command == null || command.getReleaseTransactionId() == null
                                        ? null : String.valueOf(command.getReleaseTransactionId()))
                                .setReason("flow 4 completion, flow 6 prerequisite and flow 8 material receipts "
                                        + "must be loaded from their state owners")
                                .setSuggestion("wire the persistent owner adapters before enabling finalization"))));
    }
}
