# 任务：NAS转移遇到已有V1.0时删除旧版再导入

## Goal

在当前 `DCC NAS转移` 后端中新增一条仅对 `NAS转移` 生效的规则：

- 当目标类别下已存在同一文件的 `V1.0` 版本时
- 先删除原来的 `V1.0`
- 再重新按固定 `V1.0` 导入

本任务不改变普通手工上传的版本递增规则。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-dcc\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-nas-transfer-replace-v1-conflict-backend\**`

## Non-Scope

- 不修改前端页面
- 不修改普通手工上传接口的版本规则
- 不引入自动升版到 `V2.0`

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-dcc-nas-transfer-controlled-files-backend\task.md`
- Status before this task: `Completed on 2026-05-22`
- Impact: 上一任务已实现 NAS 转移直发受控文件，并确认 `PD可编辑` 已导入 4 条 `ACTIVE / V1.0` 文件；本任务在此基础上处理重复转移冲突。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在 MES / showroom 等无关用户改动
- Impact: 本任务只修改 DCC 后端与本任务文档，避免混入其他功能域

## Milestones

- [x] M1: 识别上一任务完成状态并确认新需求范围
- [x] M2: 记录 BDD 与 RED，锁定“删除旧V1.0再导入”的行为规则
- [x] M3: 实现仅对 NAS 转移生效的旧版V1.0替换逻辑并补测试
- [x] M4: 跑定向测试、真实接口验证、证据校验和 closeout preview

## Expected Verification

- `mvn -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileNasTransferServiceTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260522-nas-transfer-replace-v1-conflict-backend/backend-api-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-nas-transfer-replace-v1-conflict-backend --mode preview`

## Current Status

Completed on 2026-05-22. 已完成实现与真实验证：`PD可编辑` 在已有 4 条 `ACTIVE / V1.0` 文件的情况下，再次重复执行 NAS 转移时，后端会先删除旧 `V1.0` 链，再成功重新导入 4 条新的 `V1.0` 文件。

## Blockers And Impact

- Blocker: none
- Impact:
  - 本任务是纯后端规则调整，可以继续实现与验证
