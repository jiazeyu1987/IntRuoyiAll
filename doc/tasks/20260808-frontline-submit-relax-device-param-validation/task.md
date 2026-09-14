# 一线生产提交放宽设备与参数校验

## Task Goal

按用户明确要求调整一线生产正式提交：授权服务只校验工序身份和工位身份，不再用 route-start 候选的 `deviceId` 拦截提交；正式提交时不执行设备参数校验。

## Milestones

- [x] M1: 读取后端、收尾、编码、经验和 backend-api 规则，确认变更边界。
- [x] M2: 新增/调整后端回归测试，先复现当前设备 ID 与参数校验会阻断提交。
- [x] M3: 修改授权服务与提交服务，实现用户要求的最小行为变更。
- [x] M4: 运行定向 Maven 测试和 backend-api evidence 校验。
- [x] M5: 更新验证报告、经验判断和收尾状态。

## Expected Verification

- `MesFrontlineSubmitAuthorizationServiceImpl` 对同一路线、路线工序、工序、工位但不同设备 ID 的提交不再抛 `PRO_FRONTLINE_SUBMIT_DEVICE_CONTEXT_MISMATCH`。
- `MesProFrontlineFeedbackSubmitServiceImpl` 正式提交不再调用 `MesFrontlineDeviceParameterValidator`，即使存在设备参数缺失/异常也不在提交阶段阻断。
- 仍保留签名员工一致、登录设备账号、工序身份、工位身份、产出/损耗、幂等和事务写入原有校验。

## Current Status

completed

已完成实现、定向验证和 cleanup：授权服务不再按设备 ID 拦截同工序同工位提交；正式提交服务不再执行设备参数校验；目标 Maven 17 个测试通过；cleanup preview/apply 均无阻塞，临时 backend-api evidence 已删除，核心任务记录保留。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；本次按用户明确业务要求移除指定提交校验，不新增默认成功、catch 吞异常或替代数据源。
- `是否从根因和长期维护角度解决`：是；不再比较不同设备 ID 域，且把“提交阶段不做参数校验”作为显式行为写入测试。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 命中并更新 `docs/backend-development.md#一线运行态 route-start 生产组长来源必须独立于班组设备绑定`：正式提交授权不比较 route-start 候选设备 ID。
- 命中并更新 `docs/backend-development.md#一线生产正式提交必须单事务落链并按唯一组长归属可见`：正式提交阶段不执行设备参数校验，但仍保留工序、工位、人员、签名、数量和事务闭环校验。
- 已同步 `docs/experience-index.md` 关键词：`授权设备 ID 不比较`、`提交不做参数校验`。
