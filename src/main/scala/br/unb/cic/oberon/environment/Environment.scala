package br.unb.cic.oberon.environment

import br.unb.cic.oberon.ast.{ArrayType, Expression, IntValue, Procedure, RecordType, Type, Undef, UserDefinedType}

import scala.collection.mutable.Map
import scala.collection.mutable.Stack
import scala.collection.mutable.ListBuffer

/**
 * The environment represents a memory region, which
 * could be used for interpreting oberon programs
 * or for type checking oberon programs.
 *
 * We have two kinds of memory: global and stack.
 * All global variables reside in the global are;
 * while local variables reside in the stack.
 *
 * Whenever we call a procedure, we push a memory
 * area (a hash map) into the stack. Whenever we
 * return from a procedure, we pop the stack.
 */
class Environment[T] {

  private val global = Map.empty[String, T]
  private val stack = Stack.empty[Map[String, T]]
  private val procedures = Map.empty[String, Procedure]
  private val userDefinedTypes = Map.empty[String, UserDefinedType]

  private val userArrayTypes = Map.empty[String, ListBuffer[Expression]]

  def setGlobalVariable(name: String, value: T) : Unit = global += name -> value
  def removeGlobalVariable(name: String) : Unit = global -= name
  def addUserDefinedType(userType: UserDefinedType) : Unit = {
    userDefinedTypes  += userDefinedTypeName(userType) -> userType
    userType match {
      case ArrayType(name, length, variableType) => userArrayTypes += name -> ListBuffer.fill(length)(Undef())
      case _ => ???
    }
  }

  def setLocalVariable(name: String, value: T) : Unit = {
    if(stack.size == 0) {
      stack.push(Map.empty[String, T])
    }
    stack.top += name -> value
  }

  def attVariablesReference(): Unit = {
    stack.foreach(m => {
      m.foreach(s =>{
        global.foreach(t =>{
          if(t._1 == s._1){
            removeGlobalVariable(s._1)
            setGlobalVariable(s._1 , s._2)
          }
        })
      })
    })
  }

  def setVariable(name: String, value: T) : Unit = {
    if(!stack.isEmpty && stack.top.contains(name)) {
      setLocalVariable(name, value)
    }
    else if(global.contains(name)) {
      setGlobalVariable(name, value)
    }
    else throw new RuntimeException("Variable " + name + " is not defined")
  }

  def lookup(name: String) : Option[T] = {
    if(!stack.isEmpty && stack.top.contains(name)) Some(stack.top(name))
    else if(global.contains(name)) Some(global(name))
    else None
  }

  def reassignArray(name: String, index: Int, value: Expression) : Unit = {
    if (userArrayTypes.contains(name)) {
      userArrayTypes(name).update(index, value)
    }
  }

  def lookupArrayIndex(name: String, index: Int) : Option[Expression] = {
    if (userArrayTypes.contains(name) && userArrayTypes(name).length > index) {
      Some(userArrayTypes(name)(index))
    }
    else None
  }

  def lookupUserDefinedType(name: String) : Option[UserDefinedType] = userDefinedTypes.get(name)

  def declareProcedure(procedure: Procedure): Unit = procedures(procedure.name) = procedure

  def findProcedure(name: String): Procedure = procedures(name)

  def push(): Unit = stack.push(Map.empty[String, T])

  def pop(): Unit = stack.pop()

  def userDefinedTypeName(userDefinedType: UserDefinedType) : String =
     userDefinedType match {
         case ArrayType(name, _, _) => name
         case RecordType(name, _) => name
     }
}
