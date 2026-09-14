package cn.iocoder.yudao.module.mes.productionrelease.core;

import lombok.Getter;

import java.util.Objects;

@Getter
public class MesReleaseFlowBlockerException extends RuntimeException {

    private final MesReleaseFlowFailureRespVO failure;

    public MesReleaseFlowBlockerException(String message, MesReleaseFlowFailureRespVO failure) {
        super(message);
        this.failure = Objects.requireNonNull(failure, "failure must not be null");
    }
}
