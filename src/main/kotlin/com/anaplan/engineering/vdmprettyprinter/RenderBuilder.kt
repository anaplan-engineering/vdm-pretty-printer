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

import com.anaplan.engineering.vdmprettyprinter.PrecedenceManager.expRequiresParentheses
import com.anaplan.engineering.vdmprettyprinter.PrecedenceManager.typeRequiresParentheses
import com.anaplan.engineering.vdmprettyprinter.RenderToken.*
import org.overture.ast.definitions.SClassDefinitionBase
import org.overture.ast.expressions.PExp
import org.overture.ast.intf.lex.ILexNameToken
import org.overture.ast.modules.AModuleModules
import org.overture.ast.node.INode
import org.overture.ast.types.PType

internal class RenderBuilder(
    private val vdmPrettyPrinter: VdmPrettyPrinter,
    private val context: IRenderContext,
    val render: String = ""
) {
    internal val strategy: IRenderStrategy
        get() = vdmPrettyPrinter.renderStrategy

    internal val config: PrettyPrintConfig
        get() = context.config

    internal fun token(token: RenderToken): RenderBuilder {
        val builder = MutableRenderBuilder(this)
        builder.setLineState(LineState.tokened, LineState.tokened)
        builder.append(strategy.renderToken(token))
        return builder.build()
    }

    internal fun token(token: String): RenderBuilder {
        val renderToken = RenderToken.lookup(token)
        return if (renderToken == null) {
            val builder = MutableRenderBuilder(this)
            builder.setLineState(LineState.tokened, LineState.tokened)
            builder.append(token)
            return builder.build()
        } else {
            token(renderToken)
        }
    }

    internal fun lparens() = token(lparens)
    internal fun rparens() = token(rparens)
    internal fun space() = token(space)

    internal fun nl() = RenderBuilder(vdmPrettyPrinter, context.markNewLine(), render + strategy.renderToken(newLine))
    internal fun vspace() = RenderBuilder(vdmPrettyPrinter, context.markNewLine(), render + strategy.vspace)

    internal fun addNavigationMarks(marker: NavigationMarker) =
        RenderBuilder(vdmPrettyPrinter, context, render + strategy.renderNavigationMarker(marker))

    internal fun setCurrentContainer(module: AModuleModules) =
        RenderBuilder(vdmPrettyPrinter, context.setCurrentContainer(module), render)

    internal fun setCurrentContainer(vdmClass: SClassDefinitionBase) =
        RenderBuilder(vdmPrettyPrinter, context.setCurrentContainer(vdmClass), render)

    internal fun setTypeDef() = RenderBuilder(vdmPrettyPrinter, context.setTypeDef(), render)
    internal fun unsetTypeDef() = RenderBuilder(vdmPrettyPrinter, context.unsetTypeDef(), render)

    internal fun node(node: INode): RenderBuilder {
        val builder = MutableRenderBuilder(this)
        builder.setLineState(newLineState = LineState.indented)
        builder.append(node.apply(vdmPrettyPrinter, builder.context))
        return builder.build()
    }

    internal fun nodeList(
        nodeList: List<INode>,
        separators: List<RenderToken> = listOf(comma, space),
        getNavigationMarker: (INode) -> NavigationMarker? = { null },
        renderNode: (INode, RenderBuilder) -> RenderBuilder = { node, builder -> builder.node(node) }
    ): RenderBuilder {
        val builder = MutableRenderBuilder(this)
        val useImplicitNls = nodeList.size >= config.minListLengthToUseNls && !separators.contains(newLine)
        if (useImplicitNls) {
            builder.nl()
            builder.incIndent()
        }
        nodeList.forEachIndexed { index, node ->
            val navigationMarker = getNavigationMarker(node)
            if (navigationMarker != null) {
                builder.append(strategy.renderNavigationMarker(navigationMarker))
            }
            builder.setLineState(newLineState = LineState.indented)
            val localBuilder = RenderBuilder(vdmPrettyPrinter, builder.context)
            builder.append(renderNode(node, localBuilder).render)
            if (index < nodeList.size - 1) {
                separators.forEach { separator ->
                    if (separator == newLine) {
                        builder.nl()
                    } else {
                        builder.append(strategy.renderToken(separator))
                    }
                }
                if (useImplicitNls) {
                    builder.nl()
                }
            }
        }
        if (useImplicitNls) {
            builder.nl()
            builder.decIndent()
        }
        return builder.build()
    }

    // Don't have an example spec where we actually need this beyond a single element node list list, will likely
    // need to be refactored/improved when we do
    internal fun nodeListList(
        nodeListList: List<List<INode>>,
        separators: List<RenderToken> = listOf(comma, space),
        delimiters: Pair<RenderToken, RenderToken> = Pair(lparens, rparens)
    ): RenderBuilder {
        val builder = MutableRenderBuilder(this)
        builder.setLineState(newLineState = LineState.indented)
        val separatorString = separators.map { strategy.renderToken(it) }.joinToString("")
        builder.append(nodeListList.map { nodeList ->
            strategy.renderToken(delimiters.first) +
                nodeList.map { it.apply(vdmPrettyPrinter, context) }.joinToString(separatorString) +
                strategy.renderToken(delimiters.second)
        }.joinToString(""))
        return builder.build()
    }

    internal fun incIndent() = RenderBuilder(vdmPrettyPrinter, context.incIndent(), render)
    internal fun decIndent() = RenderBuilder(vdmPrettyPrinter, context.decIndent(), render)

    internal fun conditional(
        condition: Boolean,
        trueExpression: (RenderBuilder) -> RenderBuilder,
        falseExpression: (RenderBuilder) -> RenderBuilder
    ) = if (condition) {
        trueExpression(this)
    } else {
        falseExpression(this)
    }

    internal fun conditional(
        condition: Boolean,
        trueExpression: (RenderBuilder) -> RenderBuilder
    ) = conditional(condition, trueExpression, { it })

    internal fun apply(fn: (RenderBuilder) -> RenderBuilder) = fn(this)

    internal fun applyAll(fns: List<(RenderBuilder) -> RenderBuilder>): RenderBuilder {
        var accumulator = this
        fns.forEach { fn ->
            accumulator = fn(accumulator)
        }
        return accumulator
    }

    internal fun ifTypeDef(
        trueExpression: (RenderBuilder) -> RenderBuilder,
        falseExpression: (RenderBuilder) -> RenderBuilder
    ) = if (context.typeDef) {
        trueExpression(this)
    } else {
        falseExpression(this)
    }

    internal fun unlessModuleIs(
        module: String,
        trueExpression: (RenderBuilder) -> RenderBuilder
    ) = if (context.containerName != module) {
        trueExpression(this)
    } else {
        this
    }

    internal fun name(name: ILexNameToken): RenderBuilder {
        return conditional(name.module.isBlank(), {
            it.unlessModuleIs(defaultModuleName, {
                it.token(defaultModuleName).token(backTick)
            }).token(name.name)
        }, {
            it.renderReference(name.module, name.name)
        })
    }

    private fun renderReference(container: String, name: String): RenderBuilder {
        if (container == context.containerName) {
            return token(name)
        }
        val resolvedReference = context.resolveReference(InterContainerReference(container, name))
        return if (resolvedReference is InterContainerReference) {
            token(resolvedReference.container).token(backTick).token(resolvedReference.name)
        } else if (resolvedReference is IntraContainerReference) {
            token(resolvedReference.name)
        } else {
            throw IllegalStateException()
        }
    }

    internal enum class Position {
        left, right
    }

    internal fun childExpression(child: PExp, parent: PExp, position: Position) =
        conditional(expRequiresParentheses(child, parent, position == Position.right), {
            it.lparens()
        }). //
        node(child). //
        conditional(expRequiresParentheses(child, parent, position == Position.right), {
            it.rparens()
        })

    internal fun childType(child: PType, parent: PType) =
        conditional(typeRequiresParentheses(child, parent), {
            it.lparens()
        }). //
        node(child). //
        conditional(typeRequiresParentheses(child, parent), {
            it.rparens()
        })

    private class MutableRenderBuilder(
        val renderBuilder: RenderBuilder,
        var context: IRenderContext = renderBuilder.context,
        val render: StringBuilder = StringBuilder(renderBuilder.render)
    ) {

        internal fun applyIndent() {
            render.append(renderBuilder.strategy.renderIndent(context.indentCount))
        }

        internal fun nl() {
            render.append(renderBuilder.strategy.renderToken(newLine))
            context = context.setLineState(LineState.new)
        }

        internal fun incIndent() {
            context = context.incIndent()
        }

        internal fun decIndent() {
            context = context.decIndent()
        }

        internal fun setLineState(newLineState: LineState? = null, existingLineState: LineState? = null) {
            context = if (context.lineState == LineState.new) {
                applyIndent()
                if (newLineState == null) {
                    context
                } else {
                    context.setLineState(newLineState)
                }
            } else {
                if (existingLineState == null) {
                    context
                } else {
                    context.setLineState(existingLineState)
                }
            }
        }

        internal fun append(text: String) {
            render.append(text)
        }

        internal fun build(): RenderBuilder {
            return RenderBuilder(renderBuilder.vdmPrettyPrinter, context, render.toString())
        }
    }
}
