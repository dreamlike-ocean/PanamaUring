use uring_sys2::{
    __kernel_timespec, io_uring, io_uring_buf_reg, io_uring_buf_ring, io_uring_cq, io_uring_cqe, io_uring_probe, io_uring_sq, io_uring_sqe, iovec, sigset_t
};

#[no_mangle]
pub unsafe extern "C" fn io_uring_struct_size_rs() -> usize {
    std::mem::size_of::<io_uring>()
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_cq_struct_size_rs() -> usize {
    std::mem::size_of::<io_uring_cq>()
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_cqe_struct_size_rs() -> usize {
    std::mem::size_of::<io_uring_cqe>()
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_sq_struct_size_rs() -> usize {
    std::mem::size_of::<io_uring_sq>()
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_sqe_struct_size_rs() -> usize {
    std::mem::size_of::<io_uring_sqe>()
}


#[no_mangle]
pub unsafe extern "C" fn io_uring_get_probe_rs() -> *mut io_uring_probe {
    uring_sys2::io_uring_get_probe()
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_free_probe_rs(ptr: *mut io_uring_probe) {
    uring_sys2::io_uring_free_probe(ptr);
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_queue_init_rs(
    entries: u32,
    ring: *mut uring_sys2::io_uring,
    flags: u32,
) -> i32 {
    uring_sys2::io_uring_queue_init(entries, ring, flags)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_queue_init_params_rs(
    entries: u32,
    ring: *mut uring_sys2::io_uring,
    param: *mut uring_sys2::io_uring_params,
) -> i32 {
    uring_sys2::io_uring_queue_init_params(entries, ring, param)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_queue_exit_rs(ring: *mut uring_sys2::io_uring) {
    uring_sys2::io_uring_queue_exit(ring)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_setup_rs(entries: u32, p: *mut uring_sys2::io_uring_params) -> i32 {
    uring_sys2::io_uring_setup(entries, p)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_peek_cqe_rs(
    ring: *mut io_uring,
    cqe_ptr: *mut *mut io_uring_cqe,
) -> i32 {
    uring_sys2::io_uring_peek_cqe(ring, cqe_ptr)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_wait_cqes_rs(
    ring: *mut io_uring,
    cqe_ptr: *mut *mut io_uring_cqe,
    wait_nr: u32,
    ts: *mut __kernel_timespec,
    sigmask: *mut sigset_t,
) -> i32 {
    //加个后缀避免链接冲突
    uring_sys2::io_uring_wait_cqes(ring, cqe_ptr, wait_nr, ts, sigmask)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_wait_cqe_timeout_rs(
    ring: *mut io_uring,
    cqe_ptr: *mut *mut io_uring_cqe,
    ts: *mut __kernel_timespec,
) -> i32 {
    //加个后缀避免链接冲突
    uring_sys2::io_uring_wait_cqe_timeout(ring, cqe_ptr, ts)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_submit_rs(ring: *mut io_uring) -> i32 {
    uring_sys2::io_uring_submit(ring)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_submit_and_wait_rs(ring: *mut io_uring, wait_nr: u32) -> i32 {
    uring_sys2::io_uring_submit_and_wait(ring, wait_nr)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_submit_and_wait_timeout_rs(
    ring: *mut io_uring,
    cqe_ptr: *mut *mut io_uring_cqe,
    wait_nr: u32,
    ts: *mut __kernel_timespec,
    sigmask: *mut sigset_t,
) -> i32 {
    //加个后缀避免链接冲突
    uring_sys2::io_uring_submit_and_wait_timeout(ring, cqe_ptr, wait_nr, ts, sigmask)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_register_buffers_rs(
    ring: *mut io_uring,
    iovecs: *const iovec,
    nr_iovecs: ::std::os::raw::c_uint,
) -> i32 {
    uring_sys2::io_uring_register_buffers(ring, iovecs, nr_iovecs)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_unregister_buffers_rs(ring: *mut io_uring) -> i32 {
    uring_sys2::io_uring_unregister_buffers(ring)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_register_files_rs(
    ring: *mut io_uring,
    files: *mut i32,
    nr_files: u32,
) -> i32 {
    uring_sys2::io_uring_register_files(ring, files, nr_files)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_unregister_files_rs(ring: *mut io_uring) -> i32 {
    uring_sys2::io_uring_unregister_files(ring)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_register_eventfd_rs(ring: *mut io_uring, fd: i32) -> i32 {
    uring_sys2::io_uring_register_eventfd(ring, fd)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_register_eventfd_async_rs(ring: *mut io_uring, fd: i32) -> i32 {
    uring_sys2::io_uring_register_eventfd_async(ring, fd)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_unregister_eventfd_rs(ring: *mut io_uring) -> i32 {
    uring_sys2::io_uring_unregister_eventfd(ring)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_register_ring_fd_rs(ring: *mut io_uring) -> i32 {
    uring_sys2::io_uring_register_ring_fd(ring)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_unregister_ring_fd_rs(ring: *mut io_uring) -> i32 {
    uring_sys2::io_uring_unregister_ring_fd(ring)
}

#[no_mangle]
pub unsafe extern "C" fn io_uring_close_ring_fd_rs(ring: *mut io_uring) -> i32 {
    uring_sys2::io_uring_close_ring_fd(ring)
}

// io_uring_register_buf_ring

#[no_mangle]
pub unsafe extern "C" fn io_uring_register_buf_ring_rs(
    ring: *mut io_uring,
    br: *mut io_uring_buf_reg,
    nr_buf_ring: u32,
) -> i32 {
    uring_sys2::io_uring_register_buf_ring(ring, br, nr_buf_ring)
}

// io_uring_unregister_buf_ring
#[no_mangle]
pub unsafe extern "C" fn io_uring_unregister_buf_ring_rs(ring: *mut io_uring, bgid: i32) -> i32 {
    uring_sys2::io_uring_unregister_buf_ring(ring, bgid)
}

// NativeIoUringBufRing io_uring_setup_buf_ring(@Pointer IoUring ioUring, int nentries, int bgid, int flags,/*int *ret*/@Pointer MemorySegment ret);
#[no_mangle]
pub unsafe extern "C" fn io_uring_setup_buf_ring_rs(
    ring: *mut io_uring,
    nentries: u32,
    bgid: i32,
    flags: u32,
    ret: *mut i32,
) -> *mut io_uring_buf_ring {
    uring_sys2::io_uring_setup_buf_ring(ring, nentries, bgid, flags, ret)
}

//  int io_uring_buf_ring_head(@Pointer IoUring ring, int buf_group, /* unsigned * head*/ @Pointer MemorySegment head);
#[no_mangle]
pub unsafe extern "C" fn io_uring_buf_ring_head_rs(
    ring: *mut io_uring,
    bgid: i32,
    head: *mut u16,
) -> i32 {
    uring_sys2::io_uring_buf_ring_head(ring, bgid, head)
}

// io_uring_submit_and_get_events
#[no_mangle]
pub unsafe extern "C" fn io_uring_submit_and_get_events_rs(
    ring: *mut io_uring
) -> i32 {
    uring_sys2::io_uring_submit_and_get_events(ring)
}

// io_uring_get_events
#[no_mangle]
pub unsafe extern "C" fn io_uring_get_events_rs(
    ring: *mut io_uring
) -> i32 {
    uring_sys2::io_uring_get_events(ring)
}