# 20260616 展厅公司荣誉展柜前端配套

## 任务目标

配合后端公司荣誉展柜规则，确保展柜管理页面将混合产品/奖项列表准确表达为“展项”，避免公司荣誉展柜只含奖项时仍显示“产品数量/维护产品”的误导文案。

## 前置任务检查

- 前端最近任务 `20260616-route-use-source-route-detail-link` 已记录为 `BLOCKED`，原因是测试租户登录失败；本任务不得混入该任务改动。
- 展厅奖项前置任务 `20260613-showroom-awards-import-display` 状态为 `COMPLETED`，当前展柜维护弹窗已支持产品和奖项混合选择。

## 经验门禁

- 命中 `docs/login-access.md`：本机后台验收默认 `http://localhost:8081`，测试租户登录失败必须阻塞记录，不得切换账号或环境替代。
- 命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：只做展柜管理表格/操作文案收敛，保持现有紧凑运营台风格。

## BDD 场景

- BDD: 展柜列表准确显示展项数量 -> Given 展柜包含产品或奖项 / When 用户查看展柜管理列表 / Then 数量列和维护入口使用“展项”，不再误写为产品。

## 里程碑

1. M1：记录任务文档和经验门禁。`DONE`
2. M2：RED：前端静态断言复现旧文案。`DONE`
3. M3：GREEN：更新展柜列表和相关静态断言。`DONE`
4. M4：REGRESSION：运行展厅前端静态测试和必要类型检查。`DONE`

## 预期验证

- `node scripts\showroom-admin-product-hall-operability.test.mjs` 通过。
- 如本机测试租户登录恢复，再用 Playwright 打开 `/showroom/hall` 确认 `公司荣誉展柜` 可见且数量列表达为展项。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；文案与实际 mixed item 数据模型一致。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：COMPLETED。
- 已完成：展柜列表数量列改为“展项数量”，展柜列表和工作台维护入口改为“维护展项”，静态测试同步为混合展项契约。
- 验证结果：`node scripts\showroom-admin-product-hall-operability.test.mjs` 与 `node scripts\showroom-admin-hall-list.test.mjs` 均通过。
- 验证缺口：Playwright 打开 `/showroom/hall` 的只读页面验证因 `测试租户/aoteman/admin123` 登录返回“账号密码不正确”被阻塞；未切换账号、租户或环境替代。
