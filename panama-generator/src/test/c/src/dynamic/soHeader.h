//
// Created by dreamlike on 23-9-9.
//

#ifndef PLAYGROUD_DYNAMIC_LIB_H
#define PLAYGROUD_DYNAMIC_LIB_H
extern "C" {
    int add(int a, int b);
    struct Person {
        int a;
        long n;
    };

    struct TestContainer {
        int size;
        Person single;
        Person* ptr;
        union {
            int union_a;
            long union_b;
        };
        Person personArray[3];
        //
        Person* arrayButPointer;
    };

    struct Person* fillPerson(int a, long n);

    int getA(Person* person);

    long getN(Person* person);

    TestContainer* initContainer(int size, Person* person, long unionValue);
    void setSize(TestContainer* container, int value);
    int getSize(TestContainer* container);
    void setSingle(TestContainer* container, Person value);
    Person getSingle(TestContainer* container);
    void setUnionA(TestContainer* container, int value);
    int getUnionA(TestContainer* container);
    void setUnionB(TestContainer* container, long value);
    long getUnionB(TestContainer* container);
    void setPersonArray(TestContainer* container, Person value, int index);
    Person getPersonArray(TestContainer* container, int index);
    void setArrayButPointer(TestContainer* container, Person* value);
    Person* getArrayButPointer(TestContainer* container);

    int testContainerSize();

    int current_error(int dummy, long dummy2);

    long set_error_no(int error, long returnValue);

    void set_array(long* array, int index, long value);
};
#endif //PLAYGROUD_DYNAMIC_LIB_H
