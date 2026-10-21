# E2E-24 原合同证据复核

复核日期：2026-10-20，Asia/Shanghai。分支：int_qms。原离线 R5/R6 复核后，用户又明确放行缺失格式真实 UI 和表格缺表最小 TDD；当前执行及修复交接见 `run-missing-formats-1792434821434/repair-handoff.json`。以下保留准备阶段与原复核事实，以本段续验结果为当前状态。

DOC4269 已由真实页面完成上传、五项签名及正式发布阅读；正文、工具、水印、12秒失焦冻结/恢复、关闭重开通过，切版尚未执行。XLS4270/XLSX4271 的两张有内容表在原生 UI 可见，但正式发布 PDF 仅第二张，第一张标记搜索无结果，均为已证实产品 FAIL。两源文件无打印区域、第二张为活动表，未修改字节。

已按 RED/GREEN 完成最小请求及缓存 key 修复，15 项定向单元/回归通过；这不能替代新运行包的全部工作表真实页面验收。旧失败发布件保留，不重签或改写；通过正式后继/未完成版本验证。签名后的 accessDenied 提示已保留，因果证据不足，不改权限。图纸首链、DWG配套PDF、CAD第二套版本及媒体合同仍有缺口，整例未 PASS。

## 合同与当前结论

合同依据：`../20261015-dcc-six-thread-followup-review/e2e-test-cases.md` 的 E2E-24。原文要求“阅读只改变画面，重开源件内容不变且不能保存回原件”，并要求按各格式正式能力验收；没有要求新增原始源件下载入口、服务器全字节下载比对，或在上传预览与正式受控浏览两个入口重复全部工具验收。

现有证据足以支持 PDF、DOCX、PNG、TXT 已验证阅读子链的正文不变及只读断言。Office 原证据的源件只有 DOCX，不能覆盖 DOC/XLS/XLSX。PNG/TXT 的原件下载/全字节比对和重复原生工具验收移出产品修复门禁。服务器源件全字节仍未读取，不将其改写成已验证；该事实不影响上述已验证断言。缺失正式格式及必要版本归属仍未验，音视频合同保持待定，整例不报全格式 PASS。

## 缺失正式格式第一阶段准备（2026-10-20，历史准备记录）

用户本轮明确授权：先准备七种缺失正式格式，复用本例账号/项目/最小夹具及既有脚本；不重跑 PDF/DOCX/PNG/TXT 已 PASS；E2E10 正在切换本机后端，本阶段不启动浏览器、不改共享生产/服务/Maven/DB，准备完成后等待重新放行真实 Playwright。以下准备不是业务 E2E PASS，也不是产品修复 RED/GREEN。

正式 SOURCE 准入为 `doc/docx/xls/xlsx/pdf/dwg/sldprt/sldasm/slddrw/png/txt`。依据后端 `DccControlledFileUploadTypePolicy.java:18` 及前端 `upload/submitter.ts:200`；图纸预览使用记录绑定的 `drawingPdfFileId`，发布使用正式 PDF 路径，不声称原生 CAD 渲染。泛用阅读器中的 PPT/JPG/音视频识别能力不能扩张此准入。

| 源扩展名 | 已有 UI 证据 / 本轮本地准备 | 当前验收状态与缺口 |
| --- | --- | --- |
| PDF | 4235 A/1 正文及适用工具已通过 | 保留 PASS；独立切版未验，本轮不重跑旧格式 |
| DOCX | 4240 原生 word 阅读及发布 PDF；4240→4253→4254 切版 | 保留这些具体子链 PASS，不覆盖其余 Office |
| DOC | 4269 A/1，真实 Word 97 原件及发布 PDF | 正式阅读子链 PASS；同链切版未验 |
| XLS | 4270 A/1，真实 BIFF 两张正文表在 cell UI 可见 | 发布仅第二张，缺第一张 FAIL；新包后继版本待验 |
| XLSX | 4271 A/1，真实 OOXML 两张正文表在 cell UI 可见 | 同样缺第一张 FAIL；新包后继版本待验 |
| PNG | 原图显示及正式 PDF 阅读子链已通过 | 保留 PASS；原图变换、独立切版未独立覆盖，不新增重复全工具门禁 |
| TXT | 原生首段、正式 PDF 正文/工具/搜索/四页翻页已通过 | 保留 PASS；原生全文/工具、独立切版未独立覆盖，不冒称已验 |
| DWG | LibreDWG 固定提交的 `circle.dwg`，上游 blob 一致、AC1015 头 | UI NOT_EXECUTED；匹配 PDF 缺失，不能上传任意 PDF 充数 |
| SLDPRT | PANO-VISTA 的 `PushRod-1.sldprt` 与同名 PDF | UI NOT_EXECUTED；单页 PDF 无提取文本；第二套版本源件/PDF 尚缺 |
| SLDASM | 同项目 `Assem350.SLDASM` 与 `PANO-VISTA.pdf` | UI NOT_EXECUTED；PDF Title=Assem350，含可搜索标注；第二套版本源件/PDF 尚缺 |
| SLDDRW | 同项目 `Assem350.SLDDRW` 与上述 PDF | UI NOT_EXECUTED；同上，不能借 SLDASM 分支代记 PASS |

