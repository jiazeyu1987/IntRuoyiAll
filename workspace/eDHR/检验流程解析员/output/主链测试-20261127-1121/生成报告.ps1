$ErrorActionPreference='Stop'
$run=$PSScriptRoot
$cases=[System.Collections.Generic.List[object]]::new()
function Add-Case($sheet,$node,$target,$operation,$expected,$actual,$next,$image){$cases.Add([pscustomobject]@{Sheet=$sheet;Node=$node;Target=$target;Operation=$operation;Expected=$expected;Actual=$actual;Next=$next;Image=$image})}
Add-Case '已通过' '环境身份和入口' '本地，测试1，潘鑫' '进入系统核对身份及检验菜单。' '环境、租户和账号正确。' '页面显示测试1和潘鑫，检验编制、通用编制和正式规程菜单可见。' '菜单可见不代表全部操作均可完成。' '02-ID导入九工序十九项目.png'
Add-Case '已通过' '产品文件解析' 'ID产品' '选择真实产品，导入对应规程。' '生成可编辑的工序和项目。' '生成9道工序、19个检验项目。' '按指南不核对原文是否正确。' '02-ID导入九工序十九项目.png'
Add-Case '已通过' '产品文件解析' 'IDE产品' '选择真实产品，导入对应规程。' '显示工序、项目和待补充内容。' '生成7道工序、28项，12处设备待补充。' '本项仅验证导入，本次未发布该产品。' '17-IDE导入.png'
Add-Case '已通过' '产品文件解析' 'IDI产品' '选择真实产品，导入对应规程。' '显示工序、项目和待补充内容。' '生成6道工序、22项，8处设备待补充。' '本项仅验证导入，本次未发布该产品。' '18-IDI导入.png'
Add-Case '已通过' '产品文件解析' 'IDML产品' '选择真实产品，导入对应规程。' '显示工序、项目和待补充内容。' '生成10道工序、27项，9处设备待补充。' '本项仅验证导入，本次未发布该产品。' '19-IDML导入.png'
Add-Case '已通过' '产品文件解析' 'IDPR产品' '选择真实产品，导入对应规程。' '显示工序、项目和待补充内容。' '生成10道工序、27项，9处设备待补充。' '本项仅验证导入，本次未发布该产品。' '20-IDPR导入.png'
Add-Case '已通过' '错误文件拦截' '通用编制' '导入内容无效的文档。' '拒绝错误文件，不允许空内容发布。' '解析失败且发布不可用，没有生成正式版本。' '英文提示不易懂的问题另列未通过。' '08-错误文件拦截.png'
Add-Case '已通过' '失败后恢复' '大中包装通用规程' '错误文件之后导入有效文件。' '可以恢复正常解析和发布。' '有效文件生成1道工序3项，成功发布第4版。' '没有绕过失败或伪造解析结果。' '09-大中包装A4发布结果.png'
Add-Case '已通过' '缺项发布拦截' 'ID产品' '缺设备时打开发布预览。' '指出缺失项并禁止发布。' '显示8处设备缺失，确认发布不可用。' '补齐设备后再继续，不关闭设备要求。' '03-缺设备发布预览.png'
Add-Case '已通过' '新增修改和排序' 'ID清洗工序' '新增临时项目、改名并上移。' '数量、名称和顺序正确变化。' '清洗从1项变2项，临时项目上移到外观之前。' '临时项目未发布，后续已删除。' '04-新增修改上移临时项目.png'
Add-Case '已通过' '取消删除和确认删除' 'ID清洗工序' '先取消删除，再确认删除。' '取消保留项目，确认恢复原数量。' '取消后仍保留；确认后清洗恢复1项，总数恢复19项。' '截图是删除后保存恢复状态，不是删除弹窗截图。' '05-刷新恢复浏览器草稿.png'
Add-Case '已通过' '设备配置' 'ID的8处设备要求' '逐项选择符合要求的真实设备候选项。' '设备缺项归零。' '撤压、砝码、扭力、高压和低压等8处完成绑定，待修正问题归零。' '仅证明编制器配置，正式版设备回查受阻。' '05-刷新恢复浏览器草稿.png'
Add-Case '已通过' '保存重复保存和刷新恢复' 'ID编制草稿' '连续保存两次，再刷新并恢复。' '无重复保存错误，内容能恢复。' '两次提示保存在当前浏览器；恢复后仍为9道工序19项，设备缺项为零。' '浏览器草稿不等于服务器正式版本。' '05-刷新恢复浏览器草稿.png'
Add-Case '已通过' '发布提交反馈' 'ID产品' '补齐设备后预览并确认发布一次。' '出现明确发布完成反馈。' '页面显示发布完成，G的第6版。' '只确认提交反馈，正式内容重新读取另列存疑。' '06-ID发布完成G6.png'
Add-Case '已通过' '通用规程发布前基线' '大中包装与初包装' '读取现有正式版本和数量。' '取得本次实际基线。' '大中包装为A的第3版，1道工序3项；初包装为A的第8版，1道工序4项。' '美联旧版本未独立取证，不使用上次报告代替。' '07-大中包装基线A3.png'
Add-Case '已通过' '通用导入保存预览发布' '大中包装通用规程' '导入文件，保存并预览发布。' '发布后显示版本和项目。' '发布A的第4版，结果窗口显示1道工序3项。' '随后从正式页面读取成功，并继续升到第5版。' '09-大中包装A4发布结果.png'
Add-Case '已通过' '通用导入保存预览发布' '初包装通用规程' '导入、保存、预览发布，再从正式页面查询。' '正式页面可找到新版本。' '发布A的第9版，1道工序4项；正式页面重新选择仍显示第9版和4项。' '器具原文仍保留，不代表执行前设备已全部绑定。' '21-初包装A9发布结果.png'
Add-Case '已通过' '通用导入保存预览发布' '美联初包装通用规程' '导入、保存、预览发布并重新查询。' '新版本及数量持久保留。' '发布B的第3版，重新查询为已发布、1道工序4项。' '没有发布临时抽样修改。' '23-美联B3重新查询.png'
Add-Case '已通过' '通用草稿隔离' '大中包装通用规程' '修改名称保存草稿，再查看旧正式版。' '草稿修改不覆盖正式版。' '草稿第5版名称增加主链复测；正式第4版仍保留原名称。' '仅验证名称与数量，未完整比较设备及产品关联。' '13-通用草稿未改变正式版.png'
Add-Case '已通过' '通用升级与刷新核验' '大中包装通用规程' '发布第5版并整页重新加载。' '新名称和数量保留。' '重新加载为A的第5版、已发布，名称含主链复测，仍为1道工序3项。' '该测试名称保留，未执行重置。' '15-通用A5刷新验证.png'
Add-Case '已通过' '历史内容保留' '大中包装历史第4版' '发布第5版后再选历史第4版。' '旧版名称和数量不被新版覆盖。' '旧版原名称和1道工序3项仍在，页面只读。' '版本字段显示矛盾另列未通过，不据此认定内容丢失。' '16-历史版本编辑版本显示错误.png'
Add-Case '已通过' '导出回导主要字段' '大中包装第4版的3项' '导出实际文件再回导，逐项查看。' '主要字段和顺序保持一致。' '外观、标签说明书合格证、包装标识三项的顺序、接受标准、方法、器具及抽样说明一致。' '隐藏字段及内部设备编号未完整核对，另列存疑。' '11-回导第三项要求.png'
Add-Case '已通过' '抽样编辑和放弃修改' '美联初包装外观项' '首检数量从20临时改21，不保存，刷新。' '未保存改动不进入正式版。' '先显示21和未保存修改；刷新恢复20，未保存提示消失。' '没有保存或发布临时数值。' '25-刷新丢弃未保存修改.png'
Add-Case '已通过' '筛选与无匹配提示' '通用规程选择框' '搜不存在的名称，再搜大中包装。' '无结果有提示，有结果只显示符合项。' '不存在的名称显示无匹配数据；搜索大中包装只显示对应规程。' '不是产品检验项目表的筛选。' '26-无匹配数据.png'
Add-Case '已通过' '单页数量与翻页边界' '美联初包装4项' '查看总数、页码及翻页按钮。' '总数4项，一页即可显示。' '共4条，每页20条，页码1，上下页均不可用。' '跨页未覆盖，不能用单页状态代替。' '25-刷新丢弃未保存修改.png'
Add-Case '未通过' '正式产品项目列表加载' '检验规程页面' '打开页面并重新加载一次。' '能选择产品并读取正式规程。' '持续显示项目代码加载失败和系统异常，产品列表不可用。' '需维护方修复本地列表加载；关联、产品历史和重置依赖它。本次未改代码或服务。' '01-正式规程项目加载失败.png'
Add-Case '未通过' '历史版本显示一致性' '大中包装历史第4版' '新第5版发布后查看历史第4版。' '当前选择与只读版本说明一致。' '顶部选择历史第4版且名称正确，但只读编辑版本显示第6版，容易误认。' '需维护方核实并修正展示；未证明数据库版本错误或数据丢失。' '16-历史版本编辑版本显示错误.png'
Add-Case '未通过' '错误提示可读性' '通用编制无效文件' '查看解析失败提示。' '用中文说明文件格式有误及处理办法。' '页面显示英文压缩结构错误，普通用户不易理解。' '拒绝文件功能本身已通过，本项只评价提示文字。' '08-错误文件拦截.png'
Add-Case '存疑' '产品正式基线与发布回查' 'ID和非目标产品' '准备读取正式版本及发布后的内容。' '取得本次前后版本和完整正式内容。' '产品列表不可用；ID显示发布第6版完成，但尚未重新读取正式内容。' '截图是阻塞证据，不是该业务动作已执行的证据。' '01-正式规程项目加载失败.png'
Add-Case '存疑' '指定通用版本关联并发布' 'ID产品与通用规程' '准备关联指定通用版本。' '产品发布后保留确定版本。' '产品正式入口受阻，未提交关联。' '列表修复后从本节点恢复，不重复已通过的解析。' '01-正式规程项目加载失败.png'
Add-Case '存疑' '产品草稿修改和升版' 'ID正式产品规程' '准备改草稿、比较旧版并发布新版。' '旧正式版不变，新版含修改。' '未进入正式产品编辑页；编制器草稿恢复不能代替产品正式草稿测试。' '等待产品列表恢复。' '01-正式规程项目加载失败.png'
Add-Case '存疑' '通用升级后旧关联不变' 'ID产品的原通用关联' '通用升版后准备回查产品旧关联。' '旧关联仍固定旧通用版本。' '大中包装第5版已核实，产品旧关联因列表故障未核实。' '发布确认框说保留关联，不等于实际关联已核验。' '15-通用A5刷新验证.png'
Add-Case '存疑' '产品主动改绑新版' 'ID产品' '准备主动改绑通用新版并发布。' '主动操作后才采用新版。' '未执行改绑或发布，产品正式入口受阻。' '先完成旧版关联核实，再继续此节点。' '01-正式规程项目加载失败.png'
Add-Case '存疑' '产品历史完整对照' 'ID新旧正式版本' '准备比较内容、设备、抽样和关联。' '新旧版本各自完整保留。' '本次未取得产品历史页证据，无法判断上次历史设备问题是否修复。' '未复用上次问题作为本次复现结论。' '01-正式规程项目加载失败.png'
Add-Case '存疑' '回导全部字段一致性' '大中包装导出文件' '检查逐项对照覆盖范围。' '所有字段都能确认一致。' '主要展示字段已比对；导出前结构化首检数量和隐藏设备编号没有逐项独立取证。' '回导可见首检3件、巡检特殊水平二及接收质量限2.5，仍不足以证明所有隐藏字段一致。' '12-回导抽样设备.png'
Add-Case '存疑' '产品项目筛选和跨页' '正式产品项目表' '准备验证项目筛选、无结果和跨页。' '表格结果、页码及总数一致。' '编制器增删总数和通用下拉筛选已测；产品正式表不可进入，通用仅4项不能验证跨页。' '需要实际可进入的产品项目表，不以其他控件代替。' '25-刷新丢弃未保存修改.png'
Add-Case '存疑' '抽样保存后正式生效' '产品抽样规则' '准备修改、保存并重新读取正式规则。' '保存后规则持久保留且旧版不变。' '只完成抽样临时编辑后放弃；未完成产品抽样保存后正式回查。' '临时21件已恢复20件，没有正式抽样变更。' '24-抽样临时改为21.png'
Add-Case '存疑' '目标项目重置' 'ID产品' '准备重置后重新查询。' '按说明清理目标数据。' '目标产品和基线不可读，未执行重置；上次重置失败不能当作本次复现。' '待列表恢复后执行并核实。' '01-正式规程项目加载失败.png'
Add-Case '存疑' '清理范围及非目标保护' 'ID及其他产品和通用规程' '准备重置后对照基线。' '只清理目标，不影响其他对象。' '未执行重置，也缺少产品非目标基线，不能判定范围正确。' '本次保留ID第6版发布反馈；通用大中包装第5版、初包装第9版、美联第3版已发布，没有清理。' '15-通用A5刷新验证.png'
$stamp=Get-Date -Format 'yyyyMMddHHmmss'
$workbook=Join-Path $run "report\$stamp.xlsx"
$commands=[System.Collections.Generic.List[object]]::new()
foreach($sheet in @('已通过','未通过','存疑')){$commands.Add(@{command='add';parent='/';type='sheet';props=@{name=$sheet}})}
$commands.Add(@{command='remove';path='/Sheet1'})
$headers=@('编号','测试环节','测试对象','操作方法','应有结果','实际结果','限制及后续处理','证据截图')
$columns=@('A','B','C','D','E','F','G','H')
$widths=@(9,24,28,34,34,48,48,80)
foreach($sheet in @('已通过','未通过','存疑')){
 $rows=@($cases|Where-Object Sheet -eq $sheet)
 $commands.Add(@{command='set';path="/$sheet";props=@{freeze='D3';autoFilter="A2:H$($rows.Count+2)"}})
 $commands.Add(@{command='set';path="/$sheet/A1:G1";props=@{merge='true';value="本地主链复测：$sheet $($rows.Count)项。2026年11月27日，测试1，潘鑫。全链未通过，项目列表异常使部分节点未执行。";fill='EAF0F6';bold='true';wrapText='true'}})
 $commands.Add(@{command='set';path="/$sheet/row[1]";props=@{height='42'}})
 for($columnIndex=0;$columnIndex -lt 8;$columnIndex++){
  $commands.Add(@{command='set';path="/$sheet/col[$($columns[$columnIndex])]";props=@{width=[string]$widths[$columnIndex]}})
  $commands.Add(@{command='set';path="/$sheet/$($columns[$columnIndex])2";props=@{value=$headers[$columnIndex];fill='21445A';'font.color'='FFFFFF';bold='true';wrapText='true'}})
 }
 $commands.Add(@{command='set';path="/$sheet/row[2]";props=@{height='28'}})
 $rowIndex=3
 foreach($case in $rows){
  $values=@(('QA'+($cases.IndexOf($case)+1).ToString('00')),$case.Node,$case.Target,$case.Operation,$case.Expected,$case.Actual,$case.Next,'')
  for($columnIndex=0;$columnIndex -lt 8;$columnIndex++){
   $commands.Add(@{command='set';path="/$sheet/$($columns[$columnIndex])$rowIndex";props=@{value=[string]$values[$columnIndex];'font.name'='微软雅黑';'font.size'='11';wrapText='true';'alignment.vertical'='top';fill=($(if($rowIndex%2 -eq 0){'F3F7FA'}else{'FFFFFF'}));border='thin';'border.color'='D9E2EA'}})
  }
  $commands.Add(@{command='set';path="/$sheet/row[$rowIndex]";props=@{height='238'}})
  $image=Join-Path (Join-Path $run '截图') $case.Image
  if(-not(Test-Path -LiteralPath $image)){throw "缺少截图：$image"}
  $commands.Add(@{command='add';parent="/$sheet";type='picture';props=@{src=$image;anchor="H${rowIndex}:I$($rowIndex+1)";alt="$($case.Node)：本次页面证据"}})
  $rowIndex++
 }
}
$inputPath=Join-Path $run '报告生成指令.json'
$commands|ConvertTo-Json -Depth 12|Set-Content -LiteralPath $inputPath -Encoding utf8
$cases|ConvertTo-Json -Depth 6|Set-Content -LiteralPath (Join-Path $run '本次测试记录.json') -Encoding utf8
officecli create $workbook
if($LASTEXITCODE -ne 0){throw '创建工作簿失败'}
officecli batch $workbook --input $inputPath --stop-on-error|Set-Content -LiteralPath (Join-Path $run '报告生成日志.txt') -Encoding utf8
if($LASTEXITCODE -ne 0){throw '生成工作簿失败，请检查任务日志'}
officecli close $workbook
if($LASTEXITCODE -ne 0){throw '保存工作簿失败'}
Write-Output $workbook
$cases|Group-Object Sheet|Select-Object Name,Count
