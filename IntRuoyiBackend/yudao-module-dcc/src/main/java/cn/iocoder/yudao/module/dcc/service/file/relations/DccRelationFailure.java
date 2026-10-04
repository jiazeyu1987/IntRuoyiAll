package cn.iocoder.yudao.module.dcc.service.file.relations;

/** Recognized D business failures. Unrelated technical exceptions retain the global error boundary. */
public class DccRelationFailure extends IllegalStateException {
    public DccRelationFailure(String code){super(code);}
    public DccRelationFailure(String code, Throwable cause){super(code,cause);}
}
