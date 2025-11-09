echo "Running Panama generator native-image test"
"$(dirname "$0")/target/panama-generator-test-native"

if [ $? -eq 0 ]; then
  echo "$BIN completed successfully."
  exit 0
else
  echo "$BIN failed."
  exit 1
fi
