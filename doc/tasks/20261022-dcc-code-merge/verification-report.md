# 验证报告

## 合并范围
- 最终分支：int_qms；第一父节点：a9a149c721b679dc70de3fcf6f87aa9eabf6cd0e。
- 输入主干：origin/int_main，b1f310b74。已成功 fetch，旧网络阻塞解除。
- 旧主干 13 个冲突现场已保存在主工作区 output/20261022-dcc-code-merge/old-merge-backup；按最新主干重做后仅 1 个权限文件冲突。
- 用户明确选择最新主干超级管理员 OWNER 规则，保留 QMS 的启用项目过滤、上传候选状态、缓存路由保护和默认属性提示。
- 修正 Long ID 单测的旧上传凭证字段为主干正式字段 readOnlyUploadTicket。

## 实际验证
- PASS：前端上传定向单测 25/25；包括缓存路由、项目产品身份、Long ID、A32、上传候选。
- PASS：后端定向单测 36/36，失败/错误/跳过均为 0；DccAdminProjectVisibilityTest、DccProjectAccessServiceImplTest、DccW01ProjectAccessIdentityTest、DccProjectAttributesBaselineContractTest、DccProjectUploadOptionsServiceTest。
- PASS：pnpm ts:check（8 GiB heap）。依赖锁文件未变，复用本机 node_modules。
- PASS：菜单迁移检查 147 项。
- PASS：隔离 worktree 端口检查，int_main profile slot 1，8082/48082；未启动服务。
- PASS：相对最新主干的本次 DCC 差异 whitespace 检查，无残留冲突。
- 记录：合入的主干既有 ERP/MES 等文件含空白告警；未批量格式化主干既有内容。
- PASS：mvn -q -pl yudao-server -am -DskipTests -Dfile.encoding=UTF-8 package，退出码 0。
- PASS：本机 int_qms 已快进到合并结果，a9a149c72 与 b1f310b74 均为其祖先；主工作区端口保护通过（8061/48061）。
- PASS：最终任务资产核对完成，仅本任务三份文档进入收尾；原有两个运行日志保留为未提交改动。
- 隔离 worktree C:/IntRuoyi/codex-merge-int-main 保留用于复查构建结果，int_main profile slot 1（8082/48082）继续登记，未运行服务；旧冲突和工作区任务草稿保存在 output/20261022-dcc-code-merge。

## 验证边界
未执行 E2E、数据库写入、服务重启、远端推送；未重新运行全部历史测试。主工作区无关日志和未跟踪资产予以保留。
