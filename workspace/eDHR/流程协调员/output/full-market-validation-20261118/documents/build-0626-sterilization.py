from pathlib import Path
import hashlib
import json
import fitz
from pypdf import PdfReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.platypus import SimpleDocTemplate, Paragraph

folder = Path(__file__).resolve().parent
pdf = folder / 'SIM-1009200626-sterilization-test.pdf'
if pdf.exists():
    raise FileExistsError('不得覆盖已有本轮资料')
pdfmetrics.registerFont(TTFont('CN', 'C:/Windows/Fonts/simhei.ttf'))
body = ParagraphStyle('body', fontName='CN', fontSize=11, leading=18, spaceAfter=10, wordWrap='CJK')
heading = ParagraphStyle('heading', parent=body, fontSize=14, spaceBefore=8, textColor=colors.HexColor('#163d52'))
title = ParagraphStyle('title', parent=body, fontSize=21, leading=28, alignment=1, spaceAfter=16)
warning = ParagraphStyle('warning', parent=body, fontSize=14, leading=23, alignment=1, textColor=colors.HexColor('#b91c1c'), borderWidth=1, borderColor=colors.HexColor('#b91c1c'), borderPadding=8, spaceAfter=18)
order = 'SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891'
story = [Paragraph('灭菌报告 · 模拟测试附件', title), Paragraph('模拟测试、非真实报告/记录<br/>不可作为真实质量依据', warning)]
sections = [
('一、对象及用途', [
'文件类别：灭菌报告。仅用于本轮完整上市放行流程模拟验证。',
'环境：本地 localhost:8081；租户：测试1。',
'周期：1009200626；型号：AI30；本轮对象数量：5000件。',
f'完整工单：{order}',
'本轮新建，不复用旧周期文件、签名或完成状态。']),
('二、模拟边界', [
'本文件仅承载类别、周期及对象关联的模拟附件数据，不代表真实灭菌活动已经实施。未填写真实灭菌方式、设备、批号、工艺参数、实测结果或机构证明。',
'不提供灭菌合格、无菌保证或上市放行结论；无真实签章、无批准签名，不替代正式责任决定及身份验证。']),
('三、交接异常披露（非本人独立核实）', [
'以下信息来自本次协调交接，非本人独立核实：生产两处5501、参数0/1001及粗洗余501未跨单分配。原交接未给出上述数字的完整字段和单位，本文不自行解释或改写。',
'本附件不掩盖或消除上述异常，不追认真实质量合格；异常处置、生产及检验完成情况以有权岗位正式记录及系统核验为准。']),
('四、本次准备状态', [
'仅完成本地模拟文件准备与格式检查；未操作业务页面、未上传、未批准、未执行任何签名。PQC正式放行及本类别实际责任指派未在本次准备中核实。',
'后续收到上传任务，且PQC正式放行及本类别责任指派核对后，方可由有效责任人在真实前端办理。本地文件准备不等于上传或上市放行完成。'])]
for label, paragraphs in sections:
    story.append(Paragraph(label, heading))
    story.extend(Paragraph(text, body) for text in paragraphs)
SimpleDocTemplate(str(pdf), pagesize=A4, leftMargin=42, rightMargin=42, topMargin=32, bottomMargin=35, title='SIM-1009200626 灭菌报告（模拟测试）', author='灭菌资料准备岗').build(story)
reader = PdfReader(pdf, strict=True)
text = ''.join(page.extract_text() for page in reader.pages)
required = ['1009200626', order, 'AI30', '5000件', '灭菌报告', '模拟测试', '非真实报告/记录', '不可作为真实质量依据', '两处5501', '0/1001', '粗洗余501未跨单分配', '非本人独立核实', '未上传', '未批准']
for phrase in required:
    if phrase not in text:
        raise ValueError(f'缺少内容：{phrase}')
if '1009200625' in text:
    raise ValueError('引用了旧周期')
rendered = fitz.open(pdf)
for index, page in enumerate(rendered):
    page.get_pixmap(matrix=fitz.Matrix(1.5, 1.5)).save(folder / f'SIM-1009200626-sterilization-preview-{index + 1}.png')
result = dict(file=str(pdf), pages=len(reader.pages), bytes=pdf.stat().st_size, sha256=hashlib.sha256(pdf.read_bytes()).hexdigest(), strict_pdf_parse=True, required_text_checks=True, visual_review='待视觉核验', business_actions='无页面操作、无上传、无批准、无签名')
(folder / 'SIM-1009200626-sterilization-format-check.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(result, ensure_ascii=False, indent=2))

