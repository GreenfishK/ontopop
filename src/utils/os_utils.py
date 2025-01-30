from pathlib import Path
import shutil
import os
import logging

def clear_dir(dir: str):
    logging.info(f"Cleaning up directory {dir}")
    if not os.path.exists(dir):
        logging.error(f"Direcotry {dir} does not exist and cannot be cleared.")
        return

    path = Path(dir)
    for file_path in path.iterdir():
        if file_path.is_file():
            file_path.unlink()

def copy_files(from_dir: str, to_dir: str):
    logging.info(f"Copying files from {from_dir} to {to_dir}")
    if not os.path.exists(from_dir) or not os.path.exists(to_dir):
        logging.error(f"Either {from_dir} or {to_dir} does not exist. Files will not be copied")
        return

    for file_path in Path(from_dir).iterdir():
        if file_path.is_file():
            shutil.copy(file_path, to_dir)