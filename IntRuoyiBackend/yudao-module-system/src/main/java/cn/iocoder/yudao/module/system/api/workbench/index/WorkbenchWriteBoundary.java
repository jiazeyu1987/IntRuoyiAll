package cn.iocoder.yudao.module.system.api.workbench.index;

import java.lang.annotation.*;

/** Explicit module-owned root boundary, ordered before Spring's business transaction advice. */
@Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE,ElementType.METHOD})
public @interface WorkbenchWriteBoundary {
    ProjectionModel.Scope value() default ProjectionModel.Scope.TENANT;
}
