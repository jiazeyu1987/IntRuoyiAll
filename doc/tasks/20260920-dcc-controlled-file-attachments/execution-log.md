# Execution Log

## BDD

- BDD: upload controlled file with additional attachments -> Given 用户已选择受控文件源文件/图纸 PDF 并额外选择多个普通附件, When 提交受控文件, Then 后端保存普通附件集合并与源文件、图纸 PDF 角色隔离。
- BDD: view controlled file attachments -> Given 受控文件版本存在普通附件, When 用户打开查看/详情页面, Then 页面展示普通附件列表并可打开查看附件。
- BDD: no implicit attachment fallback -> Given 受控文件没有普通附件但有源文件、图纸 PDF 或关联受控文件, When 查询附件列表, Then 返回空普通附件集合，不从其它文件角色推断补齐。

## Activity

- in_progress: 创建任务目录并记录需求、BDD、设计约束。
- red: 新增后端/前端附件静态合同测试，确认现有代码缺少独立普通附件上传、提交和查看合同。
- green: 新增 `dcc_controlled_file_attachment` 持久化、ATTACHMENT 上传 purpose、提交绑定、详情回显和附件预览接口。
- green: 上传页增加多普通附件上传控件，提交时带 `attachmentUploadTickets`；详情页增加普通附件列表和在线查看弹窗。
- verification: `node tests\e2e\dcc-controlled-file-attachments-static.spec.cjs` PASS。
- verification: `pnpm ts:check` PASS。
- verification: `git diff --check` PASS，仅输出既有 CRLF/LF 换行提示。
- blocked_tooling: `python` 指向 WindowsApps 占位启动器，无法运行 Python 静态测试；`mvn` 不在 PATH 且仓库无 Maven Wrapper，后端 Maven 编译未能在当前环境执行。
- ready_for_closeout: 已完成静态合同、前端类型检查和空白检查。
- completed: 按当前可用工具完成实现与验证；未执行 E2E，因为用户本轮未明确要求。
- in_progress: 用户明确授权 E2E 验证，并授权重启 `int_main` 后端 `48081` 与执行本次附件表迁移。
- verification: 使用仓库内 Maven/JDK 完成 `mvn -pl yudao-server -am -DskipTests package`，新 Jar 构建成功。
- verification: 执行 `20260920_dcc_controlled_file_attachment.sql`，只读核验确认 `dcc_controlled_file_attachment` 表存在。
- blocked: 新构建 Jar 启动到 Spring 上下文阶段失败，根因是无法解析 `MES_EDHR_INDEPENDENT_RECEIPT_ISSUER_SYSTEM`；`application-local.yaml` 同时要求 `MES_EDHR_INDEPENDENT_RECEIPT_SIGNING_SECRET`。
- blocked: 尝试恢复先前 `48081` 运行 Jar，同样被缺失 MES 独立接收单运行配置阻塞；进程/User/Machine 环境、仓库可见配置和 `D:/ProjectPackage/Int/IntPP/backend/.env` 均未找到授权值。
- blocked: 按规则未猜测或生成 MES 签名配置，Playwright E2E 未执行；当前 `48081` 未监听。
- in_progress: 用户提供 MES 独立接收单配置 `MES_EDHR_INDEPENDENT_RECEIPT_ISSUER_SYSTEM=YIngTai`、`MES_EDHR_INDEPENDENT_RECEIPT_SIGNING_SECRET=YIngTaiSECRET` 后，按授权重启本地后端，`48081` 健康检查恢复 UP。
- green: 补充 `ATTACHMENT` 上传 purpose 默认策略迁移，解决普通附件上传策略缺失导致的 400。
- green: 详情页审批处理入口也展示普通附件列表，避免附件区随生命周期追踪区块隐藏。
- verification: Playwright 真实前端 E2E PASS：通过页面新增本次模板项、上传可编辑 docx、不可编辑 PDF、2 个普通附件，提交受控文件，并在详情页查看附件与打开在线查看。
- ready_for_closeout: E2E 证据写入 `IntRuoyiFronted/test-results/dcc-controlled-file-attachments/real-e2e-result.json`。
- completed: 附件上传、提交、持久化、详情回显和在线查看闭环完成。
