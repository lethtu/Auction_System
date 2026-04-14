import os
from pypdf import PdfReader

pdfs = [
    r"client/2026-Bài tập lớn (1).pdf",
    r"client/Hướng dẫn BTL LTNC-2026-1.pdf"
]

for pdf in pdfs:
    print("="*50)
    print(f"File: {pdf}")
    print("="*50)
    try:
        reader = PdfReader(pdf)
        for i, page in enumerate(reader.pages):
            print(f"--- Page {i+1} ---")
            print(page.extract_text())
    except Exception as e:
        print(f"Error reading {pdf}: {e}")
