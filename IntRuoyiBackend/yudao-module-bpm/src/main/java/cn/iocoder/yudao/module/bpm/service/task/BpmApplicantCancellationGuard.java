package cn.iocoder.yudao.module.bpm.service.task;

import org.flowable.engine.runtime.ProcessInstance;

/** Business checks run in the cancellation transaction, including direct BPM entry points. */
public interface BpmApplicantCancellationGuard {
    void validate(Long actorId, ProcessInstance instance, String reason);
}
