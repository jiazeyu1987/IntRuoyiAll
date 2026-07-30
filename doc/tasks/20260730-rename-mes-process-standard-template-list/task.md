# MES 工序重命名为标准模板列表

## Task Goal

将用户可见的同级菜单入口和页面标题从 `MES工序` 调整为 `标准模板列表`，保留现有路由、权限、接口和资源池读模型，不改变表格数据列的业务含义。

## Milestones

1. 定位动态菜单 SQL、页面标题、静态合同和真实路径等待文本。
2. 先更新静态合同并跑出 RED，证明旧文案仍存在。
3. 修改菜单 SQL 与前端页面标题文案。
4. 运行定向静态合同、类型检查、迁移门禁和本机登录态核验。

## Expected Verification

- 静态合同：同级菜单 `system_menu.id=5718` 名称为 `标准模板列表`，查询权限 `5719` 名称为 `标准模板列表查询`。
- 静态合同：`/mes/pro/mes-process` 页面标题和 doc-alert 使用 `标准模板列表`。
- 静态合同：业务表格列仍保留 `MES工序名称`、`MES工序编码` 等数据口径，不把数据字段一起误改名。
- 迁移门禁：目标菜单 SQL 与依赖链一起通过 release migration policy gate。
- 类型检查：`pnpm ts:check` 通过。

## Current Status

ready_for_closeout

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，按动态菜单正式来源和页面标题同步重命名。
- 是否存在临时补丁或绕过：否。

## Applicable Gates

- 动态菜单页签重命名门禁：必须同步 `system_menu.name` 的正式 SQL、页面标题、静态合同和真实路径核验。
- 中文菜单名称 ASCII 安全迁移门禁：中文菜单名必须使用 UTF-8 HEX 写入并核对 HEX。
- clear-frontend-copy：用户可见标题使用规范简体中文；保留 `MES工序名称` 作为数据列业务术语。

## Runtime Note

- 本机数据库 `system_menu.id=5718/5719` 已更新并校验 HEX。
- 真实页面复验需要本机后端 `48081`；当前项目标准后端重启脚本被本任务外未跟踪测试草稿 `MesProMesProcessCatalogSchemaTest.java` 的缺失包引用阻塞，本次未扩大范围修该并行文件。
