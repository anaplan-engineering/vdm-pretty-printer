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

import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.overture.ast.definitions.AExplicitFunctionDefinition
import org.overture.ast.definitions.AImplicitFunctionDefinition
import org.overture.ast.definitions.ATypeDefinition
import org.overture.ast.definitions.AValueDefinition
import org.overture.ast.expressions.*
import org.overture.ast.node.INode
import org.overture.ast.patterns.APatternListTypePair
import org.overture.ast.patterns.APatternTypePair
import org.overture.ast.patterns.ATypeBind
import org.overture.ast.types.AIntNumericBasicType
import org.overture.ast.types.ANamedInvariantType
import org.overture.ast.types.AQuoteType
import org.overture.ast.types.AUnionType
import org.overture.interpreter.VDMSL
import java.io.File
import java.nio.file.Files

abstract class VdmPrettyPrinterTest {

    @Test
    fun caseAIntLiteralExp() {
        val valueDefinition = onlyValue(intValueSpec)
        assertEquals(AIntLiteralExp::class.java, valueDefinition.expression.javaClass)
        assertEquals(getExpectedAIntLiteralExp(), prettyPrint(valueDefinition.expression))
    }

    @Test
    fun caseAIotaExp() {
        val valueDefinition = onlyValue(iotaValueSpec)
        assertEquals(AIotaExp::class.java, valueDefinition.expression.javaClass)
        assertEquals(getExpectedAIotaExp(), prettyPrint(valueDefinition.expression))
    }

    @Test
    fun caseATypeBind() {
        val iotaExp = onlyValue(iotaValueSpec).expression as AIotaExp
        val typeBind = iotaExp.bind
        assertEquals(ATypeBind::class.java, typeBind.javaClass)
        assertEquals(getExpectedATypeBind(), prettyPrint(typeBind))
    }

    @Test
    fun caseARealLiteralExp() {
        val valueDefinition = onlyValue(realValueSpec)
        assertEquals(ARealLiteralExp::class.java, valueDefinition.expression.javaClass)
        assertEquals(getExpectedARealLiteralExp(), prettyPrint(valueDefinition.expression))
    }

    @Test
    fun caseAUnaryMinusUnaryExp() {
        val valueDefinition = onlyValue(negativeIntValueSpec)
        assertEquals(AUnaryMinusUnaryExp::class.java, valueDefinition.expression.javaClass)
        assertEquals(getExpectedAUnaryMinusUnaryExp(), prettyPrint(valueDefinition.expression))
    }

    @Test
    fun caseAIsExp() {
        val valueDefinition = onlyValue(isValueSpec)
        assertEquals(AIsExp::class.java, valueDefinition.expression.javaClass)
        assertEquals(getExpectedAIsExp(), prettyPrint(valueDefinition.expression))
    }

    @Test
    fun namedInvariantTypeContextSpec() {
        val module = defaultModule(namedInvariantTypeContextSpec)
        assertEquals(getExpectedNamedInvariantTypeContextSpec(), prettyPrint(module))
    }

    @Test
    fun structuredTypeContextSpec() {
        val module = defaultModule(structuredTypeContextSpec)
        assertEquals(getExpectedStructuredTypeContextSpec(), prettyPrint(module))
    }

    @Test
    fun caseAFloorUnaryExp() {
        val valueDefinition = onlyValue(floorValueSpec)
        assertEquals(AFloorUnaryExp::class.java, valueDefinition.expression.javaClass)
        assertEquals(getExpectedAFloorUnaryExp(), prettyPrint(valueDefinition.expression))
    }

    @Test
    fun caseAUnionType() {
        val namedInvariantType = namedType(unionTypeSpec, "a").type as ANamedInvariantType
        assertEquals(AUnionType::class.java, namedInvariantType.type.javaClass)
        assertEquals(getExpectedAUnionType(), prettyPrint(namedInvariantType.type))
    }

