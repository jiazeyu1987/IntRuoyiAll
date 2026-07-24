# 任务：电子批记录主从三栏布局

## 任务目标

- 将 `电子批记录列表` 页面改为主从三栏布局：左侧批记录名称，中间报表名称，右侧显示所选报表的表单模板。
- 复用现有报表接口与操作能力，不新增后端接口。
- 保留文件导入新增、单报表删除、报表编辑、重命名、签名位和单元格规则维护能力。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-role-management-toolbar-layout\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成，本任务仅修改电子批记录页面及对应静态测试、任务文档，不回退当前工作区已有 eDHR 模板预览相关改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 前端页面必须沿用 IntPP 运维台样式：白底、轻边框、紧凑表格、明确工具栏、稳定尺寸，不做营销式视觉重构。
  - 本次默认只做本机前端代码与静态验证；如进入真实 Playwright E2E 或登录后验证，必须先运行官方 `login-preflight.mjs` 最小登录路径并在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS`。
  - 写入型 E2E 默认只使用本机测试租户 `测试租户/aoteman`；最终只读复验才使用 `芋道源码/admin`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少模板布局、规则或签名位接口失败时在右侧预览区显示明确错误，不静默切换数据源或展示默认成功。
- `是否从根因和长期维护角度解决`：是。以批记录名称、报表列表、模板预览三层状态建模，复用现有 API 和共享模板说明组件。
- `是否存在临时补丁或绕过`：否。不新增占位模板、不新增 mock 数据、不增加后端临时接口。

## BDD 场景

- `BDD: 选择批记录名称显示对应报表 -> Given 存在多个批记录名称 / When 用户选择左侧某个批记录名称 / Then 中间报表列表只请求并显示该批记录名称下的报表。`
- `BDD: 选择报表显示表单模板 -> Given 中间列表存在报表 / When 用户选择一个报表名称 / Then 右侧并行加载单元格规则与签名位，并显示对应表单模板说明。`
- `BDD: 文件导入后选中新批记录 -> Given 用户通过文件导入新增批记录 / When 导入成功 / Then 左侧批记录名称刷新并选中新批记录，中间显示新增报表。`
- `BDD: 删除报表后刷新主从状态 -> Given 当前选中报表 / When 用户删除该报表成功 / Then 中间列表刷新，右侧预览清空或切换到剩余第一张报表。`

## 里程碑

1. M1：创建任务文档、前端证据文档和 RED 静态测试。`COMPLETED`
2. M2：实现三栏主从布局与模板预览状态。`COMPLETED`
3. M3：运行 GREEN、回归和类型检查。`COMPLETED`
4. M4：回写证据、收尾预览并按验证结果提交。`COMPLETED`

## 预期验证

- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js`
- `node scripts/electronic-batch-record-jimu-list.test.mjs`
- `node scripts/electronic-batch-record-word-import.test.mjs`
- `node scripts/electronic-batch-record-report-page.test.mjs`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260626-electronic-batch-record-master-detail-layout/frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS
- `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS
- `node scripts/electronic-batch-record-word-import.test.mjs` -> PASS
- `node scripts/electronic-batch-record-report-page.test.mjs` -> PASS
- `pnpm ts:check` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260626-electronic-batch-record-master-detail-layout/frontend-feature-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-electronic-batch-record-master-detail-layout --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` -> PASS

## Cleanup Keep

- `doc/tasks/20260626-electronic-batch-record-master-detail-layout/frontend-feature-evidence.md`
