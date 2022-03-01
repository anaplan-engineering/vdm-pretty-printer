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

import org.overture.ast.definitions.ATypeDefinition
import org.overture.ast.types.ANamedInvariantType
import org.overture.interpreter.VDMSL
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class TypePrecedenceTest {

    @Test
    fun functionType() = verifyType("nat -> nat", "nat -> nat")

    @Test
    fun functionType_rightGroup_explicit() = verifyType("nat -> (int -> char)", "nat -> int -> char")

    @Test
    fun functionType_rightGroup_implicit() = verifyType("nat -> int -> char", "nat -> int -> char")

    @Test
    fun functionType_leftGroup() = verifyType("(nat -> int) -> char", "(nat -> int) -> char")

    @Test
    fun functionType_unionType_plain() = verifyType("char | real | token -> nat", "char | real | token -> nat")

    @Test
    fun functionType_unionType_nestedFn_1() =
        verifyType("char | nat -> char | nat -> nat", "char | nat -> char | nat -> nat")

    @Test
    fun functionType_unionType_nestedFn_2() =
        verifyType("char | (nat -> char) | nat -> nat", "(nat -> char) | char | nat -> nat")

    @Test
    fun functionType_unionType_nestedFn_3() =
        verifyType("(char | nat -> char) | nat -> nat", "(char | nat -> char) | nat -> nat")

    @Test
    fun functionType_unionType_nestedFn_4() =
        verifyType("(char | nat) -> (char | nat) -> nat", "char | nat -> char | nat -> nat")

    @Test
    fun functionType_unionType_nestedFn_5() =
        verifyType("char | nat -> nat | (char -> nat)", "char | nat -> (char -> nat) | nat")

    @Test
    fun functionType_productType_multipleParams() = verifyType("nat * nat * nat -> nat", "nat * nat * nat -> nat")

    @Test
    fun functionType_productType_singleParam() = verifyType("(nat * nat * nat) -> nat", "(nat * nat * nat) -> nat")

    @Test
    fun functionType_productType_nestedFn_1() =
        verifyType("nat * nat -> nat * nat -> nat", "nat * nat -> nat * nat -> nat")

    @Test
    fun functionType_productType_nestedFn_2() =
        verifyType("(nat * nat) -> (nat * nat) -> nat", "(nat * nat) -> (nat * nat) -> nat")

    @Test
    fun functionType_productType_nestedFn_3() =
        verifyType("nat * (nat -> nat) * nat -> nat", "nat * (nat -> nat) * nat -> nat")

    @Test
    fun functionType_productType_nestedFn_4() =
        verifyType("nat * (nat -> nat * (nat -> nat))", "nat * (nat -> nat * (nat -> nat))")

    @Test
    fun functionType_productType_nestedFn_5() =
        verifyType("(nat * nat -> nat) * (nat -> nat)", "(nat * nat -> nat) * (nat -> nat)")

    @Test
    fun unionType_nestedUnion_1() = verifyType("char | (real | token)", "char | real | token")

    @Test
    fun unionType_nestedUnion_2() = verifyType("(char | real) | token", "char | real | token")

    @Test
    fun unionType_productType() = verifyType("char | real * token", "real * token | char")

    @Test
    fun productType_noGrouping_1() = verifyType("(char * real) * token", "(char * real) * token")

    @Test
    fun productType_noGrouping_2() = verifyType("char * (real * token)", "char * (real * token)")

    @Test
    fun productType_inFunction_noGrouping_1() =
        verifyType("(nat * nat) * char * real +> bool", "(nat * nat) * char * real +> bool")

    @Test
    fun productType_inFunction_noGrouping_2() =
        verifyType("nat * nat * char * real +> bool", "nat * nat * char * real +> bool")

    @Test
    fun productType_inFunction_noGrouping_3() =
        verifyType("nat * (nat * char) * real +> bool", "nat * (nat * char) * real +> bool")

    @Test
    fun productType_unionType_1() = verifyType("char | real * token", "real * token | char")

    @Test
    fun productType_unionType_2() = verifyType("(char | real) * token", "(char | real) * token")

    @Test
    fun productType_unionType_3() = verifyType("char | (real * token) | nat", "real * token | char | nat")

    @Test
    fun productType_unionType_4() = verifyType("(char * real) | (token * nat)", "char * real | token * nat")

    @Test
    fun mapType_plain() = verifyType("map nat to nat", "map nat to nat")

    @Test
    fun mapType_productType_1() = verifyType("map nat * nat to nat * nat", "map nat * nat to nat * nat")

    @Test
    fun mapType_productType_2() = verifyType("map nat * nat to (nat * nat)", "map nat * nat to (nat * nat)")

    @Test
    fun mapType_productType_3() = verifyType("(map nat * nat to nat) * nat", "map nat * nat to nat * nat")

    @Test
    fun mapType_mapType_1() =
        verifyType("map map nat to nat to map nat to nat * nat", "map map nat to nat to (map nat to nat) * nat")

    @Test
    fun mapType_mapType_2() =
        verifyType("map (map nat to nat) to (map nat to nat)", "map map nat to nat to (map nat to nat)")

    @Test
    fun mapType_mapType_3() =
        verifyType("map map nat to nat * nat to (map nat to nat)", "map map nat to nat * nat to (map nat to nat)")

    @Test
    fun inmapType_plain() = verifyType("inmap nat to nat", "inmap nat to nat")

    @Test
    fun inmapType_productType_1() = verifyType("inmap nat * nat to nat * nat", "inmap nat * nat to nat * nat")

    @Test
    fun inmapType_productType_2() = verifyType("inmap nat * nat to (nat * nat)", "inmap nat * nat to (nat * nat)")

    @Test
    fun inmapType_productType_3() = verifyType("(inmap nat * nat to nat) * nat", "inmap nat * nat to nat * nat")

    @Test
    fun inmapType_inmapType_1() = verifyType(
        "inmap inmap nat to nat to inmap nat to nat * nat",
        "inmap inmap nat to nat to (inmap nat to nat) * nat"
    )

    @Test
    fun inmapType_inmapType_2() =
        verifyType("inmap (inmap nat to nat) to (inmap nat to nat)", "inmap inmap nat to nat to (inmap nat to nat)")

    @Test
    fun inmapType_inmapType_3() = verifyType(
        "inmap inmap nat to nat * nat to (inmap nat to nat)",
        "inmap inmap nat to nat * nat to (inmap nat to nat)"
    )

    @Test
    fun setType_plain() = verifyType("set of nat", "set of nat")

    @Test
    fun setType_setType() = verifyType("set of set of nat", "set of (set of nat)")

    @Test
    fun setType_mapType_1() = verifyType("set of map nat to nat", "set of (map nat to nat)")

    @Test
    fun setType_mapType_2() = verifyType("set of map nat to set of nat", "set of (map nat to set of nat)")

    @Test
    fun setType_unionType_1() = verifyType("set of nat | char | token", "char | set of nat | token")

    @Test
    fun setType_unionType_2() = verifyType("set of (nat | char) | token", "set of (char | nat) | token")

    @Test
    fun setType_unionType_3() = verifyType("set of (nat | char | token)", "set of (char | nat | token)")

    @Test
    fun setType_productType_1() = verifyType("set of nat * char * token", "set of nat * char * token")

    @Test
    fun setType_productType_2() = verifyType("set of (nat * char) * token", "set of (nat * char) * token")

    @Test
    fun setType_productType_3() = verifyType("set of (nat * char * token)", "set of (nat * char * token)")

    @Test
    fun setType_productType_4() = verifyType("(set of nat * char) * token", "(set of nat * char) * token")

    @Test
    fun setType_productType_5() = verifyType("(set of nat) * (char * token)", "set of nat * (char * token)")

    @Test
    fun set1Type_plain() = verifyType("set1 of nat", "set1 of nat")

    @Test
    fun set1Type_set1Type() = verifyType("set1 of set1 of nat", "set1 of (set1 of nat)")

    @Test
    fun set1Type_mapType_1() = verifyType("set1 of map nat to nat", "set1 of (map nat to nat)")

    @Test
    fun set1Type_mapType_2() = verifyType("set1 of map nat to set1 of nat", "set1 of (map nat to set1 of nat)")

    @Test
    fun set1Type_unionType_1() = verifyType("set1 of nat | char | token", "char | set1 of nat | token")

    @Test
    fun set1Type_unionType_2() = verifyType("set1 of (nat | char) | token", "set1 of (char | nat) | token")

    @Test
    fun set1Type_unionType_3() = verifyType("set1 of (nat | char | token)", "set1 of (char | nat | token)")

    @Test
    fun set1Type_productType_1() = verifyType("set1 of nat * char * token", "set1 of nat * char * token")

    @Test
    fun set1Type_productType_2() = verifyType("set1 of (nat * char) * token", "set1 of (nat * char) * token")

    @Test
    fun set1Type_productType_3() = verifyType("set1 of (nat * char * token)", "set1 of (nat * char * token)")

    @Test
    fun set1Type_productType_4() = verifyType("(set1 of nat * char) * token", "(set1 of nat * char) * token")

    @Test
    fun set1Type_productType_5() = verifyType("(set1 of nat) * (char * token)", "set1 of nat * (char * token)")

    @Test
    fun seqType_plain() = verifyType("seq of nat", "seq of nat")

    @Test
    fun seqType_seqType() = verifyType("seq of seq of nat", "seq of (seq of nat)")

    @Test
    fun seqType_mapType_1() = verifyType("seq of map nat to nat", "seq of (map nat to nat)")

    @Test
    fun seqType_mapType_2() = verifyType("seq of map nat to seq of nat", "seq of (map nat to seq of nat)")

    @Test
    fun seqType_unionType_1() = verifyType("seq of nat | char | token", "char | seq of nat | token")

    @Test
    fun seqType_unionType_2() = verifyType("seq of (nat | char) | token", "seq of (char | nat) | token")

    @Test
    fun seqType_unionType_3() = verifyType("seq of (nat | char | token)", "seq of (char | nat | token)")

    @Test
    fun seqType_productType_1() = verifyType("seq of nat * char * token", "seq of nat * char * token")

    @Test
    fun seqType_productType_2() = verifyType("seq of (nat * char) * token", "seq of (nat * char) * token")

    @Test
    fun seqType_productType_3() = verifyType("seq of (nat * char * token)", "seq of (nat * char * token)")

    @Test
    fun seqType_productType_4() = verifyType("(seq of nat * char) * token", "(seq of nat * char) * token")

    @Test
    fun seqType_productType_5() = verifyType("(seq of nat) * (char * token)", "seq of nat * (char * token)")

    @Test
    fun seq1Type_plain() = verifyType("seq1 of nat", "seq1 of nat")

    @Test
    fun seq1Type_seq1Type() = verifyType("seq1 of seq1 of nat", "seq1 of (seq1 of nat)")

    @Test
    fun seq1Type_mapType_1() = verifyType("seq1 of map nat to nat", "seq1 of (map nat to nat)")

    @Test
    fun seq1Type_mapType_2() = verifyType("seq1 of map nat to seq1 of nat", "seq1 of (map nat to seq1 of nat)")

    @Test
    fun seq1Type_unionType_1() = verifyType("seq1 of nat | char | token", "char | seq1 of nat | token")

    @Test
    fun seq1Type_unionType_2() = verifyType("seq1 of (nat | char) | token", "seq1 of (char | nat) | token")

    @Test
    fun seq1Type_unionType_3() = verifyType("seq1 of (nat | char | token)", "seq1 of (char | nat | token)")

    @Test
    fun seq1Type_productType_1() = verifyType("seq1 of nat * char * token", "seq1 of nat * char * token")

    @Test
    fun seq1Type_productType_2() = verifyType("seq1 of (nat * char) * token", "seq1 of (nat * char) * token")

    @Test
    fun seq1Type_productType_3() = verifyType("seq1 of (nat * char * token)", "seq1 of (nat * char * token)")

    @Test
    fun seq1Type_productType_4() = verifyType("(seq1 of nat * char) * token", "(seq1 of nat * char) * token")

    @Test
    fun seq1Type_productType_5() = verifyType("(seq1 of nat) * (char * token)", "seq1 of nat * (char * token)")

    private fun verifyType(raw: String, expected: String) {
        val tempFile = Files.createTempFile("test-", ".vdmsl").toFile()
        tempFile.writeText("types\na = $raw")
        val vdmsl = VDMSL()
        vdmsl.parse(listOf(tempFile))
        vdmsl.typeCheck()
        val module = vdmsl.interpreter.defaultModule
        val typeDefinition = module.defs.filter { it is ATypeDefinition }.map { it as ATypeDefinition }.first()
        val type = typeDefinition.type as ANamedInvariantType
        val actual = VdmPrettyPrinter().prettyPrint(type.type, config = PrettyPrintConfig(includeHeaderFooter = false))
        assertEquals(expected, actual)
    }

}
