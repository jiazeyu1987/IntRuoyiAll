# FormCenter Template Version Route Missing

## Task Goal

Fix the user-visible error `请求地址不存在:admin-api/form-center/templates/28/versions/V3.0` by identifying the owning FormCenter navigation/API path and removing the incorrect request without adding fallback behavior.

## Milestones

- [x] Reproduce or isolate the missing-route request path.
- [x] Add a focused regression test that fails before the fix.
- [x] Implement the smallest root-cause fix.
- [x] Run targeted verification and record RED/GREEN evidence.
- [x] Complete closeout evidence without touching unrelated concurrent work.

## Expected Verification

- Focused regression test covering FormCenter slot/template navigation.
- Relevant frontend/backend targeted checks for the touched module.
- `git diff --check` for task-owned files.

## Current Status

completed

Verification passed in the current checkout and live local runtime. Cleanup preview/apply completed with no deletions, unrelated dirty workspace changes were preserved in separate baseline commits, and the FormCenter task records are complete.

## Experience Gates

- `docs/frontend-development.md#切换填写人-formcenter-槽位导航门禁`: after `openTask` returns the embedded template snapshot, rendering must not require `/form-center/templates/{id}/versions/{versionNo}`; missing unique execution context, wrong `assistUserId`, missing template snapshot, or fallback navigation must block.
- `docs/backend-development.md`: dynamic route form task details must carry complete FormCenter context; if `openTask` succeeds but rendering still requires `/form-center/templates/{id}/versions/{versionNo}`, the root chain is incomplete and must be fixed instead of masked.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；目标是移除错误接口依赖或补齐正式上下文链路。
- `是否存在临时补丁或绕过`：否。
