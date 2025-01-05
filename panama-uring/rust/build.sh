rm -f ../src/main/resources/liburing-ffi.so
cargo build --release
mkdir -p ../src/main/resources/
mv target/release/liburing_rs.so ../src/main/resources/liburing-ffi.so