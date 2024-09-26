use person::Person;

#[test]
fn test_work() {
    println!("hello test!");
    assert_eq!(2 + 2, 4);
}


#[test]
fn test_sub() {
    use person::callback;
    let res = callback(1, 2, return_two);
    assert_eq!(res, 5);
}


extern fn return_two(_: i32, _: i32) -> i32 {
    2
}

#[test]
fn test_person() {
    let person = person::fillPerson(1, 2);
    unsafe {
        assert_eq!((*person).a, 1);
        assert_eq!((*person).n, 2);

        assert_eq!(person::getA(person), 1);
        assert_eq!(person::getN(person), 2);
    }

}

#[test]
fn test_container() {
    unsafe {
        let mut original_person_struct = person::Person {
            a: 1,
            n: 2,
        };
        let container = person::initContainer(3, &mut original_person_struct as *mut person::Person, 2);

        assert_eq!((*container).size, 3);
        original_person_struct.a = 10;

        let x = (*container).ptr;
        assert_eq!((*x).a, 10);

        assert_eq!((*container).union_field.long_value, 2);

        assert_eq!((*container).person_array[2].a, 1);

        assert_eq!( (*(*container).array_but_pointer.add(2)).a, 1);

        person::setSize(container, 4);
        assert_eq!((*container).size, 4);
        assert_eq!(person::getSize(container), 4);

        let new_single = Person {
            a: 10,
            n: 20,
        };

        person::setSingle(container, new_single);
        assert_eq!((*container).single.a, 10);

        let new_single = person::getSingle(container);
        let x1 = &mut (*container).single;
        x1.a = 20;
        assert_eq!(new_single.a, 10);

        person::setUnionB(container, 10000);

        assert_eq!(person::getUnionB(container), 10000);


        let p = Person {
            a : 2001,
            n: 20
        };

        let res = person::setPersonArray(container, p ,1);
        assert_ne!(res, -1);

        let a = person::getPersonArray(container, 1).a;
        assert_eq!(a, 2001);


        let array_ptr:&mut Vec<Person> = Box::leak(Box::new(Vec::with_capacity(5)));
        std::ptr::write(array_ptr.as_mut_ptr().add(0), Person {
            a: 0214,
            n: 10
        });

        person::setArrayButPointer(container, array_ptr.as_mut_ptr());
        let array_ptr = person::getArrayButPointer(container);
        assert_eq!( (*array_ptr).a, 0214);
    }
}

#[test]
fn test_container_size() {
    let size = person::testContainerSize();
    assert_eq!(96, size);
}

#[test]
fn test_set_array() {
    unsafe {
        let mut array: [i64; 3] = [1, 2, 3];
        let ptr = array.as_mut_ptr();
        person::set_array(ptr, 1, 100);
        assert_eq!(100, array[1]);
    }
}

#[test]
fn test_error_no() {
    unsafe {
        let return_value = person::set_error_no(-38, 10000);
        assert_eq!(return_value, 10000);

        assert_eq!(-38, person::current_error(1,2))
    }
}