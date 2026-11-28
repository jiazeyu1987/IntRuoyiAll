$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$evidence=Join-Path $PSScriptRoot '2026-11-28-03-本地重测'
$report=Join-Path $root ('report\'+(Get-Date -Format 'yyyyMMddHHmmss')+'.xlsx')
$commands=[System.Collections.Generic.List[object]]::new()
function Set-Node($path,$props){$commands.Add(@{command='set';path=$path;props=$props})}
function Add-Node($parent,$type,$props){$commands.Add(@{command='add';parent=$parent;type=$type;props=$props})}
$production='本地，测试1，张飞红；生产执行、生产班长、活跃订单池；查881MO000017，确认实例1009200682。'
$inspection='本地，测试1，杨珊；生产执行、检验填报；选881MO000017、对应工序、首检或巡检轮次。'
$review='本地，测试1，吴学燕；生产执行、检验组长、PQC管理及历史表单；按881MO000017查工序、类型和轮次。'
$rows=@{
'已通过'=@(
 @('通过01','生产和检验进度','两个进度均为100%，不等于已完工。','生产检验及必要复核完成。','张飞红刷新同一实例查回双100%，放行申请未申请。',($production+'刷新查询正式进度。'),'生产为已有记录查回，本轮未重复报工。','生产组长\续办-生产检验双100未申请.png'),
 @('通过02','检验填报','首检8条，两轮巡检各11条，共30条已提交。','按本单要求提交后刷新仍可查到。','杨珊逐工序提交并查回30条，巡检统一随机为2轮。',($inspection+'分别查看首检及两轮巡检。'),'不是各工序独立随机轮数；计数不等于复核通过。','PQC检验员\04-刷新查回首检8及两轮巡检各11.png'),
 @('通过03','退回补正及最终复核','三条补正闭环，30条全部复核完成。','原检验员补正后由吴学燕独立复核。','组装一巡检第一轮、硅化一巡检第一轮、小包装首检已重提并复核通过；其余27条此前通过。',($review+'退回这三条；杨珊从检验填报补正重提；吴学燕复核后刷新查审核人及时间。'),'小包装第二密封强度明确确认5份模拟结果及标准，未沿用默认值；未发起不合格评审。','PQC组长\13-小包装首检第二密封强度复核通过.png'),
 @('通过04','第四工序数量差异澄清','不是同一字段前后不一致。','原始生产量与本单分配量分别核对。','原始生产5262件，本单分配5012件，相差250件，与此前未分配数量一致。',($production+'详情、生产表单、第四工序，对照原始生产和本单分配。'),'撤回旧报告数量冲突疑点；未重复分配。','生产组长\第四工序-原始5262本单5012.png')
)
'未通过'=@(
 @('阻塞01','双100%后无法完工','本单完工未通过，缺正式生产批号。','批号等正式前置齐备后才允许完工申请。','批号为空、完工按钮禁用，提示缺少正式生产批号，请由上游岗位维护后再申请完工。',($production+'刷新查询双100%，查看批号及完工按钮提示；管理员进入数据同步、生产订单，工单编号包含881MO000017查询并打开详情，批次号也为空且禁用。'),'是数据前置缺失造成的阻塞，未确定程序根因；未完工、未申请、未猜填批号。','生产组长\续办-刷新批号空完工禁用.png')
)
'存疑'=@(
 @('存疑01','严格5%及第二工序超限','严格比例未满足，参数超限缺少条件。','5012件增加5%为5262.6件；超出20%须有正式上限。','第一工序保存5262件，增加约4.988%；第二工序缺明确上限，无法验证超出20%。',($production+'详情、生产表单；第一工序对照计划及原产数量，第二工序精洗查看参数范围与实际值。'),'数量取整口径未确认，不重复报工；截图仅展示第二工序范围不足，第一工序数量依据生产回执。','生产组长\第二工序-参数范围查回.png'),
 @('存疑02','第四工序参数超范围','没有设备参数，条件不适用。','指南要求有参数时才测超范围。','本工序没有可填写参数，未输入超限值。',($production+'详情、生产表单、第四工序清洁；查看设备和参数区域。'),'不为测试擅自增加设备参数；不适用不等于功能故障。','生产组长\第四工序-原始5262本单5012.png'),
 @('存疑03','金蝶原单正式批号','未取得原单批号，历史来源与组织未确认。','需可对应的原单批号证据，不能凭同名工单跨环境匹配。','本地详情批号空；不能据此断定ERP原单为空。当前配置指向正式账套，但不证明本单历史来源。','本地，测试1，管理员；数据同步、生产订单；工单编号包含881MO000017查询，核对物料AW.107.02.01.4002、5012件，再打开详情查看批次号。','需集成配置责任人确认本单历史来源及组织，再由该来源ERP订单责任人核对原单。未执行同步或远程ERP操作。','ERP批号核实\02-本单详情批号为空.png'),
 @('存疑04','本地当前连接正式账套','当前配置已查明，本单来源仍未确认。','本地测试不得据当前配置猜测本单历史来源。','11月28日05时46至05时51分，真实页面显示当前连接为正式账套；具体组织及维护人未证实。','本地，测试1，管理员；ERP连接配置查看当前连接；配置管理搜索当前金蝶连接参数并核对值。','未切换连接、未保存配置、未访问正式ERP。原单维护和同步需要先明确来源与权限边界。','来源配置核实\01-测试1当前正式账套.png'),
 @('存疑05','完工至上市放行','前置阻塞，以下节点均未执行。','完工申请、生产放行、四类上传、上市放行依次完成。','尚无生产放行申请，未生产放行、未上传四类资料、未上市放行或归档。',($production+'批号恢复后张飞红完工申请；朱利江验证签名生产放行；成功后朱月浪上传来料检、丁加美上传灭菌、钱凤琴分别上传成品报告与记录；四类保存查回后宋媛验证签名上市放行，再刷新核对结果。'),'未测不等于已确认功能失败；不提前上传，不执行归档。整条主链尚未完成。','生产组长\续办-生产检验双100未申请.png')
)
}
$headers=@('编号','检查内容','判定与状态','应当怎样','实际发生什么','完整测试路径与复现边界','恢复条件与证据限制','证据截图')
$widths=@(12,27,34,36,48,65,52,98)
$colors=@{'已通过'='28785B';'未通过'='AF3947';'存疑'='A87318'}
$first=$true
foreach($sheet in @('已通过','未通过','存疑')){
 if($first){Set-Node '/Sheet1' @{name=$sheet};$first=$false}else{Add-Node '/' 'sheet' @{name=$sheet}}
 Set-Node "/$sheet" @{tabColor=$colors[$sheet];freeze='C5';autoFilter="A4:H$(4+$rows[$sheet].Count)"}
 Set-Node "/$sheet/A1" @{value="第03主链本地重测结果：$sheet";merge='A1:H1';font='Microsoft YaHei';size='20';bold='true';fill=$colors[$sheet];'font.color'='FFFFFF'}
 Set-Node "/$sheet/A2" @{value='本地，测试1；订单881MO000017，实例1009200682，计划5012件。依据截至2026年11月28日05时54分收到的执行岗证据。';merge='A2:H2';font='Microsoft YaHei';size='11';wrapText='true'}
 Set-Node "/$sheet/A3" @{value='仅第03主链。整链未完成：双100%且30条复核完成，正式批号空，完工及下游未执行。均为模拟测试，不代表真实质量批准。生产旧记录仅查回，不冒称本轮重新执行。';merge='A3:H3';font='Microsoft YaHei';size='11';wrapText='true'}
 foreach($headerRow in 1..4){Set-Node "/$sheet/row[$headerRow]" @{height='34'}}
 for($column=0;$column -lt 8;$column++){
  $letter=[string][char](65+$column)
  Set-Node "/$sheet/col[$letter]" @{width=[string]$widths[$column]}
  Set-Node "/$sheet/${letter}4" @{value=$headers[$column];fill='253E50';'font.color'='FFFFFF';font='Microsoft YaHei';size='11';bold='true';wrapText='true'}
 }
 $rowIndex=5
 $sheetRows=$rows[$sheet]
 if($sheetRows[0] -is [string]){$sheetRows=,@($sheetRows)}
 foreach($entry in $sheetRows){
  Set-Node "/$sheet/row[$rowIndex]" @{height='290'}
  for($column=0;$column -lt 8;$column++){
   $letter=[string][char](65+$column)
   $text=if($column -eq 7){''}else{[string]$entry[$column]}
   Set-Node "/$sheet/$letter$rowIndex" @{value=$text;font='Microsoft YaHei';size='11';wrapText='true';valign='top';fill='FFFFFF'}
  }
  $imagePath=Join-Path $evidence $entry[7]
  if(-not(Test-Path -LiteralPath $imagePath)){throw "缺少截图：$imagePath"}
  Add-Node "/$sheet" 'picture' @{src=$imagePath;anchor="H${rowIndex}:I$($rowIndex+1)";name=[string]$entry[0];alt=[string]$entry[1]}
  $rowIndex++
 }
}
$batch=Join-Path $evidence '本轮报告生成指令.json'
$commands | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $batch -Encoding utf8
officecli create $report
if($LASTEXITCODE -ne 0){throw '创建失败'}
officecli batch $report --input $batch --stop-on-error --json | Set-Content -LiteralPath (Join-Path $evidence '本轮报告生成结果.json') -Encoding utf8
if($LASTEXITCODE -ne 0){throw '填写失败'}
officecli close $report
if($LASTEXITCODE -ne 0){throw '保存失败'}
Write-Output "REPORT=$report"


