# 注册证批量导出 Excel：实现与验收记录

## 交付状态

已实现列表范围、生成、下载、审计和三表模板链路。按用户最新口径：国内、CE、FDA 的单列缺失均保留记录；来源明确就填，缺失或无法确认就留空。CE 两段关系与 FDA 跨记录 Listing 关系不作推断。先完成系统既有17条数据联调，随后使用用户提供的审批账号/PIN完成3条自有样本审批、正式列表、导出和重取，详见文末。审批账号阻塞已解除。

需求来源为桌面《注册证批量下载-首期需求交底与验收清单.md》。用户最新覆盖口径：范围按当前生效排序固定有序 ID，字段在生成时读取；生成后统一保留5分钟，重取不续期；到期或权限失效后等待活动下载流结束再回收。

底座原样复制到 `IntRuoyiBackend/yudao-module-dcc/src/main/resources/registration-certificate/export-template.xlsx`。未修改用户桌面文件。

## 业务链核对

| 节点 | 代码依据与实现 |
| --- | --- |
| 入口 | `IntRuoyiFronted/src/views/dcc/registration-certificate/index/index.vue`，仅 current Tab，通知设置后、上传注册证前；已有历史导入按钮保留 |
| 筛选 | `shared/CategoryFilters.vue` 与当前列表快捷条件，产品和分类已存在，无需新筛选组件 |
| 勾选 | 列表原有 `row-key=versionId`；导出以 versionId 为 record_id。同一证书可能有 CURRENT/PENDING_EFFECTIVE 两行，不能按 certificateId 合并 |
| 跨页 | Element Plus reserve-selection，去重版本 ID；筛选变化清空；表头仍仅操作当前页。原有删除功能限定当前页，避免跨页保留使删除范围意外扩大 |
| 生效条件 | 列表成功响应后更新 appliedExportQuery；分页和表头排序从生效条件构造查询，不混入未提交面板草稿 |
| 查询 | `DccRegistrationCertificateQueryServiceImpl.getExportRows` 复用 normalize、日期/排序校验、listFilterWrapper、SQL selectPage 和稳定排序。内部 exportVersionIds 限定勾选版本 |
| 数据范围 | QueryMapper.currentWhere 限制租户、未删除、ACTIVE/PENDING_FIRST_EFFECTIVE、CURRENT/PENDING_EFFECTIVE 和正式快照；公司名经企业 API 校验租户与 OWNED_COMPANY |
| 权限 | 复用 query-current；生成/重取逐条复用 assertFilePreviewAllowed。不申请或消耗证书原件下载次数；本功能导出台账 |
| 快照 | 服务端随机 snapshot_id/request_id，绑定租户与用户；30分钟内首次受理；快照只存有序 ID 与生效条件，不冻结字段 |
| 生成 | 同请求仅生成一次；同用户最多一笔生成；最多2笔全局并行；全部成功后持久化审计，再将工作簿开放下载 |
| 审计 | 复用 dcc_registration_certificate_audit；EXCEL_GENERATED，一请求一事件。复用已有可信公司/证书约束，事件锚定首条实际导出记录，完整批次范围在 detail_json |
| 重取 | 原 request_id/status/file；每次取件复核菜单和所有实际导出记录权限；失败旧 ID 终态 INVALID。HTTP断连不会改变文件原始截止时间 |
| 回收 | 5分钟固定窗口；1秒周期检查，且每次访问检查截止时间；过期不再允许新流，已有流可完成；流关闭后回收 |

## 逐列字段映射

### 国内注册证

| 列 | 模板字段 | 系统来源 |
| --- | --- | --- |
| A | 公司名称 | 企业主数据 `ownerCompanyName` |
| B | 项目代码 | 列表查询结果 `projectCode`，沿用正式关联或已声明事实的现有规则 |
| C | 产品名称 | 正式快照 `productName` |
| D | 注册证号 | facts.certificateNo |
| E | 首次获证日期 | facts.firstObtainedDate；历史原值只有部分日期时按文本保留 |
| F | 生效日期 | facts.effectiveDate；历史原值同上 |
| G | 有效期至 | facts.expiryDate；历史原值同上 |
| H | 类别 | facts.classification |
| I | 备注 | facts.remark，含等号开头内容一律字符串 |

### CE证书

模板两段表头、动态移动第二段、公司合并与组内输入排序已实现。当前没有可验证的两段业务关系，CE 记录以未分段列表放在首个数据区，第二段只保留模板表头；弹窗与审计明确记录结构提示，不据公司、自由Rev.或历史行号猜测分段。公司合并仅使用真实 ownerCompanyId。

