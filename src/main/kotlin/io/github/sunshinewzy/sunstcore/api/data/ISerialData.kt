package io.github.sunshinewzy.sunstcore.api.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import io.github.sunshinewzy.sunstcore.core.data.SerialData

interface ISerialData : IData {
    @get:JsonIgnore
    override val root: ISerialDataRoot
    @get:JsonIgnore
    override val parent: ISerialData?


    override fun getData(path: String): ISerialData?

    override fun createData(path: String): ISerialData

    @JsonValue
    fun serializeToJsonNode(): JsonNode
    
    fun serializeToString(): String

    fun deserialize(source: JsonNode): Boolean
    
    fun deserialize(source: String): Boolean
    
    
    companion object {
        
        @JvmStatic
        fun deserialize(source: JsonNode, name: String, parent: ISerialData): ISerialData {
            return SerialData(name, parent).also {
                if(!it.deserialize(source)) {
                    throw RuntimeException("Deserialization failed.")
                }
            }
        }

        @JvmStatic
        fun deserialize(source: JsonNode, name: String, root: ISerialDataRoot, parent: ISerialData? = null): ISerialData {
            return SerialData(name, root, parent).also {
                if(!it.deserialize(source)) {
                    throw RuntimeException("Deserialization failed.")
                }
            }
        }
        
    }
}