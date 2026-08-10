# 一线生产连续报工会话复位

## 任务目标

- 一线生产正式提交成功后结束当前填写会话，并恢复为可继续填写、切换工序和切换实际员工的状态。
- 每次新的填写会话使用新的幂等键；网络错误等结果不确定场景不得自动轮换幂等键或伪造成功。
- 已提交的正式报工、记录本和工序池事实保持不可修改，本任务只调整设备端下一次填写会话。

## 里程碑

- [x] M1：核对现有产品、验收、实现和相邻任务基线，完成变更分诊。
- [x] M2：补充 BDD 和失败的聚焦静态合同。
- [x] M3：实现提交成功后的生产填写会话复位。
- [x] M4：完成聚焦回归、类型检查、证据归档与任务收尾。
- [x] M5：使用真实 Playwright 页面在不刷新的同一会话中连续正式提交 4 次，并核对四次独立写入和页面复位。
- [x] M5a：在本机测试租户中补齐任务测试员工的正式电子签名授权及授权审计，并通过独立只读复核。
- [x] M6：在同一页面不刷新的四轮提交中实际切换不同员工和不同工序，并独立核对每轮人员、工序、签名和正式事实归属。

## 预期验证

- `node tests/e2e/frontline-production-repeat-submit-static.spec.cjs` 先 RED 后 GREEN。
- `node tests/e2e/frontline-formal-submit-static.spec.cjs` 通过并与连续提交行为一致。
- 相关一线生产重填、员工/工序切换静态合同通过。
- `pnpm ts:check` 通过，或记录与本任务无关的既有阻塞。
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/frontline-production-repeat-submit-static.spec.cjs IntRuoyiFronted/tests/e2e/frontline-formal-submit-static.spec.cjs doc/tasks/20260809-frontline-repeat-submit-reset docs/changes/20260809-frontline-repeat-submit-reset.md` 通过。
- 真实 Playwright：同一页面不刷新、不重新导航，连续正式提交 4 次；四次目标 POST 均成功且回执身份互不重复，每次成功后数量清空、按钮恢复“正式提交”、人员与工序入口可继续操作。
- 真实组合 Playwright：四轮至少覆盖 2 名实际员工和 2 道正式工序；每轮从页面选择员工和工序，使用所选员工签名口令提交，主 frame 导航次数保持 0，数据库中的实际员工、路线工序、MES 工序和签名主体与页面选择逐轮一致。

## 经验门禁

- 命中 `docs/frontend-development.md#前端静态契约隔离门禁`：新增聚焦合同，使用明确函数和模板锚点，稳定记录 RED/GREEN。
- 命中 `docs/frontend-development.md#前端写入成功与列表刷新失败分层门禁`：只有正式写请求明确成功后才能清空当前草稿并轮换幂等键；写失败或响应不确定时必须保留原幂等键和输入。
- 命中 `docs/backend-development.md#一线生产正式提交必须单事务落链并按唯一组长归属可见`：继续只调用一次正式提交接口，不修改后端事务、签名或身份追溯；本次需求以正式验收 R17“同一工序允许多人、多次、分片填写”为上位产品约束，替代旧页面永久锁定要求。
- Blocker：若无法区分“同一次请求重试”和“下一次独立报工”，或成功复位会复用原幂等键、失败时会清空用户输入，则停止实现。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；用明确的单次填写会话生命周期分离后端幂等重试与下一次独立提交。
- `是否存在临时补丁或绕过`：否。
- `测试数据变更`：tenant `122` 已为任务测试账号 `ffs0807worker` 新增正式 DCC 授权与授权审计；M6 将以固定任务 ID 事务补齐第 2 名正式员工、第 2 道正式工序、唯一生产组长范围和签名审计。任一来源或写后断言失败即回滚，四连提完成后保留正式审计证据。

## Current Status

completed：M6 已以 2 名正式员工、2 道正式工序完成同页不刷新四连提；独立数据库、视觉、验证器和回归复核均通过，任务清理 preview/apply 无 blocked 或 warning。
