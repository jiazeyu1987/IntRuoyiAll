# 任务：DCC 基础数据页支持新增删除修改与筛选查询

## 任务目标

将 `src/views/dcc/controlled-file/basic-data/index.vue` 从只读列表页升级为可维护的 DCC 基础数据管理页，并保证：

- 支持新增项目代码；
- 支持编辑已有项目代码；
- 支持删除未被受控文件引用的项目代码；
- 支持按筛选条件查询列表；
- 保持 `主编码` 列当前统一显示 `无`；
- 保持文控默认按数字升序展示；
- 不引入 mock、fallback、静默失败或仅前端假动作。

## 当前状态

status: completed

## Current Status

completed

## 前一任务检查

- 前端最近同页任务 `20260625-dcc-basic-data-main-code-doc-control-order` 已标记为 `completed`，允许继续本任务。
- 当前前端仓库存在其他未归属脏改动；本任务仅修改 DCC 基础数据页、定向静态测试、相关 API 与本任务文档，不覆盖其他改动。

## 经验门禁

- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：沿用 DCC 运营台密集列表样式，筛选区、工具栏、表格、行内操作和弹窗表单保持统一控制台视觉，不做无关改版。
- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`：本任务先做本机源码与定向静态/单元验证；在未记录 `experience-preflight` 前，不执行真实 E2E、服务器写入或其他高风险动作。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。接口不可用、校验失败、删除受引用数据等情况必须显式报错。
- `是否从根因和长期维护角度解决`：是。前端手工维护入口必须绑定真实后端 CRUD；删除约束由后端统一校验，避免前端绕过。
- `是否存在临时补丁或绕过`：否。`主编码` 当前固定显示 `无` 仍为用户明确展示合同，不作为兼容分支。

## BDD 场景

- `BDD: 用户可新增项目代码 -> Given 用户进入 DCC 基础数据页并拥有维护权限 When 点击新增并填写必填项 Then 列表出现新增的项目代码记录。`
- `BDD: 用户可编辑项目代码 -> Given 列表已有项目代码记录 When 用户打开编辑弹窗并修改字段 Then 列表与详情展示更新后的值。`
- `BDD: 未被引用的项目代码可删除 -> Given 某条项目代码未被受控文件引用 When 用户确认删除 Then 该记录从列表消失。`
- `BDD: 已被引用的项目代码禁止删除 -> Given 某条项目代码已被受控文件引用 When 用户尝试删除 Then 系统明确报错并保留该记录。`
- `BDD: 用户可按筛选条件查询 -> Given 列表存在不同项目名称/项目代码/类别/状态的数据 When 用户输入筛选条件查询 Then 只返回符合条件的记录。`
- `BDD: 主编码继续统一显示无 -> Given 用户查看主表行数据 When 渲染主编码列 Then 该列仍统一显示 无。`

## 里程碑

1. M1：创建任务文档并补齐门禁、BDD、验证目标。`DONE`
2. M2：先补前端静态合同 RED，锁定 CRUD 按钮、弹窗与筛选入口。`DONE`
3. M3：先补后端 RED 测试，锁定 CRUD、删除约束与查询合同。`DONE`
4. M4：实现后端 CRUD、权限种子与前端 API/页面交互。`DONE`
5. M5：运行定向验证并补齐证据、收尾。`DONE`

## 预期验证

- `node tests/e2e/dcc-project-code-basic-data-static.spec.js`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260625-dcc-basic-data-crud-query/frontend-feature-evidence.md`

## Cleanup Keep

- `doc/tasks/20260625-dcc-basic-data-crud-query/task.md`
- `doc/tasks/20260625-dcc-basic-data-crud-query/execution-log.md`
- `doc/tasks/20260625-dcc-basic-data-crud-query/frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/dcc-project-code-basic-data-static.spec.js`：PASS
