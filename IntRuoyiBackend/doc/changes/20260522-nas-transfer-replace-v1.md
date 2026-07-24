# Change Request：NAS转移遇到已有V1.0时删除旧版再导入

## Request Summary And Source

- Request: 通过 NAS 转移的文件统一是 `V1.0`；如果已有 `V1.0`，先删除原来的 `V1.0` 版本，然后再转移
- Source: user request on 2026-05-22 in current delivery thread

## Current Baseline Reviewed

- Baseline feature task:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-dcc-nas-transfer-controlled-files-backend\task.md`
- Current runtime behavior before this change:
  - NAS 转移已不走审批且不走分发/培训
  - `PD可编辑` 目录第一次真实导入成功，4 条文件进入 `ACTIVE / V1.0`
  - 第二次导入同一目录时命中 `Controlled file version must be greater than the current chain version`

## Classification

- Requirement change
- Active backend behavior adjustment

## Impact

- Product impact:
  - NAS 转移重复导入同一受控文件时改为“替换现有 `V1.0`”，不再报版本冲突
- Design impact:
  - 仅对 `submitControlledFileWithoutApproval(...)` 生效
  - 普通手工上传仍保持版本递增规则
- Data impact:
  - 旧 `V1.0` 通过逻辑删除移出当前链
  - 新导入文件继续保持 `V1.0`
- API impact:
  - `POST /admin-api/dcc/controlled-files/nas-transfer` 响应结构不变
- Test impact:
  - 新增无审批提交替换旧 `V1.0` 的回归用例
  - 需要做真实重复转移验证

## Decision

- Accept

## Required Approvals

- User approval already provided in-thread

## Downstream Skill Reruns

- `backend-api-delivery`

## Blockers And Next Action

- Blocker:
  - local OnlyOffice service is still unavailable,但不阻塞本次 NAS 重复转移规则
- Next action:
  - 实现 NAS 无审批提交前的旧 `V1.0` 链替换
  - 验证重复转移 `PD可编辑` 成功，返回 `createdFileCount=4`
