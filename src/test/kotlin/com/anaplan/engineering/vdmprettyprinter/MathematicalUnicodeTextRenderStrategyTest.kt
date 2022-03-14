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

import com.anaplan.engineering.vdmprettyprinter.MathematicalUnicodeTextRenderStrategy.Companion.CHAR
import com.anaplan.engineering.vdmprettyprinter.MathematicalUnicodeTextRenderStrategy.Companion.ELSE
import com.anaplan.engineering.vdmprettyprinter.MathematicalUnicodeTextRenderStrategy.Companion.FALSE
import com.anaplan.engineering.vdmprettyprinter.MathematicalUnicodeTextRenderStrategy.Companion.IF
import com.anaplan.engineering.vdmprettyprinter.MathematicalUnicodeTextRenderStrategy.Companion.IN
import com.anaplan.engineering.vdmprettyprinter.MathematicalUnicodeTextRenderStrategy.Companion.IS
import com.anaplan.engineering.vdmprettyprinter.MathematicalUnicodeTextRenderStrategy.Companion.LET
import com.anaplan.engineering.vdmprettyprinter.MathematicalUnicodeTextRenderStrategy.Companion.MK_
import com.anaplan.engineering.vdmprettyprinter.MathematicalUnicodeTextRenderStrategy.Companion.POST
import com.anaplan.engineering.vdmprettyprinter.MathematicalUnicodeTextRenderStrategy.Companion.THEN
import com.anaplan.engineering.vdmprettyprinter.MathematicalUnicodeTextRenderStrategy.Companion.TRUE
import com.anaplan.engineering.vdmprettyprinter.MathematicalUnicodeTextRenderStrategy.Companion.TYPES
import com.anaplan.engineering.vdmprettyprinter.MathematicalUnicodeTextRenderStrategy.Companion.VALUES

class MathematicalUnicodeTextRenderStrategyTest : VdmPrettyPrinterTest() {

    companion object {
        private const val BOOL = "\uD835\uDD39"
    }

    override fun getExpectedNamedInvariantTypeContextSpec() =
        """|$TYPES
           |
           |  a = $CHAR | ℝ
           |
           |$VALUES
           |
           |  b: a = 0
           |
           |
           |""".trimMargin()

    override fun getExpectedStructuredTypeContextSpec() =
        """|$TYPES
           |
           |  a :: 
           |    b : ℤ
           |    c : ℝ
           |
           |$VALUES
           |
           |  d = ${MK_}a(1, 2.0)
           |
           |
           |""".trimMargin()


    override fun getExpectedAQuoteType() = "<abc>"

    override fun getExpectedAUnionType() = "$CHAR | ℝ"

    override fun getExpectedANamedInvariantType() = "a"

    override fun getExpectedATypeDefinition() = "a = $CHAR | ℝ"

    override fun getExpectedAIfExp() =
        """|$IF b
           |$THEN $FALSE
           |$ELSE $TRUE""".trimMargin()

    override fun getExpectedALetDefExp() =
        """|$LET
           |  b = 1,
           |  c = 2
           |$IN
           |  b + c""".trimMargin()

    override fun getExpectedAExplicitFunctionDefinition() =
        """|fun1: ${BOOL} → ${BOOL}
           |fun1(b) ⧋
           |  $IF b
           |  $THEN $FALSE
           |  $ELSE $TRUE""".trimMargin()

    override fun getExpectedAImplicitFunctionDefinition() =
        "" +
            "Log10(number: ℝ) result: ℝ ⧋\n" +
            "  Log(number, 10)\n" +
            "$POST 10 ↑ result = number"

    override fun getExpectedAPatternTypePair() = "result: ℝ"

    override fun getExpectedAPatternListTypePair() = "number: ℝ"

    override fun getRenderStrategy() = MathematicalUnicodeTextRenderStrategy()

    override fun getExpectedAIntLiteralExp() = "3"

    override fun getExpectedARealLiteralExp() = "3.5"

    override fun getExpectedAIotaExp() = "ι z: ℝ • z = 3"

    override fun getExpectedATypeBind() = "z: ℝ"

    override fun getExpectedAIntNumericBasicType() = "ℤ"

    override fun getExpectedAValueDefinition() = "a: ℤ = 3"

    override fun getExpectedAUnaryMinusUnaryExp() = "-3"

    override fun getExpectedAIsExp() = "$IS(3, ℕ)"

    override fun getExpectedAFloorUnaryExp() = "⌊3.5⌋"
}
