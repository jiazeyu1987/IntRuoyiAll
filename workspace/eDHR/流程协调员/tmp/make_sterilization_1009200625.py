from pathlib import Path
import hashlib
import json
import fitz
from pypdf import PdfReader
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph

root = Path(__file__).resolve().parents[1]
output = root / 'output' / 'pdf'
output.mkdir(parents=True, exist_ok=True)
target = output / 'SIM-1009200625-sterilization-test.pdf'
preview = output / 'SIM-1009200625-sterilization-test-preview.png'
pdfmetrics.registerFont(TTFont('Chinese', 'C:/Windows/Fonts/msyh.ttc', subfontIndex=0))
body = ParagraphStyle('Body', fontName='Chinese', fontSize=10, leading=18, wordWrap='CJK', spaceAfter=10)
title = ParagraphStyle('Title', parent=body, fontSize=20, leading=30, textColor=colors.HexColor('#9C2020'), alignment=1)
notice = ParagraphStyle('Notice', parent=body, fontSize=12, leading=22, textColor=colors.HexColor('#9C2020'), spaceAfter=18)
heading = ParagraphStyle('Heading', parent=body, fontSize=12, leading=20, spaceBefore=8)
sections = [
('一、附件身份与关联对象', [
'资料类别：灭菌报告（模拟测试附件，非正式灭菌报告）。',
'工单：SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891',
'本轮标识：1009200625；关联数量：5000件（任务指定数量，非灭菌实收或合格数量）。',
'指定环境：本地8081／测试1租户。仅用于本轮流程附件测试，不复用为其他周期依据。']),
('二、模拟内容及证据边界', [
'仅验证灭菌报告类别文件的格式与对象关联，不证明已实施灭菌、不作无菌或合格判定。',
'未提供或独立核实实际灭菌方式、设备、灭菌批号、参数、运行记录、实测结果及实施时间；本附件不编造这些内容。',
'无真实检测机构、报告签章、签名、审核批准或质量结论。本轮标识不作为实际灭菌日期依据。']),
('三、未决事项与使用限制', [
'据协调交接信息：生产超限5501、参数0/1001及501待分配仍为待核实／处置事项；本附件不覆盖、不消除这些问题。上述信息未由本岗独立核验。',
'交接时PQC尚未独立复核，PQC生产放行未发生。仅准备附件，不表示任何前置已满足或流程已放行。',
'本次不操作业务页面、不上传、不修改业务记录、不办理批准。后续仅在PQC生产放行正式成立且另接上传任务后，再核对正式任务与关联对象。'])]
story = [Paragraph('模拟测试 · 灭菌报告', title), Paragraph('非真实报告记录 · 不可作为真实质量依据', notice)]
for label, paragraphs in sections:
    story.append(Paragraph(label, heading))
    story.extend(Paragraph(paragraph, body) for paragraph in paragraphs)
story.append(Paragraph('无真实实测、签章或批准效力，仅限指定对象本轮模拟测试。', notice))
SimpleDocTemplate(str(target), pagesize=(210*mm,297*mm), leftMargin=19*mm, rightMargin=19*mm, topMargin=16*mm, bottomMargin=16*mm, title='模拟测试灭菌报告-1009200625', author='模拟附件准备').build(story)
reader = PdfReader(str(target), strict=True)
text = ''.join(page.extract_text() for page in reader.pages)
required = ['模拟测试','非真实报告记录','不可作为真实质量依据','1009200625','5000','SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891','5501','0/1001','501','PQC生产放行未发生']
missing = [value for value in required if value not in text]
if missing or len(reader.pages) != 1:
    raise RuntimeError({'missing':missing,'pages':len(reader.pages)})
with fitz.open(target) as rendered:
    rendered[0].get_pixmap(matrix=fitz.Matrix(1.5,1.5)).save(preview)
print(json.dumps({'pdf':str(target),'preview':str(preview),'pages':len(reader.pages),'bytes':target.stat().st_size,'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'strict_parse':'PASS','required_text':'PASS','render':'PASS','business_operations':'NONE'},ensure_ascii=False,indent=2))
