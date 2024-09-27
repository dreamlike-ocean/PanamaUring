use std::os::raw::c_void;

use jemalloc_sys::{free, malloc, malloc_usable_size, posix_memalign};



#[no_mangle]
pub unsafe extern "C" fn ring_malloc(size: u64) -> *mut c_void {
    malloc(size as usize)
}

#[no_mangle]
pub unsafe extern "C" fn ring_free(ptr: *mut c_void) {
    free(ptr);
}

#[no_mangle]
pub unsafe extern "C" fn ring_malloc_usable_size(ptr: *mut c_void) -> usize {
    malloc_usable_size(ptr)
}


#[no_mangle]
pub unsafe extern "C" fn ring_posix_memalign(ptr: *mut *mut c_void, alignment: usize, size: usize) -> i32 {
    posix_memalign(ptr, alignment, size)
}


 