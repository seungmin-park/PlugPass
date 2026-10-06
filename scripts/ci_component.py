"""Create a deployment component from an already verified build (stdlib only)."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
from zipfile import ZipFile


def stage_component(root: Path, component: str, output: Path, metadata: dict):
    targets = {
        "crackcs-backend": ("crackcs", None),
        "crackcs-frontend": ("crackcs", "front/dist"),
        "demp": ("demp", None),
        "dempfrontend": ("dempfrontend", "dist"),
        "plugpass": ("plugpass", None),
    }
    if component not in targets:
        raise ValueError("Unknown component: " + component)
    app, dist_path = targets[component]
    if output.exists():
        raise ValueError("Output already exists: " + str(output))
    if dist_path:
        source = root / dist_path
        if not (source / "index.html").is_file():
            raise ValueError("Built dist/index.html is missing")
        payload = sorted(path for path in source.rglob("*") if path.is_file() or path.is_symlink())
        for path in payload:
            if path.is_symlink() or path.name.startswith(".env") or path.suffix in (".pem", ".key"):
                raise ValueError("Forbidden payload: " + str(path.relative_to(source)))
        output.mkdir(parents=True)
        shutil.copytree(source, output / app / "dist")
    else:
        jars = [path for path in (root / "build/libs").glob("*.jar") if not path.name.endswith("-plain.jar")]
        if len(jars) != 1 or jars[0].is_symlink():
            raise ValueError("Expected exactly one executable JAR")
        with ZipFile(jars[0]) as archive:
            manifest = archive.read("META-INF/MANIFEST.MF")
            if b"org.springframework.boot.loader.launch.JarLauncher" not in manifest:
                raise ValueError("JAR is not an executable Spring Boot application")
        (output / app).mkdir(parents=True)
        shutil.copyfile(jars[0], output / app / "service.jar")
    files = {str(path.relative_to(output)): hashlib.sha256(path.read_bytes()).hexdigest()
             for path in sorted(output.rglob("*")) if path.is_file()}
    record = {**metadata, "component": component, "files": files}
    (output / "component.json").write_text(json.dumps(record, indent=2) + "\n")
    return record


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("component")
    parser.add_argument("--output", default="build/deployment")
    args = parser.parse_args()
    root = Path.cwd()
    def git(*arguments):
        return subprocess.check_output(["git", *arguments], text=True).strip()
    if git("status", "--porcelain"):
        raise SystemExit("Refusing to package a dirty source checkout")
    metadata = {
        "repository": os.environ["GITHUB_REPOSITORY"],
        "headSha": os.environ["DEPLOY_HEAD_SHA"],
        "sourceSha": git("rev-parse", "HEAD"),
        "treeSha": git("rev-parse", "HEAD^{tree}"),
        "runId": int(os.environ["GITHUB_RUN_ID"]),
        "runAttempt": int(os.environ["GITHUB_RUN_ATTEMPT"]),
        "event": os.environ["GITHUB_EVENT_NAME"],
    }
    record = stage_component(root, args.component, root / args.output, metadata)
    print("Packaged", record["component"], "from", record["sourceSha"])


if __name__ == "__main__":
    main()
