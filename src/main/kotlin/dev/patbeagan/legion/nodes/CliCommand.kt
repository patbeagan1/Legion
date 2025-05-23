package dev.patbeagan.legion.nodes

import dev.patbeagan.legion.util.RunCommand
import dev.patbeagan.legion.ImpNode
import dev.patbeagan.legion.LegionScope

fun LegionScope<*>.cli(): ImpNode.Imp<List<String>, RunCommand.CommandResult> = imp {
    RunCommand.runCommand(it)
}