from pathlib import Path
from xml.sax.saxutils import escape
import fitz
from pypdf import PdfReader
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.platypus import SimpleDocTemplate, Paragraph

output = Path(__file__).resolve().parent
pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light'))
body = ParagraphStyle('body', fontName='STSong-Light', fontSize=11, leading=18, spaceAfter=9, wordWrap='CJK')
title = ParagraphStyle('title', parent=body, fontSize=19, leading=25, alignment=1, spaceAfter=15)
warning = ParagraphStyle('warning', parent=body, textColor=colors.HexColor('#A51D26'), fontSize=13, leading=20, alignment=1, spaceAfter=15)
order = 'SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891'
disclaimer = '模拟测试附件、非真实检验报告／记录、不可作为真实质量依据'

def create_pdf(category, details):
    path = output / f'SIM-1009200625-{category}-模拟测试.pdf'
    document = SimpleDocTemplate(str(path), pagesize=A4, rightMargin=42, leftMargin=42, topMargin=38, bottomMargin=38)
    paragraphs = [Paragraph(escape(f'{category}（模拟）'), title), Paragraph(escape(disclaimer), warning)]
    sections = [
        '环境与对象：本地 localhost:8081／测试1租户；本轮活跃订单／周期1009200625；目标数量5000件。',
        f'完整工单：{order}',
        f'资料类别：{category}。仅用于本轮测试附件流程，不替代其他周期、类别或正式原始检验记录。',
        *details,
        '上游异常保留：生产交接记载粗洗、清洁报工5501（参考上限5500），精洗参数0（要求≥1），光固能量1001（范围800~1000），超出量501待分配。本文不掩盖、更改或判定这些异常，也不构成处置。',
        '状态边界：按本次交接时点，PQC独立复核尚未完成，PQC生产放行未发生。本文仅为附件准备，不证明实时业务状态，不声明合格、批准或可上市。',
        '依据与限制：仅整理本轮生产填写复核交接、PQC检验提交交接及协调任务提供的模拟信息，未独立核验业务页面；未生成真实实测数据、机构证明、印章、签字或现实等待时间依据。',
        '后续使用条件：PQC生产放行正式成立、本人正式获派且收到上传任务后，再核对当前状态及附件适用性；当前未上传、未修改业务记录、未批准。',
    ]
    paragraphs.extend(Paragraph(escape(text), body) for text in sections)
    paragraphs.append(Paragraph('SIM / 1009200625 / 5000件；无质量结论、无签章。', warning))
    document.build(paragraphs)
    reader = PdfReader(path)
    extracted = chr(10).join(page.extract_text() for page in reader.pages)
    for expected in [disclaimer, order, '1009200625', '5000', category, '5501', '1001', '501']:
        if expected not in extracted:
            raise ValueError(f'PDF内容核验失败：{category} / {expected}')
    if len(reader.pages) != 1:
        raise ValueError(f'预期单页，实际{len(reader.pages)}页：{category}')
    rendered = fitz.open(path)
    rendered[0].get_pixmap(matrix=fitz.Matrix(1.4, 1.4)).save(str(path.with_suffix('.png')))
    print(f'PASS | {path} | header={path.read_bytes()[:8]!r} | pages={len(reader.pages)} | text verified | rendered')

create_pdf('成品检验报告', [
    '报告用途：成品检文件类别的流程测试附件；本轮仅整理交接摘要，不出具实际成品检验结论。',
    '模拟检验交接摘要：PQC交接记载首检6/6、巡检第1轮8/8已提交，尚待独立复核。交接中的模拟合格选择不等于真实实测、经复核检验100%或质量批准。',
])
create_pdf('成品检验记录', [
    '记录用途：其他文件／成品检验记录类别的流程测试附件；不替代系统逐项原始记录。',
    '模拟记录索引：交接记载首检6次、巡检第1轮8次，共14次提交；覆盖清洗、清洁、组装螺杆八组件、光固外套四组件、装配、整体粘结、小包装、中大包装中的适用任务。',
    '记录限制：不虚构记录ID、签名ID、测量读数或实际器具使用。交接提及的12小时条件仅属模拟条件，本文不证明现实等待已满足。',
])
