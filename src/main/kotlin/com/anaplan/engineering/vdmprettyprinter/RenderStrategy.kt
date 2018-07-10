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

import com.anaplan.engineering.vdmprettyprinter.RenderToken.*

interface IRenderStrategy {
    fun renderToken(token: RenderToken): String
    fun renderIndent(indentCount: Int): String
    fun renderNavigationMarker(marker: NavigationMarker) : String
    fun replacesParentheses(token: RenderToken): Boolean
    val vspace: String
    val header: String
    val footer: String
}

open class PlainAsciiTextRenderStrategy : IRenderStrategy {
    override val header = ""
    override val footer = ""
    override val vspace = "\n"
    override fun renderIndent(indentCount: Int) = (0 until indentCount).map { "  " }.joinToString("")
    override fun renderToken(token: RenderToken) = token.defaultRender
    override fun renderNavigationMarker(marker: NavigationMarker) = ""
    override fun replacesParentheses(token: RenderToken) = false
}

open class MathematicalUnicodeTextRenderStrategy : PlainAsciiTextRenderStrategy() {

    override fun renderToken(token: RenderToken): String {
        val render = tokenMap.get(token)
        return if (render == null) token.defaultRender else render
    }

    override fun replacesParentheses(token: RenderToken) = unicodeSymbolsThatReplaceParentheses.contains(token)

    companion object {
        internal const val IF = "\uD835\uDE5E\uD835\uDE5B"
        internal const val THEN = "\uD835\uDE69\uD835\uDE5D\uD835\uDE5A\uD835\uDE63"
        internal const val ELSE = "\uD835\uDE5A\uD835\uDE61\uD835\uDE68\uD835\uDE5A"
        internal const val TRUE = "\uD835\uDE69\uD835\uDE67\uD835\uDE6A\uD835\uDE5A"
        internal const val FALSE = "\uD835\uDE5B\uD835\uDE56\uD835\uDE61\uD835\uDE68\uD835\uDE5A"
        internal const val IS = "\uD835\uDE5E\uD835\uDE68_"
        internal const val POST = "\uD835\uDE65\uD835\uDE64\uD835\uDE68\uD835\uDE69"
        internal const val PRE = "\uD835\uDE65\uD835\uDE67\uD835\uDE5A"
        internal const val CHAR = "\uD835\uDE58\uD835\uDE5D\uD835\uDE56\uD835\uDE67"
        internal const val MODULE = "\uD835\uDE62\uD835\uDE64\uD835\uDE59\uD835\uDE6A\uD835\uDE61\uD835\uDE5A"
        internal const val DEFINITIONS = "\uD835\uDE59\uD835\uDE5A\uD835\uDE5B\uD835\uDE5E\uD835\uDE63\uD835\uDE5E\uD835\uDE69\uD835\uDE5E\uD835\uDE64\uD835\uDE63\uD835\uDE68"
        internal const val FUNCTIONS = "\uD835\uDE5B\uD835\uDE6A\uD835\uDE63\uD835\uDE58\uD835\uDE69\uD835\uDE5E\uD835\uDE64\uD835\uDE63\uD835\uDE68"
        internal const val TYPES = "\uD835\uDE69\uD835\uDE6E\uD835\uDE65\uD835\uDE5A\uD835\uDE68"
        internal const val VALUES = "\uD835\uDE6B\uD835\uDE56\uD835\uDE61\uD835\uDE6A\uD835\uDE5A\uD835\uDE68"
        internal const val END = "\uD835\uDE5A\uD835\uDE63\uD835\uDE59"
        internal const val MK_ = "\uD835\uDE62\uD835\uDE60_"
        internal const val LET = "\uD835\uDE61\uD835\uDE5A\uD835\uDE69"
        internal const val IN = "\uD835\uDE5E\uD835\uDE63"

        // http://slothsoft.net/UnicodeMapper/
        // TODO -- work out to how to do the above with a fn
        val tokenMap = unicodeSymbolTokenMap + mapOf<RenderToken, String>(
                inSeq to "in seq", // bold
                pre to PRE,
                post to POST,
                inv to "\uD835\uDE5E\uD835\uDE63\uD835\uDE6B",
                ifToken to IF,
                inToken to IN,
                then to THEN,
                elseToken to ELSE,
                let to LET,
                end to END,
                is_ to IS,
                mk_ to MK_,
                module to MODULE,
                definitions to DEFINITIONS,
                functions to FUNCTIONS,
                operations to "\uD835\uDE64\uD835\uDE65\uD835\uDE5A\uD835\uDE67\uD835\uDE56\uD835\uDE69\uD835\uDE5E\uD835\uDE64\uD835\uDE63\uD835\uDE68",
                types to TYPES,
                values to VALUES,
                importValues to VALUES,
                importTypes to TYPES,
                token to "\uD835\uDE69\uD835\uDE64\uD835\uDE60\uD835\uDE5A\uD835\uDE63",
                state to "\uD835\uDE68\uD835\uDE69\uD835\uDE56\uD835\uDE69\uD835\uDE5A",
                of to "\uD835\uDE64\uD835\uDE5B",
                undefined to "\uD835\uDE6A\uD835\uDE63\uD835\uDE59\uD835\uDE5A\uD835\uDE5B\uD835\uDE5E\uD835\uDE63\uD835\uDE5A\uD835\uDE59",
                isNotYetSpecified to "\uD835\uDE5E\uD835\uDE68 \uD835\uDE63\uD835\uDE64\uD835\uDE69 \uD835\uDE6E\uD835\uDE5A\uD835\uDE69 \uD835\uDE68\uD835\uDE65\uD835\uDE5A\uD835\uDE58\uD835\uDE5E\uD835\uDE5B\uD835\uDE5E\uD835\uDE5A\uD835\uDE59",
                returnToken to "\uD835\uDE67\uD835\uDE5A\uD835\uDE69\uD835\uDE6A\uD835\uDE67\uD835\uDE63",
                setLeft to "",
                setRight to "-\uD835\uDE68\uD835\uDE5A\uD835\uDE69",
                set1Left to "",
                set1Right to "-\uD835\uDE68\uD835\uDE5A\uD835\uDE69₁",
                seqLeft to "",
                seqRight to "-\uD835\uDE68\uD835\uDE5A\uD835\uDE69", // FIX from set to seq
                token to "\uD835\uDE69\uD835\uDE64\uD835\uDE60\uD835\uDE5A\uD835\uDE63",
                trueToken to TRUE,
                falseToken to FALSE,
                char to CHAR
        )
    }

}