| 列 | 模板字段 | 查到的依据与待确认内容 |
| --- | --- | --- |
| A | No | 按输出顺序生成 |
| B | 公司名称 | 企业主数据 ownerCompanyName |
| C | 产品英文名 | facts.productNameEn |
| D | Trademark | facts.tradeName；第二段模板本身该表头为空，照原底座保留 |
| E | 产品中文名 | 正式快照 productName |
| F | MDR Class | 历史值 MDR分类；新记录只有通用 classification，不能默认作为MDR分类 |
| G | MDR Rule | 历史值 MDR规则；无独立现行字段 |
| H | MDR Certificate No | 历史值 MDR证书编号；ceCertificates.revision 是自由文本，非规范MDR类别 |
| I | MDR Issue Date | 历史值 MDR签发日期；现行明细有 issueDate 但类别不明确 |
| J | MDR Expiry Date | 历史值 MDR有效期至；现行仅证书级 expiryDate，不能补入两类期限 |
| K | EMDN Code | 历史值 EMDN代码；不能默认将 productCode 当作EMDN |
| L | MDD Class | 历史值 MDD分类；无独立现行字段 |
| M | MDD Certificate No | 历史值 MDD证书编号；现行类别同H列待确认 |
| N | MDD Initial Issue Date | 历史值 MDD首次签发日期 |
| O | MDD Expiry Date | 历史值 MDD有效期至 |
| P | Extension Letter No | 历史值 延期函编号；现有续证合同/报价不是延期函，不代填 |
| Q | Extension Issue Date | 历史值 延期函签发日期 |
| R | Extension Expiry Date | 历史值 延期函有效期至 |
| S | CE Marker | 仅当前记录保存的历史值 CE标志；新建公告机构代码 notifiedBodyCode 不能确定等价，留空 |

已查 `domain/DccRegistrationCertificateFacts.java`、`service/historicalimport/RegistrationCertificateWorkbook.java`、`service/upload/DccRegistrationCertificateUploadService.java`、`shared/facts.ts`、`shared/FactsFields.vue`、QueryRecord、QueryMapper 与底座。historicalRow 只有原始行号，导入没有保存分段标识；不能用固定40行或底座5行推断业务分段。

### FDA证书

历史导入每行被创建为独立源记录，按每条记录自身的一条 Listing 输出。系统没有同一源记录多 Listing 集合，不按换行、逗号或相同K号猜测关联。新建 FDA 的 File No./Listing status 无对应字段时留空，整条记录仍导出。

| 列 | 模板字段 | 系统来源 |
| --- | --- | --- |
| A | K holder | facts.certificateHolder；按 ownerCompanyId + holder 分组合并。holder 缺失时不推断共同持有人，不跨记录合并 |
| B | No. | 按分组后输出次序生成 |
| C | K number | facts.kpNumber；EXEMPT 显示现有导入约定 `510(k) Exempt` |
| D | Listing number | facts.listingNumber，当前为单个字符串 |
| E | Product name in EN | facts.productNameEn |
| F | Product name in CN | 正式快照 productName |
| G | Product code | facts.productCode |
| H | Class | facts.classification |
| I | Regulation number | facts.regulationCode |
| J | Cleared/exempt listing date | facts.approvalDate/listingDate；二者都有时带中文标注分行；无结构日期时保留 historicalValues.批准或列名日期 |
| K | File No. | historicalValues.文件编号；新建无对应字段 |
| L | Listing status | historicalValues.列名状态；不等同 factoryRegistrationStatus，不自行映射 |
| M | Remark | facts.remark |

链接：当前可使用 facts.registrationLink，已限定HTTP/HTTPS，以K号原可读文本链接；空白底座已无链接。历史导入解析器未保存源文件的超链接地址，不能恢复，也不将内部原件/附件链接写入Excel。若链接须放在其他列或来自Listing专属地址，待业务确认。

## 上限及测量

当前同步上限收紧为100条源记录、每工作表10000行（含表头）、20秒生成期限、最多2笔生成、每用户1笔、单文件16MiB、保留文件和生成预留合计64MiB。max-records/max-rows/max-seconds 支持现有Spring属性覆盖。

本机Java17测试：1000条国内行38331字节，约137毫秒；三表共11998条模板行（1000国内、1000 CE、9998 FDA）482542字节，约909毫秒。这是合成模板渲染，不含真实DB读取和权限查询；不是实际 Listing 关系验收。

