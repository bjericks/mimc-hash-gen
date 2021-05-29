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
				// load in the test inputs
				dut.io.in.bits.a.poke(rA.U)
				dut.io.in.bits.b.poke(rB.U)
				dut.io.in.valid.poke(1.B)
				dut.clock.step()
				dut.io.in.valid.poke(0.B)
				for(i <- 0 until (width/2 + 2)) { // worst case calculation time
					dut.clock.step()
				}
				val expected = rA * rB & trunc
				dut.io.out.bits.expect(expected.U)
				dut.io.out.valid.expect(1.B)
				dut.clock.step()
			}
		}
	}

	def doRandomKaratsubaSingleCycleTest(width: Int, recurse: Int, numTests: Int) {
		test(new KaratsubaSingleCycle(width, recurse)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			val trunc = (BigInt(1) << width) - BigInt(1)
			for (i <- 0 until numTests) {
				val rA = BigInt(width, new Random())
				val rB = BigInt(width, new Random())
				println(s"Testing $rA * $rB")
				// load in the test inputs
				dut.io.in.bits.a.poke(rA.U)
				dut.io.in.bits.b.poke(rB.U)
				dut.io.in.valid.poke(1.B)

				val expected = rA * rB & trunc
				dut.io.out.bits.expect(expected.U)
				dut.io.out.valid.expect(1.B)
				dut.clock.step()
			}
		}
	}

	"Karatsuba should multiply random 8-bit integers" in {
		doRandomKaratsubaTest(8, 10)
	}

	"Karatsuba should multiply random 16-bit integers" in {
		doRandomKaratsubaTest(16, 10)
	}

	"Karatsuba should multiply random 256-bit integers" in {
		doRandomKaratsubaTest(256, 10)
	}

	"Recurse 0 KaratsubaSingleCycle should multiply random 8-bit integers" in {
		doRandomKaratsubaSingleCycleTest(8, 0, 10)
	}

	"Recurse 1 KaratsubaSingleCycle should multiply random 8-bit integers" in {
		doRandomKaratsubaSingleCycleTest(8, 1, 10)
	}

	"Recurse 2 KaratsubaSingleCycle should multiply random 16-bit integers" in {
		doRandomKaratsubaSingleCycleTest(16, 2, 10)
	}

	"Recurse 2 KaratsubaSingleCycle should multiply random 256-bit integers" in {
		doRandomKaratsubaSingleCycleTest(256, 2, 10)
	}

	"Recurse 2 KaratsubaSingleCycle should multiply random 512-bit integers" in {
		doRandomKaratsubaSingleCycleTest(512, 2, 10)
	}
}