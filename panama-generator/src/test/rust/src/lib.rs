#[no_mangle]
pub extern "C" fn add(i: i32, j: i32) -> i32 {
    i + j
}

#[no_mangle]
pub extern "C" fn callback(i: i32, j: i32, f: extern fn(i32, i32) -> i32) -> i32 {
    i + j + f(i, j)
}

#[repr(C)]
pub struct Person {
    pub a: i32,
    pub n: i64,
}

#[repr(C)]
pub union int_or_long {
    pub int_value: i32,
    pub long_value: i64,
}

#[repr(C)]
pub struct TestContainer {
    pub size: i32,
    pub single: Person,
    pub ptr: *mut Person,
    pub union_field: int_or_long,
    pub person_array: [Person; 3],
    pub array_but_pointer: *mut Person,
}

#[no_mangle]
pub extern "C" fn fillPerson(a: i32, n: i64) -> *mut Person {
    Box::leak(Box::new(Person { a, n }))
}

#[no_mangle]
pub unsafe extern "C" fn getA(p: *mut Person) -> i32 {
    (*p).a
}

#[no_mangle]
pub unsafe extern "C" fn getN(p: *mut Person) -> i64 {
    (*p).n
}

#[no_mangle]
pub unsafe extern "C" fn initContainer(size: i32, ptr: *mut Person, union_value: i64) -> *mut TestContainer {
    use std::ptr::{null_mut, read};
    let container_ptr = Box::leak(Box::new(TestContainer {
        size,
        single: read(ptr),
        ptr,
        union_field: int_or_long { long_value: union_value },
        person_array: [read(ptr), read(ptr), read(ptr)],
        array_but_pointer: null_mut(),
    }));
    let size = size as usize;
    let array_ptr = Box::leak(Box::new(Vec::with_capacity(size)));

    for _ in 0..size {
        array_ptr.push(read(ptr));
    }

    container_ptr.array_but_pointer = array_ptr.as_mut_ptr();
    container_ptr
}

#[no_mangle]
pub unsafe extern "C" fn setSize(container: *mut TestContainer, value: i32) -> () {
    *(&mut (*container).size) = value;
}

#[no_mangle]
pub unsafe extern "C" fn getSize(container: *mut TestContainer) -> i32 {
    (*container).size
}

#[no_mangle]
pub unsafe extern "C" fn setSingle(container: *mut TestContainer, value: Person) -> () {
    *(&mut (*container).single) = value;
}

#[no_mangle]
pub unsafe extern "C" fn getSingle(container: *mut TestContainer) -> Person {
    std::ptr::read(&(*container).single)
}


#[no_mangle]
pub unsafe  extern "C" fn getUnionB(container: *mut TestContainer) -> i64 {
    (*container).union_field.long_value
}

#[no_mangle]
pub unsafe extern "C" fn setUnionB(container: *mut TestContainer, value: i64){
    (*container).union_field.long_value = value;
}

#[no_mangle]
pub unsafe extern "C" fn setPersonArray(container: *mut TestContainer, value :Person, index: i32) -> i32 {
    if index < 0 || index > ((*container).person_array).len() as i32 {
        return -1;
    }

    let index = index as usize;
    let element_ptr = (*container).person_array.as_mut_ptr().add(index);
    std::ptr::write(element_ptr, value);
    return 0;
}

#[no_mangle]
pub unsafe extern "C" fn getPersonArray(container: *mut TestContainer, index: i32) -> Person {
    if index < 0 || index > ((*container).person_array).len() as i32 {
        return Person {
            a : 0,
            n : 0
        };
    }
    let index = index as usize;
    let element_ptr = (*container).person_array.as_mut_ptr().add(index);
    return std::ptr::read(element_ptr);
}

#[no_mangle]
pub unsafe extern "C" fn setArrayButPointer(container: *mut TestContainer, value :*mut Person) {
    (*container).array_but_pointer = value;
}

#[no_mangle]
pub unsafe extern "C" fn getArrayButPointer(container: *mut TestContainer) -> *mut Person {
    return (*container).array_but_pointer;
}

#[no_mangle]
pub extern "C" fn testContainerSize() -> usize {
    return std::mem::size_of::<TestContainer>();
}

#[no_mangle]
pub unsafe extern "C" fn set_array(array :*mut i64, index: usize, value : i64) {
    *array.add(index) = value;
}

#[no_mangle]
pub unsafe extern "C" fn current_error(_ :i32, _: i32) -> i32 {
    return *libc::__errno_location();
}

#[no_mangle]
pub unsafe extern "C" fn set_error_no(error_no: i32, return_value: i64) -> i64 {
    (*libc::__errno_location()) = error_no;
    return_value
}