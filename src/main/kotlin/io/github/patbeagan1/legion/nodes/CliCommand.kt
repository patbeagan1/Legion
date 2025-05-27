package io.github.patbeagan1.legion.nodes

import io.github.patbeagan1.legion.util.RunCommand
import io.github.patbeagan1.legion.ImpNode
import io.github.patbeagan1.legion.LegionScope

fun LegionScope<*>.cli(): ImpNode.Imp<List<String>, RunCommand.CommandResult> = imp {
    RunCommand.runCommand(it)
}