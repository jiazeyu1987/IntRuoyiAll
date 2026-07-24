# 任务：工艺路线关联产品行复制

## 任务目标

在 MES 工艺路线编辑页的“关联产品”Tab 中，为单行关联产品增加“复制”能力：用户选择目标产品后，系统复制源关联产品的生产数量、生产用时、时间单位、备注，并同步复制源产品在该路线各工序下的 BOM 物料配置。

## 经验门禁

- PowerShell / Windows shell / 中文编码陷阱：已读取 `docs/powershell-memory.md`；所有中文文件读写使用 UTF-8 路径，不使用默认 `Get-Content` / `Set-Content`。
- 项目经验索引：已读取 `docs/experience-index.md`；本次不执行真实 E2E、服务器、发布、备份、恢复、数据库写入或 worktree 合并等高风险动作。
- 前端页面 / 表格 / 样式：本次只在现有操作列增加“复制”按钮和现有弹窗风格，不做视觉重构。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；在后端提供正式复制接口，前端只调用接口，不在页面拼接伪数据。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: route_product_copy_clones_product_and_bom -> Given 用户在工艺路线编辑页打开关联产品 Tab / When 点击某行复制并选择新产品 / Then 新产品关联被创建，生产参数继承源行，源产品 BOM 配置同步复制到目标产品。

BDD: route_product_copy_rejects_invalid_target -> Given 用户复制关联产品 / When 未选择目标产品或目标产品已被其它路线关联 / Then 后端直接返回校验错误，前端不关闭弹窗且不伪造成功。

## 里程碑

- [x] M1：创建任务文档并记录 BDD / RED 证据。
- [x] M2：新增后端复制接口、服务方法和失败优先测试。
- [x] M3：新增前端复制按钮、复制弹窗和 API 调用。
- [x] M4：运行后端 Maven 目标测试、前端静态验证和 TypeScript 检查。
- [x] M5：更新执行日志、收尾预览并按验证结果提交本次改动。

## 预期验证

- `mvn -pl yudao-module-mes "-Dtest=MesProRouteProductServiceImplTest,MesProRouteVersionAndCopyTest" test`
- `node tests/e2e/mes-pro-route-product-copy-static.spec.js`
- `npm run ts:check`

## 当前状态

`COMPLETED`：后端复制接口、服务、测试和前端复制入口均已完成；目标验证通过。提交仅包含本次任务范围，未纳入其它既有工作区改动。
