# 任务：展厅后台隐藏三个协作页签

## 目标

从展厅后台前端入口中隐藏 `版本历史`、`补充指派`、`产品讨论` 三个子页签，使它们不再出现在菜单/页签导航中，但保留现有路由与组件代码，不额外引入降级逻辑。

## 非目标

- 不删除这三个功能的路由定义、页面组件或后端接口契约。
- 不调整审批中心、讲解工作台、产品管理、展厅管理、展厅公司等其他入口。
- 不引入 mock、兜底菜单或兼容分支。

## 前置任务检查

- 最近前端任务：`yudao-ui-admin-vue3/doc/tasks/20260519-showroom-hall-mapping-click-no-response/task.md`
- 启动前状态：已于 2026-05-19 显式标记为阻塞，原因是当前会话切换到更高优先级的页签可见性调整任务。
- 影响：本任务可独立推进，不会与旧任务源码改动发生冲突。

## 需求摘要

- 用户要求：前端不需要显示这三个页签。
- 范围限定：仅隐藏前端展示入口，不主动删除功能实现；如需彻底下线功能，需单独立项并同步清理后端。

## 里程碑

- [x] M1：检查前置任务状态并创建本次任务文档。
- [x] M2：补充失败测试并记录 RED 证据，证明三个页签当前仍会显示。
- [x] M3：以最小改动隐藏三个展厅后台子页签入口。
- [x] M4：运行回归验证并记录 GREEN 证据。
- [x] M5：更新任务文档、执行记录、证据文件并执行收尾预览。

## 预期验证

- `D:\Programs\node.exe --test scripts/showroom-admin-hide-three-tabs.test.mjs`
- `D:\Programs\node.exe --test --test-name-pattern "showroom-admin route module registers the back-office shell and children" scripts/showroom-admin-frontend.test.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-admin-hide-three-tabs-fresh run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-admin-hide-three-tabs\scripts\verify-showroom-admin-hide-three-tabs.mjs`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260519-showroom-admin-hide-three-tabs/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-showroom-admin-hide-three-tabs --mode preview`

## 当前状态

已完成：代码改动、脚本回归、真实前端路径复核、证据校验与 closeout 预览均已完成。

## Current Status

Completed on 2026-05-19. 展厅后台三个协作页签已隐藏，验证与收尾预览均已完成。

## 中间验证结果

- RED：`D:\Programs\node.exe --test scripts/showroom-admin-hide-three-tabs.test.mjs` 已失败，错误为三个子路由缺少 `meta.hidden: true`。
- GREEN：`D:\Programs\node.exe --test scripts/showroom-admin-hide-three-tabs.test.mjs` 已通过。
- GREEN：`D:\Programs\node.exe --test --test-name-pattern "showroom-admin route module registers the back-office shell and children" scripts/showroom-admin-frontend.test.mjs` 已通过。
- GREEN：`npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-admin-hide-three-tabs-fresh run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-admin-hide-three-tabs\scripts\verify-showroom-admin-hide-three-tabs.mjs` 已通过，真实页面只显示 `展厅公司 / 产品管理 / 展厅管理 / 审批中心 / 讲解工作台`。

## 最终验证结果

- PASS：`D:\Programs\node.exe --test scripts/showroom-admin-hide-three-tabs.test.mjs`
- PASS：`D:\Programs\node.exe --test --test-name-pattern "showroom-admin route module registers the back-office shell and children" scripts/showroom-admin-frontend.test.mjs`
- PASS：`npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-admin-hide-three-tabs-fresh run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-admin-hide-three-tabs\scripts\verify-showroom-admin-hide-three-tabs.mjs`
- PASS：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-admin-hide-three-tabs\frontend-feature-evidence.md`
- PASS：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260519-showroom-admin-hide-three-tabs --mode preview`
- PASS：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260519-showroom-admin-hide-three-tabs --mode apply`，已删除任务附属 `frontend-feature-evidence.md` 与一次性 Playwright 校验脚本。

## 写入边界

- `src/router/modules/showroom.ts`
- `scripts/showroom-admin-hide-three-tabs.test.mjs`
- `doc/tasks/20260519-showroom-admin-hide-three-tabs/scripts/*.mjs`
- `doc/tasks/20260519-showroom-admin-hide-three-tabs/**`

## 风险与约束

- 只隐藏前端入口，不静默删除功能路由，避免影响已有直接跳转与后续任务。
- 如本地前端入口不可访问，必须按 fail-fast 记录真实阻塞，不用伪造可视验证结果。
- 若菜单系统对 `hidden` 字段的处理与预期不一致，必须先暴露验证失败，再决定是否进一步调整。
