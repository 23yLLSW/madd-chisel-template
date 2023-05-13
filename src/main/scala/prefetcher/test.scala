import chisel3._
import chisel3.util._

class StridePrefetcher(val tableSize: Int, val prefetchDepth: Int, val prefetchWidth: Int) extends Module {
  val io = IO(new Bundle {
    val pc = Input(UInt(32.W))
    val in = Input(UInt(32.W))
    val out = Output(Vec(prefetchWidth, UInt(32.W)))
  })

  val tableAddrBits = log2Ceil(tableSize)//求出tablesize的对数向上取整
  val table = Mem(tableSize, Vec(3, UInt(32.W)))//创建用于存放步长信息的记忆体
  val tableIdx = io.pc(tableAddrBits - 1, 0)

  val strideTable = RegInit(VecInit(Seq.fill(prefetchDepth)(0.U(32.W))))
  val stridePtr = RegInit(0.U(log2Ceil(prefetchDepth).W))

  val matchIdx = Wire(UInt(tableAddrBits.W))
  val matchValid = Wire(Bool())
  val matchPrevAddress = Wire(UInt(32.W))
  val matchPrevStride = Wire(UInt(32.W))

  matchIdx := tableIdx
  matchValid := false.B
  matchPrevAddress := 0.U
  matchPrevStride := 0.U

  for (i <- 0 until tableSize) {
    val isMatch = table(i)(0) === io.pc && table(i)(1) =/= 0.U && table(i)(2) =/= 0.U
    when (isMatch && !matchValid) {
      matchIdx := i.U
      matchValid := true.B
      matchPrevAddress := table(i)(1)
      matchPrevStride := table(i)(2)
    }
  }

  io.out := VecInit(Seq.tabulate(prefetchWidth)(i => io.in + (i + 1).U * matchPrevStride))

  when (io.in === 0.U) {
    strideTable(stridePtr) := 0.U
  } .otherwise {
    strideTable(stridePtr) := io.in - RegNext(io.in)
  }

  stridePtr := Mux(stridePtr === (prefetchDepth - 1).U, 0.U, stridePtr + 1.U)

  when (!matchValid || io.in =/= matchPrevAddress + matchPrevStride) {
    table(matchIdx) := VecInit(Seq(io.pc, io.in, strideTable(stridePtr))))
    tableIdx := Mux(tableIdx === (tableSize - 1).U, 0.U, tableIdx + 1.U)
  }
}
