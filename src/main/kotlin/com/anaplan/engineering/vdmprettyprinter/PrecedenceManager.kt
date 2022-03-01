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

import org.overture.ast.expressions.*
import org.overture.ast.statements.AForAllStm
import org.overture.ast.types.*
import kotlin.reflect.KClass

object PrecedenceManager {

    private fun getPrecedenceLevel(node: PType) = getPrecedenceLevel(node::class)

    private fun getPrecedenceLevel(clazz: KClass<out PType>) = typePrecedence[clazz] ?: 0

    private val typePrecedence = mapOf(
        AOperationType::class to 1,
        AFunctionType::class to 2,
        AUnionType::class to 3,
        AProductType::class to 4,
        AMapMapType::class to 5,
        AInMapMapType::class to 6,
        ASeqSeqType::class to 7,
        ASeq1SeqType::class to 7,
        ASetSetType::class to 7,
        ASet1SetType::class to 7
    )

    // See https://github.com/overturetool/overture/issues/682 -- we use Overture's ordering to be consistent
    private const val combinatorFamilyOffset = 60
    private const val applicatorFamilyOffset = 70
    private const val evaluatorFamilyOffset = 50
    private const val relationFamilyOffset = 40
    private const val connectiveFamilyOffset = 30
    private const val constructorFamilyOffset = 20
    private const val otherExpressionFamilyOffset = 10

