# 任务：DCC 基础数据主表隐藏存放位置和优先级

## 任务目标

将 `src/views/dcc/controlled-file/basic-data/index.vue` 的 DCC 基础数据主列表精简为不再显示 `存放位置` 与 `优先级` 两列；保持筛选区、详情抽屉、导入预览、关联文档入口和后端接口合同不变。

## 当前状态

status: completed

## Current Status

completed

## 前一任务检查

- 前端最近任务 `20260625-showroom-base-workbook-import` 已标记为 `blocked`，允许继续本任务。
- 当前前端仓库存在其他未归属脏改动；本任务只修改 DCC 基础数据页主表、定向静态测试与本任务文档，不覆盖其他改动。

## 经验门禁

- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：保持现有 DCC 密集操作台表格布局，只做主表列裁剪，不新增装饰性结构或重排页面骨架。
- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`：本任务仅做本机源码与静态验证，不执行真实 E2E、服务器写入或其他高风险动作，因此不触发 `experience-preflight` 门禁。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仅删除正式展示列，不增加兼容分支或静默替代显示。
- `是否从根因和长期维护角度解决`：是。直接收敛主表列定义，避免主列表持续展示用户明确不需要的字段。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 主表隐藏存放位置和优先级 -> Given 用户进入 DCC 基础数据页 When 查看主列表表头 Then 不再显示 存放位置 与 优先级 两列。`
- `BDD: 详情仍保留完整字段 -> Given 用户从主列表进入某条 DCC 基础数据详情 When 查看详情抽屉 Then 存放位置 与 优先级 仍作为条目详情字段可见。`
- `BDD: 导入预览仍保留完整字段 -> Given 用户打开 DCC 基础数据导入预览 When 查看预览表头 Then 存放位置 与 优先级 仍保留在导入预览中，不改变导入合同。`

## 里程碑

1. M1：创建任务文档并补齐前置任务检查、经验门禁与 BDD。`DONE`
2. M2：先调整静态合同测试，锁定主表隐藏两列的 RED 失败。`DONE`
3. M3：最小修改基础数据主表列定义，仅删除主列表两列。`DONE`
4. M4：运行定向静态验证、补齐前端证据与收尾。`DONE`

## 预期验证

- `node tests/e2e/dcc-project-code-basic-data-static.spec.js`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260625-dcc-basic-data-hide-storage-priority-columns/frontend-feature-evidence.md`

## Cleanup Keep

- `doc/tasks/20260625-dcc-basic-data-hide-storage-priority-columns/task.md`
- `doc/tasks/20260625-dcc-basic-data-hide-storage-priority-columns/execution-log.md`
- `doc/tasks/20260625-dcc-basic-data-hide-storage-priority-columns/frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/dcc-project-code-basic-data-static.spec.js`：PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260625-dcc-basic-data-hide-storage-priority-columns/frontend-feature-evidence.md`：PASS