    @Test
    fun caseAQuoteType() {
        val namedInvariantType = namedType(quoteTypeSpec, "a").type as ANamedInvariantType
        val unionType = namedInvariantType.type as AUnionType
        assertEquals(AQuoteType::class.java, unionType.types.first.javaClass)
        assertEquals(getExpectedAQuoteType(), prettyPrint(unionType.types.first))
    }

    @Test
    fun caseANamedInvariantType() {
        val typeDefinition = namedType(unionTypeSpec, "a")
        assertEquals(ANamedInvariantType::class.java, typeDefinition.type.javaClass)
        assertEquals(getExpectedANamedInvariantType(), prettyPrint(typeDefinition.type))
    }

    @Test
    fun caseATypeDefinition() {
        val typeDefinition = namedType(unionTypeSpec, "a")
        assertEquals(getExpectedATypeDefinition(), prettyPrint(typeDefinition))
    }

    @Test
    fun caseAIntNumericBasicType() {
        val valueDefinition = onlyValue(intValueSpec)
        assertEquals(AIntNumericBasicType::class.java, valueDefinition.type.javaClass)
        assertEquals(getExpectedAIntNumericBasicType(), prettyPrint(valueDefinition.type))
    }

    @Test
    fun caseAValueDefinition() {
        assertEquals(getExpectedAValueDefinition(), prettyPrint(onlyValue(intValueSpec)))
    }

    @Test
    fun caseALetDefExp() {
        val valueDefinition = onlyValue(letValueSpec)
        assertEquals(ALetDefExp::class.java, valueDefinition.expression.javaClass)
        assertEquals(getExpectedALetDefExp(), prettyPrint(valueDefinition.expression))
    }

    @Test
    fun caseAIfExp() {
        val functionDefinition = onlyExplicitFunction(explicitFunctionSpec)
        assertEquals(AIfExp::class.java, functionDefinition.body.javaClass)
        assertEquals(getExpectedAIfExp(), prettyPrint(functionDefinition.body))
    }

    @Test
    fun caseAExplicitFunctionDefinition() {
        val functionDefinition = onlyExplicitFunction(explicitFunctionSpec)
        assertEquals(getExpectedAExplicitFunctionDefinition(), prettyPrint(functionDefinition))
    }

    @Test
    fun caseAImplicitFunctionDefinition() {
        val functionDefinition = onlyImplicitFunction(implicitFunctionSpec)
        assertEquals(getExpectedAImplicitFunctionDefinition(), prettyPrint(functionDefinition))
    }

    @Test
    fun caseAPatternTypePair() {
        val functionDefinition = onlyImplicitFunction(implicitFunctionSpec)
        assertEquals(APatternTypePair::class.java, functionDefinition.result.javaClass)
        assertEquals(getExpectedAPatternTypePair(), prettyPrint(functionDefinition.result))
    }

    @Test
    fun caseAPatternListTypePair() {
        val functionDefinition = onlyImplicitFunction(implicitFunctionSpec)
        assertEquals(APatternListTypePair::class.java, functionDefinition.paramPatterns.first.javaClass)
        assertEquals(getExpectedAPatternListTypePair(), prettyPrint(functionDefinition.paramPatterns.first))
    }

    private fun prettyPrint(node: INode) =
            VdmPrettyPrinter(renderStrategy = getRenderStrategy()).prettyPrint(node, config = PrettyPrintConfig(includeHeaderFooter = false))

    private fun onlyExplicitFunction(spec: String) = onlyDefinition(spec, AExplicitFunctionDefinition::class.java)
    private fun onlyImplicitFunction(spec: String) = onlyDefinition(spec, AImplicitFunctionDefinition::class.java)
    private fun onlyValue(spec: String) = onlyDefinition(spec, AValueDefinition::class.java)
    private fun onlyType(spec: String) = onlyDefinition(spec, ATypeDefinition::class.java)
    private fun namedType(spec: String, name: String) = namedDefinition(spec, name, ATypeDefinition::class.java)

