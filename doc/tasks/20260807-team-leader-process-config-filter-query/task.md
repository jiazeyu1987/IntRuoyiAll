# 工序配置标准过滤查询改造

## 任务目标

- 将生产组长“工序配置”页签标题说明区替换为标准 `TableMultiFilter` 条件 Tab 查询控件。
- 通过正式后端查询参数支持工艺路线、工序、损耗原因、映射设备和设备参数标准的多条件交集查询。
- 保留现有表格、新增按钮、维护弹窗、负责路线及候选数据行为。

## 里程碑

- [x] M1：读取项目规则、技能契约、现有实现及并行任务状态。
- [x] M2：记录 BDD 并完成前后端 RED 测试。
- [x] M3：实现正式后端过滤契约与前端条件 Tab 查询。
- [ ] M4：完成定向回归、类型检查和真实只读 Playwright 验证。
- [ ] M5：完成证据校验、经验沉淀和任务清理收尾。

## 预期验证

- `node tests/e2e/team-leader-process-config-filter-query-static.spec.cjs`
- `node tests/e2e/team-leader-process-config-unified-static.spec.cjs`
- `pnpm ts:check`
- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Playwright 通过真实本机页面验证单条件、双条件交集、重置清参和零写请求。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；查询失败清空展示结果并显示正式错误。
- 是否从根因和长期维护角度解决：是；补齐后端正式查询契约，禁止前端本地过滤。
- 是否存在临时补丁或绕过：否。

## 经验门禁

- 已读取 `docs/experience-index.md`，适用 `docs/frontend-development.md#统一列表复合工具栏布局门禁`：条件 Tab 默认为空、正式参数交集提交、控件必须保持可见宽度、禁止前端本地过滤。
- 已读取前端静态契约隔离门禁：使用任务专用最小静态合同记录 RED/GREEN，不修改无关失败。
- 已读取后端生产组长工序配置权限门禁：保持 `process-config/list` 权限与授权路线工序来源不变。
- 并行任务 `20260807-loss-reason-description-display` 已修改同一页面；本任务保留 `reasonName` 单独展示，不回退其改动。

## Current Status

blocked：前后端实现、任务专用静态合同和真实页面查询参数路径已完成；后端定向 JUnit 被其它任务持续占用并改写共享 `target` 阻塞，真实交集结果被未加载新实现的 `48081` 旧运行态阻塞，最新全量类型检查又被无关并行页面的 `actualEmployeeId` 类型错误阻塞。未进入 `ready_for_closeout`，未执行经验沉淀、清理或完成标记。
