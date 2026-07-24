# 任务：合并并删除展厅路由集成旧测试

## 任务目标

将 `scripts/showroom-route-integration.test.mjs` 中仍有效的展厅后台路由收口断言合并到 `scripts/showroom-admin-frontend.test.mjs`，删除包含过期断言的旧测试文件，避免后续定向展厅测试被无关历史实现细节阻塞。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。本任务只调整静态测试组织，不改变运行时代码。
- 是否从根因和长期维护角度解决：是。保留路由收口行为断言，删除已不再代表当前设计的延迟加载实现细节断言。
- 是否存在临时补丁或绕过：否。不跳过测试，不添加条件分支，只删除冗余旧测试文件。

## BDD 场景

- BDD: 展厅路由收口断言仍被覆盖 -> Given 展厅后台历史与讲解路由必须由统一后台壳承接 / When 运行展厅后台静态测试 / Then 测试仍验证 `history`、`narration-workbench` 路由使用 `showroomAdminView` 并渲染对应工作台。
- BDD: 过期旧测试文件不再阻塞 -> Given `showroom-route-integration.test.mjs` 中存在过期产品加载策略断言 / When 合并有效断言后删除该文件 / Then 展厅后台定向静态测试通过且不再引用旧文件。

## 里程碑

1. 确认前置任务已完成并建立本任务文档。
2. 记录当前旧测试文件阻塞 RED。
3. 合并有效路由收口断言到现有展厅后台静态测试。
4. 删除 `scripts/showroom-route-integration.test.mjs` 并验证无运行引用。
5. 运行定向测试、ESLint、收尾清理预览并提交。

## 预期验证

- `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-route-integration.test.mjs` -> RED before cleanup.
- `node --test scripts/showroom-admin-frontend.test.mjs` -> GREEN after cleanup.
- `rg -n "showroom-route-integration\\.test\\.mjs|showroom-route-integration" scripts package.json` -> no active script references.
- `pnpm exec eslint scripts\showroom-admin-frontend.test.mjs`

## 当前状态

completed

## 最终验证结果

- RED: `node --test scripts\showroom-admin-frontend.test.mjs scripts\showroom-route-integration.test.mjs` -> FAIL，旧文件中产品加载策略断言仍绑定过期实现细节。
- GREEN: `node --test scripts\showroom-admin-frontend.test.mjs` -> PASS，23 tests。
- GREEN: `rg -n "showroom-route-integration\.test\.mjs|showroom-route-integration" scripts package.json` -> 未发现活动脚本引用。
- GREEN: `pnpm exec eslint scripts\showroom-admin-frontend.test.mjs` -> PASS。
- CLOSEOUT PREVIEW: task-closeout-cleanup -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。

## 剩余阻塞

- 无。

## 前置任务检查

- 最近前端任务：`doc/tasks/20260611-showroom-tab-permission-gate/task.md`。
- 检查结果：已完成，可开始本任务。

## 任务边界

- 只修改展厅后台静态测试、删除过期旧测试文件、更新本任务记录。
- 不修改展厅运行时代码。
