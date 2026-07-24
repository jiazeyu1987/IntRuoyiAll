# 任务：展厅后端 B2 内容与前台展示契约对齐

## 目标

对齐内容读取与前台展示相关的后端契约，使公司 current、产品/展厅分页、产品详情、版本历史、display payload 更贴近设计文档要求。

## 证据来源

- `doc/tasks/20260519-showroom-remediation-b2-content-display-contract/execution-log.md`
- `doc/tasks/20260519-showroom-remediation-b2-content-display-contract/backend-api-evidence.md`

## 范围

- `/showroom/company/current`
- `/showroom/product/page`
- `/showroom/product/get`
- `/showroom/product/history`
- `/showroom/hall/page`
- `/showroom/display/home`
- `/showroom/display/company`
- `/showroom/display/hall/{hallId}`
- `/showroom/display/product/{productId}`

## 非范围

- 不实现审批 reject / 指派 / 讨论 / 讲解后台接口
- 不实现 BPM runtime
- 不实现前端页面

## 里程碑

- [x] M1：记录 BDD 与 TDD 目标
- [x] M2：对齐公司 current 与产品/展厅内容查询契约
- [x] M3：对齐 display payload、字段标签与 preview URL 契约
- [x] M4：补齐产品历史 revision-grouped diff 契约
- [x] M5：运行集成与内容回归测试并记录结果
- [x] M6：根据现有证据恢复完整任务记录

## 预期验证

- `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest,ShowroomCompanyContentTest,ShowroomProductContentTest,ShowroomHallContentTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 当前状态

Completed. 根据现有 `execution-log.md` 与 `backend-api-evidence.md` 恢复，本任务的 B2 内容/展示契约已完成实现并通过验证；原执行回合未单独提交 Git commit 的原因仅是共享 worktree 内存在并行非本任务改动，不构成功能阻塞。

## 完成结论

- `/showroom/company/current` 已返回公司元数据与当前 revision 详情，不再只是裸 revision 快照。
- `/showroom/product/page` 已支持更丰富的内容行与过滤契约。
- `/showroom/product/get` 已返回更完整的详情元数据。
- `/showroom/product/history` 已返回按 revision 分组的 diff 元数据，不再是扁平审计行。
- `/showroom/hall/page` 已返回带映射信息与产品计数的展厅行数据。
- display company/product payload 已改为人类可读字段标签，不再回显原始字段码。
- draft-only 的 incomplete 产品在 `/showroom/display/product/{productId}` 下仍可展示，而不是因缺少 live revision 直接失败。
- display payload 的 `previewImageUrl` 与音频 URL 已走真实文件访问契约。

## 最终验证结果

- PASS: `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest,ShowroomCompanyContentTest,ShowroomProductContentTest,ShowroomHallContentTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 历史阻塞与影响结论

- 运行时与接口契约层面：`No functional blocker remains`，B2 范围内无未解功能阻塞。
- 原执行回合的 closeout 阻塞：共享 showroom worktree 中存在重叠的并行非 B2 改动，因此当时没有创建 task-only Git commit，以避免混入无关变更。
- 对当前归档的影响：本任务可以按 completed 归档，但不能凭空补写不存在的提交结果。

## 无法从现有证据恢复的信息

- 原执行回合的精确提交哈希与提交时间。
- 原执行回合的精确源码改动文件清单；现有证据只恢复了接口范围与验证结论，未逐文件枚举实现改动。

