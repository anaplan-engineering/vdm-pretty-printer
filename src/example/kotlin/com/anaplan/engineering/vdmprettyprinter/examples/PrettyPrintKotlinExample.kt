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
package com.anaplan.engineering.vdmprettyprinter.examples

import com.anaplan.engineering.vdmprettyprinter.MathematicalUnicodeHtmlRenderStrategy
import com.anaplan.engineering.vdmprettyprinter.VdmPrettyPrinter
import org.overture.interpreter.VDMSL
import java.io.File

class PrettyPrintKotlinExample {

    fun run() {
        // parse file
        val specFile = File(PrettyPrintKotlinExample::class.java.getResource("example.vdmsl").toURI())
        val vdmsl = VDMSL()
        vdmsl.parse(listOf(specFile))
        vdmsl.typeCheck()

        // locate the module we want to pretty print
        val module = vdmsl.interpreter.modules.first()

        // pretty print
        val prettyPrinter = VdmPrettyPrinter(renderStrategy = MathematicalUnicodeHtmlRenderStrategy())
        println(prettyPrinter.prettyPrint(module))
    }

}

fun main(args: Array<String>) = PrettyPrintKotlinExample().run()