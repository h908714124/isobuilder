package net.zerobuilder.examples.beans;

import net.zerobuilder.BeanRejectNull;
import net.zerobuilder.BeanStep;

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

  @BeanStep(0)
  @BeanRejectNull
  String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }

}