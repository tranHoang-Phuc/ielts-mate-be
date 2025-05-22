#!/usr/bin/env bash



set -euo pipefail

# Danh sách module (thư mục) cần build
MODULES=(
  identity-service
  notification-service
  discovery-service
  api-gateway
)

# Kiểm tra biến DOCKERHUB_USER
if [ -z "${DOCKERHUB_USER:-}" ]; then
  echo "💡 Vui lòng export DOCKERHUB_USER trước khi chạy script."
  exit 1
fi

# Thẻ version (mặc định "latest" nếu không truyền)
IMAGE_TAG="${1:-latest}"

# Kiểm tra Docker daemon
if ! docker info > /dev/null 2>&1; then
  echo "❌ Docker daemon không chạy hoặc không thể kết nối."
  exit 1
fi

# Đăng nhập Docker Hub (không truyền registry làm host)
echo "🔑 Đăng nhập Docker Hub với user: ${DOCKERHUB_USER}"
docker login --username "${DOCKERHUB_USER}"

echo "🐳 Bắt đầu build & push images với tag: ${IMAGE_TAG}"
for MODULE in "${MODULES[@]}"; do
  IMAGE_NAME="${DOCKERHUB_USER}/${MODULE}"
  FULL_IMAGE="${IMAGE_NAME}:${IMAGE_TAG}"

  echo "----------------------------------------"
  echo "🚀 Building image: ${FULL_IMAGE}"
  docker build \
    -f "${MODULE}/Dockerfile" \
    -t "${FULL_IMAGE}" \
    "${MODULE}"

  echo "📤 Pushing image: ${FULL_IMAGE}"
  docker push "${FULL_IMAGE}"
done

echo "✅ Đã build & push xong ${#MODULES[@]} images."