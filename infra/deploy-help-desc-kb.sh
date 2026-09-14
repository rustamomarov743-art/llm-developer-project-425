#!/usr/bin/env bash

set -euo pipefail

cd "$(dirname "$0")/.."
source infra/lib/env.sh
require YC YC_FOLDER_ID

TOKEN=$("$YC" iam create-token)

yandex-ai-studio vector-stores local docs/*.md \
  --name "help-desk-kb" \
  --folder-id "$YC_FOLDER_ID" \
  --auth "$TOKEN"