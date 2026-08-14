package cn.iocoder.yudao.module.dcc.service.upload;

import java.time.LocalDateTime;

public interface DccUploadTicketService {

    DccUploadTicketCreated createTicket(DccUploadTicketCreateCommand command);

    DccUploadTicketCreated reuseActiveTicketOrReject(DccUploadTicketPreflightCommand command);

    DccUploadTicketBoundFile resolveForBinding(DccUploadTicketResolveCommand command);

    void markBound(DccUploadTicketMarkBoundCommand command);

    int cleanupExpiredTemporaryFiles(LocalDateTime cleanupTime, int limit) throws Exception;

    int cleanupSessionTemporaryFiles(Long userId, String sessionId, LocalDateTime cleanupTime, String cleanupReason)
            throws Exception;

    DccUploadTemporaryFileStatus getTemporaryFileStatusByRequestId(Long userId, String requestId);
}
