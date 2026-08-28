package com.ndds.homelauncher


object PlayStoreMetaDataExtractor {
    fun getMetaData(
        text: String,
        dataStoreIndex: Int,
        vararg addressGroups: IntArray
    ): Array<String?>? {
        val startIndex = text.indexOf("class=\"ds:" + dataStoreIndex + "\"")
        if (startIndex == -1) return null

        var addressIndex: Int
        var itemIndex: Int
        var notationBalance: Int
        val metaData = arrayOfNulls<String>(addressGroups.size)
        var isInQuotes: Boolean
        var shouldCollectText: Boolean
        for (a in addressGroups.indices) {
            addressIndex = 0
            itemIndex = 0
            notationBalance = -1
            isInQuotes = false
            shouldCollectText = false
            val address: IntArray = addressGroups[a]
            for (i in startIndex..<text.length) {
                val character = text.get(i).code
                if (!isInQuotes && character == 91) { // open bracket character
                    if (notationBalance == addressIndex && address[addressIndex] == itemIndex) {
                        itemIndex = 0
                        addressIndex++
                    }
                    if (addressIndex == address.size) break
                    notationBalance++
                } else if (!isInQuotes && character == 93) { // close bracket character
                    if (notationBalance == addressIndex) break
                    notationBalance--
                } else if (!isInQuotes && character == 44 // comma character
                    && notationBalance == addressIndex
                ) {
                    itemIndex++
                } else if (character == 34) { // quotation character
                    isInQuotes = !isInQuotes
                    if (notationBalance == addressIndex && address[addressIndex] == itemIndex) {
                        if (isInQuotes) {
                            shouldCollectText = true
                            metaData[a] = ""
                        } else break
                    }
                } else if (shouldCollectText) {
                    metaData[a] += character.toChar()
                }
            }
        }

        return metaData
    }
}