#!/usr/bin/env python3
"""资源包 mock 服务器：把 scripts/mock_packs/<id>/ 打成 zip，提供 /manifest.json 与 /packs/<id>-v<版本>.zip。

每个包目录下的 pack.json 给出 type（words / voice）、title、description、version，不打进 zip；
其余文件按相对路径打包，与 App 内 assets 的目录结构一致（audio/us/…、tts/en-US/…、words.json）。

手机连 USB 后：adb reverse tcp:8765 tcp:8765，App debug 包默认访问 http://127.0.0.1:8765。
用法：python3 scripts/mock_pack_server.py [--port 8765] [--delay 0.05] [--corrupt <id>] [--fail <id>]
"""
import argparse
import hashlib
import io
import json
import time
import zipfile
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

PACKS_DIR = Path(__file__).resolve().parent / "mock_packs"
CHUNK = 16 * 1024


def build_zip_from_dir(pack_dir):
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as zf:
        for path in sorted(pack_dir.rglob("*")):
            if path.is_file() and path.name not in ("pack.json", ".DS_Store"):
                zf.write(path, path.relative_to(pack_dir).as_posix())
    return buffer.getvalue()


def build_packs(corrupt_ids):
    """返回 (manifest, {url 路径: zip 字节})。corrupt_ids 中的包在清单里给错 sha256，用来测校验失败。"""
    entries, blobs = [], {}
    for pack_dir in sorted(p for p in PACKS_DIR.iterdir() if (p / "pack.json").is_file()):
        meta = json.loads((pack_dir / "pack.json").read_text(encoding="utf-8"))
        data = build_zip_from_dir(pack_dir)
        url = f"/packs/{pack_dir.name}-v{meta['version']}.zip"
        sha = hashlib.sha256(data).hexdigest()
        if pack_dir.name in corrupt_ids:
            sha = "0" * 64
        entries.append({"id": pack_dir.name, **meta, "size": len(data), "sha256": sha, "url": url})
        blobs[url] = data
    return {"packs": entries}, blobs


def make_handler(manifest, blobs, delay, fail_ids):
    manifest_bytes = json.dumps(manifest, ensure_ascii=False, indent=1).encode("utf-8")

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            if self.path == "/manifest.json":
                self._send(200, "application/json; charset=utf-8", manifest_bytes)
            elif self.path in blobs:
                if any(self.path.startswith(f"/packs/{i}-") for i in fail_ids):
                    self._send(500, "text/plain", b"mock failure")
                else:
                    self._send(200, "application/zip", blobs[self.path], throttle=True)
            else:
                self._send(404, "text/plain", b"not found")

        def _send(self, code, content_type, body, throttle=False):
            self.send_response(code)
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            try:
                for start in range(0, len(body), CHUNK):
                    self.wfile.write(body[start:start + CHUNK])
                    if throttle and delay:
                        time.sleep(delay)
            except (BrokenPipeError, ConnectionResetError):
                self.log_message("client closed %s", self.path)

    return Handler


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--delay", type=float, default=0.05, help="每 16KB 停顿秒数，方便在 App 里看到进度")
    parser.add_argument("--corrupt", action="append", default=[], help="清单里给错该包的 sha256")
    parser.add_argument("--fail", action="append", default=[], help="下载该包时返回 HTTP 500")
    args = parser.parse_args()
    manifest, blobs = build_packs(set(args.corrupt))
    for p in manifest["packs"]:
        print(f"  {p['id']} v{p['version']} {p['size']} bytes  {p['url']}")
    server = ThreadingHTTPServer(("0.0.0.0", args.port), make_handler(manifest, blobs, args.delay, set(args.fail)))
    print(f"serving on http://127.0.0.1:{args.port}/manifest.json")
    server.serve_forever()


if __name__ == "__main__":
    main()
