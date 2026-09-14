# Execution Log

## User Intent

- 选择生产订单后展示订单生产的产品名称和产品数量。
- 缩小 PQC 顶部信息栏整体字体，使订单号、产品、数量、工序和员工信息完整可见。
- 数量使用正式生产工单 `quantity`，不使用已生产、已排产或抽检数量。

## BDD Scenarios

BDD: 选择活跃订单后展示正式产品摘要 -> Given 活跃订单关联的生产工单具有订单号、产品名称和正数生产数量，When 一线 PQC 选择该订单，Then 顶部订单摘要同步完整展示订单号、产品名称和去除无意义小数尾零的产品数量。

BDD: 切换订单同步更新摘要 -> Given PQC 活跃订单集合包含两条正式订单，When 用户从订单选择器切换订单，Then 订单号、产品名称和产品数量从同一条选中订单原子更新，不残留上一订单信息。

BDD: 顶部信息在目标视口完整可见 -> Given 已选择包含较长订单号和产品名称的活跃订单，When 页面以 1920x1080、1440x900 或 PQC 全屏显示，Then 顶部文字允许换行且无省略号、裁切或相互重叠，主检验区字号保持不变。

BDD: 正式订单摘要数据不完整时失败 -> Given 活跃订单关联工单缺少产品名称或生产数量不为正数，When 后端组装 PQC 活跃订单响应，Then 接口返回明确上下文错误且不返回默认产品或默认数量。

## Command Intent

- 先新增聚焦测试并运行 RED，确认当前代码缺少数量契约、校验和订单摘要布局。
- 再实现最小正式后端及前端改动，运行相同命令取得 GREEN。
- 最后运行聚焦回归、TypeScript 和真实 Playwright 只读验收。

## TDD Evidence

- RED: `node tests/e2e/mes-frontline-pqc-order-product-summary-static.spec.cjs` -> FAIL，现有 `FrontlineActiveOrderVO.productName` 仍为可选且缺少 `quantity`，订单摘要布局尚不存在。
- RED: `node src/test/js/mes-frontline-pqc-order-product-summary-static.spec.cjs` -> FAIL，后端候选和响应 VO 缺少 `BigDecimal quantity`。
- RED PRECONDITION BLOCKER: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest,MesFrontlineRuntimeConfigControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 在本任务测试前被共享工作区既有 `MesTeamLeaderProcessConfigListReqVO`、`MesWorkOrderAbnormalStateService` 缺失阻塞；未把该命令记为业务 RED，也未修改并发任务文件。
- GREEN: `node src/test/js/mes-frontline-pqc-order-product-summary-static.spec.cjs` -> PASS，后端候选、响应 VO、Controller 映射、正式数据校验和 JUnit 方法合同完整。
- GREEN: `node tests/e2e/mes-frontline-pqc-order-product-summary-static.spec.cjs` -> PASS，前端必填类型、三字段摘要、数量格式化、紧凑字号和响应式布局合同完整。
- RED: 真实 Playwright 首轮 `1440x900` 布局断言 -> FAIL，顶部信息栏右边界 `1465 > 1440`，证明固定最小列宽未计入左侧导航占用空间。
- GREEN: 收紧顶部订单、工序、员工列的响应式最小宽度后重跑真实 Playwright -> PASS，`1440x900` 顶部右边界为 `1417`，订单号、产品名称、数量、工序及员工均位于各自卡片内。
- GREEN: `pnpm ts:check` -> PASS，无 TypeScript 错误。
- REGRESSION: `node tests/e2e/mes-frontline-pqc-all-active-orders-search-static.spec.cjs` -> PASS，继续使用全部生产组长活跃订单集合并支持订单号搜索。
- REGRESSION: `node tests/e2e/mes-frontline-pqc-order-picker-production-layout-static.spec.cjs` -> PASS，订单选择器布局保持正式生产选择器合同。
- REGRESSION: `node tests/e2e/mes-frontline-pqc-active-order-switching-static.spec.js`、`node tests/e2e/mes-frontline-pqc-login-employee-lock-static.spec.cjs` -> PASS，订单切换及员工锁定合同保持。
- REGRESSION: `node tests/e2e/edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs` -> PASS，PQC 全屏布局合同保持。
- E2E: `$env:PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH='C:\Program Files\Google\Chrome\Application\chrome.exe'; node pqc-order-product-summary-real.e2e.cjs` -> PASS，真实登录 `芋道源码/admin`，正式接口返回 11 条活跃订单；选择 `PQC-E2E-FS-20260804` 后页面与接口一致显示 `球囊扩张压力泵`、`100`，并通过 `1440x900`、`1920x1080` 和 PQC 全屏边界、换行、字体及无重叠断言；未发出 PQC 提交请求。
- RUNTIME: 本任务运行包 `output/runtime/int_main/backend-runtime-control-20260807-frontline-pqc-order-product-summary.jar` 已运行于 `48081`，SHA-256 为 `974F8BB0F65AC3D26F173B8DD874EEA9E110846E42426BB5BE6E031A7132CA3D`；前端运行于 `8081`。
- EXPERIENCE: 将“顶部固定信息栏必须按含左侧导航的真实视口采集 DOM 边界，并分别验证普通页面和全屏状态”合并到 `docs/e2e-rules.md#顶部固定信息栏真实视口边界门禁`，并更新 `docs/experience-index.md`。
- MAVEN BLOCKER: 聚焦 Maven 在共享工作区 `yudao-module-mes/target` 上反复遭遇并发编译写入，JVM 卡在 `FileDescriptor.close0/ClassWriter.writeClass`；隔离快照又被同期未完成模块产物的无关 `BeanUtils.toBean` 签名不一致阻塞。因此未取得可归因的目标 JUnit/Surefire PASS，未将静态合同或 E2E 冒充 JUnit 结果。