真实本地链路当前17条（国内9、CE4、FDA4），审计 generated_millis=1013，页面完成下载约1576毫秒；3条为96/210毫秒，4条为106毫秒。以前端30秒超时、服务20秒生成期限为边界，100条作为保守运行限制；原1000条缺少逐条数据库/权限读取的容量依据，已下调。100条尚无真实满载数据集，不宣称已压测；超过100条会在预览阶段拒绝。开发Vite代理已实测，部署nginx配置为300秒，线上实际网关未验证。

## 实际测试及结果

| 实际执行 | 结果 |
| --- | --- |
| Maven离线 test-compile，dcc及依赖 | 通过 |
| 最终后端完整package及源码/产物一致性校验 | 通过；2026-11-24 22:11:11（北京时间），已在当前int_qms运行包复核 |
| Maven离线 `RegistrationCertificateExcelExportTest,DccRegistrationCertificateQueryServiceTest` | 57项通过（43数据库查询/审计、14导出），失败0 |
| 前端 `node --test tests/unit/registration-certificate-excel-export.test.cjs tests/unit/registration-certificate-form-state.test.cjs` | 19项通过，失败0；含未知结果保留原请求、成功后可重新选择范围 |
| 导出弹窗与列表 Vue SFC 编译 | 通过 |
| `python scripts/preflight/check-menu-migrations.py` | passed；复用现有权限的三处原因已登记，无新增SQL |
| 全仓 vue-tsc，8GiB堆 | 未通过；输出包含CRM checkPermi、受控文件及其他既有文件错误，本次导出API、组件、范围工具和列表无诊断。首次默认4GiB运行耗尽堆内存 |
| 旧 registration-certificate-list-sort-static.spec.js | 未通过；其断言只允许两个slot参数，HEAD源码原已含resolveRows。未修改该旧断言来制造通过 |
| 本次受控源码 git diff --check | 通过；全仓检查另有既存日志尾随空格，未处理其他任务资产 |

日志位于 output/registration-export-{tests,frontend-tests,types,menu,sort-static}.log。

修复过的实际问题：审计成功事件必须有可信公司/证书身份；测试跨租户主数据主键重复；复用权限缺迁移说明；原有删除功能需避免扩大到跨页；重取前菜单权限拦截需让服务先将旧请求置为失效。

本轮新增修复：CE/FDA 空列不再阻断整条记录；公告机构代码不再误当 CE Marker；完成一次导出后可以主动选择新范围；截止时间从工作簿生成完成计算，审计耗时与重取均不延长。

真实页面操作均由 Playwright 完成，未用API/DB承担业务动作；数据库仅只读核对。当前测试记录为既有系统数据，因此以下明确区分页面联调事实与“任务自有数据 E2E PASS”。未将现有数据联调冒充完整E2E通过。

| 真实页面联调 | 已观察结果 |
| --- | --- |
| 全部17条混合导出 | 成功下载三表 .xlsx，17导出、0跳过 |
| 跨页勾选 | 第1页选versionId=4、17，第2页选11；提交准确3个ID |
| 表头排序 | certificateNo desc仍保留3个ID，导出按服务端列表排序 |
| 筛选草稿 | 草稿切CE尚未查询时，导出仍显示已生效17条并提示草稿 |
| 类型生效 | CE查询命中4条，勾选清零，4条可下载 |
| 空结果 | CE有效期起2031-01-01命中0条，整个导出按钮禁用 |
| 当页全选/半选/页大小 | 10条当页全选10条，取消首行剩9条且表头半选；改为20条/页仍保留9条；全选与取消可清零 |
| 产品/分类快捷条件 | 产品WORD1104-PC命中4条；叠加不匹配分类命中0条；移除分类恢复4条，移除最后条件恢复17条并清空勾选 |
| 查询中/失败/恢复 | Playwright真实CDP延迟期间禁用导出；断网查询失败禁用并显示原因；恢复网络重新查询后恢复可用。未mock响应 |
| 最终文件逐列核对 | all-real-final.xlsx：9国内、4 CE、4 FDA，逐列与真实响应及审计ID比对通过；CE未知列和FDA File No./Listing status为空，实际链接正确 |
| 单类型文件 | product-filter-final.xlsx只填4条国内；CE两段、FDA标题表头保留且数据区全空 |
| 版式渲染 | 已生成并人工查看domestic/ce/fda-final.png，三表表头和CE第二段保留，FDA日期多行表头完整可见；不等同原生Excel打开 |
| Tab | 老证、注册测试、我的上传申请均不显示导出按钮 |
| 重取 | 文件字节完全一致，原expiresAt不变 |
| 真实响应中断 | CDP限速后在真实生成200响应处断网，页面报Network Error；关闭重开、联网后GET原request_id成功取回，未新建快照 |
| 审计只读 | 已完成请求各恰有1条持久化EXCEL_GENERATED，含有序ID、实际ID与结构提示；重取不新增 |

