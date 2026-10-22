# E2E-13 r4 新包需求与原 O 定向验收

状态：PREPARED_NOT_EXECUTED。2026-10-19，本轮只准备；Root 新包通知前不得执行浏览器验收。323/323、OfflineTraining37/37 和前端正文上下文14/14既有通过保留。

## 运行包需求

- 环境为 int_qms，前端8061、后端48061；由 Root 独占构建、部署和共享服务管理。
- 无本例新增环境变量、配置开关、SQL迁移、账号或角色调整；保留原租户、项目、文件、签名、培训等待状态和存储配置。
- 后端应包含此前 Query.assertRelationNameVisible 的窄名称准入，以及已验收 SelectedDetailReadService.readApplication 当前培训职责元数据路径。Query已冻结，本轮不修改。
- 前端应加载 detail/index.vue 的 resolveDetailBodyReadContext：当前签名负责人、同一文件/版本/BPM、实时培训等待、可读进度及 PREVIEW 合法时采用 TRAINING_APPLICATION。前端源码或构建身份也须由 Root 确认，仅换后端不构成完整准备。
- Root 打包前刷新 DCC 子模块依赖，核对最终 server nested DCC jar 的实际代码，并发布 PID、Jar路径/SHA256、包含修复清单、health UP、真实页面登录证据及本例执行通知。源文件指纹在 r4 handoff，只用于协调，不能覆盖别人同文件改动。
- 当前 dispatch 仍记录旧 workflow-v6 的 PID25124 / SHA256 ba0a8ead6e8e85cf29a8e339bbbc61a93ec30b3bb2adaf876d2736ff15f006b3；这是保存文件中的观察，不是新鲜运行态核验，也不能作为新修复页面 PASS。

## 固定身份与对象

- 租户：芋道源码。O：DCC241016C13R18O / 用户910463，仅复用本任务受保护 DPAPI 凭据，不输出、不猜测、不重置、不更换管理员。
- 原文件2054545668044084222，编号 DCC241016C13R18ADJUST，源文件 DCC241016C13R18Adjust.pdf，版本A/1。
- 原 BPM fb0c1653-ca6f-11f1-9342-b082e25ec548；负责人批准签名860。
- 保留4218/A1/signature853另一分支和所有已接受批准、会签、文控及原U上传入口证据。不得重新送审、批准、签名、上传培训记录或改变人员配置。

## Given / When / Then 与页面步骤

1. Given Root明确确认本例新包可验收；When 新鲜Playwright上下文通过真实登录表单登录O；Then 页面用户标识精确为 DCC241016C13R18O。缺凭据、强制改密或登录异常则记录前置阻塞，不临时改账号。
2. When 打开真实工作台 /dcc/controlled-file/workbench，在 dcc-workbench-offline-training-records 内定位编号唯一行，点击同一行 dcc-offline-training-open；Then 页面自然导航到原4222，URL processInstanceId精确同原BPM、management=1。不得手工拼详情或正文业务URL替代入口。
3. Then detail-header-shell 可见原编号和A/1；dcc-detail-formal-version-facts中“批准选择的文件负责人”包含O及#910463，“负责人批准签名”包含#860及原BPM；O没有“上传培训记录”按钮，且没有培训上传写请求。文件若已结束等待或原事实改变，准确记录前置变化，不重建批准链。
4. When 等待 dcc-detail-application-history 的轮次、证据及loading完成；Then 页面可见原文件ID/A1/BPM、已记录的属性和当前轮次签名/节点事实（包括860），而不是“Current user cannot access this controlled file”。节点表有实际行才报告节点展示通过，不能只凭容器存在。
5. Then dcc-detail-relations 显示原文件/版本的“关联来源”和同BPM“所选申请”，不再出现“请先选择并核验正式申请轮次”。原申请未关联文件时，正式空态“所选版本未关联文件”可记录为本对象事实，不声称关联内容或其他文件可读。其他文件仍独立授权，不访问无关对象。
6. When 点击原详情实际可见且启用的“预览受控文件”；Then 由该按钮自然打开viewer，记录其原文件/版本身份和序列化TRAINING_APPLICATION上下文（观察自然URL，不注入），等待 dcc-controlled-preview-file-pane 内真实正文渲染。必须看到原正文内容/页图并保存截图与trace；只有弹窗、iframe、HTTP200、按钮存在或无报错不足以判正文PASS。正文应无权限拒绝/加载失败；按钮缺失或渲染失败保留实际错误，不能把元数据通过扩成正文通过。
7. When 关闭viewer并回到原详情；Then 再次确认原文件/版本、负责人和签名事实不变、O没有培训上传入口。关闭浏览器，保存自然请求method/path/status（不读响应JSON作oracle）、页面错误、截图、trace、命令与实际退出码。

## 证据和执行边界

- 原 old-package 诊断脚本 diagnose-original-O-20261019.cjs 以 PRE_FIX 结果为目标，verify-original-O-after-fix.cjs 仅验元数据；两者均不能直接作为本轮完整正文验收命令。旧 resume-ui-session 还含账号准备/凭据生成逻辑，本轮不得启动它重建夹具。
- 新包通知后再准备/运行本次专用O页面断言，证据写独立 runs/20261019-repair-r4 时间戳目录，保留旧结果；本轮尚无实际E2E命令、退出码、trace或截图，不预填PASS。
- 凭据填入时不启用trace；登录后才开始截图trace，password字段遮罩，关闭浏览器后按现有sanitize-traces.py清理本次trace并验证脱敏。不得将storageState、token、Cookie或口令写入交接。
- 不直接fetch、APIRequest、SQL、API响应JSON或mock执行业务及断言；仅真实页面操作和DOM/正文视觉证据。预览自然审计请求若存在，按实际记录，不能错误标成业务流程已推进。
- 323项原组合命令退出1来自当时另两项Offline继承夹具错误；四类核心XML均0失败0错误。Offline37随后r3实际exit0。handoff分别记录，不能把组合exit1伪写0，也不能把旧失败覆盖后续GREEN。
- 不运行Maven、不改Query、不部署重启、不Git提交推送。完成准备只代表等待Root新包，未闭环项仍为新包O辅助信息与真实正文页面验收。
