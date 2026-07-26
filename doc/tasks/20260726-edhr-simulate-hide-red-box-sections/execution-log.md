# Execution Log

## 2026-07-26

- User intent: 用户要求截图红框内的内容不显示。
- Skills: `frontend-feature-delivery`、`bug-regression-fix-loop`。
- Trigger docs read: `docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/task-closeout-rules.md`、`docs/powershell-memory.md`、`docs/powershell-encoding.md`。
- Experience gate: `docs/experience-index.md` 已读取；命中前端聚焦静态契约与静态合同同步门禁。
- Git preflight: 根仓库为 `E:\IntRuoyi`，当前分支 `int_main`，remote 为 `origin`。
- Existing dirty state: `doc/tasks/20260726-hide-word-import-form-type/` 为本任务开始前已存在的未跟踪目录，按项目脏工作区基线规则单独保存，不纳入本任务实现。
- BASELINE: `bc4ab705` (`chore: preserve pre-task dirty workspace baseline`) -> 保存本任务开始前及并行出现的 Word 导入表单类型任务改动；本任务目录未进入该提交。
- BDD: 红框区域不显示 -> Given 用户进入 eDHR 模板模拟填写页 / When 页面成功加载模板 / Then 不显示工序与模板标题、模板摘要、左侧填写说明和规则图例，同时保留返回按钮、右侧表单显示和左右模板。
- RED: `node tests/e2e/edhr-batch-template-simulate-red-box-hidden-static.spec.js` -> FAIL，首个断言确认工序标题仍在模拟填写页模板中渲染。
- CHANGE: 模拟填写页移除工序/模板标题、模板摘要和左侧辅助说明；共享可编辑模板组件新增默认开启的 `showRuleLegend` 属性，模拟填写页显式关闭。
- CHANGE: 更新既有模拟填写静态合同，不再要求已隐藏的左侧辅助标题可见。
- GREEN: `node tests/e2e/edhr-batch-template-simulate-red-box-hidden-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-template-simulate-return-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- BLOCKER: `node tests/e2e/edhr-batch-template-simulate-static.spec.js` -> FAIL，首个失败为既有 `Number(route.query.id)` 断言与当前已存在的 `parsePositiveRouteQueryId` 实现不一致，非本次红框改动。
- BLOCKER: `pnpm build:local` -> FAIL，Vite 配置加载时缺少 `@babel/helper-validator-identifier` 实体文件；锁文件声明存在但 `node_modules` 目录为空，未执行临时依赖补丁。
