# Task: 修复审批中心已办页系统异常

## Task Goal

修复审批中心左侧进入“已办”页签后列表区域显示“系统异常”的问题，确保 DONE 视图能按正式审批中心接口正常加载或显示空态，不通过前端隐藏错误或后端空成功掩盖 provider 异常。

## Milestones

- [ ] M1: 定位“已办”页系统异常的前后端根因与受影响 provider。
- [ ] M2: 先补充可复现该问题的最小回归测试并记录 RED。
- [ ] M3: 实施最小正式修复，不引入 fallback、吞异常或默认成功。
- [ ] M4: 运行定向 GREEN 与相邻回归验证，确认 DONE 页不再触发系统异常。
- [ ] M5: 更新验证报告和收尾状态。

## Expected Verification

- 静态或单元回归测试先 RED 后 GREEN，覆盖 `/approval-center/done` 或 `viewType=DONE`。
- 运行受影响审批中心前端静态契约或后端定向 Maven 测试。
- 若本地前后端运行态和登录前置可用，再通过真实页面路径验证“已办”页不显示“系统异常”。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：待确认根因后更新。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- 待读取 `docs/experience-index.md` 后补充命中的审批中心/前端/后端门禁。

## Current Status

in_progress

