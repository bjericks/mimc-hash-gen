package mimc

import chisel3._
import chisel3.util._

// Standard modular multiplication IO
// out.bits = (a * b) % mod
// out.valid signals when multiplication is done
//  * out.valid should be 1 for single-cycle multipliers
class ModMultIO(width: Int, mod: Int) extends Bundle {
	val a = Input(UInt(width.W))
	val b = Input(UInt(width.W))
	val out = Valid(UInt(log2Ceil(mod).W))
	override def cloneType = (new ModMultIO(width, mod)).asInstanceOf[this.type]
}

// Simple single-cycle multiplier using built-in operations
class ModMult(width: Int, mod: Int) extends Module {
	val io = IO(new ModMultIO(width, mod))
	io.out.bits := (io.a * io.b) % mod.U
	io.out.valid := 1.B
}