#!/bin/bash
set -e
SDK=/opt/android-sdk
BT=$SDK/build-tools/34.0.0
cd "$(dirname "$0")"
rm -rf build/gen build/obj build/dex build/base.apk build/unaligned.apk build/aligned.apk
mkdir -p build/gen build/obj build/dex
$BT/aapt2 compile --dir res -o build/res.zip
$BT/aapt2 link -o build/base.apk -I $SDK/platforms/android-34/android.jar \
  --manifest AndroidManifest.xml -A assets --java build/gen build/res.zip
javac -encoding UTF-8 -source 8 -target 8 \
  -bootclasspath $SDK/platforms/android-34/android.jar \
  -classpath $SDK/platforms/android-34/android.jar \
  -d build/obj build/gen/com/ray/kanban/R.java src/com/ray/kanban/*.java
$BT/d8 --lib $SDK/platforms/android-34/android.jar --min-api 24 --output build/dex $(find build/obj -name "*.class")
python3 - <<'PY'
import zipfile,shutil
shutil.copy('build/base.apk','build/unaligned.apk')
with zipfile.ZipFile('build/unaligned.apk','a',zipfile.ZIP_DEFLATED) as z:
    z.write('build/dex/classes.dex','classes.dex')
PY
$BT/zipalign -f 4 build/unaligned.apk build/aligned.apk
$BT/apksigner sign --ks keys/kanban.keystore --ks-pass pass:kanban2026 \
  --key-pass pass:kanban2026 --out build/kanban.apk build/aligned.apk
$BT/apksigner verify build/kanban.apk && ls -la build/kanban.apk
