/*
 * #%~
 * VDM Pretty Printer
 * %%
 * Copyright (C) 2018 Anaplan Inc
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #~%
 */
package com.anaplan.engineering.vdmprettyprinter

import org.overture.ast.definitions.SClassDefinition
import org.overture.ast.modules.AAllImport
import org.overture.ast.modules.AModuleModules

enum class LineState {
    new,
    indented,
    tokened
}

const val defaultModuleName = "DEFAULT"

interface IRenderContext {
    val indentCount: Int
    val lineState: LineState
    val typeDef: Boolean
    val containerName: String
    val containerType: ContainerType
    val importedNames: Map<InterContainerReference, IntraContainerReference>
    val config: PrettyPrintConfig

    fun incIndent(): IRenderContext

    fun decIndent(): IRenderContext

    fun markNewLine(): IRenderContext

    fun setLineState(lineState: LineState): IRenderContext

    fun setTypeDef(): IRenderContext

    fun unsetTypeDef(): IRenderContext

    fun setCurrentContainer(module: AModuleModules): IRenderContext

    fun setCurrentContainer(vdmClass: SClassDefinition): IRenderContext

    fun resolveReference(reference: InterContainerReference): Reference
}

interface Reference {
    fun render(context: IRenderContext): String
}

data class InterContainerReference(
        val container: String,
        val name: String
) : Reference {
    override fun render(context: IRenderContext): String {
        return ""
    }
}

data class IntraContainerReference(
        val name: String
) : Reference {
    override fun render(context: IRenderContext): String {
        return ""
    }
}

enum class ContainerType {
    module,
    vdmClass
}

internal class DefaultRenderContext(
        override val config: PrettyPrintConfig = PrettyPrintConfig(),
        override val indentCount: Int = 0,
        override val lineState: LineState = LineState.new,
        override val typeDef: Boolean = false,
        override val containerName: String = defaultModuleName,
        override val containerType: ContainerType = ContainerType.module,
        override val importedNames: Map<InterContainerReference, IntraContainerReference> = emptyMap()
) : IRenderContext {
    override fun setTypeDef() = new(typeDef = true)
    override fun unsetTypeDef() = new(typeDef = false)

    override fun setLineState(lineState: LineState) = new(newLine = lineState)
    override fun markNewLine() = new(newLine = LineState.new)

    override fun decIndent() = if (indentCount > 0) {
        new(indentCount = indentCount - 1)
    } else {
        throw IllegalStateException("Cannot decrement indent count below zero")
    }

    override fun incIndent() = new(indentCount = indentCount + 1)

    override fun setCurrentContainer(module: AModuleModules) = new(containerName = module.name.name, containerType = ContainerType.module, importedNames = getImportedNames(module))

    private fun getImportedNames(module: AModuleModules): Map<InterContainerReference, IntraContainerReference> {
        return module.imports?.imports?.flatMap { import ->
            val moduleName = import.name.name
            import.signatures.flatten().filter { it.renamed != null && it !is AAllImport }.map { signature ->
                InterContainerReference(moduleName, signature.name.name) to IntraContainerReference(signature.renamed.name)
            }
        }?.toMap() ?: emptyMap()
    }

    override fun setCurrentContainer(vdmClass: SClassDefinition) = new(containerName = vdmClass.name.name, containerType = ContainerType.vdmClass, importedNames = getImportedNames(vdmClass))

    private fun getImportedNames(vdmClass: SClassDefinition): Map<InterContainerReference, IntraContainerReference> {
        return getAllSuperClasses(vdmClass).flatMap { superClass ->
            val className = superClass.name.name
            // TODO - do we need to worry about public/private here -- shouldn't that a parsing issue
            superClass.definitions.filter { it.name != null }.map { def ->
                InterContainerReference(className, def.name.name) to IntraContainerReference(def.name.name)
            }
        }.toMap()
    }

    private fun getAllSuperClasses(vdmClass: SClassDefinition): Set<SClassDefinition> =
            vdmClass.superDefs?.flatMap { listOf(it) + getAllSuperClasses(it) }?.toSet() ?: emptySet()

    private fun new(
            config: PrettyPrintConfig = this.config,
            indentCount: Int = this.indentCount,
            newLine: LineState = this.lineState,
            typeDef: Boolean = this.typeDef,
            containerName: String = this.containerName,
            containerType: ContainerType = this.containerType,
            importedNames: Map<InterContainerReference, IntraContainerReference> = this.importedNames
    ) = DefaultRenderContext(config, indentCount, newLine, typeDef, containerName, containerType, importedNames)

    override fun resolveReference(reference: InterContainerReference) = importedNames[reference] ?: reference
}