## 按附件逐项验收

勾选表示所述代码/自动化层面已验证，**不代表真实页面E2E验收完成**。

- [x] 服务端拒绝其他Tab，前端入口仅current Tab；入口位置已核对模板代码。
- [x] 真实页面核对按钮、空结果禁用、提示及其他Tab隐藏；完整任务自有数据验收另计。
- [x] 生效筛选和列表排序复用；H2核对勾选ID、正反排序、租户边界与超限。
- [x] 真实页面跨页、表头当页全选/半选、改每页条数及产品/分类快捷条件增删已验证；数据为系统既有记录，任务自有数据E2E另计。
- [x] 草稿不进入已生效范围、重复确认禁用、原ID重试及关闭后重开；Vue组件行为测试通过。
- [x] 固定三表顺序、空类型保留表头、原底座宽度样式；POI打开生成结果核对。
- [x] 国内字段、前导零长编号、等号备注、多行文本、日期数字值；自动化通过。
- [x] 最终17条真实数据的每个可导出单元格、空值、链接、顺序、表头样式、列宽与源数据/底座逐项比对；verify-final-workbook.py通过。
- [x] CE有历史MDR/MDD/延期函值正确填，缺失值留空且保留整条；不推断自由Rev./通用分类/到期日。映射与生成单元格自动化通过。
- [x] FDA已确认File No./Listing status/HTTP链接正确填，缺值留空；不混用工厂状态、不拼Listing，自动化通过。
- [x] 按最新口径处理无法验证的CE两段业务关系、同源多Listing：不猜分组、不拼接；单独列为结构问题。尚不能验证不存在的业务关系或宣称已有一对多真实数据通过。
- [ ] Microsoft Excel真实打开和逐页视觉核对（含FDA多行表头、CE边框）；未执行。
- [x] 首次受理快照过期拒绝；已受理可跨快照到期重取；生成时读取新字段。
- [x] 模板未知映射明确跳过、全跳过报错、源记录计数守恒。
- [x] 同请求只生成一次、同用户并发限制；并发自动化通过。
- [x] 五分钟固定截止、重试不延期、活动流可完成、权限失效旧ID不可复活；可控时钟测试通过。
- [x] 审计失败无文件、成功审计一次、完整ID/筛选/跳过原因落库；H2真实表约束验证通过。
- [x] 真实生成响应中断后，保留原ID重取成功；为现有系统数据页面联调，尚非完整任务自有数据E2E。
- [ ] 线上实际网关与100条真实满载压测；本地真实17条及上限依据已记录。

## 仍需确认及运行限制

1. CE第一、第二段没有持久化业务判断字段；新建明细的自由Rev.不能确认为MDR/MDD类型。已查Facts、历史导入、上传、查询/SQL、表单和空白底座。实际两段关系需未来明确业务字段；当前不猜分段。各列缺值留空，不要求为此暂停功能。
2. FDA没有同源多Listing集合或关联键，只有每记录listingNumber。已查同上链路；按单条存储输出，不拼接。File No./Listing status仅使用本记录历史值，否则留空，用户已明确允许。
3. 链接只使用facts.registrationLink的HTTP/HTTPS来源，读数来自当前系统，不恢复未落库的历史超链接，不导出原件/附件内部地址。无K号可读文本时不造一个链接标签。
4. 当前请求/快照/文件存放应用内存，有明确64MiB配额及五分钟回收；未实现跨进程共享或重启恢复。多副本部署不可直接使用此进程内并发约束。生产是否多副本需确认，不能把本实现宣称为跨节点防重。
5. 本目录实际分支为int_qms，按端口契约运行8061/48061，复用原启动环境完成本地构建和服务更新；未操作int_main服务；本次按用户要求仅提交导出功能到int_qms，未推送。3个专用验收申请（53国内、54 CE、55 FDA），产品前缀EXCEL-1795526701376，已通过用户提供的注册部经理账号在真实页面签名批准，成为versionId 35/36/37；未绕过审批、未直接写数据库，未处理其他任务申请。
6. 有值/缺值映射由POI断言覆盖；自有正式样本已补验长编号、等号多行文本、CE现有字段与空列、FDA编号/链接/日期与空列。历史MDR/MDD、File No./Listing status有值仍仅自动化覆盖。Microsoft Excel未发现安装入口；WPS已启动但窗口截图返回FrameArrived timed out，原生可视核对未完成，不能写Excel视觉验收通过。

### 附件验收项对照

