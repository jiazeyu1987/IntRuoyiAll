# 展厅产品管理页签 E2E 验证

## 任务目标

使用真实本机前后端路径验证修复后的展厅产品管理页签可打开，不再出现 `showroom_product_revision_attachment` 缺表导致的“加载展柜数据失败”。

## BDD 场景

- BDD: 产品管理页签加载成功 -> Given 本机后端已创建 `showroom_product_revision_attachment` 表且本机前端运行在 `http://localhost:8081` / When 使用测试租户 `aoteman` 登录并进入展厅产品管理页签 / Then 页面应显示产品管理内容且不出现缺表错误。

## 里程碑

- [x] M1：创建 E2E 任务文档。
- [x] M2：确认本机前后端和登录凭据可用。
- [x] M3：用 Playwright 走真实登录和产品管理页面路径。
- [x] M4：记录截图/日志证据与最终结果。

## 预期验证

- `http://127.0.0.1:48081/actuator/health` 返回 200。
- Playwright 登录测试租户 `测试租户 / aoteman / admin123`。
- 产品管理页签无 `showroom_product_revision_attachment`、`加载展柜数据失败`、`SQLSyntaxErrorException` 错误。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务只做真实 E2E 验证，不用 mock、不绕过登录、不屏蔽页面错误。
- `是否从根因和长期维护角度解决`：否。本任务是独立验证；发现新的阻塞后记录失败证据，不在验证任务中临时修补。
- `是否存在临时补丁或绕过`：否。未修改生产代码，未改测试数据或受保护文件配置。

## 当前状态

blocked

## 验证结果

- PASS：后端健康检查 `http://127.0.0.1:48081/actuator/health` 返回 200。
- PASS：前端入口 `http://localhost:8081` 返回 200。
- PASS：Playwright 真实登录测试租户并进入 `http://localhost:8081/showroom/product`。
- PASS：产品列表渲染 20 行，分页显示 `共 179 条，共 9 页`，页面文本未出现 `showroom_product_revision_attachment`、`加载展柜数据失败`、`SQLSyntaxErrorException` 或 `doesn't exist`。
- BLOCKED：点击首行“基础”后未打开弹框；浏览器控制台抛出 `展柜公司信息缺失，无法设置产品归属`，调用栈位于 `showroom-admin/index.vue` 的 `openProductEdit`。

## 证据文件

- Playwright 脚本：`doc/tasks/20260606-showroom-product-management-e2e/showroom-product-management-e2e.run-code.js`
- 页面截图：`doc/tasks/20260606-showroom-product-management-e2e/showroom-product-management-current.png`
- 独立验证报告：`doc/tasks/20260606-showroom-product-management-e2e/verification-report.md`
