# 主干提交推送验证

## Scope
int_main 既有未提交变更、本次任务记录及 origin/int_main 同步。

## Results
- PASS：远端 fetch、ahead/behind=0/0、主干端口门禁。
- PASS：初始 66 文件 SHA-256 冻结核对，选择性暂存，工作区及暂存区空白检查。
- PASS：后端 Maven reactor BUILD SUCCESS，20 个测试类共 155 项，failures=0、errors=0、skipped=0；以本轮新生成的 Surefire XML 核实。
- PASS：前端初始变更相关 6 个静态/行为文件，20 项 AI 静态套件在修复后通过。
- PASS：新增 runner 行为回归 3 项、既有评审弹窗行为回归 24 项，以及评审弹窗、历史标准列表、S05/S08 静态合同。
- PASS：加入新回归后的完整 21 文件套件、runner 语法检查、缺陷证据 validator、UTF-8 与任务结构、暂存白名单、无未解决冲突、新增行凭据特征检查。
- PASS：基线 1adade43aea059424c9db34376557b91eab65dfd 与修复 e58b9eba7dd4139f6142518426241a241f128a00 已提交推送 origin/int_main；工作区干净，ahead/behind=0/0。
- PASS：cleanup preview/apply，仅保留本任务 3 份核心记录、删除 20 份临时产物；无警告或阻塞，未操作其他任务及 worktree。

## Bug / Expected
静态套件的 S05/S08 使用已退役入口；期望 runner 沿当前新建弹窗、评审弹窗和历史标准列表详情入口完成验证，保留正式身份与错误中止检查。

## Reproduction / Root Cause
命令：node tests/e2e/edhr-ai-loop-static-suite.cjs；S05 要求 data-edhr-ncr-active-order-detail，S08 要求 data-edhr-history-active-order-detail。对应页面和断言与 origin/int_main 相同，缺失为基线既有问题。

现行 NCR 新建后返回列表，创建来源为 ACTIVE_ORDER/sourceId=activeOrderId；文件上传为原生 input+li。旧 runner 把创建/评审控件定位在列表根节点，要求旧上传组件，并将正式来源误认为 PQC 申请。历史列表详情按钮已改为 data-edhr-history-detail-action。修复只涉及测试脚本与静态合同，不修改应用页面或后端业务。

## RED / GREEN / Verification
- RED: 原始 20 项静态套件 -> FAIL，S05 旧详情标记不存在；独立 S08 -> FAIL，旧历史详情选择器不存在。
- RED: 更新为当前弹窗合同后的 S05 -> FAIL，runner 未在 createDialog 内提交。
- GREEN: 修复 runner 后 S05/S08 与 20 文件静态套件 -> PASS。
- GREEN: edhr-ai-loop-dialog-contract.spec.cjs -> PASS 3/3，精确订单/评审路径、错误来源在处置前拒绝、上传失败不重复提交或处置。
- GREEN: edhr-ncr-dialog-context-regression.spec.cjs -> PASS 24/24；edhr-ncr-list-dialog-static.spec.cjs 与 edhr-history-standard-list-static.spec.cjs -> PASS。
- 风险边界：以上证明本地脚本与现行页面合同一致，不证明实际服务、权限和完整业务链路已经运行通过。

## Blockers
无。原 S05/S08 静态阻塞已修复，代码已提交推送，任务清理通过。收尾记录随最后文档提交推送并再次核对真实远端 ref。

## Limitations
本轮不执行真实 E2E、发布、数据库写入或服务重启；不把静态测试表述为业务全链路验收。