| 附件项目 | 当前结论 |
| --- | --- |
| 新增按钮及位置 | 代码与真实页面核对通过，仅注册证Tab可见；其他Tab服务端拒绝由自动化覆盖 |
| 入口状态 | 有结果、未勾选、零结果、查询中、真实断网失败与恢复均已观察 |
| 筛选与跨页 | 日期/类型、产品/分类组合、跨页ID及全筛选ID已核对；四类日期的边界由查询测试覆盖，未逐一真实页面组合穷举 |
| 勾选和草稿 | 跨页/排序/页大小保留、当页全选半选、条件增删清空、草稿不生效已验证 |
| 文件和分表 | 实际混合17条与单类型4条文件通过；原生Microsoft Excel打开仍待验证 |
| 国内注册证 | 真实9条逐列、日期、项目、备注通过；长号/前导零/等号文本由自动化覆盖 |
| CE证书 | 表头/公司合并/第二段移动及空值通过；历史字段有值由自动化覆盖；无可验证分段来源单独报告 |
| FDA证书 | 真实4条、持证主体合并、序号、链接、日期和空列通过；File No./状态有值由自动化覆盖；无多Listing关系不推断 |
| 数据与显示 | 真实单元格、模板表头样式/列宽、渲染图通过；部分日期和公式型文本由自动化覆盖 |
| 快照与上限 | 30分钟过期、记录/行数上限、全不可用均由服务测试覆盖；真实100条满载与线上网关未验证 |
| 部分跳过 | 删除/失权/未知类型、计数守恒自动化通过；未对他人测试数据执行删除或撤权 |
| 防重与并发 | 自动化通过；真实响应中断后保留原ID取回且审计一次 |
| 重取窗口 | 五分钟固定窗口、快照过期后已受理取件、活动流、重试不续期由可控时钟测试覆盖；真实网络重取已验证 |
| 权限与审计 | 重取失权永久失效与流清理由服务测试覆盖；真实数据库只读核对一请求一审计及完整有序ID |

当前不能标为“全部验收完成”的具体原因：原生Excel打开未完成；100条真实满载和线上网关没有实测环境；真实撤权/删除及完整异常矩阵尚无任务自有数据页面验收，当前由服务自动化覆盖。CE/FDA无可验证关系已按用户允许口径交付，不作为停止其他工作的原因。

## 用户提供审批账号后的补验

用户提供注册部经理账号及电子签名PIN后，通过Playwright依次批准本任务53/54/55三笔申请。全部业务动作由真实前端完成；未用API或数据库代办审批、导出；数据库仅SELECT审计。凭据未写入脚本或验收文档。

| 自有样本页面验收 | 结果 |
| --- | --- |
| 三笔签名审批 | 国内、CE、FDA均通过，待办移除，正式列表新增versionId=35/36/37 |
| 产品前缀筛选 | 精确命中本任务3条，三类型各1条；其他记录不在文件内 |
| 全部筛选结果导出 | task-owned-all.xlsx，实际导出3、跳过0，生成711毫秒 |
| 勾选及表头排序 | 3条全选后certificateNo升序，提交3个准确ID；审计有序ID为[35,37,36]，生成66毫秒 |
| 跨页自有数据 | 20条全列表改10条/页，第1页选国内35，第2页选FDA37与CE36，菜单3条，成功下载task-owned-crosspage.xlsx，仅含这3条 |
| 国内有值/缺值 | 0001234567890123456789保持文本；首次/生效/到期为日期；等号多行备注保持字符串；缺失项目代码空白 |
| CE有值/缺值 | 公司、中英文名、商标正确；MDR/MDD/延期函/CE Marker留空，整条保留；未把公告机构0123误填为CE Marker |
| FDA有值/缺值 | K0000123、Listing 00012345保留原值；真实registrationLink正确；获批/列名日期正确；File No./Listing status空白，整条保留 |
| 原ID重取 | 文件字节及SHA256相同；expiresAt不变；同请求持久化审计恰1条 |

证据：output/registration-export-e2e/task-owned-{all,retry,selected-sorted,crosspage}.xlsx、task-owned-responses.json、task-owned-source.json、task-owned-verification.json、audit-task-owned-readonly.jsonl。verify-task-owned.py已执行通过。

本轮没有修改业务源码，无需重复已通过且未变化的57项后端、19项前端测试。本节通过范围为自有数据审批到导出、字段值/空值、勾选排序/跨页及重取链路，不将其扩大为附件全部异常与容量验收通过。

联调文件与证据：`output/registration-export-e2e/`，含下载工作簿、请求响应、只读审计、断网重取记录；桌面模板未修改。
