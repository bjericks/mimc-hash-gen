MiMC Hash Generator
=======================

This project is a Chisel generator that implements the [MiMC hash function](https://byt3bit.github.io/primesym/mimc/)
with parameterized IO bit-width and number of rounds enacted on the input to produce the output hash. A simple modular
multiplier performs the round function, but you may replace the multiplier with another multiplier module to further
optimize performance.

To test the functionality of the MiMC hash generator and Scala model, simply run the included tests with
```sh
sbt test
```

## Project Components

### MiMC Model

A Scala implementation of the MiMC hash function was created to compare with the Chisel implementation for testing. The MiMC model is loosely based on an existing [Python implementation](https://wordpress-434650-1388715.cloudwaysapps.com/developers-community/hash-challenge/hash-challenge-implementation-reference-code/#marvellous) of the MiMC hash function.

### Modular Multiplier

A standard IO is implemented in the modular multiplier to facilitate interfacing with the hash generator:
* `(a, b): Flipped(Decoupled(UInt))`: The arguments for multiplication.
* `out: Valid(UInt)`: The result of modular multiplication (a * b % m). The modulus is set within a parameter of the module.
The "valid" output signals when the operation is complete to facilitate multi-cycle multipliers.

There are three multiplier modules provided in this project:
* A single-cycle multiplier that uses Chisel's built-in multiplication operation.
* A single-cycle Karatsuba multiplier that completes multiplication with fewer partial product calculations.
* A multi-cycle Karatsuba multiplier that divides partial product calculations between three simple shift-and-add multipliers in parallel.

For multi-cycle multipliers, a multiplication should proceed as follows:
1. The multiplier inputs (`io.in.bits.a` and `io.in.bits.b`) are set by the user.
2. The user pulses the input's "valid" signal (`io.in.valid`) to begin a multiplication.
3. The multiplier signals it has completed a multiplication with the output's "valid" signal (`io.out.valid`). The multiplier is now ready to perform another calculation.

### MiMC Hash Generator

The core of the MiMC Hash Generator is the APN function 
```
f(x, y) = x^3 mod y
```
where `y` is either a power of 2 or an arbitrary prime integer. In this implementation, `y` is set at 2^n for n-bit plaintexts. This function is used to create the round function
```
R(x, y, k, c) = f(x + k + c, y)
```
where `x` is the plaintext to be encrypted, `k` is an element of the key `(K1, K0)` used during encryption, and `c` is a round constant associated with the current round. The hash function consists of a sequence of round function calls, each with the result of the previous round as the plaintext.

The MiMC Hash Generator includes the following IO:
* `plaintext: Input(UInt)`: The n-bit plaintext to be encrypted
* `key: Input(UInt)`: The 2n-bit key used during encryption. The hash generator evenly divides the key into two n-bit subkeys (K1, K0) and alternates the subkey used with each round function call.
* `constants: Input(Vec(UInt))`: The round constants used during encryption. The number of elements required equals the number of rounds in the hash generation, and the first round constant (C0) must equal 0.
* `hash: Output(UInt)`: The n-bit result of encryption.

The MiMC Hash Generator uses a single multiplier to calculate one half of a round function at a time. This implementation allows for the integration of both single-cycle and multi-cycle multipliers via the `MiMCSingleCycle` and `MiMCMultiCycle` submodules.

Currently, the following characteristics are parameterizable:
* The bit-width of the hash generator input and output
* The number of rounds in the hash generation