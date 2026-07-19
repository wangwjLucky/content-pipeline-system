"""封面生成模块 —— 使用 Pillow 生成视频封面图（文字 + 背景）。"""

from pathlib import Path
from common.logging import setup_logging

logger = setup_logging("cover")


class CoverGenerator:
    """封面生成器：生成带标题文字的封面图片（3-8 个字）。"""

    def __init__(self):
        self.work_dir = Path("/tmp/cover_workspace")
        self.work_dir.mkdir(parents=True, exist_ok=True)
        self.width = 1920
        self.height = 1080

    def generate(self, title: str, output_path: str | None = None,
                 bg_color: str = "#1a1a2e", text_color: str = "#ffffff") -> str:
        """生成封面图片。

        Args:
            title: 封面标题（3-8 个字）
            output_path: 输出路径
            bg_color: 背景色（十六进制）
            text_color: 文字颜色

        Returns:
            封面图片路径
        """
        if output_path is None:
            # 清理标题中的非法字符
            safe_title = "".join(c for c in title if c.isalnum() or c in " _-")
            output_path = str(self.work_dir / f"cover_{safe_title[:20]}.png")

        try:
            from PIL import Image, ImageDraw, ImageFont

            # 创建背景
            bg_rgb = self._hex_to_rgb(bg_color)
            img = Image.new("RGB", (self.width, self.height), bg_rgb)
            draw = ImageDraw.Draw(img)

            # 尝试加载字体，失败则使用默认
            font = self._load_font()

            # 计算文字位置（居中）
            bbox = draw.textbbox((0, 0), title, font=font)
            text_w = bbox[2] - bbox[0]
            text_h = bbox[3] - bbox[1]
            x = (self.width - text_w) // 2
            y = (self.height - text_h) // 2

            # 绘制阴影
            text_rgb = self._hex_to_rgb(text_color)
            shadow_color = (0, 0, 0)
            for offset in [(4, 4), (5, 5), (6, 6)]:
                draw.text((x + offset[0], y + offset[1]), title, font=font, fill=shadow_color)

            # 绘制主文字
            draw.text((x, y), title, font=font, fill=text_rgb)

            # 底部装饰线
            line_y = y + text_h + 30
            line_color = self._hex_to_rgb(text_color)
            for i in range(3):
                ly = line_y + i * 4
                draw.rectangle([(self.width // 2 - 150, ly), (self.width // 2 + 150, ly + 2)],
                               fill=line_color)

            img.save(output_path, "PNG")
            logger.info(f"封面生成成功: {output_path}")
            return output_path

        except ImportError:
            logger.warning("Pillow 未安装，创建空白占位封面")
            self._create_placeholder(output_path)
            return output_path
        except Exception as e:
            logger.error(f"封面生成失败: {e}")
            return ""

    def _load_font(self):
        """加载中文字体。"""
        font_paths = [
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/noto-cjk/NotoSansCJK-Regular.ttc",
        ]
        for fp in font_paths:
            if Path(fp).exists():
                return ImageFont.truetype(fp, 96)
        return ImageFont.load_default()

    def _hex_to_rgb(self, hex_color: str) -> tuple[int, int, int]:
        hex_color = hex_color.lstrip("#")
        return tuple(int(hex_color[i:i + 2], 16) for i in (0, 2, 4))

    def _create_placeholder(self, path: str):
        """创建占位封面（无 Pillow 时备用）。"""
        with open(path, "wb") as f:
            f.write(b"")