夹具均位于本目录 `fixtures/formal-missing-source/`。`provenance.json` 记录固定提交、原路径、Git blob SHA1、SHA256、大小、授权文件和校验边界。Apache POI 保留 Apache-2.0 LICENSE/NOTICE；PANO-VISTA 保留 MIT LICENSE/README，仅选项目自身源件，未下载 vendor-cad；LibreDWG 保留 COPYING。所有上游文件逐一核对 Git blob，未改源字节或扩展名；Office 仅另外复制成 A1/next 同名本例文件，哈希不同。上游来源不等于已由本机 CAD 软件打开：未运行原生 CAD 或验证装配依赖；原项目也注明重排目录后装配可能需要重定位组件。

配套 PDF 已用 pypdf 读取、Poppler 渲染并目视核对真实图形；PANO-VISTA 为标注爆炸图，元数据标题 Assem350 支持配对来源。两份上游 PDF 有 xref 索引告警，原字节保留，实际是否被 DCC 正式链接受仍待 UI 验证。PushRod 无文本层、两份均为单页，不能预填正文搜索/跨页 PASS 或擅自改成全格式 N/A。

### BDD、最小顺序及脚本复用

BDD 记录在既有 `task.md` 的“缺失正式格式准备 BDD”。最小执行顺序：

1. 用户重新放行、E2E10 切包结束后，先核对新的运行 Jar/哈希/端口归属/前后端配对，再经真实页面重认本例 Config18/Reader18、DCC241016C24Project、G44文档、类别908768及现有审批路线；不改共享权限或矩阵。现存 DPAPI 缓存只核对存在，未解密、未重置凭据，账号/项目此阶段未重新在线核验。
2. DOC → XLS → XLSX：逐扩展名走源件上传/原生预览→草稿→提交→既有签名审批→正式发布→PDF 阅读。正文、实际缩放/旋转/复原、搜索/文档或工作表导航按真实能力单列，核对水印、计时、失焦/恢复和关闭重开；使用 A1/next 同名不同正文文件，只补选定版本正文/水印/阅读归属断言，不增加其余版本流程组合。
3. SLDPRT → SLDASM → SLDDRW：源件与匹配 PDF 同时从正式上传字段选择，核对所选记录实际载体及正文标记、非空画布、可用工具、只读、水印、计时和重开；DWG 只有配套 PDF 缺口补齐后才进入同一路径。没有第二套真实源件/PDF 时版本断言继续 BLOCKED，不能借 DOCX 或另一扩展名填值。
4. 只记录自然页面请求 path/method/status；不调用 API、解析接口 JSON 或写 DB 代替被验收动作。发现产品问题先留真实 RED 与根因，再最小修复、GREEN；缺数据/授权和脚本错误不能称产品 RED。

复用 `prepare-registered-body.cjs`、`submit-working.cjs`、`sign-own-task.cjs`、`verify-published-body.cjs` 及阅读/水印/计时模块；准备数据附在既有 `fixture-manifest.json`、`prepared.json`。当前脚本不可直接执行：总入口 `dcc-e2e-24-real.e2e.cjs` 强制六类别且 PDF 至少两页；准备器默认非 PDF=.docx、未选择图纸 PDF；`office-formal-tools.cjs` 固定 DOCX 标记/word DOM。后续只需在这些既有模块内按显式 extension/carrier/marker 参数适配，新增 cell 的工作表/正文定位，使用真实“图纸 PDF”文件输入；不能整跑旧六类别或用字符串替换把 XLS 伪装 DOCX。本阶段未修改/加载执行这些浏览器脚本，脚本适配仍属执行前置。

阶段结论：准备清单、BDD、顺序和已找到的真实文件已就绪；完整执行仍待放行、新运行态、DWG 配套 PDF、CAD 第二套版本夹具及必要脚本适配。音视频不扩准入、不变更 N/A/PASS。历史546/401继续 UNKNOWN_UNTOUCHED；没有新增已证实产品缺陷。

## 已通过子链及证据

| 子链 | 已验证事实 | 保存证据 |
| --- | --- | --- |
| PDF 4235 / Office 4240，A/1 | 真实非空正文、适用工具与搜索、双账号当前人水印、12 秒失焦冻结及恢复、重开画布指纹不变、账号计时隔离 | `run-20261018-225739-recovery/parameter-coverage.json` 及其逐账号结果；Office 原生只读正文另见 `run-20261018-151101/` |
| Office 4240→4253→4254 | 同链版本正文、草稿身份和阅读归属；原 A/1 内容保留 | `run-2026-10-18T22-10-44-428Z-3b2de8ff/root-version-continuation-proof.json` |
| PNG 4262 / TXT 4263，A/1 | 实际上传预览→提交→审批及五项签名→盖章发布生效；正式正文非空、变换、水印、计时和关闭重开 | `run-r5-pngtxt-1792422918342/` 内 source/signed、readonly-headed、final-text 证据 |
| TXT 搜索 | 中文首段、长行及第 4 页终末标记 R5END 实际命中 | `final-text/r5-final-readonly.json` 的 search 及页面截图/trace |
| TXT 下一页/上一页 | 1/4→2/4→1/4，实际滚动及画布改变，返回第一页指纹相同 | `run-r6-txt-pagination/pagination-result.json`、三张截图及 `pagination.zip` |

