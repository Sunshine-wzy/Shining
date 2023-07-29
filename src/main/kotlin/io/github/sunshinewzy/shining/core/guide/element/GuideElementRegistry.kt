package io.github.sunshinewzy.shining.core.guide.element

import io.github.sunshinewzy.shining.Shining
import io.github.sunshinewzy.shining.api.guide.element.IGuideElement
import io.github.sunshinewzy.shining.api.guide.state.IGuideElementState
import io.github.sunshinewzy.shining.api.namespace.NamespacedId
import io.github.sunshinewzy.shining.core.data.database.column.jackson
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.concurrent.ConcurrentHashMap

object GuideElementRegistry : LongIdTable() {
    
    val key = text("key").uniqueIndex()
    val element = jackson("element", Shining.objectMapper, IGuideElementState::class.java)
    
    private val stateCache: MutableMap<NamespacedId, IGuideElementState> = ConcurrentHashMap()
    private val elementCache: MutableMap<NamespacedId, IGuideElement> = ConcurrentHashMap()
    
    
    fun <T: IGuideElement> register(element: T): T {
        val id = element.getId()
        stateCache[id]?.let { state ->
            element.update(state)
        }
        elementCache[id] = element
        return element
    }
    
    suspend fun init() {
        newSuspendedTransaction { 
            GuideElementRegistry
                .slice(GuideElementRegistry.element)
                .selectAll()
                .forEach { 
                    val state = it[GuideElementRegistry.element]
                    state.id?.let { id ->
                        stateCache[id] = state
                    }
                }
        }
    }
    
    fun getState(id: NamespacedId): IGuideElementState? = stateCache[id]
    
    fun getElement(id: NamespacedId): IGuideElement? = elementCache[id]
    
    @Suppress("UNCHECKED_CAST")
    fun <T: IGuideElement> getElementByType(id: NamespacedId, type: Class<T>): T? {
        val theElement = getElement(id) ?: return null
        if (type.isInstance(theElement)) return theElement as T
        return null
    }
    
    inline fun <reified T: IGuideElement> getElementByType(id: NamespacedId): T? {
        val theElement = getElement(id) ?: return null
        if (theElement is T) return theElement
        return null
    }
    
    fun getElementOrDefault(id: NamespacedId, default: IGuideElement): IGuideElement =
        getElement(id) ?: default
    
    fun getElementOrDefault(default: IGuideElement): IGuideElement =
        getElementOrDefault(default.getId(), default)
    
    suspend fun saveElement(element: IGuideElement, isCheckExists: Boolean = false, checkId: NamespacedId = element.getId(), actionBeforeInsert: () -> Boolean = { true }): Boolean {
        val existsCache = elementCache.containsKey(checkId)
        if (isCheckExists && existsCache)
            return false
        
        return newSuspendedTransaction transaction@{
            val existsSQL = containsElement(checkId)
            if (isCheckExists && existsSQL)
                return@transaction false

            if (!actionBeforeInsert()) return@transaction false
            
            if (existsSQL) {
                updateElement(element)
            } else {
                insertElement(element)
            }

            if (!existsCache) {
                elementCache[checkId] = element
            }
            true
        }
    }
    
    
    private fun insertElement(element: IGuideElement): EntityID<Long> =
        insertAndGetId {
            it[GuideElementRegistry.key] = element.getId().toString()
            it[GuideElementRegistry.element] = element.getState()
        }
    
    private fun updateElement(element: IGuideElement): Int =
        update({ GuideElementRegistry.key eq element.getId().toString() }) { 
            it[GuideElementRegistry.element] = element.getState()
        }
    
    private fun deleteElement(id: NamespacedId): Int =
        deleteWhere { 
            GuideElementRegistry.key eq id.toString()
        }

    private fun deleteElement(element: IGuideElement): Int =
        deleteElement(element.getId())
    
    private fun readState(id: NamespacedId): IGuideElementState? =
        GuideElementRegistry
            .slice(GuideElementRegistry.element)
            .select { GuideElementRegistry.key eq id.toString() }
            .firstNotNullOfOrNull {
                it[GuideElementRegistry.element]
            }
    
    private fun readElement(id: NamespacedId): IGuideElement? =
        readState(id)?.toElement()
    
    private fun containsElement(id: NamespacedId): Boolean =
        GuideElementRegistry
            .slice(GuideElementRegistry.key)
            .select { GuideElementRegistry.key eq id.toString() }
            .firstOrNull() != null
    
    private fun containsElement(element: IGuideElement): Boolean =
        containsElement(element.getId())
    
}