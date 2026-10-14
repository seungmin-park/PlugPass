"""The deployment asset store accepts at most 1 MiB per file."""
import unittest
from pathlib import Path


class FontAssetTests(unittest.TestCase):
    def test_self_hosted_fonts_fit_deployment_asset_limit(self):
        fonts = list((Path(__file__).resolve().parents[1] / 'frontend/src/shared/assets/fonts').rglob('*.woff2'))
        self.assertTrue(fonts, 'Self-hosted font assets must exist')
        for font in fonts:
            with self.subTest(font=font.name):
                self.assertGreater(font.stat().st_size, 0)
                self.assertLessEqual(font.stat().st_size, 1024 * 1024, 'Deployment asset limit exceeded')
