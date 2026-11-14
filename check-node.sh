#!/usr/bin/env bash
# Fails with a readable message when Node.js is missing or older than the Angular CLI needs.
# Sourced by run.sh and test.sh; the supported range matches "engines" in frontend/package.json.
# If the current Node is unsupported and nvm is installed, switches to the version from .nvmrc.

node_is_supported() {
  command -v node >/dev/null 2>&1 || return 1
  local v major minor patch
  v="$(node -v)"
  IFS=. read -r major minor patch <<<"${v#v}"
  if [ "$major" -ge 26 ]; then return 0; fi
  if [ "$major" -eq 24 ] && [ "$minor" -ge 15 ]; then return 0; fi
  if [ "$major" -eq 22 ] && { [ "$minor" -gt 22 ] || { [ "$minor" -eq 22 ] && [ "$patch" -ge 3 ]; }; }; then return 0; fi
  return 1
}

if ! node_is_supported && [ -s "${NVM_DIR:-$HOME/.nvm}/nvm.sh" ]; then
  # nvm.sh is not safe under `set -u`; restore the flag afterwards.
  case $- in *u*) nounset=1 ;; *) nounset=0 ;; esac
  set +u
  # shellcheck disable=SC1091
  . "${NVM_DIR:-$HOME/.nvm}/nvm.sh"
  nvm use --silent "$(cat "$(dirname "${BASH_SOURCE[0]}")/.nvmrc")" >/dev/null 2>&1 || true
  [ "$nounset" -eq 1 ] && set -u
fi

if ! command -v node >/dev/null 2>&1; then
  echo "ERROR: Node.js not found. Install Node.js 22.22.3+ (22.x) or 24.15+ (see README.md, .nvmrc)." >&2
  exit 1
fi

if ! node_is_supported; then
  echo "ERROR: Node.js $(node -v) is not supported. Use Node.js 22.22.3+ (22.x) or 24.15+." >&2
  echo "       With nvm: run 'nvm install && nvm use' in the repository root (see .nvmrc)." >&2
  exit 1
fi
