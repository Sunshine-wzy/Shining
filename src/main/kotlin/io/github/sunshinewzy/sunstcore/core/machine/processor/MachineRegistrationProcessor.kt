package io.github.sunshinewzy.sunstcore.core.machine.processor

import io.github.sunshinewzy.sunstcore.core.machine.IMachine
import io.github.sunshinewzy.sunstcore.core.machine.MultiBlockMachine
import io.github.sunshinewzy.sunstcore.core.machine.PlaneMachine
import io.github.sunshinewzy.sunstcore.core.machine.SimpleMachine
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent

object MachineRegistrationProcessor : IMachineRegistrationProcessor {

    override fun onRegister(machine: IMachine) {
        when(machine) {
            is SimpleMachine -> {
                
            }
            
            is PlaneMachine -> {
                
            }
            
            is MultiBlockMachine -> {
                
            }
        }
    }


    @SubscribeEvent(EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        
    }
    
    @SubscribeEvent(EventPriority.HIGHEST)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val item = event.itemInHand
        
    }
    
    @SubscribeEvent(EventPriority.HIGHEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        
    }
    
}