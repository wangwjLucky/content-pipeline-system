"""音频处理模块 —— BGM 混音、音效叠加、音量调整。"""

import subprocess
from pathlib import Path
from common.logging import setup_logging

logger = setup_logging("audio")


class AudioProcessor:
    """音频处理器：混音、变速、音效叠加。"""

    def __init__(self):
        self.work_dir = Path("/tmp/audio_workspace")
        self.work_dir.mkdir(parents=True, exist_ok=True)

    def mix_audio(self, voice_path: str, bgm_path: str | None = None,
                  bgm_volume: float = 0.15, output_path: str | None = None) -> str:
        """将配音与 BGM 混音。"""
        if output_path is None:
            output_path = str(self.work_dir / "mixed_audio.mp3")

        if not bgm_path:
            # 无 BGM，直接复制配音
            cmd = ["ffmpeg", "-y", "-i", voice_path, "-c", "copy", output_path]
        else:
            cmd = [
                "ffmpeg", "-y",
                "-i", voice_path,
                "-i", bgm_path,
                "-filter_complex",
                f"[1:a]volume={bgm_volume}[bgm];[0:a][bgm]amix=inputs=2:duration=first[out]",
                "-map", "[out]",
                "-c:a", "aac",
                output_path,
            ]

        logger.info(f"执行音频混音: {' '.join(cmd)}")
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
        if result.returncode != 0:
            logger.error(f"混音失败: {result.stderr}")
            return voice_path
        logger.info(f"混音成功: {output_path}")
        return output_path

    def adjust_speed(self, audio_path: str, speed: float = 1.05) -> str:
        """调整音频速度（默认 1.05x）。"""
        output_path = str(self.work_dir / f"speed_{speed}.mp3")
        cmd = [
            "ffmpeg", "-y",
            "-i", audio_path,
            "-filter:a", f"atempo={speed}",
            "-c:a", "aac",
            output_path,
        ]
        logger.info(f"执行变速: speed={speed}")
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
        if result.returncode != 0:
            logger.error(f"变速失败: {result.stderr}")
            return audio_path
        logger.info(f"变速成功: {output_path}")
        return output_path

    def add_sound_effect(self, audio_path: str, effect_path: str,
                         volume: float = 0.5, position: str = "start") -> str:
        """叠加音效（Whoosh / Click / Pop 等）。"""
        output_path = str(self.work_dir / "with_effect.mp3")
        pad = "0" if position == "start" else f"0:{position}"
        cmd = [
            "ffmpeg", "-y",
            "-i", audio_path,
            "-i", effect_path,
            "-filter_complex",
            f"[1:a]volume={volume}[eff];[0:a][eff]amix=inputs=2:duration=first:dropout_transition=2[out]",
            "-map", "[out]",
            "-c:a", "aac",
            output_path,
        ]
        logger.info(f"叠加音效: {effect_path}")
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
        if result.returncode != 0:
            logger.error(f"音效叠加失败: {result.stderr}")
            return audio_path
        return output_path