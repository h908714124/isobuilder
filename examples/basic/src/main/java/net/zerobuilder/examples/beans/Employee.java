package net.zerobuilder.examples.beans;

import net.zerobuilder.GetterOrder;

abstract class Employee {

  private int id;
  private int salary;
  private String name;

  int getId() {
    return id;
  }

  void setId(int id) {
    this.id = id;
  }

  int getSalary() {
    return salary;
  }

  void setSalary(int salary) {
    this.salary = salary;
  }

  @GetterOrder(0)
  String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }

}