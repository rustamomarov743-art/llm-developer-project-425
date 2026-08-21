#!/usr/bin/env bash
#
# Собирает rag.zip для Cloud Functions.
#
# Раскладка архива — «зависимости через pom.xml»: в корне pom.xml, рядом
# исходники. Зависимости резолвит и компилирует само облако при деплое.
# Смешивать этот способ с готовыми JAR'ами в архиве нельзя.
#
# Почему не JAR'ы: архив с ними весит 38 МБ (замер), а прямая загрузка ZIP
# ограничена 3.5 МБ — пришлось бы заводить бакет Object Storage. Архив с
# pom.xml весит 8 КБ и грузится напрямую.
#
# Диагностика при этом не теряется: mvn verify ниже компилирует и прогоняет
# тесты локально, так что до облака доезжает уже проверенный код. В облаке
# остаётся только резолв зависимостей и javac.
set -euo pipefail

cd "$(dirname "$0")"

STAGE=target/package
ARCHIVE=target/help-desc.zip

echo "==> mvn verify (компиляция и тесты локально)"
mvn -B -q clean verify

echo "==> раскладка архива"
rm -rf "$STAGE"
mkdir -p "$STAGE"
cp pom.xml "$STAGE/"
# Стандартная maven-раскладка: sourceDirectory по умолчанию = src/main/java.
# Документация показывает исходники прямо в каталогах пакетов рядом с pom.xml —
# если облачный сборщик ожидает именно это, здесь достаточно поменять на
# `cp -r src/main/java/. "$STAGE/"` и прописать <sourceDirectory>.</sourceDirectory>.
mkdir -p "$STAGE/src/main"
cp -r src/main/java "$STAGE/src/main/"

# src/test в архив не кладём: в облаке тесты гонять незачем, они уже прошли выше.

echo "==> zip"
rm -f "$ARCHIVE"
(cd "$STAGE" && python3 -m zipfile -c "$OLDPWD/$ARCHIVE" ./*)

echo
echo "Готово: $ARCHIVE"
echo "  исходников: $(find "$STAGE" -name '*.java' | wc -l)"
echo "  размер:     $(du -h "$ARCHIVE" | cut -f1)  (лимит прямой загрузки — 3.5 МБ)"