## Milestone Status

- M1：completed。
- M2-M3：completed。
- M4：completed（聚焦 Maven 环境阻塞单列为残余验证缺口）。
- M5：completed。

## Closeout

- backend/frontend evidence validator 均 PASS，结论已复制到 `verification-report.md` 后，技能证据文件按收尾规则清理。
- `task-closeout-cleanup` preview 确认只保留三份核心任务文档、三张 Playwright 截图、机器可读 `result.json` 和当前任务运行包。
- 首次 apply 在隔离 Maven 快照的 Windows 超长类文件路径上触发 `WinError 3`；复核绝对路径位于本任务 `output/tmp` 后使用 Windows 长路径删除两个任务自有临时目录，再次 preview 为无 delete/blocked，最终 apply 状态为 `applied`。
- 未执行 Git 提交、合并或推送。
- POST-CLOSEOUT RUNTIME: 最终健康检查期间 `48081` 曾短暂无监听；随后确认并发 ERP 任务正在启动其更新运行包，未停止或覆盖该进程。新 PID `40088` 在 `48081` 恢复 `UP`，其内嵌 MES 类分别包含 `MesFrontlineActiveOrderRespVO.quantity`、Controller `setQuantity` 和服务端 `validateActiveOrderSummary`，因此当前共享运行态仍包含本任务功能。

## Blockers

- 聚焦 Maven/Surefire 结果受共享 MES `target` 并发写入阻塞；影响是本任务新增的后端 JUnit 尚无独立执行证据，已由后端静态合同、运行包字节码核验与真实接口/E2E 正向路径补充验证，但不将其表述为 JUnit PASS。
- 当前真实样例订单进入主检验区后会明确提示“当前工序缺少已发布 QA 检验规程，activeOrderId=30，routeProcessId=980661，processId=922985”；这是现有测试数据的下游正式 QA 规程缺口，不影响本任务顶部订单摘要，且未被吞掉或降级。
