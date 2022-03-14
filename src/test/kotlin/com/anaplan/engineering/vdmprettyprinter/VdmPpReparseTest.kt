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

import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.overture.interpreter.VDMPP
import org.overture.interpreter.util.ClassListInterpreter
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Flow of this test is to:
 *   1. parse known example
 *   2. pretty print using plain ascii render strategy
 *   3. parse rendering
 *   4. pretty again
 *   5. verify that result after steps 2 and 4 is identical
 *
 * The intent is to ensure that all examples 'can' be pretty printed, that the result of that pretty printing is valid,
 * and that the result of the pretty print is deterministic.
 */
@RunWith(Parameterized::class)
class VdmPpReparseTest(val exampleDir: String) {

    companion object {
        val baseDirectory: File by lazy {
            File(VdmPpReparseTest::class.java.getResource("/vdmpp-examples").toURI())
        }

        const val vdmPpFileExtension = "vdmpp"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun example() = locateImmediateSubDirectories(baseDirectory).map { arrayOf<Any>(it.name) }
    }

    @Test
    fun vdmReparseTest() {
        val inputDirectory = baseDirectory.resolve(exampleDir)
        checkIfShouldIgnore(inputDirectory)
        val firstParseResult = parseAndPrettyPrintVdmPp(inputDirectory)
        val secondParseResult = parseAndPrettyPrintVdmPp(firstParseResult.prettyPrintOutputDirectory)
        checkClasses(firstParseResult.classes, secondParseResult.classes)
        checkRenderings(
            firstParseResult.prettyPrintOutputDirectory,
            secondParseResult.prettyPrintOutputDirectory,
            vdmPpFileExtension
        )
    }

    private fun parseAndPrettyPrintVdmPp(inputDirectory: File): ClassParseAndPrettyPrintResult {
        val vdmpp = VDMPP()
        parseFilesInDirectory(vdmpp, inputDirectory, vdmPpFileExtension)
        return prettyPrintClasses(vdmpp.interpreter.classes)
    }

}

internal data class ClassParseAndPrettyPrintResult(
    val classes: ClassListInterpreter,
    val prettyPrintOutputDirectory: File
)

internal fun prettyPrintClasses(classes: ClassListInterpreter): ClassParseAndPrettyPrintResult {
    val prettyPrinter = VdmPrettyPrinter(renderStrategy = PlainAsciiTextRenderStrategy())
    val outputDirectory = Files.createTempDirectory(VdmPpReparseTest.vdmPpFileExtension)
    classes.forEach { clazz ->
        outputDirectory.resolve("${clazz.name.name}.${VdmPpReparseTest.vdmPpFileExtension}").toFile().writeText(
            prettyPrinter.prettyPrint(
                clazz,
                config = PrettyPrintConfig(logUnhandledCases = true, minListLengthToUseNls = 10)
            )
        )
    }
    return ClassParseAndPrettyPrintResult(
        prettyPrintOutputDirectory = outputDirectory.toFile(),
        classes = classes
    )
}

internal fun checkClasses(classes1: ClassListInterpreter, classes2: ClassListInterpreter) {
    assertEquals(classes1.map { it.name.name }.toSet(), classes2.map { it.name.name }.toSet())
    classes1.forEach { m1 ->
        val m2 = classes2.find { it.name.name == m1.name.name }
        assertEquals(m1, m2)
    }
}
