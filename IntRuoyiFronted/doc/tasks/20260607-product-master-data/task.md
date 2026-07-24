# 任务：产品主数据前端

## 任务目标

新增 `基础数据 / 产品主数据` 前端页面，提供产品查询、新增编辑、启用停用、Excel 全量导入预览与确认、导出、引用情况；改造 DCC 上传页产品选择；改造展厅产品/展柜选择使用产品主数据；支持管理员在产品主数据页预览并确认将展厅内容映射到产品主数据。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260607-showroom-hall-canvas-website-preview/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务在独立 worktree 与分支 `codex/20260607-product-master-data` 中实施。

## BDD 场景

- BDD: 管理员维护产品主数据 -> Given 用户具有产品主数据权限 / When 进入 `基础数据 / 产品主数据` / Then 可查询、新增、编辑、启用停用、导入、导出产品。
- BDD: Excel 导入必须先预览差异 -> Given 用户上传产品主数据 Excel / When 点击导入 / Then 页面展示新增、更新、停用、失败明细，确认后才写入。
- BDD: DCC 上传选择产品主数据 -> Given 用户进入 DCC 受控上传页 / When 选择产品 / Then 产品编号来自启用且有 14 位 DCC 编号的主数据产品。
- BDD: 展厅选择产品主数据 -> Given 用户维护展厅产品或展柜产品 / When 搜索或选择产品 / Then 产品编码和名称来自产品主数据。
- BDD: 芋道源码管理员预览展厅映射 -> Given `芋道源码/admin` 进入产品主数据页 / When 点击展厅映射并生成预览 / Then 页面显示新增、更新、绑定和失败明细，失败数为 0 时才允许确认映射。

## Milestones

- [x] M1：建立任务文档、执行日志、前端证据文档。
- [x] M2：补充前端 RED 静态契约测试。
- [x] M3：新增 `src/api/mdm/product` 与 `src/views/mdm/product` 页面。
- [x] M4：改造 DCC 上传页产品选择。
- [x] M5：改造展厅产品相关页面读取产品主数据字段。
- [x] M6：新增产品主数据页展厅映射预览/确认入口。
- [x] M7：运行前端验证、记录证据、cleanup 预览并提交。

## Expected Verification

- `node scripts/mdm-product-master-contract.test.mjs`
- `pnpm --dir D:\ProjectPackage\Int\IntRuoyi\worktrees\20260607-product-master-data\yudao-ui-admin-vue3 ts:check`
- `pnpm --dir D:\ProjectPackage\Int\IntRuoyi\worktrees\20260607-product-master-data\yudao-ui-admin-vue3 exec eslint src/api/mdm src/views/mdm src/views/dcc/controlled-file/upload src/views/showroom-admin`
- Playwright 从 `http://localhost:8081` 登录测试租户验证真实路径。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。接口失败、权限缺失、导入失败、产品缺失均显式报错。
- `是否从根因和长期维护角度解决`：是。新增中立产品主数据入口，DCC 和展厅只引用统一主数据。
- `是否存在临时补丁或绕过`：否。不在 DCC 页面自建产品文本，不用展厅页面临时承载主数据。

## 当前状态

completed: 产品主数据前端已变基到最新 `int_main`，静态契约、类型检查、scoped eslint 与 diff 检查均通过；可 fast-forward 合并并删除独立 worktree。
