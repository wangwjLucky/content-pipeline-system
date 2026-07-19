"""日志配置。"""

import logging
import os
import sys
from logging.handlers import RotatingFileHandler
from pathlib import Path


class LevelFilter(logging.Filter):
    """按日志级别过滤。"""

    def __init__(self, level: int):
        super().__init__()
        self._level = level

    def filter(self, record: logging.LogRecord) -> bool:
        return record.levelno == self._level


_LOG_DIR = Path(os.getenv("PIPELINE_LOG_DIR", "logs"))
_MAX_BYTES = 30 * 1024 * 1024  # 30MB
_BACKUP_COUNT = 3


def _ensure_log_dir(name: str) -> Path:
    log_dir = _LOG_DIR
    log_dir.mkdir(parents=True, exist_ok=True)
    return log_dir


def _add_file_handler(logger: logging.Logger, log_dir: Path, level: int):
    """添加按级别输出的滚动文件处理器。"""
    level_name = logging.getLevelName(level).lower()
    log_file = log_dir / f"{level_name}.log"

    handler = RotatingFileHandler(
        log_file, maxBytes=_MAX_BYTES, backupCount=_BACKUP_COUNT, encoding="utf-8"
    )
    handler.setLevel(logging.DEBUG)
    handler.addFilter(LevelFilter(level))

    formatter = logging.Formatter(
        "[%(asctime)s][%(levelname)s][%(threadName)s][%(name)s.%(funcName)s:%(lineno)d] - [%(message)s]",
        datefmt="%Y-%m-%d %H:%M:%S",
    )
    handler.setFormatter(formatter)
    logger.addHandler(handler)


def setup_logging(name: str = "pipeline") -> logging.Logger:
    """配置日志器，输出到控制台和文件。

    文件按日志级别分别写入 logs/{debug,info,warn,error}.log，每文件 30MB 滚动，保留 3 个备份。
    日志目录可通过环境变量 PIPELINE_LOG_DIR 设置，默认 logs/。
    """
    logger = logging.getLogger(name)
    logger.setLevel(logging.DEBUG)

    # 避免重复添加处理器
    if logger.handlers:
        return logger

    # 控制台输出
    console = logging.StreamHandler(sys.stdout)
    console.setLevel(logging.DEBUG)
    console_formatter = logging.Formatter(
        "%(asctime)s [%(levelname)s] %(name)s: %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )
    console.setFormatter(console_formatter)
    logger.addHandler(console)

    # 文件输出（按级别）
    log_dir = _ensure_log_dir(name)
    _add_file_handler(logger, log_dir, logging.DEBUG)
    _add_file_handler(logger, log_dir, logging.INFO)
    _add_file_handler(logger, log_dir, logging.WARNING)
    _add_file_handler(logger, log_dir, logging.ERROR)

    return logger