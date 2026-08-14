# 20260806-frontline-pqc-login-employee-lock

## Task Goal

修正一线 PQC 填写页：生产订单必须来自活跃订单池；工序必须来自所选活跃订单对应工艺路线；员工必须固定为当前登录的 PQC 员工或 PQC 组长本人，只显示本人姓名，不允许页面或接口切换为其他 PQC 人员。

## Milestones

- [x] M1: 固定 BDD/TDD 验收口径并补充前后端 RED 合同。
- [x] M2: 前端锁定 PQC 员工展示与初始化，不再暴露 PQC 员工切换入口。
- [x] M3: 后端收紧 PQC 人员与切换接口，禁止请求切换为非登录人。
- [ ] M4: 运行前端静态合同、后端目标测试与必要回归。后端目标测试被既有 MES 编译错误阻塞，尚未进入测试阶段。
- [x] M5: 更新验证与收尾记录。

## Expected Verification

- `node tests/e2e/mes-frontline-pqc-login-employee-lock-static.spec.cjs`
- `node tests/e2e/mes-frontline-pqc-active-order-switching-static.spec.js`
- `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `git diff --check`

## Current Status

blocked

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。本任务要求缺少登录人正式 PQC 人员身份或非法切换时 fail fast。
- 是否从根因和长期维护角度解决：是。前端移除切换入口，后端同时校验登录人与实际员工一致。
- 是否存在临时补丁或绕过：否。

## Experience Gate

- `docs/frontend-development.md#前端静态契约隔离门禁`：本任务需新增专用静态合同先 RED 后 GREEN，不能用无关全量失败或截图目测替代当前需求证明。
- `docs/backend-development.md#mes-pqc-项目级检验快照门禁`：PQC 提交和展示必须继续沿用发布 QA 规程与结构化 `itemResults`，本任务只收紧员工身份，不改项目级事实来源。
- `docs/backend-development.md#mes-一线设备账号权限门禁`：一线运行态不能用前端放行或空列表成功掩盖权限/身份链路，后端必须按登录用户正式校验。
- `docs/powershell-memory.md#同文件并行改动选择性暂存门禁`：当前工作区有大量既有脏改动且目标文件已被并行任务修改，后续提交如需执行必须选择性暂存本任务 hunks。
- `docs/powershell-memory.md#powershell-maven--d-参数引号门禁`：Maven 目标测试中的 `-Dtest` 与 `-Dsurefire.failIfNoSpecifiedTests=false` 必须整体加双引号。
