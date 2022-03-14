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

import org.overture.ast.definitions.AValueDefinition
import org.overture.interpreter.VDMSL
import org.overture.interpreter.util.ExitStatus
import java.nio.file.Files
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class OperatorPrecedenceTest {

    private interface Operator {
        val op: String
        val precedenceLevel: Int
        fun apply(vararg args: String): String
    }

    data class BinaryOperator(
        override val op: String,
        override val precedenceLevel: Int
    ) : Operator {
        override fun apply(vararg args: String) = "${args[0]} $op ${args[1]}"
    }

    data class UnaryOperator(
        override val op: String,
        override val precedenceLevel: Int
    ) : Operator {
        override fun apply(vararg args: String) = "$op${args[0]}"
    }

    private val arithmeticOperators = listOf<Operator>(
        BinaryOperator("+", 1),
        BinaryOperator("-", 1),
        BinaryOperator("*", 2),
        BinaryOperator("/", 2),
        BinaryOperator("rem", 2),
        BinaryOperator("mod", 2),
        BinaryOperator("div", 2),
        UnaryOperator("+", 6),
        UnaryOperator("-", 6),
        UnaryOperator("abs ", 6),
        UnaryOperator("floor ", 6)
    )

    private val setOperators = listOf<Operator>(
        BinaryOperator("union", 1),
        BinaryOperator("\\", 1),
        BinaryOperator("inter", 2),
        UnaryOperator("card ", 6),
        UnaryOperator("power ", 6),
        UnaryOperator("dinter ", 6),
        UnaryOperator("dunion ", 6)
    )

    @Test
    fun evaluators_arithmetic() = checkOperators(arithmeticOperators, listOf("1", "2", "3"))

    @Test
    fun explicitExpressionPrecedence_forall_1() =
        verifyExpression("forall b : nat & b > 2 and true", "forall b: nat & b > 2 and true")

    @Test
    fun explicitExpressionPrecedence_forall_2() =
        verifyExpression("(forall b : nat & b > 2) and true", "(forall b: nat & b > 2) and true")

    @Test
    fun explicitExpressionPrecedence_forall_3() =
        verifyExpression("(forall b : nat & b > 2 and true)", "forall b: nat & b > 2 and true")

    @Test
    fun explicitExpressionPrecedence_forall_4() = verifyExpression(
        "forall b : nat & forall c : nat & c > b and b > 5",
        "forall b: nat & forall c: nat & c > b and b > 5"
    )

    @Test
    fun explicitExpressionPrecedence_forall_5() = verifyExpression(
        "forall b : nat & (forall c : nat & c > b) and b > 5",
        "forall b: nat & (forall c: nat & c > b) and b > 5"
    )

    @Test
    fun explicitExpressionPrecedence_forall_6() = verifyExpression(
        "forall b : nat & forall c : nat & (c > b and b > 5)",
        "forall b: nat & forall c: nat & c > b and b > 5"
    )

    @Test
    fun explicitExpressionPrecedence_forall_7() = verifyExpression(
        "forall b : nat & exists c : nat & c > b and b > 5",
        "forall b: nat & exists c: nat & c > b and b > 5"
    )

    @Test
    fun explicitExpressionPrecedence_forall_8() = verifyExpression(
        "forall b : nat & (exists c : nat & c > b) and b > 5",
        "forall b: nat & (exists c: nat & c > b) and b > 5"
    )

    @Test
    fun explicitExpressionPrecedence_forall_9() = verifyExpression(
        "forall b : nat & exists c : nat & (c > b and b > 5)",
        "forall b: nat & exists c: nat & c > b and b > 5"
    )

    // TODO -- can we eliminate brackets for 1 and 2?
    @Test
    fun forall_greed_1() = verifyExpression(
        "forall x: nat & x > 1 and forall y: nat & y > 1",
        "forall x: nat & x > 1 and (forall y: nat & y > 1)"
    )

    @Test
    fun forall_greed_2() = verifyExpression(
        "forall x: nat & x > 1 and forall y: nat & y > 1 and 2 > 1",
        "forall x: nat & x > 1 and (forall y: nat & y > 1 and 2 > 1)"
    )

    @Test
    fun forall_greed_3() = verifyExpression(
        "forall x: nat & x > 1 and (forall y: nat & y > 1) and 2 > 1",
        "forall x: nat & x > 1 and (forall y: nat & y > 1) and 2 > 1"
    )

    @Test
    fun connective_grouping_1() = verifyExpression("true and false and true", "true and false and true")

    @Test
    fun connective_grouping_2() = verifyExpression("true or false or true", "true or false or true")

    @Test
    fun connective_grouping_3() = verifyExpression("true or (false and true)", "true or false and true")

    @Test
    fun connective_grouping_4() = verifyExpression("(true or false) and true", "(true or false) and true")

    @Test
    fun connective_grouping_5() = verifyExpression("true or (false and true) or true", "true or false and true or true")

    @Test
    fun connective_grouping_6() =
        verifyExpression("(true or false) and (true or true)", "(true or false) and (true or true)")

    @Test
    fun connective_grouping_7() =
        verifyExpression("true or (false and (true or true))", "true or false and (true or true)")

    @Test
    fun connective_grouping_8() =
        verifyExpression("(true or (false and true)) or true", "true or false and true or true")

    @Test
    fun connective_grouping_9() = verifyExpression(
        "(false and false or true) and (false or true)",
        "(false and false or true) and (false or true)"
    )

    @Test
    fun applicators_apply_1() = verifyExpression("(inverse { 1 |-> 2, 2 |-> 3 })(3)", "(inverse {1 |-> 2, 2 |-> 3})(3)")

    @Test
    fun applicators_apply_2() = verifyExpression("{ 1 |-> 2, 2 |-> 3 }(1)", "{1 |-> 2, 2 |-> 3}(1)")

    @Test
    fun applicators_subsequence_1() =
        verifyExpression("(reverse [6,7,3,8])(1,...,3)", "(reverse [6, 7, 3, 8])(1, ..., 3)")

    @Test
    fun applicators_subsequence_2() = verifyExpression("[6,7,3,8](1, ..., 3)", "[6, 7, 3, 8](1, ..., 3)")

    // TODO - create a test that makes the tuple dynamically
    @Test
    fun applicators_fieldSelect_2() = verifyExpression("mk_(1,2,3).#2", "mk_(1, 2, 3).#2")

    // Language reference states that these operators have equal precedence and there is no sensible grouping,
    // below tests show that right grouping is used in Overture
    @Test
    fun equalityInequality_1() = verifyExpression("(1 <> 2) = false", "(1 <> 2) = false")

    @Test
    fun equalityInequality_2() = verifyExpression("false = 1 <> 2", "false = 1 <> 2")

    @Test
    fun equalityInequality_3() = verifyExpression("false = (1 <> 2)", "false = 1 <> 2")

    @Test
    @Ignore
    // TODO - resolve typing issues!
    fun evaluators_set() = checkOperators(setOperators, listOf("{1}", "{2}", "{3}"))

    private fun checkOperators(operators: List<Operator>, values: List<String>) {
        operators.forEach { op1 ->
            operators.forEach { op2 ->
                checkNatural(op1, op2, values)
            }
        }
    }

    private fun checkNatural(op1: Operator, op2: Operator, values: List<String>) {
        val l = if (op1 is UnaryOperator && op2 is UnaryOperator) {
            "(${op1.apply(values[0], values[1])})"
        } else {
            op1.apply(values[0], values[1])
        }
        val lr = op2.apply(l, values[2])
        verifyExpression(lr, lr)
    }

    private fun verifyExpression(raw: String, expected: String) {
        val tempFile = Files.createTempFile("test-", ".vdmsl").toFile()
        tempFile.writeText("values\na = $raw")
        val vdmsl = VDMSL()
        val parseResult = vdmsl.parse(listOf(tempFile))
        assertEquals(ExitStatus.EXIT_OK, parseResult, "Unable to parse $raw")
        val typeCheckResult = vdmsl.typeCheck()
        assertEquals(ExitStatus.EXIT_OK, typeCheckResult, "Unable to type check $raw")
        val module = vdmsl.interpreter.defaultModule
        val valueDefinition = module.defs.filter { it is AValueDefinition }.map { it as AValueDefinition }.first()
        val actual = VdmPrettyPrinter().prettyPrint(
            valueDefinition.expression,
            config = PrettyPrintConfig(includeHeaderFooter = false)
        )
        assertEquals(expected, actual)
    }
}