class MathematicalUnicodeHtmlRenderStrategy : IRenderStrategy {
    override val header = "<html><body>"
    override val footer = "</body></html>"

    // blank lines handled by other styling e.g. headings
    override val vspace = ""

    override fun renderToken(token: RenderToken): String {
        val render = tokenMap.get(token)
        return if (render == null) token.defaultRender else render
    }

    override fun replacesParentheses(token: RenderToken) = unicodeSymbolsThatReplaceParentheses.contains(token)

    override fun renderNavigationMarker(marker: NavigationMarker)  = "<div id='${marker.id}'/>"

    override fun renderIndent(indentCount: Int) = (0 until indentCount).map { "&nbsp;&nbsp;" }.joinToString("")

    companion object {
        val tokenMap = unicodeSymbolTokenMap + mapOf<RenderToken, String>(
                newLine to "<br/>\n",
                lessThan to "&lt;",
                greaterThan to "&gt;",
                hd to "<b>hd</b>",
                tl to "<b>tl</b>",
                reverse to "<b>reverse</b>",
                whileToken to "<b>while</b>",
                skip to "<b>skip</b>",
                def to "<b>def</b>",
                measure to "<b>measure</b>",
                doToken to "<b>do</b>",
                toToken to "<b>to</b>",
                card to "<b>card</b>",
                conc to "<b>conc</b>",
                inSeq to "<b>in seq</b>",
                error to "<b>error</b>",
                forToken to "<b>for</b>",
                by to "<b>by</b>",
                munion to "<b>munion</b>",
                merge to "<b>merge</b>",
                nil to "<i>nil</i>",
                paramType to "<b>@</b>",
                rng to "<b>rng</b>",
                dcl to "<b>dcl</b>",
                inds to "<b>inds</b>",
                elems to "<b>elems</b>",
                dom to "<b>dom</b>",
                len to "<b>len</b>",
                pre to "<b>pre</b>",
                post to "<b>post</b>",
                be to "<b>be</b>",
                st to "<b>st</b>",
                inv to "<b>inv</b>",
                eq to "<b>eq</b>",
                withToken to "<b>with</b>",
                trap to "<b>trap</b>",
                ord to "<b>ord</b>",
                inToken to "<b>in</b>",
                ifToken to "<b>if</b>",
                then to "<b>then</b>",
                elseToken to "<b>else</b>",
                elseif to "<b>elseif</b>",
                renamed to "<b>renamed</b>",
                struct to "<b>struct</b>",
                cases to "<b>cases</b>",
                let to "<b>let</b>",
                others to "<b>others</b>",
                exit to "<b>exit</b>",
                mutex to "<b>mutex</b>",
                per to "<b>per</b>",
                thread to "<b>thread</b>",
                end to "<b>end</b>",
                ext to "<b>ext</b>",
                errs to "<b>errs</b>",
                duration to "<b>duration</b>",
                from to "<b>from</b>",
                new to "<b>new</b>",
                periodic to "<b>periodic</b>",
                cycles to "<b>cycles</b>",
                protectedToken to "<b>protected</b>",
                start to "<b>start</b>",
                all to "<b>all</b>",
                time to "<b>time</b>",
                is_ to "<b>is_</b>",
                mk_ to "<b>mk_</b>",
                lquote to "<b>&lt;",
                rquote to "&gt;</b>",
                space to "&nbsp;",
                module to "<h2>module</h2>",
                system to "<h2>system</h2>",
                classToken to "<h2>class</h2>",
                imports to "<h3>imports</h3>",
                exports to "<h3>exports</h3>",
                definitions to "<h3>definitions</h3>",
                functions to "<h3>functions</h3>",
                operations to "<h3>operations</h3>",
                traces to "<h3>traces</h3>",
                types to "<h3>types</h3>",
                sync to "<h3>sync</h3>",
                instanceVariables to "<h3>instance variables</h3>",
                values to "<h3>values</h3>",
                importFunctions to "<b>functions</b>",
                importOperations to "<b>operations</b>",
                importTypes to "<b>types</b>",
                importValues to "<b>values</b>",
                pure to "<b>pure</b>",
                self to "<b>self</b>",
                atomic to "<b>atomic</b>",
                threadid to "<b>threadid</b>",
                async to "<b>async</b>",
                publicToken to "<b>public</b>",
                static to "<b>static</b>",
                token to "<i>token</i>",
                state to "<h3>state</h3>",
                of to "<b>of</b>",
                undefined to "<b>undefined</b>",
                isNotYetSpecified to "<i>is not yet specified</i>",
                isSubclassResponsibility to "<i>is subclass responsibility</i>",
                returnToken to "<b>return</b>",
                setRight to "<i>-set</i>",
                setLeft to "",
                set1Right to "<i>-set₁</i>",
                set1Left to "",
                seqRight to "<i>*</i>",
                seqLeft to "",
                seq1Right to "<i>+</i>",
                seq1Left to "",
                trueToken to "<i>true</i>",
                falseToken to "<i>false</i>",
                char to "<i>char</i>"
        )
    }
}