以上文件 ID 均省略共同前缀 `205454566804408`。Office 行的源件实际为 DOCX。PNG 单页的跨页、纯图片无文本层的搜索记不适用，不能记为执行 PASS，且不得推广至其它新夹具。正文工具验收发生在该格式的正式受控阅读路径，上传源件预览本身只记录实际执行的原图/文本内容检查。

## “阅读后原件不变、不能保存回原件”的证据链

1. 上传前已保存源件 SHA256；阅读完成后本地夹具完整 SHA256 相同，最终管理页的源件哈希前 12 位与上传源件一致。PNG 为 `0b27bfa1b012`，TXT 为 `083e49d89d05`，见 `source-IMAGE.json`、`source-TEXT.json` 和 `final-text/final-binding-source.json`。页面只显示前缀，不能称为已获得服务器完整哈希。
2. 原脚本确实关闭后从同一业务行重新点击“浏览”。离线查看原 trace 中重新打开后的页面，PNG 的 `DCC241016C24 IMAGE BODY MARKER`、TXT 的 `DCC241016C24 中文长行验收 第一页正文` 及中文正文仍可见；身份均为原文件 ACTIVE A/1，受控章和新会话水印可见。原 JSON 未单列重开正文像素比较，因此本次使用原 trace 帧补充可审查的正文证据，没有重新执行页面操作。
3. 两份正式阅读记录的自然写请求仅有各自 reading-session start/heartbeat/stop，均 200，非阅读业务写为 0。已保存页面工具仅有阅读控制，无正文编辑/保存回原件入口；没有执行保存原件动作。PDF/Office 旧证据还保留重开画布指纹一致及只读结果。当前源码的 PDF 画布/变换实现及 OnlyOffice `mode: view`、`edit/comment/review: false` 仅作静态佐证，不替代历史页面验收。

提取帧位于 `contract-review-20261020/IMAGE-reopened-existing-trace.jpeg` 和 `TEXT-reopened-existing-trace.jpeg`；`extracted-frames.json` 记录原 ZIP、资源成员、pageId 与时间戳。帧分别取在重开后、点击“查看详情”前：PNG 209559.440 ms，TXT 520664.377 ms。两帧分别能辨认原图正文标记和中文首段，不能据此声称逐像素或服务器全文全字节比较。

结论：按原合同的实际阅读行为、已知正文和源身份检查，上述断言为 PASS；现有四格式阅读子链没有需要新增下载功能才能填补的证据缺口。此结论不延伸到绕过页面直接写存储或任意攻击路径。

## 剩余项分类

| 项目 | 当前分类 | 门禁与最小后续边界 |
| --- | --- | --- |
| 原件下载入口、服务器全字节比对 | 合同外观察 | 未执行且不要求新增；从产品修复/验收阻塞项移除，旧 source-only 记录保留 |
| PNG/TXT 上传预览中的全部原生工具及原生源件重复重开 | 未独立覆盖的入口能力 | 仅声明已做源件内容预览；正式受控阅读工具已经验证。不增加第二套工具入口或重复验收门禁 |
| PDF、PNG、TXT 各自独立多版本切换 | 未独立覆盖的参数组合 | Office 的实际版本链不能冒充这些格式独立切版 PASS；不将该证据精度限制转成新增逐格式版本矩阵要求 |
| WEBM/WAV 正式受控能力 | 未覆盖能力，业务待决 | 当前尚未作为受控源件完成发布阅读；先明确是否属于受控源件、归档/签名/盖章产物及播放导航/搜索/计时语义。原合同已注明音视频分页/搜索待明确，不能记 PASS 或做伪功能 |
| 旧 temporary 546 / cleanup 401 | UNKNOWN_UNTOUCHED | 属于历史残留状态，不是新证实的阅读产品缺陷；不检索、不清理、不推断已解决 |
| 历史 headless 失焦、PNG 旧详情选择器失败 | 历史运行/脚本结果保留 | headed 真实失焦及后续正式详情补核已有证据；不覆盖原 FAIL，不重跑已通过主链 |

原离线复核阶段没有新增已证实缺陷；续验已确认 XLS/XLSX 发布缺第一张正文表。PDF/DOCX/PNG/TXT 历史 PASS 保留，DOC 新增正式阅读 PASS；表格修复待新包业务验收，四种图纸源格式及必要版本归属须补验。音视频合同另列待定，本轮不提交、推送或清理历史资产。

## 复核验证

R5 主证据目录六份 trace 和 R6 分页 trace 的 ZIP CRC 均正常，未含 `.network` 原始网络载荷；原始 FAIL 和所有旧证据保持不变。两个重开帧从原 ZIP 原样提取，当前夹具 SHA256 与上传记录一致。机器可读结论、证据清单与结构验证结果见 `contract-evidence-review-20261020.json` 和 `contract-review-20261020/evidence-inventory.json`。
