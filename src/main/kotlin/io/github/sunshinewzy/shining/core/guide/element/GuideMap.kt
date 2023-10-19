package io.github.sunshinewzy.shining.core.guide.element

import io.github.sunshinewzy.shining.api.guide.ElementCondition
import io.github.sunshinewzy.shining.api.guide.ElementCondition.*
import io.github.sunshinewzy.shining.api.guide.ElementDescription
import io.github.sunshinewzy.shining.api.guide.context.GuideContext
import io.github.sunshinewzy.shining.api.guide.element.IGuideElement
import io.github.sunshinewzy.shining.api.guide.element.IGuideElementContainer
import io.github.sunshinewzy.shining.api.guide.state.IGuideElementState
import io.github.sunshinewzy.shining.api.guide.team.CompletedGuideTeam
import io.github.sunshinewzy.shining.api.guide.team.IGuideTeam
import io.github.sunshinewzy.shining.api.namespace.NamespacedId
import io.github.sunshinewzy.shining.api.objects.coordinate.Coordinate2D
import io.github.sunshinewzy.shining.api.objects.coordinate.Rectangle
import io.github.sunshinewzy.shining.core.guide.ShiningGuide
import io.github.sunshinewzy.shining.core.guide.ShiningGuideEditor
import io.github.sunshinewzy.shining.core.guide.ShiningGuideEditor.setEditor
import io.github.sunshinewzy.shining.core.guide.context.*
import io.github.sunshinewzy.shining.core.guide.settings.ShiningGuideSettings
import io.github.sunshinewzy.shining.core.guide.state.GuideMapState
import io.github.sunshinewzy.shining.core.lang.getLangText
import io.github.sunshinewzy.shining.core.menu.MapMenu
import io.github.sunshinewzy.shining.core.menu.onBuildEdge
import io.github.sunshinewzy.shining.objects.ShiningDispatchers
import io.github.sunshinewzy.shining.objects.item.ShiningIcon
import io.github.sunshinewzy.shining.utils.orderWith
import kotlinx.coroutines.runBlocking
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.function.submit
import taboolib.module.ui.openMenu

class GuideMap : GuideElement, IGuideElementContainerSuspend {

    private var basePoint: Coordinate2D
    private val elements: MutableMap<Coordinate2D, IGuideElement> = HashMap()
    private val idToCoordinate: MutableMap<NamespacedId, Coordinate2D> = HashMap()
    private val removedElements: MutableSet<NamespacedId> = HashSet()
    
    
    constructor(
        id: NamespacedId,
        description: ElementDescription,
        item: ItemStack,
        basePoint: Coordinate2D = Coordinate2D(3, 3)
    ) : super(id, description, item) {
        this.basePoint = basePoint
    }
    
    constructor() : super() {
        this.basePoint = Coordinate2D(3, 3)
    }


