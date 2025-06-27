package decoders

import Date
import bitmage.ByteOrder
import bitmage.fromBytes

object KeyedArchiveDecoder : ByteWitchDecoder {

    override val name = "nskeyedarchive"

    override fun confidence(data: ByteArray, sourceOffset: Int): Pair<Double, ByteWitchResult?> {
        val bpConfidence = BPListParser.confidence(data, sourceOffset)
        return if (bpConfidence.first > 0.5) {
            val parsed = (bpConfidence.second ?: BPListParser.decode(data, sourceOffset)) as BPListObject
            val nsConfidence = if(isKeyedArchive(parsed)) 1.0 else 0.0
            Pair(nsConfidence, parsed)
        } else
            Pair(0.0, null)
    }

    override fun decode(data: ByteArray, sourceOffset: Int, inlineDisplay: Boolean): ByteWitchResult {
        val parsed = BPListParser.decode(data, sourceOffset) as BPDict
        return decode(parsed)
    }

    private val topKey = BPAsciiString("\$top")
    private val rootKey = BPAsciiString("root")
    private val objectsKey = BPAsciiString("\$objects")
    private val classKey = BPAsciiString("\$class")
    private val classNameKey = BPAsciiString("\$classname")

    fun isKeyedArchive(data: BPListObject): Boolean {
        val archiverKey = BPAsciiString("\$archiver")
        val expectedArchiver = BPAsciiString("NSKeyedArchiver")
        return data is BPDict && data.values.containsKey(archiverKey) && data.values[archiverKey] == expectedArchiver
    }

    fun decode(data: BPDict): BPListObject {
        // get offset of the root object in the $objects list
        val topDict = data.values[topKey]!! as BPDict

        // so, turns out the key for the top object is not ALWAYS "root" (but almost always?)
        val top = if (topDict.values.containsKey(rootKey))
            topDict.values[rootKey]!! as BPUid
        // empty archive case
        else if (topDict.values.isEmpty()) {
            return BPNull
        } else
            topDict.values.values.first { it is BPUid } as BPUid // this is about as good as we can do?

        val topIndex = Int.fromBytes(top.value, ByteOrder.BIG)
        val objects = data.values[objectsKey]!! as BPArray

        val rootObj = objects.values[topIndex]
        val resolved = optionallyResolveObjectReference(rootObj, objects)
        return transformSupportedClasses(resolved)
    }

    private fun optionallyResolveObjectReference(thing: BPListObject, objects: BPArray, currentlyResolving: List<Int> = emptyList()): BPListObject {
        return when (thing) {
            is BPUid -> {
                val id = Int.fromBytes(thing.value, ByteOrder.BIG)
                if(currentlyResolving.contains(id))
                    RecursiveBacklink(id, null)
                else
                    optionallyResolveObjectReference(objects.values[id], objects, currentlyResolving + id)
            }

            is BPArray -> BPArray(thing.values.map { optionallyResolveObjectReference(it, objects, currentlyResolving) }, null)
            is BPSet -> BPSet(thing.entries, thing.values.map { optionallyResolveObjectReference(it, objects, currentlyResolving) }, null)
            is BPDict -> {
                // nested keyed archives will be decoded separately
                if (isKeyedArchive(thing))
                    thing
                else
                    BPDict(thing.values.map {
                        Pair(
                            optionallyResolveObjectReference(it.key, objects, currentlyResolving),
                            optionallyResolveObjectReference(it.value, objects, currentlyResolving)
                        )
                    }.toMap(), null)
            }

            else -> thing
        }
    }

    private fun transformSupportedClasses(thing: BPListObject): BPListObject {
        // decode nested archives
        if (isKeyedArchive(thing))
            return decode(thing as BPDict)

        return when (thing) {
            is BPAsciiString -> {
                if(thing.value == "\$null")
                    BPNull
                else
                    thing
            }
            is BPArray -> {
                val transformedValues = thing.values.map { transformSupportedClasses(it) }
                BPArray(transformedValues, thing.sourceByteRange)
            }

            is BPSet -> {
                val transformedValues = thing.values.map { transformSupportedClasses(it) }
                BPSet(thing.entries, transformedValues, thing.sourceByteRange)
            }

            is BPDict -> {
                if (thing.values.containsKey(classKey)) {
                    val className = ((thing.values[classKey] as BPDict).values[classNameKey] as BPAsciiString).value
                    when (className) {
                        "NSDictionary", "NSMutableDictionary" -> {
                            val keyList = (thing.values[BPAsciiString("NS.keys")]!! as BPArray).values.map {
                                transformSupportedClasses(it)
                            }
                            val valueList = (thing.values[BPAsciiString("NS.objects")]!! as BPArray).values.map {
                                transformSupportedClasses(it)
                            }
                            val map = keyList.zip(valueList).toMap()
                            NSDict(map)
                        }

                        "NSMutableString", "NSString" -> {
                            val string = (thing.values[BPAsciiString("NS.string")]!! as BPAsciiString)
                            string
                        }

                        "NSMutableArray", "NSArray" -> {
                            val valueArray = thing.values[BPAsciiString("NS.objects")]!! as BPArray
                            val list = valueArray.values.map {
                                transformSupportedClasses(it)
                            }
                            NSArray(list, valueArray.sourceByteRange)
                        }

                        "NSMutableSet", "NSSet" -> {
                            val valueArray = thing.values[BPAsciiString("NS.objects")]!! as BPArray
                            val list = valueArray.values.map {
                                transformSupportedClasses(it)
                            }
                            NSSet(list.toSet(), valueArray.sourceByteRange)
                        }

                        "NSData", "NSMutableData" -> {
                            val value = thing.values[BPAsciiString("NS.data")]!!

                            // why do some NSData objects contain an NSDict with a nested keyed archive?
                            if(isKeyedArchive(value)) {
                                decode(value as BPDict)
                            }
                            else {
                                val bytes = (value as BPData)
                                NSData(bytes.value, bytes.sourceByteRange)
                            }
                        }

                        "NSDate" -> {
                            val timestamp = (thing.values[BPAsciiString("NS.time")]!! as BPReal)
                            // NSDates encode time as seconds from Jan 01 2001, we convert to standard unix time here
                            NSDate(Date((timestamp.value * 1000).toLong() + 978307200000), timestamp.sourceByteRange)
                        }

                        /*"NSURL" -> {
                            val base = thing.values[BPAsciiString("NS.base")]!!
                            val relative = thing.values[BPAsciiString("NS.relative")] as BPString

                            val url = if(base is BPString) base.value + relative.value else relative.value
                            NSURL(url, relative.sourceByteRange!!)
                        }*/

                        "NSUUID" -> {
                            val uuidData = thing.values[BPAsciiString("NS.uuidbytes")]!! as BPData
                            NSUUID(uuidData.value, uuidData.sourceByteRange)
                        }

                        else -> {
                            val entries = thing.values.map {
                                Pair(
                                    transformSupportedClasses(it.key),
                                    transformSupportedClasses(it.value)
                                )
                            }
                            BPDict(entries.associate { it }, thing.sourceByteRange)
                        }
                    }
                } else {
                    BPDict(thing.values.map {
                        Pair(
                            transformSupportedClasses(it.key),
                            transformSupportedClasses(it.value)
                        )
                    }.toMap(), thing.sourceByteRange)
                }
            }

            else -> thing
        }
    }
}