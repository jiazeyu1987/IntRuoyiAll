# 20260811 工艺路线发布继承生产组长工序配置

## Task Goal

- 在 worktree 中修复工艺路线重新发布后，生产组长工序配置中的损耗原因和设备参数标准仍绑定旧 routeProcessId，导致一线生产“不良/设备参数”为空的问题。
- 发布新路线版本时，系统应把未变化工序的正式生产组长配置迁移到新的 routeProcessId；新增、删除或无法唯一映射的工序不得用 fallback 回读旧数据。
- 验证通过后，将修复融合进 int_main。

## Milestones

- [x] M1：审计目标 worktree、读取适用规则和缺陷修复技能。
- [x] M2：建立 BDD 场景和 RED 回归测试，证明当前发布逻辑不会迁移配置。
- [x] M3：实现发布投影后配置继承，覆盖损耗原因和设备参数标准。
- [x] M4：运行目标后端回归和相关静态/合同验证。
- [ ] M5：验证成功后合并回 int_main，并完成任务记录和收尾。

## Expected Verification

- RED：路线发布投影替换 routeProcessId 后，旧 routeProcessId 上的损耗原因和设备参数标准没有迁移到新 routeProcessId，目标测试失败。
- GREEN：同一发布投影测试通过，断言新 routeProcessId 继承损耗原因和设备参数标准，旧工序保持删除状态，不引入运行态旧 ID fallback。
- REGRESSION：运行受影响 MES 发布投影后端测试；合并前运行 branch runtime port guard。

## Current Status

ready_for_merge

## Verification Evidence

- RED: 新增发布继承回归测试覆盖旧 routeProcessId 配置迁移；相邻回归首次运行暴露新增 Mapper 未注入旧测试类导致 NPE，命令：`mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPublishProjectionServiceTest,MesProRouteVersionPublishProjectionServiceImplTest,MesTeamLeaderProcessConfigServiceImplTest,MesTeamLeaderLossReasonServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL。
- GREEN: 发布投影定向测试通过，命令：`mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPublishProjectionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS，12 tests。
- REGRESSION: 发布投影相邻回归通过，命令：`mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPublishProjectionServiceTest,MesProRouteVersionPublishProjectionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS，18 tests。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；禁止运行态回读旧 routeProcessId。
- 是否从根因和长期维护角度解决：是；发布投影阶段迁移正式配置，保持当前 active routeProcessId 为唯一读取口径。
- 是否存在临时补丁或绕过：否；无法唯一映射时不迁移，避免覆盖或复制错误配置。