private val unicodeSymbolsThatReplaceParentheses = setOf(
        floorLeft, floorRight, absLeft, absRight
)

private val unicodeSymbolTokenMap = mapOf<RenderToken, String>(
        emptyTuple to "∅", // https://math.stackexchange.com/contexts/964092/the-empty-tuple-or-0-tuple-its-definition-and-properties
        partialFunction to "→",
        totalFunction to "⇸",
        emptySet to "∅",
        multiplication to "✕",
        operation to "⟹",
        maplet to "↦",
        union to "∪",
        inter to "∩",
        dunion to "⋃",
        dinter to "⋂",
        real to "ℝ",
        iota to "ι",
        rat to "ℚ",
        subset to "⊆",
        psubset to "⊂",
        lambda to "λ",
        nat to "ℕ",
        mu to "μ",
        concat to "⁀",
        implies to "⇒",
        and to "∧",
        bullet to "•",
        inSet to "∈",
        nat1 to "ℕ₁",
        int to "ℤ",
        power to "↑",
        powerset to "ℙ",
        floorLeft to "⌊",
        floorRight to "⌋",
        forall to "∀",
        exists to "∃",
        exists1 to "∃₁",
        ceilingLeft to "⌈",
        ceilingRight to "⌉",
        range to "…",
        bool to "\uD835\uDD39",
        fnEquals to "⧋",
        absLeft to "|",
        absRight to "|",
        assign to "≔",
        relationalOverride to "†",
        lessThanEq to "≤",
        greaterThanEq to "≥",
        not to "¬",
        map to "",
        inmap to "",
        mapTo to "ᵐ⟶",
        inmapTo to "ᵐ⟷",
        inverseLeft to "",
        inverseRight to "⁻¹"
)
