# G19 最终 Review 增量

当前状态 in_progress；数据库升级审批仍pending。本轮有代码/有效测试/静态审查进展，不累计整目标impasse。

## 业务修复与证据

- 项目属性 Long/context：detail_closure正式交付精确字符串、unsafe/重复/停用/外项目拒绝、关闭/换项目/卸载/ABA晚响应隔离；原属性回归、SFC/lint通过。Reviewer组件唯一归属upload，Root已解决原分派文字冲突，未覆盖并行成果。
- 审核人配置：upload_closure真实userStore doc_control+update permission+tenant/actor，取消零写、失权/改人/ABA/晚响应隔离。配置真实后端权限保持。
- 本机Quartz启动：backend_closure startup-sync-enabled条件（默认true）有效上下文RED→GREEN，7项PASS；Root再次定向7项及主应用compile PASS。任务运行可以关闭全库启动同步，不改变其它正常runtime默认行为。
- 详情→检出/检入：普通management详情增加真实存储操作入口，使用正式所选file/Master/目录/编号；既有返工入口复用。缓存页面查询切storage，路由更新保留目标identity，实际授权列表/历史同时匹配才选中。较早WORKING不自动改成ACTIVE；missing/unsafe/mismatch明确失败，不API代办。
- 导航有效RED：原返工handler没有identity、旧browser默认project、初始选版改成ACTIVE；普通详情缺入口、缓存query缺切换和路由丢身份另有RED。九项最终GREEN。旧尝试两项测试写法曾被移除；现在按真正缺口重新记录有效RED并完成修复，不称不存在入口已完成。
- 文案：controlled-file浏览/详情/基础信息/摘要/出版后续/日志及打印件文案按受控版本统一；WORKING与法规证书有效期概念保持。旧术语测试禁止工作版本与“最新待提交”要求不符合用户当前确认，已更新合同。最新版如果尚未受控仍显示“最新版本”，不误报受控。

Root最终受影响组合148/16文件PASS（g19-navigation-final-regression.log），正式types exit0（72902）、定向lint exit0，后端560/11类PASS（50381）。这些为离线/隔离测试，不是实际页面通过。当前最后Jar/FEbuild收据另核实源指纹；实际数据库backup/writer只读结果保留。新实现仍在integration，int_qms没有合入中间代码。

## 本机真实验收准备

只读查得AGENTS测试账号admin tenant1为启用，有doc_control/electronic_signature_admin等实际角色；其签名授权ENABLED、图片第7版ACTIVE。文件配置主S3端点127.0.0.1:9000，现有NAS/存储配置host127.0.0.1；未改配置或输出凭据、签名图片。此结果仅前置事实，仍须真实登录/UI/实际文件存储与签名验证，不能由SQL记录代替。

MySQL当前events查询空、活动事务0，前次Java清单空。只读采样不永久证明无writer，演练/原库升级前必须重查。审批未答，没有create schema、restore、DDL/DML、服务启动、E2E或Git操作。
