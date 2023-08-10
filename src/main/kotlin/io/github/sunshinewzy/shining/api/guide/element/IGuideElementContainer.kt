package io.github.sunshinewzy.shining.api.guide.element

import io.github.sunshinewzy.shining.api.guide.ElementCondition
import io.github.sunshinewzy.shining.api.namespace.NamespacedId
import io.github.sunshinewzy.shining.core.guide.team.GuideTeam

interface IGuideElementContainer : IGuideElement {

    fun registerElement(element: IGuideElement)
    
    fun unregisterElement(id: NamespacedId)
    
    fun unregisterElement(element: IGuideElement) {
        unregisterElement(element.getId())
    }
    
    fun getElement(id: NamespacedId, isDeep: Boolean = false): IGuideElement?
    
    fun getElements(isDeep: Boolean = false): List<IGuideElement>
    
    suspend fun getElementsByCondition(team: GuideTeam, condition: ElementCondition, isDeep: Boolean = false): List<IGuideElement>

    fun updateElementId(element: IGuideElement, oldId: NamespacedId) {}

}