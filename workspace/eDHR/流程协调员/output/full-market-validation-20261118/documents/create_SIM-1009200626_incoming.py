from pathlib import Path
import hashlib
import json
import fitz
from pypdf import PdfReader
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib import colors
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer

folder = Path(__file__).resolve().parent
path = folder / "SIM-1009200626-来料检验报告.pdf"
if path.exists():
    raise FileExistsError(str(path))
pdfmetrics.registerFont(TTFont("Chinese", "C:/Windows/Fonts/simhei.ttf"))
body = ParagraphStyle("Body", fontName="Chinese", fontSize=11, leading=18, spaceAfter=10, wordWrap="CJK")
title = ParagraphStyle("Title", parent=body, fontSize=20, leading=28, alignment=1)
warning = ParagraphStyle("Warning", parent=body, fontSize=14, leading=23, alignment=1, textColor=colors.darkred)
order = "SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891"
content = [
    ("来料检验报告（模拟附件）", title),
    ("模拟测试、非真实报告/记录<br/>不可作为真实质量依据或上市依据", warning),
    ("一、适用对象", body),
    ("文档编号：SIM-1009200626-INCOMING-01；类别：来料检验报告。", body),
    ("环境：本地 localhost:8081；租户：测试1；周期：1009200626。", body),
    (f"完整工单：{order}", body),
    ("产品型号：AI30；本轮工单数量：5000件（不代表实物到货或已检数量）。", body),
    ("二、用途与数据边界", body),
    ("仅为上述对象本轮完整上市放行流程验证准备来料类别模拟附件。本文件新建于0626周期，不复用0625文件、签名或完成状态。", body),
    ("未开展真实来料抽样或检测，未取得真实供应商批号、物料批号、原始实测记录或外部机构报告。不提供虚构实测值、合格判定、机构签章或批准结论。", body),
    ("三、交接异常披露（非本人独立核实）", body),
    ("以下仅来自本轮任务交接，未由本人独立核对系统原始记录：生产记录两处5501；参数0/1001；粗洗余501尚未跨单分配。原样披露，不掩盖异常、不消除差异、不追认真实质量。", body),
    ("四、准备状态与后续边界", body),
    ("仅文件准备。交接时PQC负责人正在办理，尚无正式放行确认；本人未核验生产、检验复核及PQC放行结果。文件准备不等于系统保存、上传、审批或上市放行完成。", body),
    ("PQC正式放行结果及本类别实际责任指派确认后，另按后续任务办理上传。无真人亲签、无电子签名复用、无真实质量批准。", body),
]

def footer(canvas, document):
    canvas.setFont("Chinese", 8)
    canvas.setFillColor(colors.darkred)
    canvas.drawString(40, 25, "SIM-1009200626 | 模拟测试，非真实报告，不可用于真实质量或上市依据")
    canvas.drawRightString(555, 25, f"第{document.page}页")

document = SimpleDocTemplate(str(path), leftMargin=40, rightMargin=40, topMargin=35, bottomMargin=45, title="SIM-1009200626 来料检验报告（模拟测试）")
document.build([Paragraph(text, style) for text, style in content], onFirstPage=footer, onLaterPages=footer)
reader = PdfReader(str(path), strict=True)
text = "".join(page.extract_text() for page in reader.pages).replace("\n", "")
for expected in [order, "1009200626", "AI30", "5000", "来料检验报告", "模拟测试", "非真实报告", "不可作为真实质量依据", "5501", "0/1001", "501", "非本人独立核实"]:
    assert expected in text, expected
rendered = fitz.open(path)
for index, page in enumerate(rendered):
    page.get_pixmap(matrix=fitz.Matrix(1.5, 1.5), alpha=False).save(folder / f"SIM-1009200626-来料检验报告-preview-{index+1}.png")
print(json.dumps({"file": str(path), "pages": len(reader.pages), "bytes": path.stat().st_size, "sha256": hashlib.sha256(path.read_bytes()).hexdigest(), "strict_parse": "PASS", "content": "PASS", "uploaded": False, "approved": False}, ensure_ascii=False, indent=2))

