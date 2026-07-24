# 任务：电子签名治理自动回填后端支持

## 任务目标

如现有 API 无法提供真实签名治理候选，则新增只读候选接口，为前端自动回填 ID、Hash、对象 Key、版本和证据引用提供真实数据来源。

## 里程碑

- [x] M1：确认现有接口是否满足候选回填。
- [x] M2：补充后端 RED 测试，覆盖候选接口真实数据字段。
- [x] M3：复用现有签名分页只读接口并补齐回填字段，不改变签名记录、授权、留存、CSV 写入 API 结构。
- [x] M4：运行后端相关测试。

## 预期验证

- 后端候选 API 单元/契约测试。
- 前端真实 E2E 使用该接口加载候选。

## 当前状态

已完成。

## 经验门禁

- `docs/login-access.md`：本次真实验证仅使用本机测试租户。
- `docs/experience-index.md`：高风险真实 E2E 前必须记录 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，提供真实候选数据来源，避免人工编写审计字段。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 查询电子签名治理候选 -> Given 当前租户存在真实 DCC 签名记录 / When 前端请求治理候选 / Then 后端返回签名 ID、业务 Key、版本、哈希、动作和含义字段，不返回伪造数据。`

## Cleanup Keep

- `doc/tasks/20260624-signature-governance-auto-fill/task.md`
- `doc/tasks/20260624-signature-governance-auto-fill/execution-log.md`
- `doc/tasks/20260624-signature-governance-auto-fill/backend-api-evidence.md`
