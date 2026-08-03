# 设备账号切换工序启用工艺路线绑定修复

## Task Goal

修复 `设备账号 1 未绑定启用工艺路线，无法切换工序` 的问题，确保设备账号切换工序时使用正式启用工艺路线绑定链路，不用默认路线、空绑定、隐藏异常或前端文案掩盖配置缺失。

## Milestones

- [ ] 定位设备账号切换工序的后端/前端触发链路和错误来源。
- [ ] 补充最小失败回归，复现设备账号已有可用正式绑定但仍报未绑定启用路线的场景。
- [ ] 实施最小正式修复，保持设备账号、工艺路线、工序切换配置来源清晰。
- [ ] 运行定向 GREEN 和相邻回归验证。
- [ ] 记录 bug evidence、verification report，并按 closeout 规则收尾。

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

in_progress

- 已创建任务目录；正在读取经验门禁并定位错误来源。
