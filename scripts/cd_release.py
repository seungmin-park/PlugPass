"""Assemble successful GitHub CI components for a manual GCP VM deployment."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import shutil
import stat
import subprocess
import tarfile
import tempfile
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
REPOSITORIES = {
    "crackcs": ("seungmin-park/crackCS", ("crackcs-backend", "crackcs-frontend"), "CrackCS verify"),
    "demp": ("seungmin-park/demp", ("demp",), "DEMP verify"),
    "dempfrontend": ("seungmin-park/dempfrontend", ("dempfrontend",), "DEMP frontend verify"),
    "plugpass": ("seungmin-park/PlugPass", ("plugpass",), "PlugPass verify"),
}
APPLICATIONS = {"crackcs": ("crackcs",), "demp": ("demp", "dempfrontend"), "plugpass": ("plugpass",)}


def application_repositories(application):
    if application == "all":
        return tuple(REPOSITORIES)
    if application not in APPLICATIONS:
        raise ValueError("Unknown application: " + application)
    return APPLICATIONS[application]


def sha(path):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def verify_component(directory, component, run, tree_sha):
    directory = Path(directory)
    record = json.loads((directory / "component.json").read_text())
    expected = {"component": component, "repository": run["repository"], "headSha": run["head_sha"],
                "runId": run["id"], "runAttempt": run["run_attempt"], "treeSha": tree_sha}
    for key, value in expected.items():
        if record.get(key) != value:
            raise ValueError("Component identity mismatch: " + key)
    if not re.fullmatch(r"[a-f0-9]{40}", record.get("sourceSha", "")):
        raise ValueError("Invalid sourceSha")
    actual = {str(path.relative_to(directory)) for path in directory.rglob("*")
              if path.is_file() and path.name != "component.json"}
    files = record.get("files", {})
    if not files or set(files) != actual:
        raise ValueError("Payload file set does not match verified manifest")
    app = "crackcs" if component.startswith("crackcs-") else component
    for relative, checksum in files.items():
        path = PurePosixPath(relative)
        if path.is_absolute() or ".." in path.parts or path.parts[0] != app:
            raise ValueError("Forbidden payload path")
        source = directory / relative
        if source.is_symlink() or source.name.startswith(".env") or source.suffix in (".pem", ".key"):
            raise ValueError("Forbidden payload file")
        if sha(source) != checksum:
            raise ValueError("Checksum mismatch: " + relative)
    required = app + ("/dist/index.html" if component in ("crackcs-frontend", "dempfrontend") else "/service.jar")
    if required not in files:
        raise ValueError("Required application payload missing: " + required)
    return record


def require_success(run, jobs):
    if run.get("status") != "completed" or run.get("conclusion") != "success":
        raise ValueError("CI run has not succeeded")
    if not jobs:
        raise ValueError("CI jobs are missing")
    for job in jobs:
        if job.get("status") != "completed" or job.get("conclusion") != "success":
            raise ValueError("Job has not succeeded: " + job.get("name", "unknown"))


def api(path):
    return json.loads(subprocess.check_output(["gh", "api", path], text=True))


def extract_component(archive_path, destination):
    with ZipFile(archive_path) as archive:
        seen = set()
        for item in archive.infolist():
            path = PurePosixPath(item.filename)
            if path.is_absolute() or ".." in path.parts or item.filename in seen:
                raise ValueError("Unsafe or duplicate artifact archive path")
            if stat.S_ISLNK(item.external_attr >> 16):
                raise ValueError("Artifact symlink is forbidden")
            seen.add(item.filename)
        archive.extractall(destination)


def config_files():
    selections = ("compose.yaml", "env", "init", "schema/crackcs", "nginx", "ai-gateway", "monitoring",
                  "ops-agent", "backup-lifecycle.json", "scripts/backup.sh", "scripts/upload-backup.sh",
                  "README.md", "START-HERE.md", "AI-SETUP.md", "VERIFICATION.md", "RESEARCH.md", "DEPLOYMENT-CI.md",
                  "PORTS-PROFILES.md", "scripts/deploy_app.py", "INDEPENDENT-DEPLOYMENT.md")
    files = []
    for relative in selections:
        path = ROOT / relative
        candidates = sorted(path.rglob("*")) if path.is_dir() else [path]
        for candidate in candidates:
            if not candidate.is_file():
                continue
            if candidate.is_symlink() or candidate.name.endswith(".env") or candidate.suffix in (".pem", ".key"):
                raise ValueError("Secrets/symlinks cannot enter config archive")
            files.append(candidate)
    return files


def write_application(application, staging, records, config_commit, output):
    selected = {name: records[name] for name in application_repositories(application)}
    allowed_roots = set(application_repositories(application))
    files = {}
    for record in selected.values():
        for component in record["components"]:
            for relative, checksum in component["files"].items():
                path = PurePosixPath(relative)
                if path.is_absolute() or ".." in path.parts or path.parts[0] not in allowed_roots:
                    raise ValueError("Payload belongs to another application: " + relative)
                if relative in files or sha(staging / relative) != checksum:
                    raise ValueError("Duplicate or changed application payload: " + relative)
                files[relative] = checksum
    identity = hashlib.sha256(json.dumps({"application": application, "repositories": selected,
                                          "files": files}, sort_keys=True).encode()).hexdigest()[:12]
    release_id = application + "-" + identity
    manifest = {"application": application, "releaseId": release_id,
                "builtAt": datetime.now(timezone.utc).isoformat(), "repositories": selected, "files": files,
                "configCommit": config_commit,
                "scope": "successful hosted CI build; only this application is deployed; GCP remains unverified"}
    manifest_path = output / (release_id + ".manifest.json")
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
    archive_path = output / (release_id + ".tar.gz")
    with tarfile.open(archive_path, "w:gz") as archive:
        for relative in sorted(files):
            archive.add(staging / relative, arcname=relative, recursive=False)
        archive.add(manifest_path, arcname="manifest.json")
    return {"releaseId": release_id, "archive": archive_path.name, "manifest": manifest_path.name,
            "sha256": sha(archive_path), "repositories": selected}


def assemble(inputs, application):
    selected_repositories = application_repositories(application)
    if subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT, text=True).strip():
        raise ValueError("Commit the deployment configuration before assembling a CI release")
    config_commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    (ROOT / "bundle").mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="ci-assembly-", dir=ROOT / "bundle") as temporary:
        working = Path(temporary)
        staging = working / "release"
        staging.mkdir()
        records = {}
        for app in selected_repositories:
            repo, components, gate = REPOSITORIES[app]
            selected = inputs["repositories"][app]
            run_id = int(selected["runId"])
            run = api(f"repos/{repo}/actions/runs/{run_id}")
            run["repository"] = repo
            if run["head_sha"] != selected["head"]:
                raise ValueError("Selected run does not match locked source: " + app)
            jobs = api(f"repos/{repo}/actions/runs/{run_id}/jobs?filter=latest&per_page=100")["jobs"]
            require_success(run, jobs)
            if gate not in {job["name"] for job in jobs}:
                raise ValueError("Required verification gate missing: " + gate)
            tree = api(f"repos/{repo}/git/commits/{run['head_sha']}")["tree"]["sha"]
            artifacts = api(f"repos/{repo}/actions/runs/{run_id}/artifacts?per_page=100")["artifacts"]
            verified = []
            for component in components:
                name = f"deployment-{component}-{run_id}-{run['run_attempt']}"
                matches = [item for item in artifacts if item["name"] == name and not item["expired"]]
                if len(matches) != 1:
                    raise ValueError("Expected one unexpired artifact: " + name)
                artifact = matches[0]
                archive_path = working / (component + ".zip")
                with archive_path.open("wb") as output:
                    subprocess.run(["gh", "api", f"repos/{repo}/actions/artifacts/{artifact['id']}/zip"],
                                   stdout=output, check=True)
                digest = artifact.get("digest")
                if digest and digest != "sha256:" + sha(archive_path):
                    raise ValueError("GitHub artifact digest mismatch: " + name)
                directory = working / component
                extract_component(archive_path, directory)
                record = verify_component(directory, component, run, tree)
                for relative in record["files"]:
                    destination = staging / relative
                    if destination.exists():
                        raise ValueError("Duplicate release payload: " + relative)
                    destination.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(directory / relative, destination)
                verified.append({**record, "artifactId": artifact["id"], "artifactDigest": digest})
            records[app] = {"head": run["head_sha"], "treeSha": tree, "dirty": False,
                            "runId": run_id, "runAttempt": run["run_attempt"], "runUrl": run["html_url"],
                            "components": verified}
            print("Verified CI payload:", app, run["head_sha"], flush=True)
        config = config_files() if application == "all" else []
        config_hashes = {str(path.relative_to(ROOT)): sha(path) for path in config}
        identity = hashlib.sha256(json.dumps({"repositories": records,
                                              "configCommit": config_commit, "configFiles": config_hashes}, sort_keys=True).encode()).hexdigest()[:12]
        release_id = "assembly-" + identity
        output = ROOT / "bundle" / release_id
        output.mkdir()
        applications = tuple(APPLICATIONS) if application == "all" else (application,)
        application_records = {name: write_application(name, staging, records, config_commit, output)
                               for name in applications}
        if config:
            with tarfile.open(output / "config.tar.gz", "w:gz") as archive:
                for path in config:
                    archive.add(path, arcname=str(path.relative_to(ROOT)))
        manifest = {"assemblyId": release_id, "builtAt": datetime.now(timezone.utc).isoformat(),
                    "applications": application_records, "configCommit": config_commit, "configFiles": config_hashes,
                    "scope": "independent application archives; shared config is installed separately; GCP remains unverified"}
        (output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
        names = sorted(path.name for path in output.iterdir() if path.is_file())
        (output / "SHA256SUMS").write_text("".join(f"{sha(output / name)}  {name}\n" for name in names))
        print(output)
        return output


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--inputs", type=Path, required=True, help="JSON containing four runId/head pairs")
    parser.add_argument("--app", choices=("all", *APPLICATIONS), required=True,
                        help="all prepares initial files; a service selects only its own successful CI runs")
    args = parser.parse_args()
    assemble(json.loads(args.inputs.read_text()), args.app)


if __name__ == "__main__":
    main()
