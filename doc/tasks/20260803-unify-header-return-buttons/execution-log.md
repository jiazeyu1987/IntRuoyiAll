# Execution Log

## User Intent

用户基于截图指出红框里的“返回表单模板”这类按钮需要统一成“返回”按钮，并要求检查其它前端页面是否存在类似按钮并统一修改。

## BDD / TDD Plan

- BDD: Header return button copy is unified -> Given a page/workspace header contains a left-arrow return control, When the control returns to the previous list/workspace, Then the visible label is the standard “返回” and the existing click handler remains unchanged.
- BDD: Similar return controls are scanned globally -> Given frontend pages may contain “返回xxx” controls, When the static contract scans scoped Vue pages, Then disallowed long header return labels are rejected unless they are non-header business copy.
- BDD: Business behavior is preserved -> Given the user clicks the unified return button, When the existing handler runs, Then route/API/permission/save/error behavior remains the same as before.

## Initial Notes

- Existing worktree has unrelated concurrent dirty changes and local commits. This task must use selective staging only and must not use `git add -A`.
- Protected files: backend, API wrappers, route guards, permission SQL/data, and unrelated task documents.
