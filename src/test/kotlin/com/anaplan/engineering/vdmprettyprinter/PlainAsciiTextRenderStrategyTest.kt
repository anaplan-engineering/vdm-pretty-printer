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

class PlainAsciiTextRenderStrategyTest : VdmPrettyPrinterTest() {
    override fun getExpectedNamedInvariantTypeContextSpec() =
            """|types
               |
               |  a = char | real
               |
               |values
               |
               |  b: a = 0
               |
               |
               |""".trimMargin()

    override fun getExpectedStructuredTypeContextSpec() =
            """|types
               |
               |  a :: 
               |    b : int
               |    c : real
               |
               |values
               |
               |  d = mk_a(1, 2.0)
               |
               |
               |""".trimMargin()

    override fun getExpectedAQuoteType() = "<abc>"

    override fun getExpectedAUnionType() = "char | real"

    override fun getExpectedANamedInvariantType() = "a"

    override fun getExpectedATypeDefinition() = "a = char | real"

    override fun getExpectedAIfExp() =
            """|if b
               |then false
               |else true""".trimMargin()

    override fun getExpectedALetDefExp() =
            """|let
               |  b = 1,
               |  c = 2
               |in
               |  b + c""".trimMargin()

    override fun getExpectedAExplicitFunctionDefinition() =
            """|fun1: bool -> bool
               |fun1(b) ==
               |  if b
               |  then false
               |  else true""".trimMargin()

    override fun getExpectedAImplicitFunctionDefinition() =
            "" +
                    "Log10(number: real) result: real ==\n" +
                    "  Log(number, 10)\n" +
                    "post 10 ** result = number"

    override fun getExpectedAPatternTypePair() = "result: real"

    override fun getExpectedAPatternListTypePair() = "number: real"

    override fun getRenderStrategy() = PlainAsciiTextRenderStrategy()

    override fun getExpectedAIntLiteralExp() = "3"

    override fun getExpectedARealLiteralExp() = "3.5"

    override fun getExpectedAIotaExp() = "iota z: real & z = 3"

    override fun getExpectedATypeBind() = "z: real"

    override fun getExpectedAIntNumericBasicType() = "int"

    override fun getExpectedAValueDefinition() = "a: int = 3"

    override fun getExpectedAUnaryMinusUnaryExp() = "-3"

    override fun getExpectedAIsExp() = "is_(3, nat)"

    override fun getExpectedAFloorUnaryExp() = "floor 3.5"

}