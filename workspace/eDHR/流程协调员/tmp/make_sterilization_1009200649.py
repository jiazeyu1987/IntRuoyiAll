from pathlib import Path
import hashlib
import json
import fitz
from pypdf import PdfReader
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph

output = Path(__file__).resolve().parents[1] / 'output' / 'pdf'
output.mkdir(parents=True, exist_ok=True)
target = output / 'SIM-1009200649-881MO084059-sterilization-software-simulation.pdf'
if target.exists():
    raise FileExistsError(target)
pdfmetrics.registerFont(TTFont('SimulationChinese0649', 'C:/Windows/Fonts/msyh.ttc', subfontIndex=0))
body = ParagraphStyle('Body', fontName='SimulationChinese0649', fontSize=11, leading=21, spaceAfter=12, wordWrap='CJK')
title = ParagraphStyle('Title', parent=body, fontSize=21, leading=32, textColor=colors.HexColor('#a02020'), alignment=1)
notice = ParagraphStyle('Notice', parent=body, fontSize=14, leading=25, textColor=colors.HexColor('#a02020'))
paragraphs = [
('软件模拟 · 灭菌报告', title),
('非真实实测／质量批准，不可作为真实质量依据', notice),
('类别：灭菌报告（软件模拟附件）<br/>生产订单：881MO084059<br/>本轮标识：1009200649<br/>生产批号：34125090001<br/>批次执行：900000001352<br/>关联PQC申请：343<br/>本人正式资料任务：3100（丁加美）<br/>限定环境：本地8081／测试1', body),
('用途：仅用于本轮正常生产至上市放行的软件附件及对象关联测试，不复用为其他订单或周期依据。本次仅准备，未上传、未保存业务附件。', body),
('模拟边界：不证明实际实施灭菌，不包含真实灭菌设备、参数、运行时间、实测结果、无菌或合格结论；未提供并核实的数据不予编造。无真实机构签章、签名或质量批准。', body),
('异常保留：据协调交接，本轮损耗1及整体不合格均保留，且已在PQC放行意见披露。本附件不修改、不掩盖上述结论，不补造批准；上述处置依据未由本岗独立审核。', body),
('责任边界：正式任务已在本人工作台核对，详情显示待上市放行、灭菌文件暂无数据。关联申请编号不由本附件证明审批有效性；资料准备不代表已上传、上市放行或归档。', body),
('软件模拟，非真实报告记录，非真实实测／质量批准。', notice),
]
SimpleDocTemplate(str(target), title='软件模拟灭菌报告-1009200649', author='软件模拟附件', leftMargin=48, rightMargin=48, topMargin=42, bottomMargin=42).build([Paragraph(text, style) for text, style in paragraphs])
reader = PdfReader(target, strict=True)
text = ''.join(page.extract_text() for page in reader.pages)
for value in ['881MO084059','1009200649','34125090001','900000001352','343','3100','软件模拟','非真实实测','质量批准','损耗1','整体不合格']:
    if value not in text:
        raise ValueError(value)
if len(reader.pages) != 1:
    raise ValueError('Expected one page')
with fitz.open(target) as rendered:
    rendered[0].get_pixmap(matrix=fitz.Matrix(1.2,1.2)).save(target.with_suffix('.png'))
print(json.dumps({'path':str(target),'pages':len(reader.pages),'bytes':target.stat().st_size,'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'strict_parse':'PASS','object_text':'PASS','render':'PASS'},ensure_ascii=False,indent=2))
