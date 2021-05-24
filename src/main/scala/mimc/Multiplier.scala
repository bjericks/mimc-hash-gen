package mimc

import chisel3._
import chisel3.util._

// Standard modular multiplication IO
// out.bits = (a * b) % mod
// out.valid signals when multiplication is done
//  * out.valid should be 1 for single-cycle multipliers
class ModMultIn(width: Int) extends Bundle {
	val a = UInt(width.W)
	val b = UInt(width.W)
	override def cloneType = (new ModMultIn(width)).asInstanceOf[this.type]
}

class ModMultIO(width: Int) extends Bundle {
	val in = Flipped(Decoupled(new ModMultIn(width)))
	val out = Valid(UInt(width.W))
	override def cloneType = (new ModMultIO(width)).asInstanceOf[this.type]
}

// Simple single-cycle multiplier using built-in operations
class ModMult(width: Int) extends Module {
	val io = IO(new ModMultIO(width))
	io.out.bits := 0.U
	when(io.in.valid) {
		io.out.bits := (io.in.bits.a * io.in.bits.b) & Fill(width, 1.U(1.W))
	}
	io.out.valid := 1.B
	io.in.ready := 1.B
}

class ShiftAddMultIO(width: Int) extends Bundle {
	val in = Flipped(Decoupled(new ModMultIn(width)))
	val out = Valid(UInt((2*width).W))
	override def cloneType = (new ShiftAddMultIO(width)).asInstanceOf[this.type]
}

class ShiftAddMult(width: Int) extends Module {
	val io = IO(new ShiftAddMultIO(width))

	val storeA = Reg(UInt((2*width).W))
	val storeB = Reg(UInt(width.W))
	val acc = RegInit(0.U((2*width).W))
	val calc = RegInit(0.B)

	when (io.in.fire) {
		storeA := Cat(Fill(width, 0.B), io.in.bits.a)
		storeB := io.in.bits.b
		acc := 0.U
		calc := 1.B
	} .elsewhen (calc) {
		when (storeB(0)) {
			acc := acc + storeA
		}
		storeA := storeA << 1
		storeB := storeB >> 1
		calc := (storeB =/= 0.U)
	}

	io.out.bits := acc
	io.out.valid := !calc
	io.in.ready := !calc
}

// Karatsuba multiplier (truncated)
class Karatsuba(width: Int) extends Module {
	val io = IO(new ModMultIO(width))
	
	val (a1, a0) = (io.in.bits.a(width-1, width/2), io.in.bits.a(width/2 - 1, 0))
  val (b1, b0) = (io.in.bits.b(width-1, width/2), io.in.bits.b(width/2 - 1, 0))

  val subMults = Seq(Module(new ShiftAddMult(width/2)),
										 Module(new ShiftAddMult(width/2)),
										 Module(new ShiftAddMult(width/2 + 1)))
  val subValid = subMults.map(_.io.out.valid).reduce(_ & _)
	subMults.foreach { m => 
		m.io.in.bits.a := 0.U
		m.io.in.bits.b := 0.U
		m.io.in.valid := 0.U 
	}

  val done = RegInit(1.B)

  when (done & io.in.fire) {
  	subMults.foreach(_.io.in.valid := 1.B)
  	val Seq(sub0a, sub1a, sub2a) = subMults.map(_.io.in.bits.a)
  	val Seq(sub0b, sub1b, sub2b) = subMults.map(_.io.in.bits.b)
  	sub0a := a1; sub0b := b1
  	sub1a := a0; sub1b := b0
  	sub2a := a1 +& a0; sub2b := b1 +& b0
  	done := 0.B
  } .otherwise { done := subValid }

  val subOut = subMults.map(_.io.out.bits)
  io.out.bits := (subOut(0) << width) +&
  							 ((subOut(2) - subOut(1) - subOut(0)) << (width/2)) +&
  							 (subOut(1))
  io.out.valid := subValid
  io.in.ready := subValid
  // subMults.zipWithIndex.foreach { case(m, i) =>
	// 	printf(p"Mult($i): ${m.io.out.valid}\n")
	// }
}