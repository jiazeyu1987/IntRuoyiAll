# Task: 修复审批中心已办页系统异常

## Task Goal

修复审批中心左侧进入“已办”页签后列表区域显示“系统异常”的问题，确保 DONE 视图能按正式审批中心接口正常加载或显示空态，不通过前端隐藏错误或后端空成功掩盖 provider 异常。

## Milestones

- [x] M1: 定位“已办”页系统异常的前后端根因与受影响 provider。
- [x] M2: 先补充可复现该问题的最小回归测试并记录 RED。
- [x] M3: 实施最小正式修复，不引入 fallback、吞异常或默认成功。
- [x] M4: 运行定向 GREEN 与相邻回归验证，确认 DONE 页不再触发系统异常。
- [x] M5: 更新验证报告和收尾状态。

## Expected Verification

- 静态或单元回归测试先 RED 后 GREEN，覆盖 `/approval-center/done` 或 `viewType=DONE`。
- 运行受影响审批中心前端静态契约或后端定向 Maven 测试。
- 若本地前后端运行态和登录前置可用，再通过真实页面路径验证“已办”页不显示“系统异常”。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按标准 BPM done-page 对历史任务空状态的既有读取口径修复统一审批中心 DONE mapper。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- `docs/frontend-development.md#前端主查询错误重复提示门禁`：主查询错误不得通过空数据或隐藏 alert 掩盖；本次修复后端根因，未删除页面错误展示。
- `docs/backend-development.md#统一审批中心-bpm-已办历史状态门禁`：统一审批中心 DONE 视图遇到 legacy `TASK_STATUS=null` 时保留历史任务行并让 `approvalResult` 为空，非空未知状态仍 fail-fast。
- `docs/powershell-memory.md#PowerShell-Maven--D-参数引号门禁`：Maven `-Dtest` 与 `-Dsurefire.failIfNoSpecifiedTests=false` 均整体加引号执行。

## Cleanup Keep

- doc/tasks/20260804-approval-center-done-system-exception/bug-regression-evidence.md

## Current Status

ready_for_closeout

