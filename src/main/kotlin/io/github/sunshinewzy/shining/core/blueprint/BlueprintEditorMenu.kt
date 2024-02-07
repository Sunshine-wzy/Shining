package io.github.sunshinewzy.shining.core.blueprint

import io.github.sunshinewzy.shining.api.blueprint.IBlueprintClass
import io.github.sunshinewzy.shining.api.blueprint.IBlueprintNode
import io.github.sunshinewzy.shining.api.blueprint.IBlueprintNodeTree
import io.github.sunshinewzy.shining.api.objects.coordinate.Coordinate2D
import io.github.sunshinewzy.shining.core.blueprint.node.EmptyBlueprintNode
import io.github.sunshinewzy.shining.core.lang.getLangText
import io.github.sunshinewzy.shining.core.menu.MapMenu
import io.github.sunshinewzy.shining.core.menu.onBack
import io.github.sunshinewzy.shining.objects.item.ShiningIcon
import io.github.sunshinewzy.shining.utils.orderWith
import io.github.sunshinewzy.shining.utils.toCoordinate2D
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import taboolib.common.util.subList
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Basic
import taboolib.platform.util.buildItem
import taboolib.platform.util.isAir

open class BlueprintEditorMenu(title: String) : MapMenu<IBlueprintNode>(title) {
    
    var blueprintClass: IBlueprintClass = BlueprintClass()
        private set
    var nodeTreePage: Int = 0
        private set
    lateinit var currentNodeTree: IBlueprintNodeTree
        private set
    
    protected var nodeTreePageChangeCallback: ((player: Player) -> Unit) = { _ -> }
    
    
    open fun blueprint(blueprint: IBlueprintClass) {
        this.blueprintClass = blueprint
    }

    open fun setNodeTreePreviousPage(slot: Int, callback: (page: Int, hasPreviousPage: Boolean) -> ItemStack) {
        set(slot) { callback(nodeTreePage, hasNodeTreePreviousPage()) }
        onClick(slot) {
            if (hasNodeTreePreviousPage()) {
                nodeTreePage--
                player.openInventory(build())
                nodeTreePageChangeCallback(player)
            }
        }
    }
    
    open fun setNodeTreeNextPage(slot: Int, callback: (page: Int, hasNextPage: Boolean) -> ItemStack) {
        set(slot) { callback(nodeTreePage, hasNodeTreeNextPage()) }
        onClick(slot) {
            if (hasNodeTreeNextPage()) {
                nodeTreePage++
                player.openInventory(build())
                nodeTreePageChangeCallback(player)
            }
        }
    }
    
    open fun hasNodeTreePreviousPage(): Boolean = nodeTreePage > 0
    
    open fun hasNodeTreeNextPage(): Boolean =
        blueprintClass.getNodeTrees().size / 5f > nodeTreePage + 1

    open fun onNodeTreePageChange(callback: (player: Player) -> Unit) {
        nodeTreePageChangeCallback = callback
    }
    
    open fun switchNodeTree(tree: IBlueprintNodeTree) {
        currentNodeTree = tree
        val map = HashMap<Coordinate2D, IBlueprintNode>()
        putBlueprintNode(map, tree.root, 0, 0)
        elementsCache = map
    }
    
    private fun putBlueprintNode(map: HashMap<Coordinate2D, IBlueprintNode>, node: IBlueprintNode, x: Int, y: Int): Int {
        map[Coordinate2D(x, y)] = node
        val successors = node.successors
        if (successors.isEmpty()) return 1

        var sum = 0
        for (successor in successors) {
            sum += putBlueprintNode(map, successor, x + 1, y + sum)
        }
        return sum
    }
    
    
    protected val nodeTreeElementMap = HashMap<Int, IBlueprintNodeTree>()
    protected var nodeTreeElementItems: List<IBlueprintNodeTree> = emptyList()

    override fun processBuild(player: Player, inventory: Inventory, async: Boolean) {
        super.processBuild(player, inventory, async)
        nodeTreeElementItems.forEachIndexed { index, tree ->
            val slot = NODE_TREE_BASE_INDEX + index
            nodeTreeElementMap[slot] = tree
            
            inventory.setItem(
                slot,
                buildItem(tree.root.icon) {
                    name = "&f${player.getLangText("text-blueprint-node_tree")} ${nodeTreePage * 5 + index + 1}"
                    lore += ""
                    lore += "&6${player.getLangText("text-blueprint-node_tree-root")}:"
                    lore += tree.root.getName(player)
                    colored()
                    if (tree == currentNodeTree) shiny()
                }
            )
        }
    }

