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

data class PrettyPrintConfig (
        /**
         * If true, log to the console if an unhandled node type is encountered
         */
        val logUnhandledCases: Boolean = false,

        /**
         * When a node list is equal to or greater than this value each element will be displayed on a new line
         */
        val minListLengthToUseNls: Int = 5,

        /**
         * If true will wrap rendering in any strategy-specific header and footer
         */
        val includeHeaderFooter: Boolean = true
)