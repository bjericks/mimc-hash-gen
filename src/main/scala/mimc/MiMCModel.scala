package mimc

// Inputs:
// x: BigInt - plaintext acted on by the hash generator
// k: BigInt - key (K1, K0) added in hash function
// c: Seq(BigInt) - round constants added in hash function
//
// numRounds: Int - number of times the round function is called (also the length of c)
// mod: Int - modulus of the round function's multiplication
// width: Int - the bitwidth of x (x => width bits, k => 2*width bits)

object MiMCModel {

	def splitKey(k: BigInt, width: Int): (BigInt, BigInt) = {
		val splitMASK = (BigInt(1) << width) - BigInt(1)
		(k & splitMASK, (k >> width) & splitMASK)
	}

	def round(x: BigInt, k: BigInt, c: BigInt, mod: Int): BigInt = {
		val sum = x + k + c
		(((sum * sum) % mod) * sum) % mod 
	}

	def hashGen(x: BigInt, k: BigInt, c: Seq[BigInt], numRounds: Int, mod: Int, width: Int): BigInt = {
		assert(c.length == numRounds)
		val (lower, upper) = splitKey(k, width)
		println(f"key: upper = $upper%x, lower = $lower%x")
		
		// var usage is very suspicious
		var hash = x
		for (r <- 0 until numRounds) {
			val currKey = if (r % 2 == 0) lower else upper
			hash = round(hash, currKey, c(r), mod)
			println(f"Round ${r+1}%d: $hash%x ($hash%d)")
		}
		hash
	}
}