#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/release_tag_deploy.sh --tag vX.Y.Z [options]

Options:
  --tag <tag>           Release tag (required), ex) v1.0.0
  --remote <name>       Git remote name (default: origin)
  --branch <name>       Release branch to verify (default: main)
  --message <text>      Annotated tag message (default: "release <tag>")
  --allow-dirty         Allow uncommitted changes
  --force               Skip main branch head sync check
  --yes                 Skip confirmation prompt
  --dry-run             Validate only, do not create/push tag
  -h, --help            Show help

Examples:
  scripts/release_tag_deploy.sh --tag v1.0.0
  scripts/release_tag_deploy.sh --tag v1.0.1 --yes
  scripts/release_tag_deploy.sh --tag v1.1.0 --dry-run
EOF
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing required command: $1" >&2
    exit 1
  fi
}

TAG=""
REMOTE="origin"
BRANCH="main"
MESSAGE=""
ALLOW_DIRTY="false"
FORCE="false"
ASSUME_YES="false"
DRY_RUN="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --tag) TAG="$2"; shift 2 ;;
    --remote) REMOTE="$2"; shift 2 ;;
    --branch) BRANCH="$2"; shift 2 ;;
    --message) MESSAGE="$2"; shift 2 ;;
    --allow-dirty) ALLOW_DIRTY="true"; shift 1 ;;
    --force) FORCE="true"; shift 1 ;;
    --yes) ASSUME_YES="true"; shift 1 ;;
    --dry-run) DRY_RUN="true"; shift 1 ;;
    -h|--help) usage; exit 0 ;;
    *)
      echo "unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

require_cmd git

if [[ -z "$TAG" ]]; then
  echo "--tag is required." >&2
  usage
  exit 1
fi

if [[ ! "$TAG" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "invalid tag format: $TAG (expected: vX.Y.Z)" >&2
  exit 1
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "not inside a git repository." >&2
  exit 1
fi

if [[ "$ALLOW_DIRTY" != "true" ]]; then
  if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "working tree is not clean. Commit/stash changes or use --allow-dirty." >&2
    exit 1
  fi
fi

current_branch="$(git rev-parse --abbrev-ref HEAD)"
if [[ "$current_branch" != "$BRANCH" ]]; then
  echo "current branch is '$current_branch'. Switch to '$BRANCH' before tagging." >&2
  exit 1
fi

echo "[1/4] fetch remote refs..."
git fetch "$REMOTE" "$BRANCH" --tags >/dev/null

if ! git rev-parse --verify "refs/remotes/$REMOTE/$BRANCH" >/dev/null 2>&1; then
  echo "remote branch not found: $REMOTE/$BRANCH" >&2
  exit 1
fi

local_head="$(git rev-parse HEAD)"
remote_head="$(git rev-parse "refs/remotes/$REMOTE/$BRANCH")"

if [[ "$FORCE" != "true" && "$local_head" != "$remote_head" ]]; then
  echo "HEAD is not synced with $REMOTE/$BRANCH." >&2
  echo "local : $local_head" >&2
  echo "remote: $remote_head" >&2
  echo "sync branch first, or use --force if intentional." >&2
  exit 1
fi

if git rev-parse --verify "refs/tags/$TAG" >/dev/null 2>&1; then
  echo "local tag already exists: $TAG" >&2
  exit 1
fi

if [[ -n "$(git ls-remote --tags "$REMOTE" "refs/tags/$TAG")" ]]; then
  echo "remote tag already exists: $TAG" >&2
  exit 1
fi

if [[ -z "$MESSAGE" ]]; then
  MESSAGE="release $TAG"
fi

echo "[2/4] release summary"
echo "  repo      : $(basename "$(git rev-parse --show-toplevel)")"
echo "  remote    : $REMOTE"
echo "  branch    : $BRANCH"
echo "  tag       : $TAG"
echo "  commit    : $local_head"
echo "  message   : $MESSAGE"
echo "  dry-run   : $DRY_RUN"

if [[ "$ASSUME_YES" != "true" ]]; then
  printf "continue? [y/N] "
  read -r answer
  if [[ "$answer" != "y" && "$answer" != "Y" ]]; then
    echo "aborted."
    exit 0
  fi
fi

if [[ "$DRY_RUN" == "true" ]]; then
  echo "[3/4] dry-run: skip tag creation"
  echo "[4/4] dry-run: skip tag push"
  exit 0
fi

echo "[3/4] create annotated tag..."
git tag -a "$TAG" -m "$MESSAGE"

echo "[4/4] push tag..."
git push "$REMOTE" "refs/tags/$TAG"

echo "done."
echo "deployment workflow should start for tag: $TAG"
