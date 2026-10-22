# E2E-24 原件全字节验证边界

当前结论：BLOCKED_NO_FORMAL_SOURCE_DOWNLOAD_ENTRY。PNG/TXT已通过审批、盖章、正式正文、水印和计时，本轮未重跑。服务器原始PNG/TXT字节未下载、未比对，不能用发布PDF代替。

## 正式页面与原对象

以原作者DCC241016C24Config18真实登录8061，经受控浏览的同一行“操作面板”进入：

| 参数 | IMAGE | TEXT |
| --- | --- | --- |
| 原对象ID | 2054545668044084262 | 2054545668044084263 |
| 编号 | DCC241016C24IMAGE | DCC241016C24TEXT |
| 版本 | A/1 | A/1 |
| 源文件名 | DCC241016C24IMAGE.png | DCC241016C24TEXT.txt |
| 原夹具SHA256 | 0b27bfa1b012f2d701fa6eaf7174bc97124369a34f1a9982035eb4c7cbd64981 | 083e49d89d0564ad060937ca1c61191c1e6a77f2624b5829a6aa3eddd5fb278b |
| 页面源哈希前缀 | 0b27bfa1b012 | 083e49d89d05 |
| 下载说明 | CATEGORY_DOWNLOAD_DENIED | CATEGORY_DOWNLOAD_DENIED |

两份详情均未见“下载原件/原件下载/下载源文件/源文件下载”。现有按钮是“预览受控文件”“查看受控版本正文”“查看盖章/发布文件”等；TEXT“更多”实际菜单只有BPM详情、流程打印、流程导出Word。项目文件夹表只有浏览、关联、操作面板。没有把任何这些动作当成原件下载。

## 配置与下载链

类别908768（DCC241016C24正文类别）正式“类别权限→维护权限”完整读取3条可配置规则：Config18上传、Config18查看、Reader18查看，均用户主体、启用；无DOWNLOAD。前后完整UI字段（动作、主体、对象、范围、启用、备注）相等，见category-rules-before.json和source-only-result.json.categoryAfter。

若目标是现有发布件下载，最小缺失类别动作是本类别对本例用户的DOWNLOAD；但该授权不能选择原件。当前Query服务还要求目录DOWNLOAD。目录初读展示本例两用户下载关闭；随后完整回读“预期2条”遇到3条，因此未完成稳定的全目录快照相等核验，不以首读代表整个目录最终配置。此过程没有点保存、增删或改开关，未发生授权变更，不需要恢复任何临时规则。

静态入口链（仓库相对路径、方法锚点）：

- IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue：下载按钮调用openDownload → triggerControlledFileDownload。
- IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts：triggerControlledFileDownload → downloadControlledFileWithName → buildControlledFileDownloadEndpoint，只有受控文件/{id}/download，没有source/original选择。
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java：downloadControlledFile → queryService.readDownloadFile；附件download是普通附件，不是本例原主文件。
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java：readDownloadBinary → resolveBinaryFileId(file, DOWNLOAD)，PREVIEW分支之外固定返回file.getPublishedFileId()；decideDownloadBinary要求发布件、ACTIVE、类别DOWNLOAD与目录DOWNLOAD。

因此缺口包含独立原件读取入口与其服务端精确sourceFileId读取路径；单加类别DOWNLOAD不能补齐。若未来业务批准原件功能，最小边界应是显式原件入口、当前记录/版本的sourceFileId、既有项目/类别/目录权限及审计，不改变现有发布件下载语义、不退回master/latest或猜测originalFileId。本轮按Root最新要求不新增该功能或授权，也未运行Maven。

## 停止点与证据

Root准备13新包时，本例停止所有只读浏览；浏览器全部关闭，r5-ui-session runner exit0且进程核对为0，可安全重启服务。本轮DCC非GET业务请求0、权限变更0、下载0；没有URL/fetch/API/SQL直调、存储管理员下载、换文件、共享源码/服务修改。未触碰546/401，WEBM/WAV仍未验。

证据目录run-r5-source-only：两原对象身份/权限截图、详情正文、类别完整快照、source-only-result.json、2份脱敏trace及仅path/method/status网络摘要。raw.zip均移除，ZIP CRC通过。保留原脚本失败：抽屉英文关闭名不匹配、交互代码语法错误、目录条数旧预期失败；这些不作为产品下载成功或失败的依据。最后一次目录复核因Root重启指令中止，不继续浏览补验。
