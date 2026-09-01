#!/usr/bin/env bash

YC="${YC:-$HOME/yandex-cloud/bin/yc}"
FOLDER_ID=$("$YC" config get folder-id)
TOKEN=$("$YC" iam create-token)


cd "$(dirname "$0")/.."

yandex-ai-studio vector-stores local docs/*.md \
  --name "help-desk-kb" \
  --folder-id "$FOLDER_ID" \
  --auth "$TOKEN"