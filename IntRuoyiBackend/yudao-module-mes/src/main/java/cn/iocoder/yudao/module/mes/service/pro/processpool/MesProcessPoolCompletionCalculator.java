package cn.iocoder.yudao.module.mes.service.pro.processpool;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_FIFO_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_FIFO_QUANTITY_INVALID;

public class MesProcessPoolCompletionCalculator {

    public MesProcessPoolCompletionResult calculate(MesProcessPoolCompletionCommand command) {
        Objects.requireNonNull(command, "command");
        requirePresent(command.getWorkOrderId(), "workOrderId");
        requirePresent(command.getTargetRouteProcessId(), "targetRouteProcessId");
        requirePositive(command.getRequiredQuantity(), "requiredQuantity");
        List<MesProcessPoolSubmissionQuantity> submissions = Objects.requireNonNull(
                command.getSubmissions(), "submissions");

        BigDecimal totalSubmittedQuantity = BigDecimal.ZERO;
        LinkedHashSet<Long> submittedEventIds = new LinkedHashSet<>();
        LinkedHashSet<Long> employeeUserIds = new LinkedHashSet<>();
        for (MesProcessPoolSubmissionQuantity submission : submissions) {
            Objects.requireNonNull(submission, "submission");
            requirePresent(submission.getSourceEventId(), "sourceEventId");
            requirePresent(submission.getEmployeeUserId(), "employeeUserId");
            requirePositive(submission.getSubmittedQuantity(), "submittedQuantity");
            totalSubmittedQuantity = totalSubmittedQuantity.add(submission.getSubmittedQuantity());
            submittedEventIds.add(submission.getSourceEventId());
            employeeUserIds.add(submission.getEmployeeUserId());
        }

        return MesProcessPoolCompletionResult.of(
                totalSubmittedQuantity.compareTo(command.getRequiredQuantity()) >= 0,
                totalSubmittedQuantity,
                submissions.size(),
                List.copyOf(submittedEventIds),
                List.copyOf(employeeUserIds));
    }

    private static void requirePresent(Object value, String fieldName) {
        if (value == null) {
            throw exception(PRO_PROCESS_POOL_FIFO_CONTEXT_REQUIRED, fieldName);
        }
    }

    private static void requirePositive(BigDecimal quantity, String fieldName) {
        requirePresent(quantity, fieldName);
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_FIFO_QUANTITY_INVALID, fieldName);
        }
    }

}
