//
// Created by dreamlike on 23-9-9.
//
#include <iostream>
#include <stdlib.h>
#include "soHeader.h"
#include <errno.h>

int add(int a, int b) {
    return a + b;
}

Person* fillPerson(int a, long n) {
    auto p = static_cast<Person*>(malloc(sizeof(Person)));
    p->a = a;
    p->n = n;
    return p;
}

int getA(Person* p) {
    return p -> a;
}

long getN(Person* p) {
    return p->n;
}

TestContainer* initContainer(int size, Person* person, long unionValue) {
    auto p = static_cast<TestContainer*>(malloc(sizeof(TestContainer)));
    p->size = size;
    p->single = *person;
    p->union_b = unionValue;
    p->ptr = person;
    for(auto & i : p->personArray) {
        i = *person;
    }
    p-> arrayButPointer = static_cast<Person*>(malloc(size * sizeof(Person)));
    for(int i = 0; i < size; i++) {
        p-> arrayButPointer[i] = *person;
    }
    return p;
}

void setSize(TestContainer* container, int value) {
    container->size = value;
}

int getSize(TestContainer* container) {
    return container->size;
}

// Setter and Getter for single
void setSingle(TestContainer* container, Person value) {
    container->single = value;
}

Person getSingle(TestContainer* container) {
    return container->single;
}

// Setter and Getter for union_a
void setUnionA(TestContainer* container, int value) {
    container->union_a = value;
}

int getUnionA(TestContainer* container) {
    return container->union_a;
}

// Setter and Getter for union_b
void setUnionB(TestContainer* container, long value) {
    container->union_b = value;
}

long getUnionB(TestContainer* container) {
    return container->union_b;
}

// Setter and Getter for personArray
void setPersonArray(TestContainer* container, Person value, int index) {
    if (index >= 0 && index < 3) {
        container->personArray[index] = value;
    }
}

Person getPersonArray(TestContainer* container, int index) {
    if (index >= 0 && index < 3) {
        return container->personArray[index];
    }
}

Person* getPersonArrayPointer(TestContainer* container, int index) {
    if (index >= 0 && index < 3) {
        return &container->personArray[index];
    }
}


// Setter and Getter for arrayButPointer
void setArrayButPointer(TestContainer* container, Person* value) {
    container->arrayButPointer = value;
}

Person* getArrayButPointer(TestContainer* container) {
    return container->arrayButPointer;
}

int testContainerSize() {
    return sizeof(TestContainer);
}

int current_error(int dummy, long dummy2) {
    return errno;
}

long set_error_no(int error, long returnValue) {
   int* errno_location = &errno;
    *errno_location = error;
    return returnValue;
}