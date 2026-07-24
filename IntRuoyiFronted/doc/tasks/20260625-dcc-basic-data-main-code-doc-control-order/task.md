# 任务：DCC 基础数据主表增加主编码并调整列与排序合同

## 任务目标

将 `src/views/dcc/controlled-file/basic-data/index.vue` 的 DCC 基础数据主列表调整为：

- 删除主表 `委托生产`、`项目工程师`、`状态` 三列；
- 新增主表 `主编码` 列，当前所有行固定显示 `无`；
- 保持详情抽屉、导入预览、筛选区、关联文档入口和现有接口合同不变；
- 列表默认排序改为由后端提供 `文控` 数字从小到大顺序。

## 当前状态

status: completed

## Current Status

completed

## 前一任务检查

- 前端最近同页任务 `20260625-dcc-basic-data-hide-storage-priority-columns` 已标记为 `completed`，允许继续本任务。
- 当前前端仓库存在其他未归属脏改动；本任务仅修改 DCC 基础数据主表、定向静态测试与本任务文档，不覆盖其他改动。

## 经验门禁

- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：保持现有 DCC 密集表格布局，只做主表列裁剪与补充一列占位文本，不重排页面骨架。
- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`：本任务仅做本机源码与定向静态验证，不执行真实 E2E、服务器写入或其他高风险动作，因此不触发 `experience-preflight` 门禁。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。`主编码` 固定显示 `无` 是用户明确指定的暂时展示要求，不属于兼容分支或降级路径。
- `是否从根因和长期维护角度解决`：是。主表展示需求直接收敛到列定义；默认排序交由后端统一处理，避免分页后前端排序失真。
- `是否存在临时补丁或绕过`：否。`主编码` 暂时固定为 `无` 属于当前正式展示合同。

## BDD 场景

- `BDD: 主表隐藏三列并新增主编码占位 -> Given 用户进入 DCC 基础数据页 When 查看主列表表头 Then 主表显示 文控/主编码/项目名称/项目代码/类别/项目组负责人/更新时间/关联文档，且不再显示 委托生产/项目工程师/状态。`
- `BDD: 主编码当前统一显示无 -> Given 用户查看任意 DCC 基础数据主表行 When 渲染主编码列 Then 该列统一显示 无。`
- `BDD: 详情和导入预览保持原合同 -> Given 用户打开详情抽屉或导入预览 When 查看字段 Then 存放位置/优先级/委托生产/项目工程师/状态等既有详情或导入合同保持不变。`

## 里程碑

1. M1：创建任务文档并补齐前置任务检查、经验门禁与 BDD。`DONE`
2. M2：先调整静态合同测试，锁定主表列收敛与主编码占位的 RED 失败。`DONE`
3. M3：最小修改基础数据主表列定义，新增 `主编码=无`，删除三列。`DONE`
4. M4：运行定向静态验证、补齐前端证据与收尾。`DONE`

## 预期验证

- `node tests/e2e/dcc-project-code-basic-data-static.spec.js`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260625-dcc-basic-data-main-code-doc-control-order/frontend-feature-evidence.md`

## Cleanup Keep

- `doc/tasks/20260625-dcc-basic-data-main-code-doc-control-order/task.md`
- `doc/tasks/20260625-dcc-basic-data-main-code-doc-control-order/execution-log.md`
- `doc/tasks/20260625-dcc-basic-data-main-code-doc-control-order/frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/dcc-project-code-basic-data-static.spec.js`：PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260625-dcc-basic-data-main-code-doc-control-order/frontend-feature-evidence.md`：待执行