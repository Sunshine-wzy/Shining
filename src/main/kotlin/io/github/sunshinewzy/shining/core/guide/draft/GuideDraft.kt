package io.github.sunshinewzy.shining.core.guide.draft

import io.github.sunshinewzy.shining.Shining
import io.github.sunshinewzy.shining.api.guide.draft.IGuideDraft
import io.github.sunshinewzy.shining.api.guide.state.IGuideElementState
import io.github.sunshinewzy.shining.api.namespace.NamespacedId
import io.github.sunshinewzy.shining.core.lang.getLangText
import io.github.sunshinewzy.shining.core.lang.item.NamespacedIdItem
import io.github.sunshinewzy.shining.objects.item.ShiningIcon
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import taboolib.common.platform.function.submit
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.Basic
import taboolib.platform.util.buildItem

class GuideDraft(id: EntityID<Long>) : LongEntity(id), IGuideDraft {
    
    var state: IGuideElementState by GuideDrafts.state


    override fun getSymbol(player: Player): ItemStack {
        return transaction {
            buildItem(state.symbol ?: ItemStack(Material.PAPER)) {
                name = state.descriptionName
                lore.addAll(0, listOf(player.getLangText("menu-shining_guide-draft-symbol-draft"), ""))
                lore.addAll(2, state.descriptionLore)
                colored()
            }
        }
    }

    override suspend fun open(player: Player, previousFolder: GuideDraftFolder?) {
        submit { 
            openMenu(player, previousFolder)
        }
    }
    
    fun openMenu(player: Player, previousFolder: GuideDraftFolder? = null) {
        player.openMenu<Basic>(player.getLangText("menu-shining_guide-draft-editor-title")) {
            rows(3)

            map(
                "-B-------",
                "-  a d  -",
                "---------"
            )

            set('-', ShiningIcon.EDGE.item)

            set('B', ShiningIcon.BACK_MENU.toLocalizedItem(player)) {
                if (clickEvent().isShiftClick || previousFolder == null) {
                    ShiningGuideDraft.openMainMenu(player)
                } else {
                    Shining.launchIO {
                        previousFolder.open(player)
                    }
                }
            }
            
            set('a', itemEditState.toLocalizedItem(player)) {
                Shining.launchIO { 
                    newSuspendedTransaction { 
                        state.openEditor(player)
                    }
                }
            }
            
            if (previousFolder != null) {
                set('d', ShiningIcon.REMOVE.toLocalizedItem(player)) {
                    Shining.launchIO {
                        previousFolder.removeDraft(id.value)
                        newSuspendedTransaction {
                            delete()
                        }
                    }
                }
            }
        }
    }
    

    companion object : LongEntityClass<GuideDraft>(GuideDrafts) {
        private val itemEditState = NamespacedIdItem(Material.REDSTONE_LAMP, NamespacedId(Shining, "shining_guide-draft-editor-state"))
    }
    
}