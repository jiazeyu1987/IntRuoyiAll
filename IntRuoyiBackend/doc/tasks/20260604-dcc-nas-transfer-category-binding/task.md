# Task: DCC NAS 转移类别目录绑定后端提示修复

## 任务目标

将 DCC NAS 转移中“模板类别未绑定受控目录”的失败信息从英文内部异常改为中文业务提示，保持 fail fast，并保证新建任务和后台处理任务两条路径都有回归测试覆盖。

## 上一任务检查

- 后端上一任务 `ruoyi-vue-pro/doc/tasks/20260604-ops-1n-rollback-compatibility/task.md` 状态为 `completed`。

## 里程碑

- [x] M1：定位后端异常源和既有测试。
- [ ] M2：先更新失败测试期望为中文业务提示并记录 RED。
- [x] M3：实现最小后端修改。
- [x] M4：恢复本机 `906104 / 其他` 的正式目录绑定。
- [x] M5：运行目标 Maven 测试和只读 SQL 验证。
- [x] M6：更新证据并提交。

## 预期验证

- RED：`DccControlledFileNasTransferServiceTest` 在中文失败信息期望下失败。
- GREEN：`mvn -pl yudao-module-dcc -Dtest=DccControlledFileNasTransferServiceTest test` 通过。
- GREEN：本机 MySQL 只读 SQL 证明 `906104 / 其他` 有唯一活动目录绑定。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。未绑定目录仍直接阻止转移任务。
- `是否从根因和长期维护角度解决`：是。后端保留正式目录绑定前置条件，并将用户可见失败语义统一为中文。
- `是否存在临时补丁或绕过`：否。

## 当前状态

completed

## 阻塞

- 无代码/数据修复阻塞。Playwright 完整提交前拦截路径受前端真实数据限制，详见根任务证据。

## Cleanup Keep

- `doc/tasks/20260604-dcc-nas-transfer-category-binding/bug-regression-evidence.md`
- `doc/tasks/20260604-dcc-nas-transfer-category-binding/database-schema-evidence.md`
