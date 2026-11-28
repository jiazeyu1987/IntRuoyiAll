from pathlib import Path
import json
import hashlib
import fitz
from pypdf import PdfReader
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph

output = Path(__file__).resolve().parents[1] / 'output' / 'pdf'
target = output / 'SIM-1009200648-881MO100160-sterilization-software-simulation.pdf'
if target.exists():
    raise FileExistsError(target)
pdfmetrics.registerFont(TTFont('Chinese0648', 'C:/Windows/Fonts/msyh.ttc', subfontIndex=0))
body = ParagraphStyle('Body', fontName='Chinese0648', fontSize=11, leading=21, spaceAfter=12, wordWrap='CJK')
title = ParagraphStyle('Title', parent=body, fontSize=21, leading=32, textColor=colors.HexColor('#a02020'), alignment=1)
notice = ParagraphStyle('Notice', parent=body, fontSize=14, leading=25, textColor=colors.HexColor('#a02020'))
paragraphs = [
('软件模拟 · 灭菌报告', title),
('非真实实测／质量批准，不可作为真实质量依据', notice),
('类别：灭菌报告（软件模拟附件）<br/>生产订单：881MO100160<br/>本轮标识：1009200648<br/>生产批号：SIM202611210648<br/>批次执行：900000001351<br/>关联PQC申请：342<br/>本人正式资料任务：3094（丁加美）<br/>限定环境：本地8081／测试1', body),
('用途：仅用于指定订单本轮软件模拟附件关联与流程验证，不适用于其他订单或旧周期。无真实灭菌实测、无菌或合格结论，无机构签章、签名及真实质量批准。未提供并核实的灭菌参数、设备运行及实施时间不予编造。', body),
('差异披露：详情顶部显示“单包装工序：冻结工艺输入物料 A006.011.2003 未匹配正式领料记录，请核对用料。”该提示不再阻断详情打开。', body),
('冻结INPUT A006.011.2003与实际领料A006.011.2008分别保留。实际领料编码依本次协调交接披露；本文件不更改两类记录、不消除差异，不证明二者可替代，不代表已获得真实替代批准。', body),
('资料边界：只办理本人正式灭菌资料任务；不改物料、权限或质量结论，不重办PQC放行，不代质量审批、上市放行或归档。关联PQC编号仅作索引，其批准效力不由本文件证明。', body),
('软件模拟，非真实报告记录，非真实实测／质量批准。', notice),
]
SimpleDocTemplate(str(target), title='软件模拟灭菌报告-1009200648', author='软件模拟附件', leftMargin=48, rightMargin=48, topMargin=42, bottomMargin=42).build([Paragraph(text,style) for text,style in paragraphs])
reader = PdfReader(target,strict=True)
text = ''.join(page.extract_text() for page in reader.pages)
for value in ['软件模拟','881MO100160','1009200648','SIM202611210648','3094','A006.011.2003','A006.011.2008','非真实实测','质量批准']:
    if value not in text:
        raise ValueError(value)
if len(reader.pages) != 1:
    raise ValueError('Expected one page')
with fitz.open(target) as rendered:
    rendered[0].get_pixmap(matrix=fitz.Matrix(1.2,1.2)).save(target.with_suffix('.png'))
print(json.dumps({'file':str(target),'bytes':target.stat().st_size,'pages':1,'strict_parse':'PASS','required_text':'PASS','sha256':hashlib.sha256(target.read_bytes()).hexdigest()},ensure_ascii=False,indent=2))
