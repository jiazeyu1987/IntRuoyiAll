# 20260805 生产组长人员统一状态列表

## Task Goal

调整生产组长“人员管理”：

- 删除“未禁用 / 已禁用”状态分组筛选。
- 已禁用与未禁用人员显示在同一个列表中。
- 已禁用人员的显示名使用红色文字。

不改变新增人员、修改显示名、启用/禁用、重置签名密码、分页和后端接口契约。

## Milestones

- [x] 创建任务记录并确认现有页面和查询逻辑
- [x] 编写聚焦静态合同并取得 RED
- [x] 实现统一列表与禁用姓名红色显示
- [x] 运行 GREEN、相邻回归和 TypeScript 检查
- [x] 完成 evidence 校验与 cleanup
- [x] 完成最终收尾提交和推送

## Expected Verification

- `workdir=IntRuoyiFronted; node tests\e2e\production-personnel-unified-status-list-static.spec.cjs`
- `workdir=IntRuoyiFronted; node tests\e2e\production-leader-remove-header-content-static.spec.js`
- `workdir=IntRuoyiFronted; node tests\e2e\production-personnel-add-dialog-static.spec.cjs`
- `workdir=IntRuoyiFronted; node tests\e2e\production-personnel-management-real.e2e.js`（仅在真实账号、运行态和测试数据前置齐备时）
- `workdir=IntRuoyiFronted; pnpm ts:check`
- `workdir=E:\IntRuoyi; git diff --check -- IntRuoyiFronted\src\views\mes\pro\processpool\TeamLeaderWorkbenchPage.vue IntRuoyiFronted\tests\e2e\production-personnel-unified-status-list-static.spec.cjs doc\tasks\20260805-production-leader-personnel-unified-status-list`

## Current Status

completed

- 已定位状态分组来源：前端默认 `productionPersonnelQuery.enabled = true`，列表请求透传 `enabled`，并渲染“未禁用 / 已禁用”选择器。
- 聚焦静态合同、相邻合同、真实 E2E 语法检查和 TypeScript 检查已通过。
- frontend feature evidence validator 与 validator self-test 已通过。
- 真实写入型 E2E 的 6 个必需环境变量均缺失，未执行真实页面写入链路，未使用默认账号、API-only 或 mock 替代。
- 并发基线提交 `3db8a7030` 已将本任务核心 Vue 改动、聚焦合同和初始任务文档与其它任务改动混合提交；当前源码行为已复验通过，后续提交只处理仍未提交的本任务真实 E2E 合同与收尾记录。
- 本任务独立后续提交 `d068655c2` 已提交真实 E2E 新行为合同与验证记录。
- task-closeout-cleanup preview/apply 已通过，仅删除临时 `frontend-feature-evidence.md`，保留 `task.md`、`execution-log.md` 和 `verification-report.md`。
- cleanup 记录提交 `59db160a1` 已推送到 `origin/int_main`；最终验证结果为 PASS（真实写入型 E2E 因正式前置缺失未执行并已记录）。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，移除前端状态过滤入口和查询参数，让正式列表数据源一次返回全部关联人员。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- `docs/frontend-development.md#前端静态契约隔离门禁`：新增任务专用最小静态合同，独立证明统一列表和禁用姓名样式。
- `docs/frontend-development.md#前端截图样式块静态契约门禁`：合同锁定显示名列和人员查询逻辑，不使用跨整文件的宽泛正则。
- `docs/powershell-memory.md#脏工作区基线门禁`：基线提交不得混入本任务文件，提交前复核 staged 清单。
- `docs/powershell-memory.md#共享分支并发基线提交门禁`：每次提交前复查最近提交、任务文件 diff 和并行改动归属。
- `docs/task-closeout-rules.md#技能证据文件清理前归档门禁`：validator 通过后把结论归档到保留文档，再执行 cleanup。
