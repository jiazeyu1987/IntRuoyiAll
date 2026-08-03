# 压力泵角色授权一线工序切换

## Task Goal

实现一线生产填写页面的压力泵工序切换授权：拥有指定权限角色/权限的账号登录后，可以切换压力泵相关启用工艺路线的全部工序；该授权不依赖账号岗位工作站绑定，同时不得影响普通账号仍按岗位工作站正式链路授权。

## Milestones

- [ ] 核对现有一线生产填写前后端调用链路与角色/权限 API。
- [ ] 记录需求变更边界和 BDD 场景。
- [ ] 编写 RED 回归测试覆盖“有权限可看压力泵全部工序”和“无权限仍按岗位链路”。
- [ ] 实施最小后端正式授权链路，不引入默认路线或空成功。
- [ ] 运行 GREEN、相邻回归和 evidence 校验。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineDeviceAccountContextServiceTest,MesFrontlineWorkstationPostRouteBindingSourceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- backend-api evidence validator: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260803-pressure-pump-role-process-switch/backend-api-evidence.md`

## Applicable Gates

- Strict no-fallback：无授权、无启用路线、无压力泵工序配置时必须明确失败，不得返回默认全量或空成功。
- BDD + strict TDD：生产代码前先记录 BDD 和 RED。
- 权限边界：普通账号仍走岗位/工作站链路；角色授权只覆盖压力泵范围。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否；角色授权是显式新授权链路，不作为岗位链路失败后的兜底。
- `是否从根因和长期维护角度解决`：是；目标是把压力泵特殊全工序授权建模为正式权限，而不是岗位绑定伪装。
- `是否存在临时补丁或绕过`：否；禁止硬编码账号 ID、岗位 ID、前端放行或默认路线。

## Current Status

in_progress

- 已创建任务目录，正在核对现有一线生产填写和角色权限链路。
