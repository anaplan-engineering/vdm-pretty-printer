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

import kotlin.test.assertEquals
import org.junit.Assume
import kotlin.test.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.overture.interpreter.VDMJ
import org.overture.interpreter.VDMSL
import org.overture.interpreter.util.ExitStatus
import org.overture.interpreter.util.ModuleListInterpreter
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

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
class VdmSlReparseTest(val exampleDir: String) {

    companion object {
        val baseDirectory: File by lazy {
            File(VdmSlReparseTest::class.java.getResource("/vdmsl-examples").toURI())
        }

        const val vdmSlFileExtension = "vdmsl"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun example() = locateImmediateSubDirectories(baseDirectory).map { arrayOf<Any>(it.name) }
    }

    @Test
    fun vdmReparseTest() {
        val inputDirectory = baseDirectory.resolve(exampleDir)
        checkIfShouldIgnore(inputDirectory)
        val firstParseResult = parseAndPrettyPrintVdmSl(inputDirectory)
        val secondParseResult = parseAndPrettyPrintVdmSl(firstParseResult.prettyPrintOutputDirectory)
        checkModules(firstParseResult.modules, secondParseResult.modules)
        checkRenderings(firstParseResult.prettyPrintOutputDirectory, secondParseResult.prettyPrintOutputDirectory, vdmSlFileExtension)
    }

    private fun checkModules(modules1: ModuleListInterpreter, modules2: ModuleListInterpreter) {
        assertEquals(modules1.map { it.name.name }.toSet(), modules2.map { it.name.name }.toSet())
        modules1.forEach { m1 ->
            val m2 = modules2.find { it.name.name == m1.name.name }
            assertEquals(m1, m2)
        }
    }

    private data class ModuleParseAndPrettyPrintResult(
            val modules: ModuleListInterpreter,
            val prettyPrintOutputDirectory: File
    )

    private fun parseAndPrettyPrintVdmSl(inputDirectory: File): ModuleParseAndPrettyPrintResult {
        val vdmsl = VDMSL()
        parseFilesInDirectory(vdmsl, inputDirectory, vdmSlFileExtension)
        val prettyPrinter = VdmPrettyPrinter(renderStrategy = PlainAsciiTextRenderStrategy())
        val outputDirectory = Files.createTempDirectory(vdmSlFileExtension)
        vdmsl.interpreter.modules.forEach { module ->
            outputDirectory.resolve("${module.name.name}.${vdmSlFileExtension}").toFile().writeText(prettyPrinter.prettyPrint(module,
                    config = PrettyPrintConfig(logUnhandledCases = true, minListLengthToUseNls = 10)))
        }
        return ModuleParseAndPrettyPrintResult(
                prettyPrintOutputDirectory = outputDirectory.toFile(),
                modules = vdmsl.interpreter.modules
        )
    }

}

internal fun parseFilesInDirectory(vdmj: VDMJ, directory: File, extension: String) {
    val files = locateFilesWithExtension(directory, extension)
    println("Parsing files:\n * ${files.joinToString("\n * ")}")
    val parseStatus = vdmj.parse(files)
    assertEquals(ExitStatus.EXIT_OK, parseStatus)
    val typeCheckStatus = vdmj.typeCheck()
    assertEquals(ExitStatus.EXIT_OK, typeCheckStatus)
}

internal fun locateImmediateSubDirectories(directory: File): List<File> {
    if (!directory.exists() || !directory.isDirectory) {
        return emptyList()
    }
    val dirs = ArrayList<Path>()
    val fileVisitor = object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            if (dir == directory.toPath()) {
                return FileVisitResult.CONTINUE
            }
            dirs.add(dir)
            return FileVisitResult.SKIP_SUBTREE
        }
    }
    Files.walkFileTree(directory.toPath(), fileVisitor)
    return dirs.map { it.toFile() }
}

internal fun checkRenderings(directory1: File, directory2: File, extension: String) {
    val files1 = locateFilesWithExtension(directory1, extension)
    val files2 = locateFilesWithExtension(directory2, extension)
    assertEquals(files1.map { it.name }.toSet(), files2.map { it.name }.toSet())
    files1.forEach { f1 ->
        val f2 = File("$directory2/${f1.name}")
        assertEquals(f1.readText(), f2.readText())
    }
}

internal fun locateFilesWithExtension(directory: File, vararg extensions: String): List<File> {
    if (!directory.exists() || !directory.isDirectory) {
        return emptyList()
    }
    val files = ArrayList<Path>()
    val fileVisitor = object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
            if (extensions.any { file.fileName.toString().endsWith(it) }) {
                files.add(file)
            }
            return FileVisitResult.CONTINUE
        }
    }
    Files.walkFileTree(directory.toPath(), fileVisitor)
    return files.map { it.toFile() }
}

internal fun checkIfShouldIgnore(baseDirectory: File) {
    val ignoreFile = File(baseDirectory, "ignore")
    val ignore = ignoreFile.exists()
    if (!ignore) {
        return
    }
    println("Ignoring specification in $baseDirectory")
    println(ignoreFile.readText())
    Assume.assumeFalse(ignore)
}