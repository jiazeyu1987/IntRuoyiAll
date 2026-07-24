# Change Request：NAS转移开始前增加确认弹框

## Request Summary And Source

- Request: 开始转移前先弹框确认
- Constraint: 有的目录下存在 `10000+` 子文件夹和子文件，确认前不能做全量递归统计，避免卡顿
- Source: user request on 2026-05-23 in current delivery thread

## Current Baseline Reviewed

- Current page: `src/views/system/nas/index.vue`
- Current flow:
  - user opens `转移到 DCC` dialog
  - clicking `确认转移` directly sends API request
- Current page does not add a last-step confirm prompt

## Classification

- Requirement change
- Frontend interaction change

## Impact

- Product impact:
  - transfer action gains an explicit final confirmation gate
- Design impact:
  - confirm dialog must avoid descendant pre-scan and only summarize selected root directories
- API impact:
  - none
- Test impact:
  - update NAS management static test coverage

## Decision

- Accept

## Required Approvals

- User request is explicit

## Downstream Skill Reruns

- `frontend-feature-delivery`

## Blockers And Next Action

- Blocker:
  - none
- Next action:
  - add preflight confirm prompt before actual transfer request
  - explicitly state that no full subtree counting is done before confirmation for performance
