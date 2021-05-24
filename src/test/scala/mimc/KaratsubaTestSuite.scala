package mimc

import chisel3._
import chisel3.tester._
import org.scalatest.FreeSpec

import treadle._
import chisel3.tester.experimental.TestOptionBuilder._

import scala.util._

class KaratsubaTester extends FreeSpec with ChiselScalatestTester {
	def doRandomKaratsubaTest(width: Int, numTests: Int) = {
		test(new Karatsuba(width)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			val trunc = (BigInt(1) << width) - BigInt(1)
			for (i <- 0 until numTests) {
				val rA = BigInt(width, new Random())
				val rB = BigInt(width, new Random())
				println(s"Testing $rA * $rB")
				dut.io.in.bits.a.poke(rA.U)
				dut.io.in.bits.b.poke(rB.U)
				dut.io.in.valid.poke(1.B)
				dut.clock.step()
				dut.io.in.valid.poke(0.B)
				for(i <- 0 until (width/2 + 2)) {
					dut.clock.step()
				}
				val expected = rA * rB & trunc
				dut.io.out.bits.expect(expected.U)
				dut.io.out.valid.expect(1.B)
				dut.clock.step()
			}
		}
	}

	// "Karatsuba should multiply selected 8-bit integers" in {
	// 	test(new Karatsuba(8)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
	// 		dut.io.in.bits.a.poke(12.U)
	// 		dut.io.in.bits.b.poke(11.U)
	// 		dut.io.out.bits.expect(132.U)
	// 		dut.io.out.valid.expect(1.B)
	// 	}
	// }

	// "Karatsuba should multiply selected 16-bit integers" in {
	// 	test(new Karatsuba(8)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
	// 		dut.io.in.bits.a.poke(32767.U)
	// 		dut.io.in.bits.b.poke(2.U)
	// 		dut.io.out.bits.expect(65534.U)
	// 		dut.io.out.valid.expect(1.B)
	// 	}
	// }

	"Karatsuba should multiply random 8-bit integers" in {
		doRandomKaratsubaTest(8, 10)
	}

	"Karatsuba should multiply random 16-bit integers" in {
		doRandomKaratsubaTest(16, 10)
	}

	"Karatsuba should multiply random 256-bit integers" in {
		doRandomKaratsubaTest(256, 10)
	}
}