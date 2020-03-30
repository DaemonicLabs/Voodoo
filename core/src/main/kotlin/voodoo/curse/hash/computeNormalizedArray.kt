package voodoo.curse.hash

// Credit to https://github.com/modmuss50/CAV2/blob/master/murmur.go
fun computeNormalizedArray(input: ByteArray): ByteArray {
	val output = ByteArray(input.size)
	var index = 0
	for (b in input) {
		when (b) {
			9.toByte(), 10.toByte(), 13.toByte(), 32.toByte() -> {}
			else -> {
				output[index] = b
				index++
			}
		}
	}
	val outputTrimmed = ByteArray(index)
	System.arraycopy(output, 0, outputTrimmed, 0, index)
	return outputTrimmed
}