package io.elfinos.machine

sealed trait MachineEvent extends CborSerializable {
  def ncId: String
}

object MachineEvent {}
