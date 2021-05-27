package mimc

import chisel3._
import chisel3.tester._
import org.scalatest.FreeSpec

import treadle._
import chisel3.tester.experimental.TestOptionBuilder._

import scala.util._

object MiMCConst {
	def genRoundConst(numRounds: Int): Seq[BigInt] = Seq.tabulate(numRounds)(BigInt(_))
}

class MiMCModelTester extends FreeSpec with ChiselScalatestTester {
	val key = BigInt("00010001", 16)
	val inp = BigInt("0001", 16)

	"MiMC model should single round function" in {
		val testKey = key & BigInt("ffff", 16)
		assert(MiMCModel.round(inp, testKey, BigInt(1), 16) == BigInt(27))
	}

	"MiMC model should generate hash after 5 rounds" in {
		assert(MiMCModel.hashGen(inp, key, MiMCConst.genRoundConst(5), 5, 16) == BigInt(27328))
	}

	// "MiMC model should generate hash after 5 rounds with non-power-of-2 mod" in {
	// 	assert(MiMCModel.hashGen(inp, key, MiMCConst.genRoundConst(5), 5, 16) == BigInt(8336))
	// }

	"MiMC model should generate hash after 10 rounds" in {
		assert(MiMCModel.hashGen(inp, key, MiMCConst.genRoundConst(10), 10, 16) == BigInt(1000))
	}
}

class MiMCTester extends FreeSpec with ChiselScalatestTester {

	// Tests functionality of MiMC with 2-cycle rounds (for single-cycle multipliers)
	def doMiMC_SCTest(inp: BigInt, key: BigInt, width: Int, numRounds: Int): Boolean = {
		val p = MiMCParams(width, numRounds)
		val roundConst = MiMCConst.genRoundConst(numRounds)
		test(new MiMCSingleCycle(p)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			// provide test inputs and start hash generation
			dut.io.in.valid.poke(true.B)
			dut.io.in.ready.expect(true.B)
			dut.io.in.bits.plaintext.poke(inp.U)
			dut.io.in.bits.key.poke(key.U)
			for (i <- 0 until numRounds) { dut.io.in.bits.constants(i).poke(roundConst(i).U) }
			dut.clock.step() // 1 cycle delay to load test inputs into generator
			dut.io.in.valid.poke(false.B)

			// full hash generation should complete in 2*numRounds cycles
			for (i <- 0 until numRounds) {
				dut.io.hash.valid.expect(false.B)
				dut.clock.step(2)
			}
			dut.io.hash.valid.expect(true.B)
			val expected = MiMCModel.hashGen(inp, key, roundConst, numRounds, width)
			dut.io.hash.bits.expect(expected.U)
		}
		true
	}

	// Tests functionality of MiMC with included Karatsuba multiplier (for multi-cycle multipliers)
	def doMiMC_KMTest(inp: BigInt, key: BigInt, width: Int, numRounds: Int): Boolean = {
		val p = MiMCParams(width, numRounds)
		val roundConst = MiMCConst.genRoundConst(numRounds)
		test(new MiMCMultiCycle(p)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			dut.io.in.ready.expect(true.B)

			dut.io.in.valid.poke(true.B)
			dut.io.in.bits.plaintext.poke(inp.U)
			dut.io.in.bits.key.poke(key.U)
			for (i <- 0 until numRounds) { dut.io.in.bits.constants(i).poke(roundConst(i).U) }
			dut.clock.step() // 1 cycle delay to load test inputs into generator
			dut.io.in.valid.poke(false.B)

			// Worst case calculation time:
			// A. 1 cycle loading multiplication arguments
			// B. width/2 + 2 cycles to calculate valid output (according to Karatsuba Test)
			// 2 multiplications per round over numRounds rounds = 2*numRounds*(A+B)
			dut.clock.step(2*numRounds*(width/2 + 4)) // this is worse than the worst case for sure

			println(s"hashGen done, hash = ${dut.io.hash.bits.peek}")
			dut.io.hash.valid.expect(true.B)
			val expected = MiMCModel.hashGen(inp, key, roundConst, numRounds, width)
			dut.io.hash.bits.expect(expected.U)
		}
		true
	}

	"MiMCSingleCycle should produce correct hash in 5 rounds with random input and key" in {
		val width = 32
		val inp = BigInt(width, new Random())
		val key = BigInt(2*width, new Random())
		doMiMC_SCTest(inp, key, width, 5)
	}
	"MiMCSingleCycle should produce correct hash in 10 rounds with random input and key" in {
		val width = 32
		val inp = BigInt(width, new Random())
		val key = BigInt(2*width, new Random())
		doMiMC_SCTest(inp, key, width, 10)
	}
	"MiMCMultiCycle should produce correct hash in 5 rounds with random input and key" in {
		val width = 32
		val inp = BigInt(width, new Random())
		val key = BigInt(2*width, new Random())
		doMiMC_KMTest(inp, key, width, 5)
	}
	"MiMCMultiCycle should produce correct hash in 10 rounds with random input and key" in {
		val width = 32
		val inp = BigInt(width, new Random())
		val key = BigInt(2*width, new Random())
		doMiMC_KMTest(inp, key, width, 10)	}
}