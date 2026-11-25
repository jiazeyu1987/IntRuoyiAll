# 共享解析与批量路线升级验收

验收范围以用户最新要求为准：当前关闭审批，按关闭审批流程验证，不开启全租户审批，不再将启用审批的真实BPM流程作为本次完成前提。保留审批模式实现及已通过的服务测试，不宣称完成真实BPM端到端验收。

| 需求 | 已检查证据与结论 |
| --- | --- |
| 解析工序和参数，展示车间候选 | shared-parser-independent-identical-match.txt、shared-parser-different-parameters.txt；真实Word解析得到5项参数，候选显示车间1/车间2、工序编码。解析匹配6项、发布35项测试通过。 |
| 同名同参数提示复用；明确不复用则独立创建 | shared-parser-independent-reuse-preview.txt、shared-parser-independent-published.txt、shared-parser-independent-proof.json；复用预览选择GY0065，不复用实际建立独立工序与GY0066，参数完全一致、身份不同。 |
| 同名不同参数复用工序、新建工艺 | shared-parser-new-craft-proof.json及对应真实UI；GX0002保持，建立GY0065并发布路线V2。 |
| 各路线明确使用哪套参数 | shared-route-specific-comparison-ui.txt（PASS）和shared-parser-independent-proof.json；A/B用GY0063，C用GY0065，同工序但独立工艺指纹与参数。多工艺11项测试通过。 |
| 修改工序、工艺、设备先展示影响及二次确认 | shared-craft-impact.txt、shared-device-impact.txt、shared-workstation-replace-success.txt及实际变更日志；显示车间、受影响路线、版本与前后差异，填写原因后统一提交。 |
| 换工序、工艺、设备统一升级引用路线 | shared-workstation-publish-proof.json、shared-device-replace-proof.json、shared-device-update-proof.json及对应UI；两条任务路线实际批量发布，换工序后工作站工序一致，历史工作站未覆盖。 |
| 关闭审批后直接执行、无需逐条发布 | 本轮只读shared-direct-publish-proof.json：8个已生效变更，16个关联请求全部DIRECT_EXECUTED、无流程实例；对应路线版本ACTIVE或已被后续版本替代。两条当前策略均DIRECT。 |
| 变更日志与发布结果 | 本轮shared-direct-final-log-ui.txt（PASS）：修改前后、原因、2条路线V11→V12及已发布状态可见；数据库保留操作者、时间与版本关联。 |
| 整批失败不局部生效、并发及重复处理 | MesProCraftGroupTransactionTest 15项、MesProSharedApprovalTransactionTest 7项，覆盖第二条发布失败/审计失败回滚、重复回调、取消失败等；属于事务测试，不称真实BPM验收。 |
| 历史版本与执行参数保持冻结 | shared-device-update-v11.txt/v12.txt、shared-device-update-proof.json、shared-history-identity-after.txt；旧名称、参数与设备保持。活跃订单按routeVersionId冻结，运行配置读订单冻结参数；15项一线配置测试覆盖现有规则变更后仍读原参数。发布投影不写已完成报工payload。 |
| 友好交互与交付检查 | 实际页面覆盖复用确认、差异预览、批量确认、历史参数、日志；前端vue-tsc通过，菜单迁移检查通过。已修复返回路线页面缓存和版本列表竞态。 |

说明：5000路线中识别1000引用由服务测试验证，未进行1000路线真实发布压测。旧设备历史迁移仅建立明确标记的当前资料基线；缺失原设备资料的既有异常版本不能恢复当时名称，保持明确报错。没有新增历史工序步骤浏览入口，不将变更快照等同此入口。上述边界不伪装为已验收功能。

本轮恢复任务自有服务8506/48506，后端PID35924、前端PID74012；运行包SHA256与已验收包80EF7C5FA53E2704E1BEB4048DD4A96EB109AEE5E8181DF2C6C20B94217D787D一致。无审批策略写入、无新业务变更、无提交推送。
