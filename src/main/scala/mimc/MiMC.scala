package mimc

import chisel3._
import chisel3.util._

case class MiMCParams(mod: Int, width: Int, numRounds: Int=10) {
	val modWidth = log2Ceil(mod)
}

/* MiMCPacket: container for MiMC input data */
class MiMCPacket(p: MiMCParams) extends Bundle {
	val plaintext = UInt(p.width.W)
	val key       = UInt((2*p.width).W)
	val constants = Vec(p.numRounds, UInt(p.width.W))
	override def cloneType = (new MiMCPacket(p)).asInstanceOf[this.type]
}

/* MiMCIO: Decoupled MiMC input and output */
class MiMCIO(p: MiMCParams) extends Bundle {
	val in = Flipped(Decoupled(new MiMCPacket(p)))
	val hash = Valid(UInt(p.width.W))
	override def cloneType = (new MiMCIO(p)).asInstanceOf[this.type]
}

/* MiMC FSM states */
object MiMC {
	val idle :: mul1 :: mul2 :: Nil = Enum(3)
	// idle: hash generator is awaiting valid input
	// mul1: first multiplication in a round [(sum * sum) % mod]
	// mul2: second multiplication in a round [(mul1Result * sum) % mod]
}

class MiMC(p: MiMCParams) extends Module {
	val io = IO(new MiMCIO(p))

	// Input storage registers
	val xReg = RegInit(0.U(p.width.W))
	val kReg = RegInit(0.U((2*p.width).W))
	val cReg = Reg(Vec(p.numRounds, UInt(p.width.W)))
	// For large numRounds, loading to cReg can be hardware intensive
	
	// Modulo multiplier - Can replace with your own module!
	val mul = Module(new ModMult(p.width, p.mod))

	// Modulo multiplier IO
	val outBits  = mul.io.out.bits
	val outValid = mul.io.out.valid
	mul.io.a := 0.U
	mul.io.b := 0.U

	// Holds intermediate result of round function multiplication
	val mul1Result = RegInit(0.U(p.width.W))

	// Current FSM state
	val state = RegInit(MiMC.idle)

	// Current round (counts from 0 until numRounds)
	val roundCount = RegInit(0.U(log2Ceil(p.numRounds).W))

	def getSum: UInt = {
		val x = xReg
		val k = Mux(roundCount(0), kReg(2*p.width-1, p.width), kReg(p.width-1, 0))
		val c = cReg(roundCount)
		(x + k + c)
	}

	when (state === MiMC.idle) {
		when (io.in.fire) {
			xReg := io.in.bits.plaintext
			kReg := io.in.bits.key
			cReg := io.in.bits.constants
			state := MiMC.mul1
		}
	} .elsewhen (state === MiMC.mul1) {
		mul.io.a := getSum
		mul.io.b := getSum

		when (outValid) {
			mul1Result := outBits 
			state := MiMC.mul2 
		}
	} .elsewhen (state === MiMC.mul2) {
		mul.io.a := mul1Result
		mul.io.b := getSum

		when (outValid) {
			xReg := outBits

			val last = roundCount === (p.numRounds-1).U
			roundCount := Mux(last, 0.U, roundCount + 1.U)
			state := Mux(last, MiMC.idle, MiMC.mul1)
		}
	}

	// Output and Decoupled logic
	io.hash.bits := xReg
	io.hash.valid := (state === MiMC.idle)
	io.in.ready := (state === MiMC.idle)
}