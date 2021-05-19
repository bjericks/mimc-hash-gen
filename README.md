MiMC Hash Generator
=======================

This project is a Chisel generator that implements the [MiMC hash function](https://byt3bit.github.io/primesym/mimc/)
with parameterized IO bit-width and number of rounds enacted on the input to produce the output hash. A simple modular
multiplier performs the round function, but you may replace the multiplier with another multiplier module to further
optimize performance.

To test the functionality of the MiMC hash generator, simply run the included tests with
```sh
sbt test
```

## Project Components

### MiMC Model

A Scala implementation of the MiMC hash function was created to compare with the Chisel implementation for testing. The MiMC model is loosely based on an existing [Python implementation](https://wordpress-434650-1388715.cloudwaysapps.com/developers-community/hash-challenge/hash-challenge-implementation-reference-code/#marvellous) of the MiMC hash function.

### Modular Multiplier

A standard IO is implemented in the modular multiplier to facilitate interfacing with the hash generator:
* (a, b): Input(UInt): The arguments for multiplication
* out: Valid(UInt): The result of modular multiplication (a * b % m). The modulus is set within a parameter of the module.
The "valid" output signals when the operation is complete to facilitate multi-cycle multipliers.

The current implementation uses Chisel operators to calculate the result. Future implementations may apply more complex multiplication algorithms to improve performace.

### MiMC Hash Generator

The core of the MiMC Hash Generator is the APN function 
```
f(x, y) = x^3 mod y
```
where y is either a power of 2 or an arbitrary prime integer. This function is used to create the round function
```
R(x, y, k, c) = f(x + k + c, y)
```
where x is the plaintext to be encrypted, k is an element of the key (K1, K0) used during encryption, and c is a round constant associated with the current round. The hash function consists of a sequence of round function calls, each with the result of the previous round as the plaintext.

The MiMC Hash Generator includes the following IO:
* plaintext: Input(UInt): The n-bit plaintext to be encrypted
* key: Input(UInt): The 2n-bit key used during encryption. The round function
* constants: Input(Vec(UInt)): The round constants used during encryption. The number of elements required equals the number of rounds in the hash generation.
* hash: Output(UInt): The n-bit result of encryption.

The hash generator currently uses a single multiplier module to carry out a single round function calculation over two clock cycles. To optimize the speed of hash generation, a second multiplier may be added to perform single-cycle round function calculation.

Currently, the following characteristics are parameterizable:
* The modulus applied in the core APN function
* The bit-width of the hash generator input and output
* The number of rounds in the hash generation

## Known Issues

The MiMC Hash Generator currently works correctly for any modulus that is a power of 2. When selecting a prime modulus, the hash generator's output may differ from the model output.