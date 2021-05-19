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
		assert(MiMCModel.round(inp, testKey, BigInt(1), 65536) == BigInt(27))
	}

	"MiMC model should generate hash after 5 rounds" in {
		assert(MiMCModel.hashGen(inp, key, MiMCConst.genRoundConst(5), 5, 65536, 16) == BigInt(27328))
	}

	"MiMC model should generate hash after 5 rounds with non-power-of-2 mod" in {
		assert(MiMCModel.hashGen(inp, key, MiMCConst.genRoundConst(5), 5, 10000, 16) == BigInt(8336))
	}

	"MiMC model should generate hash after 10 rounds" in {
		assert(MiMCModel.hashGen(inp, key, MiMCConst.genRoundConst(10), 10, 65536, 16) == BigInt(1000))
	}
}

class MiMCTester extends FreeSpec with ChiselScalatestTester {

	// Tests functionality of MiMC with 2-cycle rounds
	def doMiMCTest(inp: BigInt, key: BigInt, mod: Int, width: Int, numRounds: Int) {
		val p = MiMCParams(mod, width, numRounds)
		val roundConst = MiMCConst.genRoundConst(numRounds)
		test(new MiMC(p)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			// provide test inputs and start hash generation
			dut.io.in.valid.poke(true.B)
			dut.io.in.ready.expect(true.B)
			dut.io.in.bits.plaintext.poke(inp.U)
			dut.io.in.bits.key.poke(key.U)
			for (i <- 0 until numRounds) { dut.io.in.bits.constants(i).poke(roundConst(i).U) }
			dut.clock.step()

			for (i <- 0 until numRounds) {
				dut.io.hash.valid.expect(false.B)
				dut.clock.step(2)
			}
			dut.io.hash.valid.expect(true.B)
			val expected = MiMCModel.hashGen(inp, key, roundConst, numRounds, mod, width)
			dut.io.hash.bits.expect(expected.U)
		}
	}

	"MiMC should produce correct hash in 5 rounds with random input and key" in {
		val width = 16
		val mod = 65536
		val inp = BigInt(width, new Random())
		val key = BigInt(2*width, new Random())
		doMiMCTest(inp, key, mod, width, 5)
	}
}