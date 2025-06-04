#!/usr/bin/env bash

# =========================================
# Script: run-all.sh
# Mục đích:
#   1. Build project cha (và tất cả module con) bằng mvn, bỏ qua test
#   2. Sau khi build xong, vào từng module con, tìm JAR và chạy background
# Cách dùng:
#   1. Đặt file này ở thư mục gốc (chứa pom.xml của project cha)
#   2. chmod +x run-all.sh
#   3. ./run-all.sh
# =========================================

modules=(
  "api-gateway"
  "discovery-service"
  "identity-service"
  "notification-service"
  "reading-service"
)

log_dir="logs"
if [ ! -d "$log_dir" ]; then
  mkdir "$log_dir"
fi

# 1. Build project cha (skip tests) bằng mvn
echo "============================================"
echo "🔨 Building parent project (skip tests) with mvn..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
  echo "❌ Parent build failed. Exiting."
  exit 1
fi
echo "✅ Build parent & all modules thành công."
echo "============================================"
echo

for module in "${modules[@]}"; do
  echo "============================================"
  echo "🚀 Entering module: $module"

  if [ ! -d "$module" ]; then
    echo "❌ Directory '$module' does not exist! Skipping."
    continue
  fi

  cd "$module" || { echo "❌ Cannot cd into $module"; exit 1; }

  jar_file=$(find target -maxdepth 1 -type f -name "*.jar" | head -n 1)
  if [ -z "$jar_file" ]; then
    echo "⚠️ No *.jar file found in ${module}/target."
    echo "   Có thể module này không tạo jar hoặc build chưa khớp. Skipping."
    cd .. && continue
  fi

  module_name=$(basename "$module")
  log_file="../$log_dir/${module_name}.log"

  echo "   ▶️ Running: java -jar $jar_file"
  nohup java -jar "$jar_file" > "$log_file" 2>&1 &

  pid=$!
  echo "   ✅ Started [$module_name] with PID=$pid, log → $log_file"

  cd ..
done

echo "============================================"
echo "🎉 All modules have been started."
echo "   Check logs in '$log_dir/' if needed."
echo

# Giữ màn hình terminal mở cho đến khi bấm phím
read -n 1 -s -r -p "Press any key to exit..."
