package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

public record MesProEdhrBatchTraceValidationResult(boolean valid, String blockerCode, String blockerScope) {

    public static MesProEdhrBatchTraceValidationResult ok() {
        return new MesProEdhrBatchTraceValidationResult(true, null, null);
    }

    public static MesProEdhrBatchTraceValidationResult blocked(String code, String scope) {
        return new MesProEdhrBatchTraceValidationResult(false, code, scope);
    }
}
