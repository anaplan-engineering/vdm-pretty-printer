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

class MathematicalUnicodeHtmlRenderStrategyTest : VdmPrettyPrinterTest() {

    companion object {
        private const val BOOL = "\uD835\uDD39"
    }

    override fun getExpectedNamedInvariantTypeContextSpec() =
            """|<h3>types</h3><div id='types'/><br/>
               |<div id='a'/>&nbsp;&nbsp;a&nbsp;=&nbsp;<i>char</i>&nbsp;|&nbsp;ℝ<br/>
               |<h3>values</h3><div id='values'/><br/>
               |&nbsp;&nbsp;b:&nbsp;a&nbsp;=&nbsp;0<br/>
               |<br/>
               |""".trimMargin()

    override fun getExpectedStructuredTypeContextSpec() =
            """|<h3>types</h3><div id='types'/><br/>
               |<div id='a'/>&nbsp;&nbsp;a&nbsp;::&nbsp;<br/>
               |&nbsp;&nbsp;&nbsp;&nbsp;b&nbsp;:&nbsp;ℤ<br/>
               |&nbsp;&nbsp;&nbsp;&nbsp;c&nbsp;:&nbsp;ℝ<br/>
               |<h3>values</h3><div id='values'/><br/>
               |&nbsp;&nbsp;d&nbsp;=&nbsp;<b>mk_</b>a(1,&nbsp;2.0)<br/>
               |<br/>
               |""".trimMargin()

    override fun getExpectedAQuoteType() = "<b>&lt;abc&gt;</b>"

    override fun getExpectedAUnionType() = "<i>char</i>&nbsp;|&nbsp;ℝ"

    override fun getExpectedANamedInvariantType() = "a"

    override fun getExpectedATypeDefinition() = "a&nbsp;=&nbsp;<i>char</i>&nbsp;|&nbsp;ℝ"

    override fun getExpectedAIfExp() =
            """|<b>if</b>&nbsp;b<br/>
               |<b>then</b>&nbsp;<i>false</i><br/>
               |<b>else</b>&nbsp;<i>true</i>""".trimMargin()

    override fun getExpectedALetDefExp() =
            """|<b>let</b><br/>
               |&nbsp;&nbsp;b&nbsp;=&nbsp;1,<br/>
               |&nbsp;&nbsp;c&nbsp;=&nbsp;2<br/>
               |<b>in</b><br/>
               |&nbsp;&nbsp;b&nbsp;+&nbsp;c""".trimMargin()

    override fun getExpectedAExplicitFunctionDefinition() =
            """|fun1:&nbsp;${BOOL}&nbsp;→&nbsp;${BOOL}<br/>
               |fun1(b)&nbsp;⧋<br/>
               |&nbsp;&nbsp;<b>if</b>&nbsp;b<br/>
               |&nbsp;&nbsp;<b>then</b>&nbsp;<i>false</i><br/>
               |&nbsp;&nbsp;<b>else</b>&nbsp;<i>true</i>""".trimMargin()

    override fun getExpectedAImplicitFunctionDefinition() =
            """|Log10(number:&nbsp;ℝ)&nbsp;result:&nbsp;ℝ&nbsp;⧋<br/>
               |&nbsp;&nbsp;Log(number,&nbsp;10)<br/>
               |<b>post</b>&nbsp;10&nbsp;↑&nbsp;result&nbsp;=&nbsp;number""".trimMargin()

    override fun getExpectedAPatternTypePair() = "result:&nbsp;ℝ"

    override fun getExpectedAPatternListTypePair() = "number:&nbsp;ℝ"

    override fun getRenderStrategy() = MathematicalUnicodeHtmlRenderStrategy()

    override fun getExpectedAIntLiteralExp() = "3"

    override fun getExpectedARealLiteralExp() = "3.5"

    override fun getExpectedAIotaExp() = "ι&nbsp;z:&nbsp;ℝ&nbsp;•&nbsp;z&nbsp;=&nbsp;3"

    override fun getExpectedATypeBind() = "z:&nbsp;ℝ"

    override fun getExpectedAIntNumericBasicType() = "ℤ"

    override fun getExpectedAValueDefinition() = "a:&nbsp;ℤ&nbsp;=&nbsp;3"

    override fun getExpectedAUnaryMinusUnaryExp() = "-3"

    override fun getExpectedAIsExp() = "<b>is_</b>(3,&nbsp;ℕ)"

    override fun getExpectedAFloorUnaryExp() = "⌊3.5⌋"
}