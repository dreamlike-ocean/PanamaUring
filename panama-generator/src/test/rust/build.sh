rm -f ../resources/libperson.so
cargo build || { echo "cargo build fail!"; exit 1; }
mkdir -p ../resources

platform=$(uname -s)

if [[ "$platform" == "Linux" ]]; then
  mv target/debug/libperson.so ../resources/libperson.so
  echo "build on Linux"
elif [[ "$platform" == "Darwin" ]]; then
  mv target/debug/libperson.dylib ../resources/libperson.so
  echo "build on MacOS"
else
  echo "Unsupported platform"
  exit 1
fi