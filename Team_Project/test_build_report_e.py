import subprocess
import sys
import unittest
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TEAM_PROJECT = ROOT / "Team_Project"
OUTPUT_DOCX = TEAM_PROJECT / "E_구현결과.docx"
sys.path.insert(0, str(TEAM_PROJECT))


class ReportEBuilderTest(unittest.TestCase):
    def test_builds_docx_with_all_report_screenshots(self):
        import build_report_e

        specs = build_report_e.screen_specs()

        self.assertEqual(14, len(specs))
        for spec in specs:
            for toolkit in ("javafx", "swing"):
                image = ROOT / "docs" / "screenshots" / "report-ui-2026-06-02" / toolkit / spec.filename
                self.assertTrue(image.exists(), f"missing {image}")
                self.assertEqual((1280, 900), build_report_e.png_size(image))

        subprocess.run(
            [sys.executable, str(TEAM_PROJECT / "build_report_e.py")],
            cwd=ROOT,
            check=True,
        )

        self.assertTrue(OUTPUT_DOCX.exists())
        with zipfile.ZipFile(OUTPUT_DOCX) as docx:
            media = [name for name in docx.namelist() if name.startswith("word/media/")]
            document_xml = docx.read("word/document.xml").decode("utf-8")

        self.assertEqual(28, len(media))
        self.assertIn("E. 구현 결과", document_xml)
        self.assertIn("계정 관리", document_xml)
        self.assertIn("프로젝트 관리", document_xml)
        self.assertIn("Noto Sans CJK KR", document_xml)


if __name__ == "__main__":
    unittest.main()
