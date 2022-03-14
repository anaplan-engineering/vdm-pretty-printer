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

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.overture.ast.node.INode
import kotlin.test.Test
import kotlin.test.assertEquals

class RenderBuilderTest {

    @Test
    fun token() {
        val rendered = renderBuilder().token("abc").render
        assertEquals("abc", rendered)
    }

    @Test
    fun tokenAfterNl_noIndent() {
        val rendered = renderBuilder(context = DefaultRenderContext().markNewLine()).token("abc").render
        assertEquals("abc", rendered)
    }

    @Test
    fun tokenAfterNl_withIndent() {
        val rendered = renderBuilder(context = DefaultRenderContext().incIndent().markNewLine()).token("abc").render
        assertEquals("  abc", rendered)
    }

    @Test
    fun tokensWithSpace() {
        val rendered = renderBuilder().token("abc").space().token("def").render
        assertEquals("abc def", rendered)
    }

    @Test
    fun tokensWithParentheses() {
        val rendered = renderBuilder().token("abc").lparens().token("def").rparens().render
        assertEquals("abc(def)", rendered)
    }

    @Test
    fun tokensWithNl() {
        val rendered = renderBuilder().token("abc").nl().token("def").render
        assertEquals("abc\ndef", rendered)
    }

    @Test
    fun tokensWithNlAndIndent() {
        val rendered = renderBuilder().token("abc").nl().incIndent().token("def").render
        assertEquals("abc\n  def", rendered)
    }

    @Test
    fun tokensWithNlsAndIncAndDecOfIndent() {
        val rendered = renderBuilder().token("abc").nl().incIndent().token("def").decIndent().nl().token("ghi").render
        assertEquals("abc\n  def\nghi", rendered)
    }

    @Test
    fun tokensWithNlAndDoubleIndent() {
        val rendered = renderBuilder().token("abc").nl().incIndent().incIndent().token("def").render
        assertEquals("abc\n    def", rendered)
    }

    @Test
    fun node() {
        val node = createNode("node1")
        val rendered = renderBuilder().node(node).render
        assertEquals("node1", rendered)
    }

    @Test
    fun nodeList() {
        val nodeList = listOf(createNode("node1"), createNode("node2"), createNode("node3"))
        val rendered = renderBuilder().nodeList(nodeList).render
        assertEquals("node1, node2, node3", rendered)
    }

    @Test
    fun nodeList_newLines_implicit() {
        val nodeList = listOf(createNode("node1"), createNode("node2"), createNode("node3"))
        val rendered = renderBuilder(config = PrettyPrintConfig(minListLengthToUseNls = 0)).nodeList(nodeList).render
        assertEquals("\n  node1, \n  node2, \n  node3\n", rendered)
    }

    @Test
    fun nodeList_newLines_explicit() {
        val nodeList = listOf(createNode("node1"), createNode("node2"), createNode("node3"))
        val rendered = renderBuilder().nodeList(nodeList, listOf(RenderToken.comma, RenderToken.newLine)).render
        assertEquals("node1,\nnode2,\nnode3", rendered)
    }

    @Test
    fun nodeListList_multiple() {
        val nodeList1 = listOf(createNode("node1"), createNode("node2"), createNode("node3"))
        val nodeList2 = listOf(createNode("node4"), createNode("node5"))
        val rendered = renderBuilder().nodeListList(listOf(nodeList1, nodeList2)).render
        assertEquals("(node1, node2, node3)(node4, node5)", rendered)
    }

    @Test
    fun nodeListList_single() {
        val nodeList1 = listOf(createNode("node1"), createNode("node2"), createNode("node3"))
        val rendered = renderBuilder().nodeListList(listOf(nodeList1)).render
        assertEquals("(node1, node2, node3)", rendered)
    }

    // TODO -- nodeListList -- explicit delimiters and separators

    @Test
    fun nodeListWithExplicitSeparator() {
        val nodeList = listOf(createNode("node1"), createNode("node2"), createNode("node3"))
        val rendered = renderBuilder().nodeList(nodeList, listOf(RenderToken.semiColon, RenderToken.space)).render
        assertEquals("node1; node2; node3", rendered)
    }

    @Test
    fun conditional_dualBranch_true() {
        val nodeList = listOf(createNode("node1"), createNode("node2"), createNode("node3"))
        val rendered = renderBuilder().conditional(2 > 0, { it.token("{}") }, { it.nodeList(nodeList) }).render
        assertEquals("{}", rendered)
    }

    @Test
    fun conditional_dualBranch_false() {
        val nodeList = listOf(createNode("node1"), createNode("node2"), createNode("node3"))
        val rendered = renderBuilder().conditional(0 > 2, { it.token("{}") }, { it.nodeList(nodeList) }).render
        assertEquals("node1, node2, node3", rendered)
    }

    @Test
    fun conditional_singleBranch_true() {
        val nodeList = listOf(createNode("node1"), createNode("node2"), createNode("node3"))
        val rendered = renderBuilder().conditional(2 > 0, { it.nodeList(nodeList) }).render
        assertEquals("node1, node2, node3", rendered)
    }

    @Test
    fun conditional_singleBranch_false() {
        val nodeList = listOf(createNode("node1"), createNode("node2"), createNode("node3"))
        val rendered = renderBuilder().conditional(0 > 2, { it.nodeList(nodeList) }).render
        assertEquals("", rendered)
    }

    @Test
    fun apply() {
        val function = { builder: RenderBuilder ->
            builder.token("def")
        }
        val rendered = renderBuilder().token("abc").apply(function).token("ghi").render
        assertEquals("abcdefghi", rendered)
    }

    @Test
    fun mixedBlock() {
        val nodeList = listOf(createNode("node1"), createNode("node2"), createNode("node3"))
        val node4 = createNode("node4")
        val rendered =
            renderBuilder().node(node4).space().token(":").space().token("nat").token("-set").space().token("=")
                .incIndent().nl().token("{").nodeList(nodeList).token("}").render
        assertEquals("node4 : nat-set =\n  {node1, node2, node3}", rendered)
    }

    private fun createNode(id: String): INode {
        val node = mock<INode>()
        whenever(node.apply(any<VdmPrettyPrinter>(), any())).thenReturn(id)
        return node
    }

    private fun renderBuilder(
        vdmPrettyPrinter: VdmPrettyPrinter = mock<VdmPrettyPrinter>(),
        config: PrettyPrintConfig = PrettyPrintConfig(minListLengthToUseNls = 10),
        context: DefaultRenderContext = DefaultRenderContext(config)
    ): RenderBuilder {
        whenever(vdmPrettyPrinter.renderStrategy).thenReturn(PlainAsciiTextRenderStrategy())
        return RenderBuilder(vdmPrettyPrinter, context)
    }

}
