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

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.overture.interpreter.VDMRT
import java.io.File

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
class VdmRtReparseTest(val exampleDir: String) {

    companion object {
        val baseDirectory: File by lazy {
            File(VdmRtReparseTest::class.java.getResource("/vdmrt-examples").toURI())
        }

        const val vdmRtFileExtension = "vdmrt"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun example() = locateImmediateSubDirectories(baseDirectory).map { arrayOf<Any>(it.name) }
    }

    @Test
    fun vdmReparseTest() {
        val inputDirectory = baseDirectory.resolve(exampleDir)
        checkIfShouldIgnore(inputDirectory)
        val firstParseResult = parseAndPrettyPrintVdmRt(inputDirectory)
        val secondParseResult = parseAndPrettyPrintVdmRt(firstParseResult.prettyPrintOutputDirectory)
        checkClasses(firstParseResult.classes, secondParseResult.classes)
        checkRenderings(firstParseResult.prettyPrintOutputDirectory, secondParseResult.prettyPrintOutputDirectory, vdmRtFileExtension)
    }

    private fun parseAndPrettyPrintVdmRt(inputDirectory: File): ClassParseAndPrettyPrintResult {
        val vdmrt = VDMRT()
        parseFilesInDirectory(vdmrt, inputDirectory, vdmRtFileExtension)
        return prettyPrintClasses(vdmrt.interpreter.classes)
    }
}