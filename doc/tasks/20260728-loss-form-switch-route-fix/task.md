# 20260728 Loss Form Switch Route Fix

## Task Goal

修复 eDHR 填写页“切换填写人”选择工艺路线表单槽位候选时，应该切换到“损耗单”，却报错 `eDHR 批次缺少唯一批记录路线` 的问题。修复必须保持三类配置入口边界：批记录表单、表单槽位、工序开始不得混用。

## Milestones

- [x] M1: 在隔离 worktree 中建立任务证据，核对 worktree、前端、后端、E2E、数据库、PowerShell 和经验门禁。
- [x] M2: 复现并锁定“表单槽位候选被当作批记录路线打开”的根因，先补 RED 静态/后端回归。
- [x] M3: 最小化修复切换填写人打开逻辑，使 `formBindingKey/formTemplateId/formCenterInstanceId` 的损耗单任务走表单槽位路径。
- [x] M4: 运行目标静态合同、后端定向测试、编译/前端校验和必要真实路径验证。
- [ ] M5: 更新证据、沉淀经验、融合回 `int_main`，并完成提交/推送或记录明确阻塞。

## Expected Verification

- RED/GREEN: 聚焦静态合同覆盖切换填写人选择表单槽位候选时不得调用批记录路线唯一性解析。
- RED/GREEN: 后端定向测试覆盖 `openTask` 对表单槽位任务返回损耗单 FormCenter 上下文。
- REGRESSION: 既有 `edhr-switch-filler-selectability-static.spec.js` 与 `mes-edhr-assist-filler-switch-snapshot-static.spec.cjs`。
- REGRESSION: 受影响 Maven 定向测试与 `mvn -pl yudao-module-mes -am "-DskipTests" compile`。
- E2E: 若本地数据存在可打开样本，使用 worktree 成对端口或融合后 `int_main` 真实页面验证；若缺夹具，记录正式阻塞，不用 mock 或 API-only 冒充通过。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是让表单槽位任务走正式 FormCenter/表单槽位上下文，而不是批记录路线兜底。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- `AGENTS.md#工艺路线三类配置术语契约`：损耗单属于表单槽位链路，不得替代或推断批记录表单。
- `docs/backend-development.md#切换填写人快照读取边界`：候选必须来自执行详情 `assistSwitchTasks` 快照，打开任务必须按具体任务上下文隔离。
- `docs/e2e-rules.md#eDHR 路线表单跳过口径门禁`：损耗单等路线表单卡片必须按 FormCenter/表单槽位上下文打开，不得误走批记录报表来源。
- `docs/frontend-development.md#前端 Route Query ID 比较门禁`：切换后 URL query、active 高亮和上下文 key 必须跟随所选任务/填写人。
- `docs/worktree-restrictions.md` 与 `docs/branch-runtime-ports.md`：本修复在 `D:\IntRuoyiWorktree\loss-form-switch-fix`，`int_main slot 9`，端口 `8090/48090`。

## Cleanup Keep

- doc/tasks/20260728-loss-form-switch-route-fix/bug-regression-evidence.md
- doc/tasks/20260728-loss-form-switch-route-fix/backend-api-evidence.md
- doc/tasks/20260728-loss-form-switch-route-fix/verification-report.md
