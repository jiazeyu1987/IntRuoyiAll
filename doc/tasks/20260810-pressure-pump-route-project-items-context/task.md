# 20260810-pressure-pump-route-project-items-context

## Task Goal

修复一线 PQC 切换到“按压式球囊扩张压力泵”订单时报错：设备账号上下文不完整或不一致：routeProjectItems routeId=980091，missingItemIds=[14]。截图中工艺路线和 QA 检验规程均可见时，不应因项目级路线项目完整性校验阻断订单选择或工序进入。

## Milestones

- [ ] 定位 routeProjectItems missingItemIds=[14] 的校验来源和受影响接口。
- [ ] 补充可复现该错误的回归测试，先形成 RED。
- [ ] 移除“截图可找到时仍阻断”的不合理限制，保留真实缺配置的失败语义。
- [ ] 运行定向回归验证并记录结果。

## Expected Verification

- 定向后端回归测试覆盖 routeProjectItems 项目级缺口不再阻断一线 PQC 压力泵工序。
- 相邻一线设备账号/QA/PQC 选择路径不出现默认成功、吞异常或 fallback。
- 技能 evidence validator 通过。

## Current Status

in_progress

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，目标是移除错误的上下文一致性阻断，而不是用前端隐藏或空成功绕过。
- 是否存在临时补丁或绕过：否。

## Applicable Experience Gates

- MES 一线设备账号权限门禁：压力泵全工序授权必须区分标准权限与岗位/工作站绑定，不能用岗位缺失或空结果掩盖权限链路。
- PQC 待检准入与工序选择必须分离：已配置 QA 规程、路线产品绑定、待检工单和工序卡片必须按正式链路判断，不能让非必要项目级快照缺口阻断可见工序。
- QA 多工序正式发布与退役夹具唯一键必须隔离：QA 质检工序承载与业务工序、路线工序身份要分清，不能用批记录或表单槽位替代。