    private val unaryExpPrecedence = mapOf(
        // arithmetic
        AUnaryPlusUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),
        AUnaryMinusUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),
        AAbsoluteUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),
        AFloorUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),

        // set
        ACardinalityUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),
        APowerSetUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),
        ADistUnionUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),
        ADistIntersectUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),

        // map
        AMapInverseUnaryExp::class to Precedence(evaluatorFamilyOffset + 3, OperatorGrouping.left),
        AMapDomainUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),
        AMapRangeUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),
        ADistMergeUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),

        // sequence
        ALenUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),
        AElementsUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),
        AHeadUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),
        ATailUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),
        ADistConcatUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),
        AIndicesUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),
        AReverseUnaryExp::class to Precedence(evaluatorFamilyOffset + 6, OperatorGrouping.left),

        // connectives
        ANotUnaryExp::class to Precedence(connectiveFamilyOffset + 5, OperatorGrouping.left)
    )

    private enum class OperatorGrouping { left, none, right }

    private data class Precedence(
        val level: Int,
        val grouping: OperatorGrouping
    )

    private val binaryExpPrecedence = mapOf(
        // combinators
        "comp" to Precedence(combinatorFamilyOffset + 1, OperatorGrouping.right),
        "**" to Precedence(combinatorFamilyOffset + 2, OperatorGrouping.right),

        // arithmetic
        "+" to Precedence(evaluatorFamilyOffset + 1, OperatorGrouping.left),
        "-" to Precedence(evaluatorFamilyOffset + 1, OperatorGrouping.left),
        "*" to Precedence(evaluatorFamilyOffset + 2, OperatorGrouping.left),
        "/" to Precedence(evaluatorFamilyOffset + 2, OperatorGrouping.left),
        "rem" to Precedence(evaluatorFamilyOffset + 2, OperatorGrouping.left),
        "mod" to Precedence(evaluatorFamilyOffset + 2, OperatorGrouping.left),
        "div" to Precedence(evaluatorFamilyOffset + 2, OperatorGrouping.left),

        // set
        "union" to Precedence(evaluatorFamilyOffset + 1, OperatorGrouping.left),
        "\\" to Precedence(evaluatorFamilyOffset + 1, OperatorGrouping.left),
        "inter" to Precedence(evaluatorFamilyOffset + 2, OperatorGrouping.left),

        // map
        "munion" to Precedence(evaluatorFamilyOffset + 1, OperatorGrouping.left),
        "++" to Precedence(evaluatorFamilyOffset + 1, OperatorGrouping.left),
        "<:" to Precedence(evaluatorFamilyOffset + 4, OperatorGrouping.left),
        "<-:" to Precedence(evaluatorFamilyOffset + 4, OperatorGrouping.left),
        ":>" to Precedence(evaluatorFamilyOffset + 5, OperatorGrouping.left),
        ":->" to Precedence(evaluatorFamilyOffset + 5, OperatorGrouping.left),

        // sequence
        "^" to Precedence(evaluatorFamilyOffset + 1, OperatorGrouping.left),

        // Language reference states that relation family operators have equal precedence and there is no sensible grouping,
        // Tests show that, in practice, right grouping is used in Overture
        // relation family
        "=" to Precedence(relationFamilyOffset + 1, OperatorGrouping.right),
        "<=" to Precedence(relationFamilyOffset + 1, OperatorGrouping.right),
        ">=" to Precedence(relationFamilyOffset + 1, OperatorGrouping.right),
        ">" to Precedence(relationFamilyOffset + 1, OperatorGrouping.right),
        "<" to Precedence(relationFamilyOffset + 1, OperatorGrouping.right),
        "<>" to Precedence(relationFamilyOffset + 1, OperatorGrouping.right),
        "subset" to Precedence(relationFamilyOffset + 1, OperatorGrouping.none),
        "psubset" to Precedence(relationFamilyOffset + 1, OperatorGrouping.none),
        "in set" to Precedence(relationFamilyOffset + 1, OperatorGrouping.none),
        "not in set" to Precedence(relationFamilyOffset + 1, OperatorGrouping.none),

        // connective family
        "<=>" to Precedence(connectiveFamilyOffset + 1, OperatorGrouping.none),
        "=>" to Precedence(connectiveFamilyOffset + 2, OperatorGrouping.right),
        "or" to Precedence(connectiveFamilyOffset + 3, OperatorGrouping.none),
        "and" to Precedence(connectiveFamilyOffset + 4, OperatorGrouping.none)
    )

    private val explicitExpressionPrecedence = mapOf<KClass<*>, Precedence>(
        // applicators
        AApplyExp::class to Precedence(applicatorFamilyOffset, OperatorGrouping.left),
        AFuncInstatiationExp::class to Precedence(applicatorFamilyOffset, OperatorGrouping.left),
        AFieldExp::class to Precedence(applicatorFamilyOffset, OperatorGrouping.left),
        ASubseqExp::class to Precedence(applicatorFamilyOffset, OperatorGrouping.left),

        // constructors
        ASetRangeSetExp::class to Precedence(constructorFamilyOffset, OperatorGrouping.left),
        AMapletExp::class to Precedence(constructorFamilyOffset, OperatorGrouping.left),
        AIfExp::class to Precedence(constructorFamilyOffset, OperatorGrouping.left),

        // other
        AForAllExp::class to Precedence(otherExpressionFamilyOffset, OperatorGrouping.right),
        AForAllStm::class to Precedence(otherExpressionFamilyOffset, OperatorGrouping.right),
        AExistsExp::class to Precedence(otherExpressionFamilyOffset, OperatorGrouping.right),
        AExists1Exp::class to Precedence(otherExpressionFamilyOffset, OperatorGrouping.right),
        AIotaExp::class to Precedence(otherExpressionFamilyOffset, OperatorGrouping.right),
        ALambdaExp::class to Precedence(otherExpressionFamilyOffset, OperatorGrouping.right),
        AMuExp::class to Precedence(otherExpressionFamilyOffset, OperatorGrouping.right)
    )

    private val tokenPrecedence = Precedence(0, OperatorGrouping.none)

    private fun getPrecedence(exp: PExp) =
        if (exp is SBinaryExp) {
            binaryExpPrecedence[exp.op.toString()]
                ?: throw IllegalStateException("Missing precedence level for binary operator ${exp.op}")
        } else if (exp is SUnaryExp) {
            unaryExpPrecedence[exp::class]
                ?: throw IllegalStateException("Missing precedence level for unary operator ${exp::class}")
        } else {
            explicitExpressionPrecedence[exp::class] ?: tokenPrecedence
        }

    internal fun expRequiresParentheses(child: PExp, parent: PExp, childRight: Boolean): Boolean {
        val childPrecedence = getPrecedence(child)
        val parentPrecedence = getPrecedence(parent)

        return if (childPrecedence.level == 0 || childPrecedence.level > parentPrecedence.level) {
            false
        } else if (childPrecedence.level < parentPrecedence.level) {
            true
        } else { // childPrecedence.level == parentPrecedence.level
            (parentPrecedence.grouping == OperatorGrouping.left && childRight) || (parentPrecedence.grouping == OperatorGrouping.right && !childRight)
        }
    }

    internal fun typeRequiresParentheses(child: PType, parent: PType) =
        getPrecedenceLevel(child) != 0 && getPrecedenceLevel(child) <= getPrecedenceLevel(parent)

    internal fun typeRequiresParentheses(child: PType, parentClass: KClass<out PType>) =
        getPrecedenceLevel(child) != 0 && getPrecedenceLevel(child) <= getPrecedenceLevel(parentClass)
}
