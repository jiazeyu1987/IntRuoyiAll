# G67 关联整改通知代码复核

HTML v1.6 第9条要求关联整改负责人收到通知。正式发送器已提交 sourceControlledFileId、relatedMasterId、dueAt 和只读 detailUrl，现有消息解析器却没有对应导航分支。此次只补已有消息组件的准确来源入口，不新增流程、队列或权限。

Root 已复核实际发送载荷与前端专用解析分支：精确模板编码，Long 字符串身份，完整编号/版本/合法期限，同源且准确来源路径，唯一 viewer=1/from=notification；错误载荷不转为其他审批导航。消息按钮关闭弹框后进入只读详情，服务端原权限继续有效。关联 Master 不被错误当作文件版本 ID。

有效 RED 3项中2项失败；最终17项回归、2个生产文件 lint、完整类型检查及任务独立目录构建均通过，源码冻结指纹与 g67-relation-remediation-notification-fingerprints.json 一致。这里证明当前源码行为，尚不宣称真实收件人通知点击通过；该链路由 Root 后续真实页面验收。

原 AGENTS.md 和两个 infra 文件 raw SHA 与本任务既有基线一致，未纳入此次提交。主任务仍 in_progress；G67 升版关联冻结修复和真实多账号验收继续进行。