    override fun openMenu(player: Player, team: IGuideTeam, context: GuideContext) {
        ShiningDispatchers.launchDB { 
            val isCompleted = isTeamCompleted(team)
            val remainingTime = if (getRepeatableSettings().hasRepeatablePeriod()) getRepeatablePeriodRemainingTime(team) else 0

            submit {
                player.openMenu<MapMenu<IGuideElement>>(player.getLangText(ShiningGuide.TITLE)) {
                    rows(6)
                    area(Rectangle(2, 2, 8, 5))

                    base(basePoint)
                    elements { elements }
                    
                    context[OffsetContext]?.let { 
                        offset(it.offset)
                    }

                    val dependencyLockedElements = HashSet<IGuideElement>()
                    val lockLockedElements = HashSet<IGuideElement>()
                    val repeatableElements = HashMap<IGuideElement, Long>()
                    onGenerate(true) { player, elementFuture, _, _ ->
                        val element = elementFuture as IGuideElementSuspend
                        runBlocking(ShiningDispatchers.DB) {
                            if (context[GuideEditModeContext]?.mode == true || team == CompletedGuideTeam.getInstance()) {
                                return@runBlocking element.getUnlockedSymbol(player)
                            }

                            val condition = element.getCondition(team)
                            when (condition) {
                                LOCKED_DEPENDENCY -> dependencyLockedElements += element
                                LOCKED_LOCK -> lockLockedElements += element
                                REPEATABLE -> repeatableElements[element] = getRepeatablePeriodRemainingTime(team)
                                else -> {}
                            }
                            element.getSymbolByCondition(player, team, condition)
                        }
                    }

                    onBuildEdge(edgeOrders)

                    setMoveRight(9 orderWith 4) { ShiningIcon.MOVE_RIGHT.toLocalizedItem(player) }
                    setMoveLeft(1 orderWith 4) { ShiningIcon.MOVE_LEFT.toLocalizedItem(player) }
                    setMoveUp(2 orderWith 6) { ShiningIcon.MOVE_UP.toLocalizedItem(player) }
                    setMoveDown(8 orderWith 6) { ShiningIcon.MOVE_DOWN.toLocalizedItem(player) }
                    setMoveToOrigin(8 orderWith 1) { ShiningIcon.MOVE_TO_ORIGIN.toLocalizedItem(player) }
                    
                    onClick { event, element, coordinate ->
                        if (context[GuideEditModeContext]?.isEditorEnabled() == true) {
                            ShiningGuideEditor.openEditor(
                                player, team, GuideEditorContext.Back {
                                    openMenu(player, team, context + OffsetContext(offset))
                                } + ShiningGuideEditor.CreateContext {
                                    registerElement(it, coordinate)
                                }, element, this@GuideMap
                            )
                            return@onClick
                        }

                        // Select elements
                        context[GuideSelectElementsContext]?.let { ctxt ->
                            if (ctxt.mode) {
                                if (ctxt.elements.contains(element)) {
                                    ctxt.elements.remove(element)
                                } else if (ctxt.filter(element)) {
                                    ctxt.elements.add(element)
                                }

                                // Update shortcut bar
                                ShiningDispatchers.launchDB {
                                    val list = ctxt.elements.map {
                                        (it as IGuideElementSuspend).getUnlockedSymbol(player)
                                    }

                                    submit {
                                        context[GuideShortcutBarContext]?.setItems(list)
                                        openMenu(player, team, context + OffsetContext(offset))
                                    }
                                }
                                return@onClick
                            }
                        }

                        if (element in dependencyLockedElements) return@onClick

                        if (element in lockLockedElements) {
                            if (element.unlock(player, team)) {
                                ShiningGuide.fireworkCongratulate(player)
                                open(player, team, null, context + OffsetContext(offset))
                            }
                            return@onClick
                        }

                        repeatableElements[element]?.let { remainingTime ->
                            if (remainingTime > 0) return@onClick
                        }

                        element.open(event.clicker, team, this@GuideMap, context)
                    }

                    if (context[GuideEditModeContext]?.isEditorEnabled() == true) {
                        onClickEmpty { _, coordinate ->
                            ShiningGuideEditor.openEditor(
                                player, team, GuideEditorContext.Back {
                                    openMenu(player, team, context + OffsetContext(offset))
                                } + ShiningGuideEditor.CreateContext {
                                    registerElement(it, coordinate)
                                },null, this@GuideMap
                            )
                        }
                    } else {
                        if (canComplete(isCompleted, remainingTime)) {
                            if (getRewards().isNotEmpty()) {
                                set(5 orderWith 6, ShiningIcon.VIEW_REWARDS.toLocalizedItem(player)) {
                                    openViewRewardsMenu(player, team, context)
                                }
                            }
                        } else {
                            set(5 orderWith 6, ShiningIcon.VIEW_REWARDS_AND_SUBMIT.toLocalizedItem(player)) {
                                openViewRewardsMenu(player, team, context)
                                ShiningDispatchers.launchDB {
                                    val checkCompleted = checkChildElementsCompleted(team)
                                    submit {
                                        if (checkCompleted) {
                                            complete(player, team)
                                        } else {
                                            fail(player)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    setEditor(player, context) {
                        openMenu(player, team, context + OffsetContext(offset))
                    }

                    setBackButton(player, team, context)

                    set(5 orderWith 1, ShiningIcon.SETTINGS.getLanguageItem().toLocalizedItem(player)) {
                        ShiningGuideSettings.openSettingsMenu(player, team)
                    }

                    // Select elements
                    context[GuideSelectElementsContext]?.let { ctxt ->
                        set(4 orderWith 1, ctxt.getSelectorItem(player)) {
                            if (clickEvent().isShiftClick) {
                                ctxt.submit()
                            } else {
                                ctxt.switchMode()
                                openMenu(player, team, context + OffsetContext(offset))
                            }
                        }
                    }

                    // Shortcut bar
                    context[GuideShortcutBarContext]?.update(this)
                    
                    onClose(once = false) {
                        ShiningGuide.recordElementAdditionalContext(player, this@GuideMap, OffsetContext(offset))
                    }
                }
            }
        }
    }

    override fun back(player: Player, team: IGuideTeam, context: GuideContext) {
        super.back(player, team, context.minusKey(OffsetContext))
    }

    override fun getState(): IGuideElementState =
        GuideMapState().correlateElement(this)

    override fun saveToState(state: IGuideElementState): Boolean {
        if (state !is GuideMapState) return false
        if (!super.saveToState(state)) return false
        
        state.basePoint = basePoint
        state.elements.clear()
        state.setElementsByMap(elements)
        state.idToCoordinate.clear()
        state.idToCoordinate += idToCoordinate
        state.removedElements.clear()
        state.removedElements += removedElements
        return true
    }

    override fun update(state: IGuideElementState, merge: Boolean): Boolean {
        if (state !is GuideMapState) return false
        if (!super<GuideElement>.update(state, merge)) return false
        
        removedElements.clear()
        removedElements += state.removedElements
        removedElements.forEach { id ->
            if (idToCoordinate.contains(id)) {
                unregisterElement(id)
            }
        }
        
        basePoint = state.basePoint
        if (!merge) {
            elements.clear()
            idToCoordinate.clear()
        }
        state.getElementsMapTo(elements)
        idToCoordinate += state.idToCoordinate
        return true
    }

    override fun register(): GuideMap {
        getElements().forEach { it.register() }
        return super.register() as GuideMap
    }

    fun registerElement(element: IGuideElement, coordinate: Coordinate2D) {
        val id = element.getId()
        elements[coordinate] = element
        idToCoordinate[id] = coordinate
        removedElements -= id
    }
    
    override fun registerElement(element: IGuideElement) {
        registerElement(element, Coordinate2D.ORIGIN)
    }

    override fun unregisterElement(id: NamespacedId) {
        val coordinate = idToCoordinate[id] ?: return
        elements[coordinate]?.let { element ->
            if (element.getId() == id) {
                elements -= coordinate
                ShiningDispatchers.launchDB {
                    GuideElementRegistry.removeElement(element)
                }
            }
        }
    }

    override fun getElement(id: NamespacedId, isDeep: Boolean): IGuideElement? {
        idToCoordinate[id]?.let { coordinate ->
            elements[coordinate]?.let {
                return it
            }
        }
        
        if (isDeep) {
            elements.forEach { (coordinate, element) -> 
                if (element is IGuideElementContainer) {
                    element.getElement(id, true)?.let { 
                        return it
                    }
                }
            }
        }
        return null
    }

    override fun getElements(isDeep: Boolean): List<IGuideElement> {
        val list = ArrayList<IGuideElement>()
        if (isDeep) {
            elements.forEach { (coordinate, element) -> 
                if (element is IGuideElementContainer) {
                    list += element.getElements(true)
                } else list += element
            }
        } else {
            elements.mapTo(list) { it.value }
        }
        return list
    }

    override suspend fun getElementsByCondition(team: IGuideTeam, condition: ElementCondition, isDeep: Boolean): List<IGuideElement> {
        val list = ArrayList<IGuideElement>()
        if (isDeep) {
            elements.forEach { (_, elementFuture) ->
                val element = elementFuture as IGuideElementSuspend
                val elementCondition = element.getCondition(team)
                if (element is IGuideElementContainerSuspend) {
                    when (elementCondition) {
                        COMPLETE, UNLOCKED, REPEATABLE -> {
                            list += element.getElementsByCondition(team, condition, true)
                        }
                        LOCKED_DEPENDENCY, LOCKED_LOCK -> {
                            if (elementCondition == condition) {
                                list += element
                            }
                        }
                    }
                } else if (elementCondition == condition) {
                    list += element
                }
            }
        } else {
            elements.forEach { (_, element) ->
                if ((element as IGuideElementSuspend).getCondition(team) == condition) {
                    list += element
                }
            }
        }
        return list
    }
    
    private suspend fun checkChildElementsCompleted(team: IGuideTeam): Boolean {
        elements.forEach { (_, element) -> 
            if (!(element as IGuideElementSuspend).isTeamCompleted(team))
                return false
        }
        return true
    }
    
    
    class OffsetContext(val offset: Coordinate2D) : AbstractGuideContextElement(OffsetContext) {
        companion object Key : GuideContext.Key<OffsetContext>
    }

    companion object {
        val edgeOrders: List<Int> = ArrayList(ShiningGuide.edgeOrders).also {
            for (i in 2..5) {
                it += 1 orderWith i
                it += 9 orderWith i
            }
        }
    }
    
}