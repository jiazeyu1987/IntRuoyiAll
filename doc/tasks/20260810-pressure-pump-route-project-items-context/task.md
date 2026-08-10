# 20260810-pressure-pump-route-project-items-context

## Task Goal

修复一线 PQC 切换压力泵订单时的两阶段错误：先出现 `routeProjectItems routeId=980091，missingItemIds=[14]`，修正后又在正式路线 `922119` 出现 `routeProjectCode productId=902101，routeProductMasterIds=[924005, 902149, 902101, 901965]，matchedProjectIds=[]`。正式链路必须按路线绑定物料代码解析 DCC 项目代码，再使用该 DCC 项目的 `productMasterId` 查询已发布 QA 规程。

## Milestones

- [x] 定位 routeProjectItems missingItemIds=[14] 的校验来源和受影响接口。
- [x] 补充可复现该错误的回归测试，记录运行时 RED。
- [x] 按订单产品、有效路线、路线物料代码、DCC 项目代码、已发布 QA 规程的正式链路修复。
- [x] 完成本次服务和测试类的隔离编译及 JUnit 回归。
- [x] 完成标准 Maven 验证，进入任务收尾。
- [x] 将本轮修复提交到 int_main。

## Expected Verification

- 定向后端回归测试覆盖 routeProjectItems 项目级缺口不再阻断一线 PQC 压力泵工序。
- 相邻一线设备账号/QA/PQC 选择路径不出现默认成功、吞异常或 fallback。
- 技能 evidence validator 通过。

## Current Status

completed

## Completed Work

- 订单选择仍先校验工单 productId 与所选 routeId 的正式产品路线绑定。
- 已确认当前二次报错的根因：路线 `922119` 绑定 `924005 / ID` 作为 DCC 项目代码物料，当前实现却把路线物料 ID 直接与 DCC `productMasterId` 比较，身份类型不一致。
- 已确认正式建模代码会把 DCC `projectCode` 创建为 MES 物料代码并绑定到路线；PQC 应使用路线物料代码匹配 DCC 项目，不能把路线物料 ID 当作 DCC `productMasterId`。
- QA 规程仍应只按命中 DCC 项目的 `productMasterId` 查询，并严格限定 PUBLISHED、当前路线 ID、当前路线版本 ID。
- QA 规程中的 routeProcessId、processId、currentVersionId 和检验项目仍执行完整一致性校验。
- 第一阶段修复已进入 int_main，但其“路线物料 ID 直接匹配 DCC productMasterId”判断已被真实路线 `922119` 证明错误，本轮继续修正。
- 本轮已改为读取路线绑定的可解析 MES 物料代码，精确匹配唯一启用 DCC `projectCode`；不再把路线物料 ID 当作 DCC `productMasterId`。
- 路线中单个无关物料无法解析时不再阻断已存在的项目代码物料；DCC 未唯一命中、缺 productMasterId 或 QA 配置不完整仍 fail fast。

## Verification

- RED：新增路线物料 `924005 / ID` 与 DCC productMasterId 分离场景后，旧实现 40/41 通过并复现 `matchedProjectIds=[]`。
- 隔离 javac：PASS。
- 隔离 JUnit：PASS，MesFrontlinePqcContextServiceTest 41/41。
- 标准 Maven：PASS，`mvn -q -pl yudao-module-mes -Dtest=MesFrontlinePqcContextServiceTest test` exit 0，Surefire 41/41。
- bug-regression-fix-loop evidence validator：PASS。
- backend-api-delivery evidence validator：PASS。
- git diff --check：PASS。
- task-closeout-cleanup preview/apply：PASS，仅保留 task.md、execution-log.md、verification-report.md，已删除本任务临时 evidence、javac/JUnit 参数、隔离 class 和 RED 源副本。
- int_main 实现提交：PASS，`c81c8fb2d fix: resolve PQC QA by route project code`，仅包含本任务服务与回归测试。

## Blockers

- 无。并发索引冲突已由对应任务处理完成，本任务未修改其文件；使用 `git commit --only` 只提交本任务服务与测试，原有暂存内容保持不变。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，正式身份链路改为 route item code -> enabled DCC projectCode -> DCC productMasterId -> published QA regulation。
- 是否存在临时补丁或绕过：否。

## Applicable Experience Gates

- MES 一线设备账号权限门禁：压力泵全工序授权必须区分标准权限与岗位/工作站绑定，不能用岗位缺失或空结果掩盖权限链路。
- PQC 待检准入与工序选择必须分离：已配置 QA 规程、路线产品绑定、待检工单和工序卡片必须按正式链路判断，不能让非必要项目级快照缺口阻断可见工序。
- QA 多工序正式发布与退役夹具唯一键必须隔离：QA 质检工序承载与业务工序、路线工序身份要分清，不能用批记录或表单槽位替代。
