# 设备账号切换工序启用工艺路线绑定修复

## Task Goal

修复 `设备账号 1 未绑定启用工艺路线，无法切换工序` 的问题，确保设备账号切换工序时使用正式启用工艺路线绑定链路，不用默认路线、空绑定、隐藏异常或前端文案掩盖配置缺失。

## Milestones

- [x] 定位设备账号切换工序的后端/前端触发链路和错误来源。
- [x] 补充最小失败回归，复现正式岗位缺少工作站绑定时被泛化为未绑定启用路线的场景。
- [x] 实施最小正式修复，保持设备账号、岗位、工作站、启用工艺路线来源清晰。
- [x] 运行定向 GREEN 和相邻回归验证。
- [x] 记录 bug evidence、verification report。
- [ ] 按 closeout 规则提交、推送并完成收尾。

## Expected Verification

- 后端定向 JUnit：覆盖设备账号切换工序绑定解析。
- 如涉及前端切换入口，补充前端静态合同或真实路径验证。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260803-device-account-route-switch-binding/bug-regression-evidence.md`

## Applicable Gates

- Strict no-fallback：缺少正式启用工艺路线绑定时必须明确报错，不得用默认路线、mock、空列表成功或吞异常替代。
- BDD + strict TDD：生产代码变更前必须有可复现失败测试，修复后记录 RED/GREEN。
- MES/eDHR 路线配置术语边界：工艺路线绑定、工序开始、批记录表单、表单槽位不得互相替代。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否；如发现正式配置缺失，保持 fail fast。
- `是否从根因和长期维护角度解决`：是；目标为修复正式绑定解析链路。
- `是否存在临时补丁或绕过`：否；不使用前端硬编码、直接 SQL、默认成功或 mock 数据。

## Current Status

ready_for_closeout

- 代码与回归验证已完成：正式岗位存在但无工作站绑定时，后端现在 fail fast 返回 `post workstation binding` 缺失上下文，不再让上层落到泛化的“未绑定启用工艺路线”。
- 本地只读数据确认：`system_users.id=1` 启用且未删除；`system_user_post` 当前未删除岗位为 `post_id=14`；`mes_md_workstation_worker` 对 `post_id=14` 的未删除绑定数量为 `0`。
- 实际配置修复动作仍需业务侧补齐：将用户 1 的正式启用岗位 14 绑定到参与启用工艺路线工序的工作站，或将用户 1 的正式岗位关系调整为已绑定工作站的岗位。
- 当前收尾阻塞：工作区存在无关 DCC 前端改动且分支已 ahead，不能在本任务中提交/推送无关变更。
- 经验沉淀阻塞：`docs/experience-index.md` 已有无关 DCC 页签缓存 dirty 改动，本任务不混写共享经验索引。
