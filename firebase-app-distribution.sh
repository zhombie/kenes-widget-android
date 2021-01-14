echo "=== start SOS DEV upload ==="
./gradlew assembleDev appDistributionUploadDevDebug
echo ""
echo "=== start SOS upload ==="
./gradlew assembleProd appDistributionUploadProdDebug