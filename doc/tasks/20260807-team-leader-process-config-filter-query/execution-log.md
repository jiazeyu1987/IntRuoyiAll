# 执行日志

## 用户意图

- 将截图红框中的标题说明区替换为标准列表条件 Tab 查询控件。
- 仅替换查询区，不把整张表改为 `UnifiedListTemplate`，不新增分页或显示字段。
- 五个业务字段通过正式后端参数执行多条件交集查询。

## BDD

- BDD: 默认显示空条件 Tab -> Given 用户进入生产组长工序配置页签；When 未添加筛选条件；Then 红框位置显示标准空条件 Tab 和加减按钮，右侧新增按钮保持可用，首屏请求不携带隐藏筛选参数。
- BDD: 单条件正式查询 -> Given 工序配置存在可见路线；When 用户按工艺路线关键字查询；Then 请求携带 `routeKeyword`，表格只显示后端返回的匹配工序行。
- BDD: 多条件交集查询 -> Given 用户已添加路线和设备两个条件；When 点击查询；Then 同一请求同时携带 `routeKeyword` 与 `deviceKeyword`，结果满足两个条件交集。
- BDD: 重置恢复全量授权列表 -> Given 当前存在筛选结果；When 用户点击重置；Then 五个正式参数全部清除并重新请求未过滤授权列表。
- BDD: 筛选不缩小维护候选 -> Given 当前表格已按条件过滤；When 用户打开新增或设备维护入口；Then 负责路线、路线工序候选和设备候选继续来自未过滤正式基线。
- BDD: 查询失败不展示旧结果 -> Given 当前表格存在上一次查询结果；When 新查询接口失败；Then 页面显示明确错误并清空展示结果，不用旧结果冒充本次成功。

## 命令意图与证据

- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/backend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`。
- 已读取 `frontend-feature-delivery`、`backend-api-delivery` 及其证据契约。
- 已检查 Git 状态；工作区存在其它任务改动，本任务不清理、不提交、不覆盖无关文件。
- 已核对并行损耗描述任务的最新页面改动：损耗标签当前仅渲染 `reasonName`，本任务将保留该行为。
- RED: `node tests/e2e/team-leader-process-config-filter-query-static.spec.cjs` -> FAIL，预期原因：生产页面尚未导入和渲染 `TableMultiFilter`，五项正式查询合同尚未实现。
- RED: `rg -n "listProcessConfigs\(Long leaderUserId, MesTeamLeaderProcessConfigListReqVO reqVO\)" yudao-module-mes/src/main/java` -> FAIL，预期原因：`MesTeamLeaderProcessConfigService` 尚未接收 `MesTeamLeaderProcessConfigListReqVO` 正式查询对象。
- Maven RED 因另有四个进程并发写入同一后端 `target` 目录而终止；仅终止本任务进程，未操作其它任务进程。完整 Maven 回归待共享构建冲突解除后执行。
- 已实现后端五项正式关键词 VO、Controller 校验转发、授权结果上的大小写不敏感包含匹配和多条件 AND 交集；嵌套命中返回完整工序行。
- 已实现前端 `TableMultiFilter` 空条件 Tab、稳定 table key、五项 contains 定义，以及未过滤基线 `processConfigRows` 与展示结果 `processConfigDisplayRows` 分离。
- 查询失败清空展示结果并显示明确错误；初始加载和维护刷新先更新未过滤基线，再按当前条件查询展示结果。
- GREEN: `node tests/e2e/team-leader-process-config-filter-query-static.spec.cjs` -> PASS。
- `node tests/e2e/team-leader-process-config-unified-static.spec.cjs` -> FAIL，当前并行损耗维护改动已将入口替换为 `data-team-leader-process-config-manage-loss`，但其共享回归仍要求旧 `data-team-leader-process-config-add-loss`；本任务未修改该并行任务测试。
- `pnpm ts:check` -> FAIL，两处现有并行异常上报改动缺少 `openAbnormalDialog`、`resetAbnormalForm`，本次过滤改造没有新增类型诊断。
- 并行前端改动闭合后复跑：GREEN: `node tests/e2e/team-leader-process-config-filter-query-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/team-leader-process-config-unified-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- Playwright 真实页面：本机前端 HTTP 200、后端 health `UP`；通过默认本机身份登录 `/mes/pro/process-pool/team-leader`，目标位置显示默认空条件 Tab、加减按钮、右侧新增按钮和五个字段选项。
- Playwright 单条件请求：`GET .../process-config/list?routeKeyword=R-PCU`，HTTP 200、业务码 0。
- Playwright 双条件请求：`GET .../process-config/list?routeKeyword=R-PCU&deviceKeyword=RLR0807M-001-01`，HTTP 200、业务码 0。
- Playwright 重置请求：`GET .../process-config/list`，五个关键词均已移除；检查过程写请求数 0、page error 0、新增 console error 0。
- Playwright 运行态阻塞：单条件、双条件和重置均返回 106 行，证明当前 `48081` 仍未加载本次后端过滤实现；不得把旧运行态忽略查询参数的响应作为交集通过证据。
- 授权边界回归已补充：过滤服务即使收到当前工序的设备映射，也不得让其它组长所属设备被设备关键词命中。
- GREEN: `node tests/e2e/team-leader-process-config-filter-query-static.spec.cjs` -> PASS（最新复跑）。
- GREEN: `node tests/e2e/team-leader-process-config-unified-static.spec.cjs` -> PASS（最新复跑）。
- `pnpm ts:check` 曾在本任务实现后 PASS；最新复跑 -> FAIL，唯一诊断为无关并行文件 `src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue:2772` 向 `FrontlinePqcInspectionSubmitReqVO` 传入尚不存在的 `actualEmployeeId`，本任务不修改该文件。
- 指定 Maven 命令复跑 -> FAIL，未进入本任务测试：并行 Maven 持续改写共享 `target`，`yudao-module-system-api` 的 `AdminUserApi.class` 和 `yudao-module-system-biz` 的 `AdminUserDO.class` 等依赖类缺失，MES 在主源码编译阶段就终止。

## 里程碑状态

- M1：已完成。
- M2：已完成。
- M3：已完成。
- M4：进行中。
- M5：未开始。

## 阻塞项

- 共享前端文件仍有其它任务进行中的不完整改动，导致统一静态回归和全量类型检查暂不能通过。
- 共享前端阻塞已解除，计划内三项前端回归均已通过。
- 后端共享 `target` 持续被其它任务 Maven 编译占用；包含本任务测试的并行命令在生成新报告前退出，定向 JUnit 尚未取得有效结果。
- 当前 `48081` 是旧后端运行态；按脏主工作区运行 Jar 门禁，本任务不从并行脏源码重启后端，因此真实交集结果验证阻塞。
- 最新全量 `pnpm ts:check` 被无关并行文件 `FrontlineFixedTemplatePanel.vue:2772` 的 `actualEmployeeId` 类型错误阻塞；本任务的两个前端文件未出现新诊断。
- 尚有其它任务的 Maven 进程长时间占用 `E:\IntRuoyi\IntRuoyiBackend` 的共享构建产物；按并发规则本任务未终止它们、未并行写入、未删除 `target`。
