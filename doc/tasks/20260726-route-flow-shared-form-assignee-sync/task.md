# 共享动态表单填写人联动

## Task Goal

修复工艺路线流程图中动态表单填写人配置：当表单未开启“工序独立”（批次共享）时，在任一工序更换填写人来源或填写人后，同路线同表单的其他工序草稿必须同步更新；开启“工序独立”的表单不得被共享填写人同步影响。

## Milestones

- [x] 创建任务记录并记录 BDD 场景
- [x] 增加共享动态表单填写人联动的 RED 静态回归合同
- [x] 实现最小前端同步逻辑
- [x] 运行 GREEN 与相关静态回归验证
- [x] 记录验证报告与收尾状态

## Expected Verification

- `node tests/e2e/mes-route-flow-shared-form-filler-sync-static.spec.js`
- `node tests/e2e/mes-route-flow-form-process-independent-switch-static.spec.js`
- `node tests/e2e/mes-route-flow-shared-form-simplify-static.spec.js`

## Current Status

ready_for_closeout

## 经验门禁

- `docs/frontend-development.md#前端静态契约隔离门禁`：本任务是窄范围前端行为修复，优先新增任务专用最小静态合同覆盖当前行为，避免被无关历史大契约阻塞。
- `docs/e2e-rules.md#静态合同与真实-e2e-同步门禁`：修改 `tests/e2e/*static.spec.js` 后必须运行目标静态合同；若宽合同失败在无关历史问题，需记录阻塞而不是顺手扩大范围。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；将共享表单填写人同步收敛为显式 helper，并按 `formTemplateId + BATCH_SHARED` 范围更新所有工序草稿。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260726-route-flow-shared-form-assignee-sync/bug-regression-evidence.md
