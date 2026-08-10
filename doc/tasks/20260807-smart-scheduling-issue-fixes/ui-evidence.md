# 真实页面验证证据

## 环境

- 前端：`http://127.0.0.1:8081`
- 后端健康：`http://127.0.0.1:48081/actuator/health` 返回 `UP`
- 页面：`/mes/pro/schedule-order`
- 视口：桌面端 1440x1000；移动端 390x844
- 操作边界：只读浏览、筛选查询和弹窗查看；没有执行强制完成、冻结、交期或重排写操作。

## 筛选一致性

| 选择值 | 正式请求参数 | 返回总数 | 可见行状态 |
| --- | --- | ---: | --- |
| 可入池 | `READY_TO_ADMIT` | 5 | 全部 `READY_TO_ADMIT` |
| 已入池 | `ALREADY_ADMITTED` | 13 | 全部 `ALREADY_ADMITTED` |
| 阻断 | `BLOCKED` | 1529 | 全部 `BLOCKED` |

- 草稿从“可入池”改为“阻断”但未点击查询时，请求数为 0，列表仍保留 5 条可入池结果，并显示“筛选条件待应用”。
- 点击查询后，请求携带 `admissionStatus=BLOCKED`，返回行状态全为 `BLOCKED`。

## 排产工单 UI

- 桌面端 20 条可见行：开工风险 6 条、承诺交期风险 7 条、不可重排 8 条、可重排 12 条、当前工序缺失提示 4 条。
- 第一条禁选记录直接显示“不可重排 / 已冻结”，辅助技术标签为“不可重排：已冻结”。
- 当前工序缺失 tooltip 支持悬停和键盘聚焦，宽 360px、高 72px，长文自动换行。
- 当前用户个性化隐藏了生产用料清单列，因此未通过写入用户列配置来强行展示；该提示由静态合同覆盖。
- “完成状态”筛选名称可见；“排产工单强制完成”弹窗明确说明强制关闭、汇总 100%、真实工序进度保留和可撤销。
- 移动端 body 宽度与视口一致，无页面级横向溢出；表格使用自身滚动，固定重排状态列保持可读。
- 打开强制完成弹窗后点击取消，目标写请求数为 0。

## 截图索引

- `IntRuoyiFronted/output/playwright/20260807-smart-scheduling-issue-fixes/desktop-schedule-order.png`
- `IntRuoyiFronted/output/playwright/20260807-smart-scheduling-issue-fixes/desktop-blocked-reason.png`
- `IntRuoyiFronted/output/playwright/20260807-smart-scheduling-issue-fixes/desktop-current-process-tooltip.png`
- `IntRuoyiFronted/output/playwright/20260807-smart-scheduling-issue-fixes/desktop-sync-draft-pending.png`
- `IntRuoyiFronted/output/playwright/20260807-smart-scheduling-issue-fixes/desktop-force-finish-dialog.png`
- `IntRuoyiFronted/output/playwright/20260807-smart-scheduling-issue-fixes/mobile-schedule-order.png`

截图不作为唯一放行依据；请求参数、返回状态、可访问属性和目标写请求数均通过 Playwright DOM/网络证据核对。
