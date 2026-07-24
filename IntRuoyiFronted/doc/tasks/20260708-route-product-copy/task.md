# 任务：工艺路线关联产品行复制

## 任务目标

在 MES 工艺路线编辑页的“关联产品”Tab 中增加单行复制入口，复用现有关联产品表单字段与 `MdItemSelect`，通过后端正式复制接口创建目标产品关联并同步 BOM 配置。

## 经验门禁

- PowerShell / Windows shell / 中文编码陷阱：已读取 `docs/powershell-memory.md`；中文文件读写使用 UTF-8 路径。
- 前端页面 / 表格 / 样式：沿用现有 `RouteProductList.vue` 表格、操作列、Dialog、Element Plus 表单风格，不做无关重构。
- 登录 / E2E：本轮计划先做静态验证和类型检查；如后续执行真实 E2E，必须先读取登录文档并跑登录 preflight。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；复制由后端接口完成，前端不复制本地假数据。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: route_product_copy_clones_product_and_bom -> Given 用户在工艺路线编辑页打开关联产品 Tab / When 点击某行复制并选择新产品 / Then 新产品关联被创建，生产参数继承源行，源产品 BOM 配置同步复制到目标产品。

BDD: route_product_copy_rejects_invalid_target -> Given 用户复制关联产品 / When 未选择目标产品或目标产品已被其它路线关联 / Then 后端直接返回校验错误，前端不关闭弹窗且不伪造成功。

## 里程碑

- [x] M1：创建任务文档并记录 BDD / RED 证据。
- [x] M2：接入 `copyRouteProduct` 前端 API。
- [x] M3：在关联产品行增加“复制”按钮和复制弹窗。
- [x] M4：增加静态验证脚本并运行 TypeScript 检查。
- [x] M5：更新执行日志、收尾预览并按验证结果提交本次改动。

## 预期验证

- `node tests/e2e/mes-pro-route-product-copy-static.spec.js`
- `npm run ts:check`

## 当前状态

`COMPLETED`：前端 API、关联产品行复制按钮、复制弹窗和静态验证均已完成；`ts:check` 通过。提交仅包含本次任务范围，未纳入其它既有工作区改动。