    private fun <T> onlyDefinition(spec: String, defClass: Class<T>): T {
        val matchingDefs = defaultModule(spec).defs.filter { it.javaClass == defClass }
        assertEquals(1, matchingDefs.size)
        @Suppress("UNCHECKED_CAST") return matchingDefs.first() as T
    }

    private fun <T> namedDefinition(spec: String, name: String, defClass: Class<T>): T {
        val matchingDefs = defaultModule(spec).defs.filter { it.javaClass == defClass && it.name.name == name }
        assertEquals(1, matchingDefs.size)
        @Suppress("UNCHECKED_CAST") return matchingDefs.first() as T
    }

    private fun defaultModule(spec: String) = vdmsl(spec).interpreter.defaultModule

    abstract fun getRenderStrategy(): IRenderStrategy
    abstract fun getExpectedAIntLiteralExp(): String
    abstract fun getExpectedAIotaExp(): String
    abstract fun getExpectedARealLiteralExp(): String
    abstract fun getExpectedAIntNumericBasicType(): String
    abstract fun getExpectedAValueDefinition(): String
    abstract fun getExpectedAIfExp(): String
    abstract fun getExpectedALetDefExp(): String
    abstract fun getExpectedAExplicitFunctionDefinition(): String
    abstract fun getExpectedAImplicitFunctionDefinition(): String
    abstract fun getExpectedAUnaryMinusUnaryExp(): String
    abstract fun getExpectedAIsExp(): String
    abstract fun getExpectedATypeBind(): String
    abstract fun getExpectedAPatternTypePair(): String
    abstract fun getExpectedAPatternListTypePair(): String
    abstract fun getExpectedAFloorUnaryExp(): String
    abstract fun getExpectedAUnionType(): String
    abstract fun getExpectedANamedInvariantType(): String
    abstract fun getExpectedATypeDefinition(): String
    abstract fun getExpectedAQuoteType(): String
    abstract fun getExpectedStructuredTypeContextSpec(): String
    abstract fun getExpectedNamedInvariantTypeContextSpec(): String
}

const val unionTypeSpec = "types a = char | real"

const val quoteTypeSpec = "types a = <abc> | <def>"

const val namedInvariantTypeContextSpec = """
types a = char | real
values b : a = 0
"""

const val structuredTypeContextSpec = """
types a :: b : int
           c : real
values d = mk_a(1,2.0)
"""

const val intValueSpec = "values a : int = 3"

const val floorValueSpec = "values a : int = floor 3.5"

const val realValueSpec = "values a : real = 3.5"

const val iotaValueSpec = "values a = iota z : real & z = 3"

const val isValueSpec = "values a : bool = is_nat(3)"

const val negativeIntValueSpec = "values a : int = -3"

const val explicitFunctionSpec = """
functions
fun1: bool ->  bool
fun1(b) == if b then false else true
"""

const val letValueSpec = """
values
a = let
b = 1, c= 2
in b +c
"""

const val implicitFunctionSpec = """
functions
Log10 (number : real) result : real
== Log(number, 10)
post 10**result = number
"""

private fun vdmsl(spec: String): VDMSL {
    val tempFile = Files.createTempFile("test-", ".vdmsl").toFile()
    tempFile.writeText(spec)
    val vdmsl = VDMSL()
    vdmsl.parse(listOf(tempFile))
    vdmsl.typeCheck()
    return vdmsl
}

// use this for debugging random specs
fun main(args: Array<String>) {
    val file = File(args.first())
    val vdmsl = VDMSL()
    vdmsl.parse(listOf(file))
    vdmsl.typeCheck()
    val module = vdmsl.interpreter.defaultModule
    println(VdmPrettyPrinter(renderStrategy = MathematicalUnicodeHtmlRenderStrategy()).prettyPrint(module))

}