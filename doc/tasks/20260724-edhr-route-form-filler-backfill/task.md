# Task: eDHR 动态表单任务填写人回填

## Task Goal

修复批次执行详情中动态表单任务 `fillableUsers` 为空的问题：当工艺路线工序绑定的损耗单、过程检验单、参数记录表等动态表单已配置填写人时，`/mes/pro/edhr-batch-execution/get` 返回的对应任务必须带出该单据级填写人，右侧单据卡片才能显示真实填写人。

## Milestones

- [x] M1: 定位现有任务生成、详情组装和填写人解析逻辑。
- [x] M2: 记录 BDD 场景并新增 RED 回归测试。
- [x] M3: 最小化修复后端详情任务 `fillableUsers` 回填。
- [x] M4: 运行目标 Maven/静态验证并记录 GREEN 证据。

## Expected Verification

- RED: 目标后端测试在修复前失败，证明路线绑定有填写人但动态表单任务 `fillableUsers` 为空。
- GREEN: 目标后端测试通过。
- GREEN: 后端 API 证据与 bug regression 证据校验通过。

## Current Status

ready_for_closeout

Implementation and required verification passed. Closeout cleanup and final evidence consolidation remain.

## Experience Gate

- `docs/experience-index.md`：缺失。
- Gate decision: 本次为本地后端只读详情接口修复，不涉及远端服务器、发布、数据库结构变更或生产数据写入；记录缺失但不阻塞实施。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，修复执行任务填写人来源映射，而不是前端推断。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- `doc/tasks/20260724-edhr-route-form-filler-backfill/backend-api-evidence.md`
- `doc/tasks/20260724-edhr-route-form-filler-backfill/bug-regression-evidence.md`
