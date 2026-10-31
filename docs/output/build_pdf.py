"""
Builds IAIP_2026_Employee_Management_System_Assignment.pdf from the HTML source in this folder.

Requirements: Python 3.10+, `pip install playwright pypdf`, and Google Chrome (or run
`playwright install chromium` and pass --chrome "").

    python build_pdf.py [--chrome "C:/Program Files/Google/Chrome/Application/chrome.exe"]

Steps: render once to find the page of every section (from the PDF bookmarks), write those
page numbers into the table of contents, render again, then replace page 1 with a version
without header/footer (the cover).
"""
import argparse
import io
import re
from pathlib import Path

from playwright.sync_api import sync_playwright
from pypdf import PdfReader, PdfWriter

HERE = Path(__file__).resolve().parent
SOURCE = HERE / "IAIP_2026_Employee_Management_System_Assignment.html"
TARGET = HERE / "IAIP_2026_Employee_Management_System_Assignment.pdf"
DEFAULT_CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"

FONT = "font-family:'Segoe UI',Arial,sans-serif;font-size:7.5pt;color:#5a6577;width:100%;"
HEADER = (f'<div style="{FONT}padding:0 17mm;display:flex;justify-content:space-between;">'
          '<span>IAIP 2026 · Employee Management System</span><span>Assignment Documentation</span></div>')
FOOTER = (f'<div style="{FONT}padding:0 17mm;display:flex;justify-content:space-between;">'
          '<span>Dharmik Rakeshbhai Prajapati</span>'
          '<span>Page <span class="pageNumber"></span> of <span class="totalPages"></span></span></div>')


def render(page, with_header_footer):
    return page.pdf(format="A4", print_background=True, prefer_css_page_size=True, outline=True, tagged=True,
                    display_header_footer=with_header_footer, header_template=HEADER, footer_template=FOOTER)


def section_pages(pdf_bytes):
    """Section number -> 1-based page, read from the bookmarks Chrome creates for the <h1> headings."""
    reader = PdfReader(io.BytesIO(pdf_bytes))
    pages = {}

    def walk(items):
        for item in items:
            if isinstance(item, list):
                walk(item)
                continue
            match = re.match(r"\s*(\d+)\s", item.title)
            if match and match.group(1) not in pages:
                pages[match.group(1)] = reader.get_destination_page_number(item) + 1

    walk(reader.outline)
    return pages


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--chrome", default=DEFAULT_CHROME)
    args = parser.parse_args()

    with sync_playwright() as p:
        browser = p.chromium.launch(executable_path=args.chrome or None)
        page = browser.new_page()
        page.goto(SOURCE.as_uri())
        page.wait_for_load_state("networkidle")
        page.evaluate("Promise.all([...document.images].map(i => i.decode()))")

        pages = section_pages(render(page, True))
        page.evaluate("""pages => {
            for (const el of document.querySelectorAll('#toc .p')) el.textContent = pages[el.dataset.sec] ?? '–'
        }""", pages)
        body = render(page, True)
        cover = render(page, False)
        browser.close()

    # A changed TOC must not move any section
    assert section_pages(body) == pages, "page numbers changed after filling the table of contents"

    writer = PdfWriter(clone_from=io.BytesIO(body))
    writer.insert_page(PdfReader(io.BytesIO(cover)).pages[0], 0)
    writer.remove_page(1)
    writer.add_metadata({
        "/Title": "Employee Management System – Assignment Documentation",
        "/Author": "Dharmik Rakeshbhai Prajapati",
        "/Subject": "IAIP 2026 – Process Automation and Application Development Intern",
    })
    with open(TARGET, "wb") as out:
        writer.write(out)
    print(f"{TARGET.name}: {len(writer.pages)} pages; sections on pages {pages}")


if __name__ == "__main__":
    main()
