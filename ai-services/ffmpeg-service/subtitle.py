"""字幕生成模块 —— 使用 FFmpeg 为视频添加字幕（支持关键词高亮）。"""

import subprocess
import json
from pathlib import Path
from common.logging import setup_logging

logger = setup_logging("subtitle")


class SubtitleGenerator:
    """字幕生成器：将字幕文本合成为 ASS/SRT 并嵌入视频。"""

    def __init__(self):
        self.work_dir = Path("/tmp/subtitle_workspace")
        self.work_dir.mkdir(parents=True, exist_ok=True)

    def generate_ass(self, text: str, highlight_words: list[str] | None = None) -> str:
        """生成 ASS 字幕文件，支持关键词高亮（放大、黄色、描边）。"""
        ass_path = self.work_dir / "subtitle.ass"
        highlight_words = highlight_words or []

        # 按句子或逗号分割为逐条字幕
        segments = self._split_text(text)

        with open(ass_path, "w", encoding="utf-8") as f:
            f.write(self._ass_header())
            for idx, seg in enumerate(segments):
                start = idx * 3
                end = start + 3
                line = self._format_ass_line(idx + 1, start, end, seg, highlight_words)
                f.write(line)

        logger.info(f"ASS 字幕生成完成: {ass_path}, segments={len(segments)}")
        return str(ass_path)

    def burn_to_video(self, video_path: str, ass_path: str, output_path: str) -> str:
        """将 ASS 字幕嵌入视频。"""
        cmd = [
            "ffmpeg", "-y",
            "-i", video_path,
            "-vf", f"ass={ass_path}",
            "-c:a", "copy",
            output_path,
        ]
        logger.info(f"执行字幕嵌入: {' '.join(cmd)}")
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=300)
        if result.returncode != 0:
            logger.error(f"字幕嵌入失败: {result.stderr}")
            return video_path  # 失败时返回原视频
        logger.info(f"字幕嵌入成功: {output_path}")
        return output_path

    def _split_text(self, text: str) -> list[str]:
        """将长文本按句子分割为字幕片段。"""
        import re
        parts = re.split(r'[。！？\n]', text)
        return [p.strip() for p in parts if p.strip()]

    def _ass_header(self) -> str:
        return """[Script Info]
ScriptType: v4.00+
PlayResX: 1920
PlayResY: 1080
ScaledBorderAndShadow: yes

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Default,Microsoft YaHei,36,&H00FFFFFF,&H0000FFFF,&H00000000,&H80000000,-1,0,0,0,100,100,0,0,1,2,1,2,10,10,30,1
Style: Highlight,Microsoft YaHei,48,&H0000FFFF,&H0000FFFF,&H00000000,&H80000000,-1,0,0,0,100,100,0,0,1,3,1,2,10,10,35,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
"""

    def _format_ass_line(self, idx: int, start_sec: int, end_sec: int,
                         text: str, highlight_words: list[str]) -> str:
        start = f"0:{start_sec // 60:02d}:{start_sec % 60:02d}.00"
        end = f"0:{end_sec // 60:02d}:{end_sec % 60:02d}.00"
        # 关键词高亮：替换为 Highlight 样式
        for word in highlight_words:
            if word in text:
                text = text.replace(word, f"{{\\rHighlight}}{word}{{\\rDefault}}")
        return f"Dialogue: 0,{start},{end},Default,,0,0,0,,{text}\n"