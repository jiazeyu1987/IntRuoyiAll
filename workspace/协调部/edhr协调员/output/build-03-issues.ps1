$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$evidence=Join-Path $PSScriptRoot '2026-11-27-03-正常生产至上市放行\生产组长'
New-Item -ItemType Directory -Path (Join-Path $root 'report') -Force | Out-Null
$report=Join-Path $root ('report\'+(Get-Date -Format 'yyyyMMddHHmmss')+'.xlsx')
$commands=[System.Collections.Generic.List[object]]::new()
function Set-Node($path,$props){$commands.Add(@{command='set';path=$path;props=$props})}
function Add-Node($parent,$type,$props){$commands.Add(@{command='add';parent=$parent;type=$type;props=$props})}
$basePath='本地服务器，测试1租户，张飞红登录。进入生产执行、生产班长、活跃订单池，筛选订单881MO000017，确认实例1009200682。'
$detail=$basePath+'打开详情，进入生产表单，选择对应工序。'
$rows=@{
'已通过'=@(
 @('通过01','第5工序驳回后重提与复核','本项通过，非整条主链通过。','重新提交后可复核，数量不重复累计。','刘智慧重新提交5012件，22时37分34秒提交，22时43分17秒完成张飞红复核；有效数量仍5012件。',($basePath+'报工管理驳回组装Ⅰ原记录；生产作业选择本单、第5工序、刘智慧，重新填5012件并正式提交；报工管理分配及组长复核；详情查回数量及签名。'),'截图证明重提结果。驳回后人员日期清空无单独截图，不作为程序缺陷。','05-重提后5012件及组长复核查回.png'),
 @('通过02','生产达到100%','生产阶段通过，检验和放行未完成。','生产提交与组长复核后，生产达100%。','回执称14工序使用不同员工并逐项复核；活跃池截图显示生产100%、检验0%、未申请。',($basePath+'刷新活跃订单池，核对生产、检验进度及申请状态。'),'截图核实汇总进度；14条明细依执行岗逐项查回回执，未由协调岗重复操作。','14工序生产100检验0.png'),
 @('通过03','超量保存后可继续生产','保存及续办通过；提示另列存疑。','超量不阻断正常生产，原始数量可查回。','粗洗保存5262件，计划5012件，有操作及复核记录；后续生产已100%。',($detail+'选第1工序粗洗，查看5262件及操作、复核记录；返回活跃池核对后续生产进度。'),'仅确认保存与续办，不把约4.988%当严格5%，不代表提示已验证。','01-粗洗超量及组长复核.png')
)
'未通过'=@(
 @('问题01','重复变量错误遮罩中断填写','发生过页面中断，重进后恢复。','正常填写不应被代码错误遮罩挡住。','第8工序草稿操作期间出现编译错误，提示变量重复声明。',($basePath+'进入生产作业，选择本单并操作第8工序草稿；当时出现遮罩。重进后恢复，尚无稳定必现步骤。'),'前端开发排查CompanyVersionWorkbench.vue第6行第6列，变量voiceLabel重复声明；仅证明当时发生错误，未确定业务根因。','环境中断-重复变量错误.png'),
 @('问题02','模板解析错误遮罩中断填写','发生过页面中断，重进后恢复。','正常填写不应因模板解析错误中断。','第11工序操作期间出现表达式解析错误，涉及转义写法。',($basePath+'进入生产作业，操作本单第11工序；当时出现解析错误遮罩。重进后恢复，尚无稳定必现步骤。'),'前端开发排查FolderTemplateLibraryEditor.vue第7行第68列。截图证明错误发生，不代表已定位根因。','环境中断-模板解析错误.png'),
 @('问题03','第10工序SOP读取失败','当时未能打开，原因待查。','有有效SOP时应能打开；没有SOP不自动构成缺陷。','第10工序SOP弹窗显示读取失败；未确认文件、配置或请求原因。',($basePath+'进入生产作业，选第10工序并打开SOP，查看读取失败提示；责任岗核对当时文件关联与请求。'),'交工艺及软件责任岗核对。不把允许无SOP误判为缺陷，不声称已稳定复现或修复。','环境中断-SOP请求失败.png')
)
'存疑'=@(
 @('存疑01','严格超量5%未达到','比例未达到，不是已确认程序缺陷。','5012件增加5%为5262.6件，需明确整数取整规则。','回执记录输入5262.6后变5262；增加250件约4.988%。未重复报工追比例。',($basePath+'生产作业选第1工序粗洗，观察数量栏输入5262.6时精度；本实例只读查回5262，不重复提交。'),'确认整数数量下取整口径。截图仅证明保存5262件，不证明截断瞬间或严格5%通过。','01-粗洗超量及组长复核.png'),
 @('存疑02','第2工序无法判断超出20%','缺明确上限，不能验证。','有正式范围后才能计算超出上限20%。','精洗功率参考值无，实际100%；次数仅至少1次，无可用上限。',($detail+'选第2工序精洗，查看设备B09353功率参考、实际值与次数要求。'),'由参数责任岗确认范围或条件，不虚构上限，不把保存100%当超限通过。','02-精洗参数范围缺失查回.png'),
 @('存疑03','第4工序没有设备参数','条件不适用，不是功能故障。','现行指南要求有参数时才测超范围。','清洁工序暂无设备信息，无参数可填，本项未做超限输入。',($detail+'选第4工序清洁，检查设备信息和参数区域。'),'保留不适用结论，不额外配置设备参数，不加入旧版第8工序要求。','04-清洁无参数及分配查回.png'),
 @('存疑04','超量提示证据不足','数量已保存，提示尚未核实。','规定入口有超量提示且不阻断续办。','截图确认粗洗5262件及复核，未充分记录超量提示内容及位置。',($basePath+'在活跃订单内查看超量提示入口或专用抽屉，对照第1工序计划5012件、产出5262件。'),'仅缺提示证据，不撤销已确认保存及续办；不要求普通列表必须显示提示。','01-粗洗超量及组长复核.png'),
 @('存疑05','第4工序分配证据不一致','暂不确认最终剩余250件。','最终正式表单与签名分配应一致。','分配弹窗显示5012件、剩250件；正式表单截图本单数量5262件，现有证据不能证明最终剩250件。',($basePath+'报工管理查看第4工序清洁最终分配、签名记录；详情生产表单核对原产、本单分配和余额。'),'一次只读对照最终状态，不重复分配。属证据冲突，旧版第4超量不是当前门槛。','04-清洁无参数及分配查回.png'),
 @('存疑06','检验至上市放行未完成','主链未完成；未测不等于失败。','检验复核完成，双100%后依次完工、生产放行、四类上传、上市放行。','现生产100%、检验0%、未申请。检验、退回补正、完工、PQC放行、上传及上市放行均无本轮完成证据。',($basePath+'杨珊检验，吴学燕逐条复核及退回补正；双100%后张飞红完工申请，朱利江生产放行，责任岗各自上传四类资料，宋媛上市放行并查回。'),'此为阶段问题清单，非完整验收。不执行归档，不复用历史签名。','14工序生产100检验0.png')
)
}
$headers=@('编号','检查内容','判定与状态','应当怎样','实际发生什么','完整测试路径与复现边界','处理建议与证据限制','证据截图')
$widths=@(12,27,30,34,45,60,50,98)
$colors=@{'已通过'='28785B';'未通过'='AF3947';'存疑'='A87318'}
Add-Type -AssemblyName System.Drawing
$first=$true
foreach($sheet in @('已通过','未通过','存疑')){
 if($first){Set-Node '/Sheet1' @{name=$sheet};$first=$false}else{Add-Node '/' 'sheet' @{name=$sheet}}
 Set-Node "/$sheet" @{tabColor=$colors[$sheet];freeze='C5';autoFilter="A4:H$(4+$rows[$sheet].Count)"}
 Set-Node "/$sheet/A1" @{value="第03主链阶段问题清单 · $sheet";merge='A1:H1';font='Microsoft YaHei';size='20';bold='true';fill=$colors[$sheet];'font.color'='FFFFFF'}
 Set-Node "/$sheet/A2" @{value='本地 · 测试1 · 订单881MO000017 · 实例1009200682 · 计划5012件 · 截止2026年11月27日23时04分收到的回执';merge='A2:H2';font='Microsoft YaHei';size='11';fill='EAF0F4';wrapText='true'}
 Set-Node "/$sheet/A3" @{value='仅第03主链，均为模拟测试，不代表真实质量批准。生产100%，检验及上市放行未完成。截图为执行岗原图，协调岗已目视核对；文字注明证据局限。';merge='A3:H3';font='Microsoft YaHei';size='11';wrapText='true'}
 foreach($headerRow in 1..4){Set-Node "/$sheet/row[$headerRow]" @{height='32'}}
 for($column=0;$column -lt 8;$column++){
  $letter=[string][char](65+$column)
  Set-Node "/$sheet/col[$letter]" @{width=[string]$widths[$column]}
  Set-Node "/$sheet/${letter}4" @{value=$headers[$column];fill='253E50';'font.color'='FFFFFF';font='Microsoft YaHei';size='11';bold='true';wrapText='true';valign='center'}
 }
 $rowIndex=5
 foreach($entry in $rows[$sheet]){
  Set-Node "/$sheet/row[$rowIndex]" @{height='290'}
  for($column=0;$column -lt 8;$column++){
   $letter=[string][char](65+$column)
   $text=if($column -eq 7){''}else{[string]$entry[$column]}
   $fill=if($rowIndex%2 -eq 0){'F0F4F7'}else{'FFFFFF'}
   Set-Node "/$sheet/$letter$rowIndex" @{value=$text;font='Microsoft YaHei';size='11';wrapText='true';valign='top';fill=$fill;'font.color'='243747'}
  }
  $imagePath=Join-Path $evidence $entry[7]
  $image=[System.Drawing.Image]::FromFile($imagePath)
  $scale=[Math]::Min(675/$image.Width,425/$image.Height)
  $imageWidth=($image.Width*$scale/96).ToString('0.000',[Globalization.CultureInfo]::InvariantCulture)+'in'
  $imageHeight=($image.Height*$scale/96).ToString('0.000',[Globalization.CultureInfo]::InvariantCulture)+'in'
  $image.Dispose()
  Add-Node "/$sheet" 'picture' @{src=$imagePath;anchor="H${rowIndex}:I$($rowIndex+1)";name=[string]$entry[0];alt=[string]$entry[1]}
  $rowIndex++
 }
}
$batch=Join-Path $PSScriptRoot '第03问题清单-生成指令.json'
$commands | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $batch -Encoding utf8
officecli create $report
if($LASTEXITCODE -ne 0){throw '创建工作簿失败'}
officecli batch $report --input $batch --stop-on-error --json | Set-Content -LiteralPath (Join-Path $PSScriptRoot '第03问题清单-生成结果.json') -Encoding utf8
if($LASTEXITCODE -ne 0){throw '填写或插图失败'}
officecli close $report
if($LASTEXITCODE -ne 0){throw '保存工作簿失败'}
Write-Output "REPORT=$report"
