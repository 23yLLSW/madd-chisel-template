package mypackage

import chisel3._

class StridePrefetcherIO(val tableSize: Int, val prefetchDepth: Int, val prefetchWidth: Int) extends Bundle {
  val pc = Input(UInt(32.W))
  val in = Input(UInt(32.W))
  val out = Output(Vec(prefetchWidth, UInt(32.W)))
}
