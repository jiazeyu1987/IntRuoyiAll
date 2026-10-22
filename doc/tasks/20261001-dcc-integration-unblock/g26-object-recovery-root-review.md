# G26 三个缺失对象恢复的主管理 Review

状态：工具及实际只读前置已通过；对象写入等待用户对以下具体范围授权。此授权不同于已执行的19项数据库升级。

## 已核实范围

四个历史infra_file ID 9198354931064、9198354931068、9198354931079、9198354931095的当前记录、配置、完整原文件名、源哈希和字节数已只读核对。它们对应三个唯一object key，后两项共用同一对象。原件候选均来自本地保留附件，SHA/字节数与原记录完全相同，三个正文共111112字节。

实际39源文件核验已有35项匹配，剩余4项确为404 NoSuchKey；不是哈希错误或权限错误。方案不修改任何数据库引用、历史名称、编号、版本、Master或签名。

## 工具检查

恢复工具77项离线测试、独立只读probe34项离线测试通过。真实SDK marshalling证明发送If-None-Match:*、正确校验和及正文bytes；无copy/delete/multipart/策略修改。凭据只进程内/STDIN，输出为受限状态/ID/hash/字节数，服务器异常文本不会输出。

全部三个key的实际GET前置必须都是404 NoSuchKey，才开始最多三次条件创建。已存在对象、delete marker、412/409、条件不支持均停止；网络超时/5xx视为写入结果不确定，不重试。跨三个对象没有事务，若部分成功则保留真实结果；没有自动删除“回滚”。成功后再次读取四条引用，再用原39项独立只读脚本验证全量。

Root校验源文件、41编译产物及42依赖全部指纹、精确目录库存，没有加载其它未封存文件。准备工具/编译测试不是实际恢复。

## 实际存储前置

本机现有docker-minio-1是Silo RELEASE.2026-08-06T00-00-00Z，commit3be10fcc1a44f6620ded0bd303461f9d688cca23，真实启动仅一个本地/data存储端点。工具固定config28、本机9000、bucket指纹、三key指纹、四源ID/正文哈希。当前fork对应源码具有对象锁内If-None-Match条件检查；实际运行仍须按真实响应验证，不撤销条件头。

Root已实际执行两次bucket只读SDK调用：版本化状态UNSET（HTTP200），Object Lock未配置（HTTP404 ObjectLockConfigurationNotFoundError），没有默认retention。这是当前事实；应用配置中的7天/hold声明不等于bucket已启用。本恢复保持bucket策略原样，不设置或删除retention/hold，不宣称完成20年WORM保护。

应用服务尚未启动，实际MySQL事务/其它客户端/事件写入检查通过；恢复执行前再核对当前应用进程/单pool配置，不启用他人服务、不重建容器。

## 授权后的执行

Root准备输入内存载荷，重新读取并核对当前记录；新受保护运行目录保存无凭据的摘要/许可引用/指纹/安全结果。最多3次条件PUT后4条引用独立验证，随后全39项只读校验。遇到冲突/拒绝/不确定写结果立即停止，说明已产生影响；不换文件、不更改权限、不覆盖现存对象。

此范围不包含新增三张名称占用表、登记历史占用证据、25项审计启用、质量批准或前端业务动作。那些项按各自Review/用户条件执行，不把恢复当业务E2E。

方案：[G26精确恢复方案](C:/IntRuoyi/20261001-dcc-integration/doc/tasks/20261002-dcc-public-backend-completion/g26-exact-object-recovery-plan.md)。实际前置证据在保护目录g26-bucket-policy-probe、g26-silo-topology-readonly.json、g26-four-source-object-identity.jsonl及g25-missing-source-recovery-candidates.json。
