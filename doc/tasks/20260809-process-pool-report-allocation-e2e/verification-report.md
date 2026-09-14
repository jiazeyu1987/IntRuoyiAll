# Verification Report

## Summary

- 目标：事件 `176`，提交时间 `2026-08-07 13:30:38`，员工“刘悦悦”，工序“清洗工序”，完成数量 `411111`。
- 真实入口：`http://127.0.0.1:8081/mes/pro/process-pool/production-leader`，身份标签 `芋道源码/admin`，后端 `http://127.0.0.1:48081` health `UP`。
- Playwright 通过页面切换到“报工管理”，按页面可见业务字段唯一定位目标行，分别执行 FIFO 自动分配和手动分配。

## FIFO Result

- 请求：`POST /admin-api/mes/pro/process-pool/team-leader/submission/allocation/preview-fifo`，`eventId=176`，`leaderType=PRODUCTION`。
- 响应：HTTP 200，业务码 `1040760313`，提示“活跃订单当前工序剩余数量不足，无法确认分配：176”。
- 页面结果：未生成任何 FIFO 分配行。
- 正式数据：7 个有效活跃订单在事件当前工序均缺 `planned_quantity_snapshot`，所以 FIFO 正式剩余量合计为 `0`。其 ERP 固定数量合计仅 `2398`，也小于本次待分配数量 `411111`。

## Manual Result

- 页面选择：活跃订单 `activeOrderId=35`，订单 `CODX-AO5-20260807-01`，订单数量 `10`；手动填写分配数量 `411111`。
- 请求：`POST /admin-api/mes/pro/process-pool/team-leader/submission/allocation/confirm`，模式 `MANUAL`。
- 响应：HTTP 200，业务码 `1040760326`，提示“报工确认缺少唯一正式 PQC 结构化绑定，eventId=176”。
- 判定：手动确认在订单剩余量校验之前被 PQC 质量门禁拦截，不能用该响应证明手动数量校验通过或失败。
- 正式数据：同工单/工序有 13 个 PQC 候选事件，但 `production_submit_event_id=176` 的正式 PQC 结构化记录为 0 条。

## No-Write Verification

- E2E 前后 `review` 追溯均为 `REVIEW_MISSING`。
- E2E 前后 `allocation` 追溯均为 `ALLOCATION_MISSING`。
- 事件 `176` 仍为 `PENDING`，页面仍显示“分配”按钮。
- `pageErrors=[]`，目标请求失败数为 0；本次负向验证没有产生分配记录或审核终态。

## Conclusion

- `411111` 是本次报工需要分配出去的数量，不是活跃订单可提供的剩余量；数字越大，越容易超过订单容量。
- 当前 FIFO 的直接数据根因是目标工序计划快照缺失，正式可分配量为 0。
- 手动确认还存在更早的独立阻塞：事件 176 缺唯一正式 PQC 结构化绑定。
