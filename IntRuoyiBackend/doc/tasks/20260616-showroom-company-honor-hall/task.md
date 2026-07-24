# 20260616 展厅公司荣誉展柜

## 任务目标

在展厅展柜管理中增加固定的 `公司荣誉展柜`，将当前测试租户已有奖项全部归入该展柜，并保证后续新增或导入发布的奖项也自动归入该展柜；奖项不得再放入其他展柜。

## 前置任务检查

- 后端最近任务 `20260616-scheduler-workbench-smoke-toggle` 状态为 `COMPLETED`。
- 展厅奖项前置任务 `20260613-showroom-awards-import-display` 状态为 `COMPLETED`，已具备奖项模型、混合展项和 Website 奖项详情发布能力。
- 前端最近任务 `20260616-route-use-source-route-detail-link` 已记录为 `BLOCKED`，阻塞原因为测试租户登录失败；本任务不得混入该任务改动。

## 经验门禁

- 命中 `docs/login-access.md`：本机后台验收默认 `http://localhost:8081`；写入型 E2E 使用 `测试租户/aoteman`；登录失败必须记录实际租户、账号、入口、失败位置和影响，不得静默切换租户、账号或环境。
- 命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：展柜管理仍保持蓝/中性、紧凑表格和操作台风格，不做营销式重设计。
- 未经当前任务授权，不访问测试服或正式服。

## BDD 场景

- BDD: 当前奖项归入公司荣誉展柜 -> Given 测试租户已有已发布奖项 / When 应用公司荣誉展柜规则 / Then 展柜管理出现 `公司荣誉展柜`，且所有奖项都以 `AWARD` 展项归入该展柜并具备完整画布布局。
- BDD: 后续奖项自动归入公司荣誉展柜 -> Given 用户新增或导入发布一个奖项 / When 奖项发布成功 / Then 系统自动确保 `公司荣誉展柜` 存在，并将该奖项加入该展柜。
- BDD: 奖项唯一归属公司荣誉展柜 -> Given 管理员维护非公司荣誉展柜 / When 保存包含 `AWARD` 的展项映射 / Then 后端明确失败，不能把奖项放入其他展柜。

## 里程碑

1. M1：创建任务文档、记录经验门禁与 RED/GREEN 证据。`DONE`
2. M2：RED：新增后端持久化测试和前端静态断言，复现缺少公司荣誉展柜自动归属与文案不准确。`DONE`
3. M3：GREEN：实现公司荣誉展柜持久化规则、历史数据迁移 SQL、前端展项文案调整。`DONE`
4. M4：REGRESSION：运行 showroom 目标 Maven 测试、前端静态测试、必要类型检查和本机后台只读/写入前置验证。`DONE`
5. M5：收尾：更新证据、运行 task-closeout-cleanup 预览，并只提交本任务改动。`DONE`

## 预期验证

- `mvn -pl yudao-module-showroom -Dtest=ShowroomPersistentContentServiceTest,ShowroomProductExcelImportExportIntegrationTest test` 通过。
- 本机数据库中 `tenant_id=122` 存在 `company_honor/公司荣誉展柜`，`showroom_hall_item` 中 46 个奖项全部归入该展柜且无奖项映射到其他展柜。
- 前端静态测试确认展柜维护入口和数量文案使用“展项”而不是“产品”。
- Playwright 若测试租户登录前置失败，记录 `BLOCKER`，不得切换账号或环境替代。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺少展柜、奖项或非法跨展柜保存奖项时明确失败或自动创建正式业务展柜，不返回伪成功。
- `是否从根因和长期维护角度解决`：是；把公司荣誉展柜作为奖项持久化归属规则，不做一次性手工界面操作。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：COMPLETED。
- 已完成：后端发布奖项自动创建/维护 `company_honor`，非荣誉展柜保存 `AWARD` 明确失败；新增历史数据迁移 SQL；本机测试租户 `tenant_id=122` 已迁移出 `公司荣誉展柜` 且 46 个已发布奖项全部归入该展柜。
- 验证结果：后端目标单测、内存契约测试、奖项发布集成测试、前端静态测试均通过；数据库只读核验显示荣誉展柜 46 个 `AWARD` 映射、非荣誉展柜奖项映射 0、布局完整且面积合计 1.000000。
- 验证缺口：Playwright 页面只读验证因 `测试租户/aoteman/admin123` 登录返回“账号密码不正确”被阻塞；已确认实际提交租户/账号/密码与登录基线一致，未切换账号或环境替代。
