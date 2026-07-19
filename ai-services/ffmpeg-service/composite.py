"""视频合成模块 —— 编排图片/视频/音频/字幕合成最终视频。"""

import json
import subprocess
from pathlib import Path
from common.logging import setup_logging
from common.config import settings

logger = setup_logging("composite")


class VideoCompositor:
    """FFmpeg 视频合成器。"""

    def __init__(self):
        self.work_dir = Path("/tmp/ffmpeg_workspace")
        self.work_dir.mkdir(parents=True, exist_ok=True)

    def compile(self, task_id: int, params: dict) -> dict:
        """执行视频合成，返回合成结果。"""
        materials = params.get("materials", [])
        voice_url = params.get("voice_url", "")
        subtitle_text = params.get("subtitle", "")

        logger.info(f"开始视频合成: taskId={task_id}, materials_count={len(materials)}")

        if not materials:
            logger.warning("没有素材，跳过合成")
            return {"url": "", "status": "skipped"}

        output_path = str(self.work_dir / f"output_{task_id}.mp4")

        try:
            # 构建 FFmpeg 滤镜复杂图
            filter_parts = []
            inputs = []
            for i, mat in enumerate(materials):
                url = mat.get("url", "")
                if url:
                    inputs.extend(["-i", url])

            if not inputs:
                logger.warning("无有效输入文件")
                return {"url": "", "status": "skipped"}

            # 简单拼接（MVP 阶段）
            cmd = ["ffmpeg", "-y"]
            cmd.extend(inputs)

            # 构建 concat 滤镜
            filter_str = "".join(f"[{i}:v:0][{i}:a:0]" for i in range(len(materials)))
            filter_str += f"concat=n={len(materials)}:v=1:a=1[out]"

            cmd.extend([
                "-filter_complex", filter_str,
                "-map", "[out]",
                "-c:v", "libx264",
                "-preset", "medium",
                "-c:a", "aac",
                output_path,
            ])

            logger.info(f"执行 FFmpeg: {' '.join(cmd)}")
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=600)
            if result.returncode != 0:
                logger.error(f"FFmpeg 失败: {result.stderr}")

            if Path(output_path).exists():
                logger.info(f"视频合成成功: {output_path}")
                return {"url": output_path, "status": "success", "task_id": task_id}
            else:
                logger.warning("合成未生成文件，返回模拟结果")
                return {"url": "", "status": "mock", "task_id": task_id}

        except subprocess.TimeoutExpired:
            logger.error("FFmpeg 超时")
            return {"url": "", "status": "timeout", "task_id": task_id}
        except FileNotFoundError:
            logger.warning("FFmpeg 未安装，返回模拟结果")
            return {"url": "", "status": "mock", "task_id": task_id}
        except Exception as e:
            logger.error(f"合成异常: {e}")
            raise