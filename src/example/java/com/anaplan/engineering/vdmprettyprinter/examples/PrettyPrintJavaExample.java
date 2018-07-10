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
package com.anaplan.engineering.vdmprettyprinter.examples;

import com.anaplan.engineering.vdmprettyprinter.PrettyPrintConfig;
import com.anaplan.engineering.vdmprettyprinter.VdmPrettyPrinter;
import org.overture.ast.modules.AModuleModules;
import org.overture.interpreter.VDMSL;

import java.io.File;
import java.util.Arrays;

public class PrettyPrintJavaExample {

    private void run() throws Exception {
        // parse file
        File specFile = new File(PrettyPrintJavaExample.class.getResource("example.vdmsl").toURI());
        VDMSL vdmsl = new VDMSL();
        vdmsl.parse(Arrays.asList(specFile));
        vdmsl.typeCheck();

        // locate the module we want to pretty print
        AModuleModules module = vdmsl.getInterpreter().modules.get(0);

        // pretty print
        VdmPrettyPrinter prettyPrinter = new VdmPrettyPrinter();
        System.out.println(prettyPrinter.prettyPrint(module, new PrettyPrintConfig()));
    }

    public static void main(String[] args) throws Exception {
        new PrettyPrintJavaExample().run();
    }
}
