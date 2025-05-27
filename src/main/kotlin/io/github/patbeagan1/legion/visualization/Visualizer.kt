package io.github.patbeagan1.legion.visualization

sealed interface Visualizer {
    val linkCollection: LinkCollection
    fun resolve(): String

    class Graphviz(override val linkCollection: LinkCollection) : Visualizer {
        override fun resolve(): String = buildString {
            appendLine("digraph {")
            appendLine("rankdir=\"LR\";")

            linkCollection.subgraphs.forEach { (name, members) ->
                appendLine("subgraph cluster_${name.replace("-", "_")} {")
                appendLine("""label="$name";""")
                members.forEach { metaChan ->
                    appendLine("    \"${metaChan.name}\";")
                }
                appendLine("}")
            }

            linkCollection.linksHard.forEach { entry ->
                entry.value.forEach {
                    appendLine(""""${entry.key.name}" -> "${it.name}"""")
                }
            }
            linkCollection.linksSoft.forEach { entry ->
                entry.value.forEach {
                    appendLine(""""${entry.key.name}" -> "${it.name}" [style=dotted]""")
                }
            }
            appendLine("}")
        }
    }
}