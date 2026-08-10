# 执行日志

## 用户意图

- 用户要求在截图红框位置提供“测试账套 / 正式账套”连接切换。
- 切换选择本身不能立即生效，必须点击保存后才改变实际 ERP 连接。
- 页面必须明确显示当前正在连接测试账套还是正式账套。

## 现有证据

- `ProfileErpTableAutoSyncSetting.vue` 已使用正式 `infra/job` 与 `ErpKingdeeSyncApi.runIncrementalSyncJob` 链路。
- `ErpKingdeeConfigServiceImpl.getEffectiveProperties()` 是商品、库存、采购、销售、生产工单等同步服务共用的运行连接来源。
- 既有测试账套配置位于 `yudao.erp.kingdee.config`；正式账套登录和 `PRD_MO` 只读查询已在前序任务验证成功。
- Profile“配置”页签受 `mes:pro-batch-record-execution:golden-finger` 权限控制，新连接 API 沿用该入口权限边界。

## BDD

- BDD: 页面显示后端已保存的当前账套 -> Given 后端当前连接为测试账套或正式账套 When 用户打开 ERP 表格自动同步页签 Then 页面以明确标签显示同一当前连接，并提供两个固定选项。
- BDD: 选择后必须保存才生效 -> Given 当前连接为测试账套 When 用户只选择正式账套但未保存 Then 当前连接标签仍显示测试账套且后端有效连接不变；When 用户点击保存并成功 Then 标签和后端有效连接都变为正式账套。
- BDD: 目标连接缺失时失败 -> Given 正式账套隐藏配置缺失或无效 When 用户保存正式账套 Then 后端返回明确错误且当前连接保持原值，前端显示错误。
- BDD: 保存后所有同步使用同一连接 -> Given 当前连接已保存为正式账套 When 任一 ERP 同步服务调用 `getEffectiveProperties()` Then 返回正式账套连接字段并保留既有同步参数。
- BDD: 页面回切测试账套 -> Given 当前连接为正式账套 When 用户选择测试账套并保存 Then 后端恢复使用既有测试账套连接，刷新后仍显示测试账套。

## TDD 证据

- RED: `node tests/e2e/profile-erp-table-auto-sync-static.spec.js` -> FAIL，缺少 `ErpKingdeeConnectionType` 等账套切换 API 与页面契约，符合预期。
- RED: `mvn -pl yudao-module-erp -am "-Dtest=ErpKingdeeConfigServiceImplTest,ErpKingdeeConfigControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少账套切换 VO 与枚举，符合预期。
- GREEN: `mvn "-Dtest=ErpKingdeeConfigServiceImplTest,ErpKingdeeConfigControllerTest" test`（`yudao-module-erp`）-> PASS，17 个测试全部通过。
- GREEN: `node tests/e2e/profile-erp-table-auto-sync-static.spec.js` -> PASS。
- GREEN: 定向 ESLint 和 Prettier 检查 -> PASS。
- BLOCKER: `pnpm ts:check` -> FAIL，失败位于本任务未修改的 `FrontlineFixedTemplatePanel.vue:2772`，其 `actualEmployeeId` 不属于 `FrontlinePqcInspectionSubmitReqVO`；本任务目标文件的定向 ESLint 已通过。
- REGRESSION：`mvn "-Dtest=ErpKingdeeConfigServiceImplTest,ErpKingdeeConfigControllerTest" test` -> PASS，17 个测试通过；前端静态合同、定向 ESLint、Prettier 和任务文件 `git diff --check` 均通过。

## 数据变更计划

- 新增隐藏配置 `yudao.erp.kingdee.connection.production`，只在本机运行数据库保存正式账套连接字段。
- 新增隐藏配置 `yudao.erp.kingdee.connection.active`，值仅为 `TEST` 或 `PRODUCTION`。
- 初始值和 E2E 最终恢复值均为 `TEST`；回滚时删除这两个任务新增配置即可恢复功能前行为。

## 当前状态

- M1：完成。
- M2：完成。
- M3：完成。
- M4：完成。
- M5：完成。本机新增两条隐藏配置；正式账套配置 JSON 有效、必需连接字段完整，当前连接最终为 `TEST`。
- M6：完成。`task-closeout-cleanup` preview 无 blocked/warning，apply 仅删除本任务中间类文件、一次性浏览器脚本、验证截图和临时证据文件，保留三份核心任务记录及当前运行包。

## 运行与真实页面证据

- 运行后端使用本任务构建类更新现有可执行包中的 ERP 模块，外层运行包 SHA-256 为 `0A4C30B257A8C9ACEA2CF171A8F60234702CEDD07B077532133B689C2EB43868`；`48081/actuator/health` 返回 `UP`。
- Playwright 桌面真实路径：初始显示“当前连接测试账套”；仅选择正式账套时仍显示测试账套和“待保存”；点击“保存连接”后显示正式账套；刷新后仍为正式账套。
- Playwright 回切真实路径：初始显示正式账套；仅选择测试账套时仍显示正式账套和“待保存”；保存后显示测试账套；刷新后仍为测试账套。
- 390x844 窄屏截图验证：当前连接、二选一控件和保存按钮纵向排列，无相互遮挡或文字溢出。
- 浏览器控制台在上述最新真实路径中无错误，仅有应用欢迎日志。
- 数据库最终核对：当前连接值为 `TEST`；正式账套配置隐藏、未删除、JSON 有效，账套、用户名和密码字段均非空。验证记录只保留字段存在性和密码长度，不记录任何凭据值。
- 前端和后端服务均保持运行，入口为 `http://localhost:8081/user/profile`。

## 收尾证据

- 清理后任务目录仅保留 `task.md`、`execution-log.md` 和 `verification-report.md`。
- 清理后后端健康状态仍为 `UP`，前端 `8081` 仍在监听，运行包仍存在。
- 清理后再次核对数据库当前连接为 `TEST`。
- 当前目录是主工作区 `int_main`，没有额外 worktree 合并或删除操作；未执行 Git 暂存、提交或推送。
- `project-experience-consolidation` 将多账套切换的持久化、脱敏、统一有效配置入口和真实页面回切规则合并到既有 `docs/login-access.md`，并更新 `docs/experience-index.md` 关键词路由；未新建长期经验文档。
- 最终复核遇到并行任务替换 `48081` 主运行包时的短暂无监听；未抢占端口或干预对方进程。新进程启动后健康状态为 `UP`，只读检查其内嵌 ERP 模块仍包含本任务的 Controller、Service、三个 VO 和连接枚举类，数据库当前连接仍为 `TEST`；一次性审计目录随后经 cleanup preview/apply 删除。