    override fun processSelfBuild() {
        onGenerate { player, element, _, _ -> 
            buildItem(element.icon) {
                name = element.getName(player)
                lore += element.getDescription(player)
                colored()
            }
        }
        val nodeTrees = blueprintClass.nodeTrees
        if (nodeTrees.isNotEmpty() && !this::currentNodeTree.isInitialized) {
            switchNodeTree(nodeTrees[0])
        }
        
        nodeTreeElementMap.clear()
        nodeTreeElementItems = subList(nodeTrees, nodeTreePage * 5, (nodeTreePage + 1) * 5)

        selfBuild { player, inventory -> processBuild(player, inventory, false) }
        selfBuild(async = true) { player, inventory -> processBuild(player, inventory, true) }
        selfClick {
            if (menuLocked) {
                it.isCancelled = true
            }
            elementMap[it.rawSlot]?.let { pair ->
                editNode(pair.first)
                elementClickCallback(it, pair.first, pair.second)
            } ?: nodeTreeElementMap[it.rawSlot]?.let { tree ->
                switchNodeTree(tree)
                player.openInventory(build())
            } ?: kotlin.run {
                val rawCoordinate = it.rawSlot.toCoordinate2D()
                if (rawCoordinate in menuArea && it.currentItem.isAir()) {
                    clickEmptyCallback(it, rawCoordinate + offset - baseCoordinate)
                }
            }
        }
    }
    
    protected open fun editNode(node: IBlueprintNode) {
        player.openMenu<Basic>(title) { 
            rows(5)
            map(
                "-B-------",
                "- u   x -",
                "-lir    -",
                "- d   e -",
                "---------"
            )
            
            set('-', ShiningIcon.EDGE.item)
            onBack(player) { 
                player.openInventory(this@BlueprintEditorMenu.build())
            }
            
            if (node != EmptyBlueprintNode) {
                val pre = node.predecessorOrNull
                if (pre != null) {
                    var index = 0
                    val array = pre.successors
                    for (nodeInArray in array) {
                        if (node == nodeInArray) break
                        index++
                    }
                    
                    if (index > 0) {
                        set('u', ShiningIcon.MOVE_UP.toLocalizedItem(player)) {
                            val temp = array[index - 1]
                            array[index - 1] = array[index]
                            array[index] = temp
                            reopen()
                        }
                    }
                    
                    if (index < array.size - 1) {
                        set('d', ShiningIcon.MOVE_DOWN.toLocalizedItem(player)) {
                            val temp = array[index + 1]
                            array[index + 1] = array[index]
                            array[index] = temp
                            reopen()
                        }
                    }
                    
                    if (node.successorAmount == 1 && pre.successorAmount == 1) {
                        set('l', ShiningIcon.MOVE_LEFT.toLocalizedItem(player)) {
                            val prePre = pre.predecessorOrNull
                            if (prePre == null) {
                                pre.setSuccessor(0, node.successor)
                                node.setSuccessor(0, pre)
                                currentNodeTree.root = node
                            } else {
                                var preIndex = 0
                                val preArray = prePre.successors
                                for (nodeInArray in preArray) {
                                    if (pre == nodeInArray) break
                                    preIndex++
                                }

                                pre.setSuccessor(0, node.successor)
                                node.setSuccessor(0, pre)
                                prePre.setSuccessor(preIndex, node)
                            }
                            reopen()
                        }
                    }
                }
                
                if (node.successorAmount == 1) {
                    val suc = node.successor
                    if (suc.successorAmount == 1) {
                        set('r', ShiningIcon.MOVE_RIGHT.toLocalizedItem(player)) {
                            if (pre == null) {
                                node.setSuccessor(0, suc.successor)
                                suc.setSuccessor(0, node)
                                currentNodeTree.root = suc
                            } else {
                                var index = 0
                                val array = pre.successors
                                for (nodeInArray in array) {
                                    if (node == nodeInArray) break
                                    index++
                                }
                                
                                node.setSuccessor(0, suc.successor)
                                suc.setSuccessor(0, node)
                                pre.setSuccessor(index, suc)
                            }
                            reopen()
                        }
                    }
                }
            }
            
            onClick(lock = true)
        }
    }
    
    open fun reopen() {
        switchNodeTree(currentNodeTree)
        player.openInventory(build())
    }
    
    
    companion object {
        val NODE_TREE_BASE_INDEX = 3 orderWith 6
    }
    
}