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

import com.anaplan.engineering.vdmprettyprinter.PrecedenceManager.expRequiresParentheses
import com.anaplan.engineering.vdmprettyprinter.PrecedenceManager.typeRequiresParentheses
import com.anaplan.engineering.vdmprettyprinter.RenderToken.*
import org.overture.ast.analysis.intf.IQuestionAnswer
import org.overture.ast.annotations.AAnnotationAnnotation
import org.overture.ast.annotations.Annotation
import org.overture.ast.annotations.PAnnotation
import org.overture.ast.definitions.*
import org.overture.ast.definitions.relations.AEqRelation
import org.overture.ast.definitions.relations.AOrdRelation
import org.overture.ast.definitions.relations.PRelation
import org.overture.ast.definitions.traces.*
import org.overture.ast.expressions.*
import org.overture.ast.intf.lex.*
import org.overture.ast.modules.*
import org.overture.ast.node.INode
import org.overture.ast.node.IToken
import org.overture.ast.node.tokens.TAsync
import org.overture.ast.node.tokens.TStatic
import org.overture.ast.patterns.*
import org.overture.ast.statements.*
import org.overture.ast.typechecker.ClassDefinitionSettings
import org.overture.ast.typechecker.NameScope
import org.overture.ast.typechecker.Pass
import org.overture.ast.types.*
import org.overture.ast.util.ClonableFile
import org.overture.ast.util.ClonableString
import org.overture.ast.util.PTypeSet

class VdmPrettyPrinter(
    val renderStrategy: IRenderStrategy = PlainAsciiTextRenderStrategy()
) : IQuestionAnswer<IRenderContext, String> {

    fun prettyPrint(
        node: INode,
        config: PrettyPrintConfig = PrettyPrintConfig()
    ): String {
        val sb = StringBuilder()
        if (config.includeHeaderFooter) sb.append(renderStrategy.header)
        sb.append(node.apply(this, DefaultRenderContext(config)))
        if (config.includeHeaderFooter) sb.append(renderStrategy.footer)
        return sb.toString()
    }

    private fun builder(context: IRenderContext) = RenderBuilder(this, context)

    private fun unhandled(clazz: Class<*>, context: IRenderContext): String {
        if (context.config.logUnhandledCases) {
            println("ERROR: no rendering for class $clazz")
        }
        return ""
    }

    private fun renderSBinaryExp(node: SBinaryExp, context: IRenderContext) =
        builder(context)
            .childExpression(node.left, node, RenderBuilder.Position.left)
            .space().node(node.op).space()
            .childExpression(node.right, node, RenderBuilder.Position.right)
            .render

    private fun renderSUnaryExp(
        node: SUnaryExp,
        token: RenderToken,
        context: IRenderContext,
        requiresSpace: Boolean = true
    ) =
        builder(context).token(token)
            .conditional(expRequiresParentheses(node.exp, node, true), {
                it.lparens()
            }, {
                it.conditional(requiresSpace, { it.space() })
            })
            .node(node.exp)
            .conditional(expRequiresParentheses(node.exp, node, true), {
                it.rparens()
            })
            .render

    private fun renderLeftRightSUnaryExp(
        node: SUnaryExp,
        left: RenderToken,
        right: RenderToken,
        context: IRenderContext
    ) =
        builder(context)
            .token(left)
            .conditional(expRequiresParentheses(node.exp, node, true) && !renderStrategy.replacesParentheses(left), {
                it.lparens()
            })
            .node(node.exp).token(right)
            .conditional(expRequiresParentheses(node.exp, node, true) && !renderStrategy.replacesParentheses(left), {
                it.rparens()
            })
            .render

    private fun renderToken(token: RenderToken, context: IRenderContext) = builder(context).token(token).render

    private fun renderToken(token: String, context: IRenderContext) = builder(context).token(token).render

    override fun defaultPTraceDefinition(node: PTraceDefinition, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseATrapStm(node: ATrapStm, context: IRenderContext) =
        builder(context).token(trap).space().node(node.patternBind).space().token(withToken).space().node(node.with)
            .space().token(inToken).space().node(node.body).render

    override fun caseASubclassResponsibilityExp(node: ASubclassResponsibilityExp, context: IRenderContext) =
        renderToken(isSubclassResponsibility, context)

    override fun caseAUnionType(node: AUnionType, context: IRenderContext) =
        builder(context).conditional(node.types.size == 1 && typeRequiresParentheses(node.types.first, node), {
            it.lparens().node(node.types.first).rparens()
        }, {
            it.nodeList(node.types, listOf(space, pipe, space), renderNode = { listNode, builder ->
                if (typeRequiresParentheses(listNode as PType, node)) {
                    builder.lparens().node(listNode).rparens()
                } else {
                    builder.node(listNode)
                }
            })
        }).render

    override fun caseAExistsExp(node: AExistsExp, context: IRenderContext) =
        builder(context).token(exists).space().nodeList(node.bindList).space().token(bullet).space()
            .node(node.predicate).render

    override fun defaultPExp(node: PExp, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseABooleanConstExp(node: ABooleanConstExp, context: IRenderContext) =
        renderToken(node.value.value.toString(), context)

    override fun caseASubsetBinaryExp(node: ASubsetBinaryExp, context: IRenderContext) = renderSBinaryExp(node, context)

    override fun defaultPAlternative(node: PAlternative, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseANamedTraceDefinition(node: ANamedTraceDefinition, context: IRenderContext) =
        builder(context).token(node.pathname.joinToString(", ")).token(colon).incIndent().nl()
            .nodeList(node.terms, listOf(semiColon, newLine)).render

    override fun caseALetBeStStm(node: ALetBeStStm, context: IRenderContext) =
        builder(context)
            .conditional(context.lineState == LineState.tokened, {
                it.nl().incIndent()
            })
            .token(let).space().node(node.bind)
            .conditional(node.suchThat != null, {
                it.space().token(be).space().token(st).nl().incIndent().node(node.suchThat).decIndent()
            })
            .nl().token(inToken).space().node(node.statement).render

    override fun defaultPStmtAlternative(node: PStmtAlternative, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAMapRangeUnaryExp(node: AMapRangeUnaryExp, context: IRenderContext) =
        renderSUnaryExp(node, rng, context)

    override fun caseAMapEnumMapExp(node: AMapEnumMapExp, context: IRenderContext) =
        builder(context).conditional(node.members.isEmpty(), {
            it.token(lbrace).token(maplet).token(rbrace)
        }, {
            it.token(lbrace).nodeList(node.members).token(rbrace)
        }).render

    override fun defaultSValueImport(node: SValueImport, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun defaultSSeqType(node: SSeqType, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseABusClassDefinition(node: ABusClassDefinition, context: IRenderContext) =
        renderClass(node, NavigationMarker.bus, context)

    override fun caseAAtomicStm(node: AAtomicStm, context: IRenderContext) =
        builder(context).token(atomic).space().lparens().nl().incIndent()
            .nodeList(node.assignments, listOf(semiColon, newLine)).nl().decIndent()
            .rparens().render

    override fun caseAOptionalType(node: AOptionalType, context: IRenderContext) =
        builder(context).token(lsquare).node(node.type).token(rsquare).render

    override fun caseAElseIfExp(node: AElseIfExp, context: IRenderContext) =
        builder(context).token(elseif).space().node(node.elseIf).nl()
            .token(then).space().node(node.then).render

    override fun caseADefPatternBind(node: ADefPatternBind, context: IRenderContext) =
        builder(context)
            // TODO - can bind and pattern both be null or non-null??
            .conditional(node.pattern == null, {
                it.node(node.bind)
            }, {
                it.node(node.pattern)
            })
            .conditional(node.type != null, {
                it.token(colon).space().node(node.type)
            })
            .render

    override fun defaultSNumericBinaryExp(node: SNumericBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseASetUnionBinaryExp(node: ASetUnionBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseABracketType(node: ABracketType, context: IRenderContext) =
        builder(context).node(node.type).render

    override fun defaultINode(node: INode, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseALambdaExp(node: ALambdaExp, context: IRenderContext) =
        builder(context).token(lambda).space().nodeList(node.bindList).space().token(bullet).space()
            .node(node.expression).render

    override fun caseATixeStm(node: ATixeStm, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAForPatternBindStm(node: AForPatternBindStm, context: IRenderContext) =
        builder(context).token(forToken).space().node(node.patternBind).space().token(inToken).space()
            .conditional(node.reverse, {
                it.token(reverse).space()
            })
            .node(node.exp).nl()
            .token(doToken).space().node(node.statement).render

    override fun caseAQuotePattern(node: AQuotePattern, context: IRenderContext) =
        builder(context).token(lquote).token(node.value.value).token(rquote).render

    override fun defaultSClassDefinition(node: SClassDefinition, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun defaultPAccessSpecifier(node: PAccessSpecifier, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAIdentifierObjectDesignator(node: AIdentifierObjectDesignator, context: IRenderContext) =
        builder(context).node(node.name).render

    override fun defaultSBinaryExp(node: SBinaryExp, context: IRenderContext) = renderSBinaryExp(node, context)

    override fun caseAExternalDefinition(node: AExternalDefinition, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAImpliesBooleanBinaryExp(node: AImpliesBooleanBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseAOrBooleanBinaryExp(node: AOrBooleanBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseAIntNumericBasicType(node: AIntNumericBasicType, context: IRenderContext) =
        renderToken(int, context)

    override fun caseATimeExp(node: ATimeExp, context: IRenderContext) = renderToken(time, context)

    override fun caseAErrorCase(node: AErrorCase, context: IRenderContext) =
        builder(context).node(node.name).token(colon).incIndent().nl()
            .node(node.left).nl()
            .token(partialFunction).space().node(node.right).render

    override fun defaultPType(node: PType, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun defaultSMapExp(node: SMapExp, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAMapletExp(node: AMapletExp, context: IRenderContext) =
        builder(context).node(node.left).space().token(maplet).space().node(node.right).render

    override fun caseAHistoryExp(node: AHistoryExp, context: IRenderContext) =
        builder(context).node(node.hop).lparens().nodeList(node.opnames).rparens().render

    override fun caseATokenBasicType(node: ATokenBasicType, context: IRenderContext) = renderToken(token, context)

    override fun caseAHeadUnaryExp(node: AHeadUnaryExp, context: IRenderContext) =
        renderSUnaryExp(node, hd, context)

    override fun caseAValueValueImport(node: AValueValueImport, context: IRenderContext) =
        builder(context).node(node.name).conditional(node.renamed != null, {
            it.space().token(renamed).space().node(node.renamed)
        }).render

    override fun caseAErrorStm(node: AErrorStm, context: IRenderContext) = renderToken(error, context)

    // From a printing pov, the unresolvedness is irrelevant, so just print the typename
    override fun caseAUnresolvedType(node: AUnresolvedType, context: IRenderContext) =
        renderToken(node.name.getExplicit(true).name, context)

    override fun caseAMapSeqStateDesignator(node: AMapSeqStateDesignator, context: IRenderContext) =
        builder(context).node(node.mapseq).lparens().node(node.exp).rparens().render

    override fun caseAIsOfBaseClassExp(node: AIsOfBaseClassExp, context: IRenderContext) =
        builder(context).token(isOfBaseClass).lparens().node(node.baseClass).token(comma).space().node(node.exp)
            .rparens().render

    override fun caseACharLiteralExp(node: ACharLiteralExp, context: IRenderContext) =
        builder(context).token(quote).token(node.value.value.toString()).token(quote).render

    override fun caseASeq1SeqType(node: ASeq1SeqType, context: IRenderContext) =
        builder(context).token(seq1Left).childType(node.seqof, node).token(seq1Right).render

    override fun caseAMapDomainUnaryExp(node: AMapDomainUnaryExp, context: IRenderContext) =
        renderSUnaryExp(node, dom, context)

    override fun caseABooleanBasicType(node: ABooleanBasicType, context: IRenderContext) =
        renderToken(bool, context)

    override fun caseClonableString(node: ClonableString, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAOperationType(node: AOperationType, context: IRenderContext) =
        builder(context).conditional(node.parameters.isEmpty(), {
            it.token(emptyTuple)
        }, {
            // Note that, is necessary to disambiguate between one parameter that is a product and a list of parameters
            if (node.parameters.size == 1 && (node.parameters.first is AProductType || typeRequiresParentheses(
                    node.parameters.first,
                    node
                ))
            ) {
                it.lparens().node(node.parameters.first).rparens()
            } else {
                it.nodeList(
                    nodeList = node.parameters,
                    separators = listOf(space, multiplication, space),
                    renderNode = { listNode, builder ->
                        if (typeRequiresParentheses(
                                listNode as PType,
                                AProductType::class
                            ) && node.parameters.size > 1
                        ) {
                            builder.lparens().node(listNode).rparens()
                        } else {
                            builder.node(listNode)
                        }
                    })
            }
        }).space().token(operation).space().node(node.result).render

    override fun caseAAssignmentDefinition(node: AAssignmentDefinition, context: IRenderContext) =
        builder(context).token(dcl).space().node(node.name).token(colon).space().node(node.type).space().token(assign)
            .space().node(node.expression).render

    override fun caseAUnknownType(node: AUnknownType, context: IRenderContext) = renderToken(anyType, context)

    override fun caseAStarStarBinaryExp(node: AStarStarBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun defaultPBind(node: PBind, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseALocalDefinition(node: ALocalDefinition, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAPowerSetUnaryExp(node: APowerSetUnaryExp, context: IRenderContext) =
        renderSUnaryExp(node, powerset, context)

    override fun caseAFieldObjectDesignator(node: AFieldObjectDesignator, context: IRenderContext) =
        builder(context).node(node.`object`).token(dot).node(node.fieldName).render

    override fun caseAAssignmentStm(node: AAssignmentStm, context: IRenderContext) =
        builder(context).node(node.target).space().token(assign).space().node(node.exp).render

    override fun caseAPlusNumericBinaryExp(node: APlusNumericBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseAStartStm(node: AStartStm, context: IRenderContext) =
        builder(context).token(start).lparens().node(node.obj).rparens().render

    override fun caseARecordInvariantType(node: ARecordInvariantType, context: IRenderContext) =
        builder(context).ifTypeDef({
            it.nl().incIndent().unsetTypeDef()
                .nodeList(node.fields, listOf(newLine))
        }, {
            it.name(node.name)
        }).render

    override fun caseAMapCompMapExp(node: AMapCompMapExp, context: IRenderContext) =
        builder(context).token(lbrace).node(node.first).space().token(pipe).space().nodeList(node.bindings)
            .conditional(node.predicate != null, {
                it.space().token(bullet).space().node(node.predicate)
            }).token(rbrace).render

    override fun defaultPObjectDesignator(node: PObjectDesignator, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseASubseqExp(node: ASubseqExp, context: IRenderContext) =
        builder(context)
            .childExpression(node.seq, node, RenderBuilder.Position.left)
            .lparens().node(node.from).token(comma).space().token(range).token(comma).space().node(node.to)
            .rparens().render

    override fun caseACharacterPattern(node: ACharacterPattern, context: IRenderContext) =
        builder(context).token(quote).node(node.value).token(quote).render

    override fun caseANewObjectDesignator(node: ANewObjectDesignator, context: IRenderContext) =
        builder(context).node(node.expression).render

    override fun caseAIsExp(node: AIsExp, context: IRenderContext) =
        builder(context).token(is_).lparens().node(node.test).token(comma).space().conditional(node.typeName == null, {
            it.node(node.basicType)
        }, {
            it.node(node.typeName)
        }).rparens().render

    override fun caseARealNumericBasicType(node: ARealNumericBasicType, context: IRenderContext) =
        renderToken(real, context)

    override fun caseASkipStm(node: ASkipStm, context: IRenderContext) = renderToken(skip, context)

    override fun caseAInstanceTraceDefinition(node: AInstanceTraceDefinition, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAValueDefinition(node: AValueDefinition, context: IRenderContext) =
        builder(context)
            .conditional(node.access != null, {
                it.node(node.access)
            })
            .node(node.pattern)
            // should only print the type if it can't be inferred from the expression
            .conditional(node.type != null && node.type != node.expType, {
                it.token(colon).space().node(node.type)
            }).space().token(equals).space().node(node.expression).render

    override fun caseAForAllExp(node: AForAllExp, context: IRenderContext) =
        builder(context).token(forall).space().nodeList(node.bindList).space().token(bullet).space()
            .node(node.predicate).render

    override fun defaultPImport(node: PImport, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAExternalClause(node: AExternalClause, context: IRenderContext) =
        builder(context).node(node.mode).space().nodeList(node.identifiers).token(colon).space().node(node.type).render

    override fun caseASetMultipleBind(node: ASetMultipleBind, context: IRenderContext) =
        builder(context).nodeList(node.plist).space().token(inSet).space().node(node.set).render

    override fun caseATupleExp(node: ATupleExp, context: IRenderContext) =
        builder(context).token(mk_).lparens().nodeList(node.args).rparens().render

    override fun caseAPlusPlusBinaryExp(node: APlusPlusBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseASeqCompSeqExp(node: ASeqCompSeqExp, context: IRenderContext): String {
        return builder(context).token(lsquare).node(node.first).space().token(pipe).space()
            .node(node.bind)
            .conditional(node.predicate != null) {
                it.space().token(bullet).space().node(node.predicate)
            }
            .token(rsquare).render
    }

    override fun caseAFieldStateDesignator(node: AFieldStateDesignator, context: IRenderContext) =
        builder(context).node(node.`object`).token(dot).node(node.field).render

    override fun caseAMapUnionBinaryExp(node: AMapUnionBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseANarrowExp(node: ANarrowExp, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseARationalNumericBasicType(node: ARationalNumericBasicType, context: IRenderContext) =
        renderToken(rat, context)

    override fun defaultPPatternBind(node: PPatternBind, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAClassInvariantDefinition(node: AClassInvariantDefinition, context: IRenderContext) =
        builder(context).token(inv).space().node(node.expression).render

    override fun defaultPStm(node: PStm, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAThreadDefinition(node: AThreadDefinition, context: IRenderContext) =
        builder(context).node(node.statement).render

    override fun defaultPModifier(node: PModifier, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseABlockSimpleBlockStm(node: ABlockSimpleBlockStm, context: IRenderContext) =
        builder(context).lparens().incIndent().nl()
            .conditional(node.assignmentDefs.isNotEmpty(), {
                it.nodeList(node.assignmentDefs, listOf(semiColon, newLine)).token(semiColon).nl() //
            }).nodeList(node.statements, listOf(semiColon, newLine)).token(semiColon).decIndent().nl()
            .rparens().render

    override fun caseAElseIfStm(node: AElseIfStm, context: IRenderContext) =
        builder(context).token(elseif).space().node(node.elseIf).nl()
            .token(then).space().node(node.thenStm).render

    override fun caseALetStm(node: ALetStm, context: IRenderContext) =
        builder(context)
            .conditional(context.lineState == LineState.tokened, {
                it.nl().incIndent()
            })
            .token(let).nl().incIndent()
            .nodeList(node.localDefs, listOf(comma, newLine)).nl()
            .decIndent().token(inToken).nl().incIndent()
            .node(node.statement).render


    override fun caseALetDefExp(node: ALetDefExp, context: IRenderContext) =
        builder(context)
            .conditional(context.lineState == LineState.tokened, {
                it.nl().incIndent()
            })
            .token(let).nl().incIndent()
            .nodeList(node.localDefs, listOf(comma, newLine)).nl()
            .decIndent().token(inToken).nl().incIndent()
            .node(node.expression).render

    override fun caseAMapletPatternMaplet(node: AMapletPatternMaplet, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAUnaryPlusUnaryExp(node: AUnaryPlusUnaryExp, context: IRenderContext) =
        renderSUnaryExp(node, plus, context, false)

    override fun caseASubtractNumericBinaryExp(node: ASubtractNumericBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseASetRangeSetExp(node: ASetRangeSetExp, context: IRenderContext) =
        builder(context).token(lbrace).node(node.first).token(comma).space().token(range).token(comma).space()
            .node(node.last).token(rbrace).render

    override fun caseAUnaryMinusUnaryExp(node: AUnaryMinusUnaryExp, context: IRenderContext) =
        renderSUnaryExp(node, minus, context, false)

    override fun caseAStopStm(node: AStopStm, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseARecordModifier(node: ARecordModifier, context: IRenderContext) =
        builder(context).node(node.tag).space().token(maplet).space().node(node.value).render

    override fun caseAMutexSyncDefinition(node: AMutexSyncDefinition, context: IRenderContext) =
        builder(context).token(mutex).space().lparens()
            .conditional(node.operations.isEmpty(), {
                it.token(all)
            }, {
                it.nodeList(node.operations)
            }).rparens().render

    override fun caseAClassType(node: AClassType, context: IRenderContext) = renderToken(node.name.name, context)

    override fun caseAFuncInstatiationExp(node: AFuncInstatiationExp, context: IRenderContext) =
        builder(context)
            .childExpression(node.function, node, RenderBuilder.Position.left)
            .token(lsquare).nodeList(node.actualTypes).token(rsquare).render


    override fun caseAMkTypeExp(node: AMkTypeExp, context: IRenderContext) =
        builder(context).token(mk_)
            .conditional(node.type != null, {
                it.node(node.type)
            })
            .lparens().nodeList(node.args).rparens().render

    override fun caseClassDefinitionSettings(node: ClassDefinitionSettings, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAExplicitFunctionDefinition(node: AExplicitFunctionDefinition, context: IRenderContext) =
        builder(context)
            .conditional(node.access != null, {
                it.node(node.access)
            })
            .token(node.name.name)
            .conditional(node.typeParams.isNotEmpty(), {
                it.token(lsquare).token(paramType).nodeList(node.typeParams, listOf(comma, space, paramType))
                    .token(rsquare)
            }).token(colon).space().node(node.type).nl()
            .token(node.name.name).nodeListList(node.paramPatternList).space().token(fnEquals).nl().incIndent()
            .node(node.body).decIndent()
            .conditional(node.precondition != null, {
                it.nl().token(pre).space().node(node.precondition)
            })
            .conditional(node.postcondition != null, {
                it.nl().token(post).space().node(node.postcondition)
            })
            .conditional(node.measure != null, {
                it.nl().token(measure).space().node(node.measure)
            })
            .render

    override fun caseAApplyExpressionTraceCoreDefinition(
        node: AApplyExpressionTraceCoreDefinition,
        context: IRenderContext
    ) =
        builder(context).node(node.callStatement).render

    override fun caseTAsync(node: TAsync, context: IRenderContext) = renderToken(sync, context)

    override fun caseANotUnaryExp(node: ANotUnaryExp, context: IRenderContext) =
        renderSUnaryExp(node, not, context)

    override fun caseANamePatternPair(node: ANamePatternPair, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAMkBasicExp(node: AMkBasicExp, context: IRenderContext) =
        builder(context).token(mk_)
            .conditional(node.type != null, {
                it.node(node.type)
            })
            .lparens().node(node.arg).rparens().render

    override fun caseAModuleImports(node: AModuleImports, context: IRenderContext) =
        builder(context).nodeList(node.imports, listOf(comma, newLine, newLine)).render


    override fun caseILexStringToken(node: ILexStringToken, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun defaultPTraceCoreDefinition(node: PTraceCoreDefinition, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseACallStm(node: ACallStm, context: IRenderContext): String =
        builder(context).name(node.name).lparens().nodeList(node.args).rparens().render

    override fun caseAFieldNumberExp(node: AFieldNumberExp, context: IRenderContext) =
        builder(context).node(node.tuple).token(dot).token(hash).node(node.field).render

    override fun caseAExitStm(node: AExitStm, context: IRenderContext) =
        builder(context).token(exit).conditional(node.expression != null, {
            it.lparens().node(node.expression).rparens()
        }).render

    override fun caseILexRealToken(node: ILexRealToken, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAElementsUnaryExp(node: AElementsUnaryExp, context: IRenderContext) =
        renderSUnaryExp(node, elems, context)

    override fun caseAEqualsBinaryExp(node: AEqualsBinaryExp, context: IRenderContext) = renderSBinaryExp(node, context)

    override fun caseAModuleModules(node: AModuleModules, context: IRenderContext): String {
        val sectionTypes = mapOf(
            ATypeDefinition::class.java to NavigationMarker.types,
            AStateDefinition::class.java to NavigationMarker.state,
            AValueDefinition::class.java to NavigationMarker.values,
            SFunctionDefinition::class.java to NavigationMarker.functions,
            SOperationDefinition::class.java to NavigationMarker.operations,
            ANamedTraceDefinition::class.java to NavigationMarker.traces
        )

        fun processDefsOfType(clazz: Class<*>, defs: List<PDefinition>): (RenderBuilder) -> RenderBuilder {
            val section = sectionTypes.get(clazz) ?: throw IllegalArgumentException("Unexpected definition type $clazz")
            return { builder ->
                builder.token(section.token as RenderToken).addNavigationMarks(section).nl().vspace().incIndent()
                    .nodeList(defs, listOf(semiColon, newLine, newLine), { node ->
                        if (node is PDefinitionBase && node.name != null) {
                            NavigationMarker(node.name.simpleName)
                        } else {
                            null
                        }
                    }).nl().vspace().decIndent()
            }
        }

        fun <T, K> Iterable<T>.groups(keySelector: (T) -> K): List<Pair<K, List<T>>> {
            val groups = mutableListOf<Pair<K, MutableList<T>>>()
            this.forEach { t ->
                val key = keySelector(t)
                if (groups.isEmpty() || groups.last().first != key) {
                    groups.add(Pair(key, mutableListOf<T>(t)))
                } else {
                    groups.last().second.add(t)
                }
            }
            return groups
        }


        fun processDefs() =
            node.defs.groups { def ->
                sectionTypes.keys.find { st -> st.isAssignableFrom(def.javaClass) }
                    ?: throw IllegalArgumentException("Unexpected definition type ${def.javaClass}")
            }.map { def ->
                { b: RenderBuilder -> b.apply(processDefsOfType(def.first, def.second)) }
            }

        return builder(context).setCurrentContainer(node)
            .conditional(node.name.name != defaultModuleName, {
                it.token(module).space().token(node.name.name).addNavigationMarks(NavigationMarker.module).nl().vspace()
                    .conditional(node.imports != null, {
                        it.token(imports).addNavigationMarks(NavigationMarker.imports).nl().vspace()
                            .node(node.imports).nl().vspace()
                    })
                    .conditional(node.exports != null, {
                        it.token(exports).addNavigationMarks(NavigationMarker.exports).nl().vspace()
                            .node(node.exports).nl().vspace()
                    })
                    .token(definitions).nl().vspace()
            })
            .applyAll(processDefs())
            .nl()
            .conditional(node.name.name != defaultModuleName, {
                it.token(end).space().token(node.name.name).nl()
            }).render
    }

    override fun caseAOperationExport(node: AOperationExport, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAInSetBinaryExp(node: AInSetBinaryExp, context: IRenderContext) = renderSBinaryExp(node, context)

    override fun caseANatNumericBasicType(node: ANatNumericBasicType, context: IRenderContext) =
        renderToken(nat, context)

    override fun caseAPostOpExp(node: APostOpExp, context: IRenderContext) =
        builder(context).node(node.postexpression).render

    override fun defaultSSetExp(node: SSetExp, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseASetDifferenceBinaryExp(node: ASetDifferenceBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseAUndefinedType(node: AUndefinedType, context: IRenderContext) = renderToken(undefined, context)

    override fun caseARangeResByBinaryExp(node: ARangeResByBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseAForAllStm(node: AForAllStm, context: IRenderContext) =
        builder(context).token(forToken).space().token(all).space().node(node.pattern).space().token(inSet).space()
            .node(node.set).nl()
            .token(doToken).space().node(node.statement).render

    override fun caseACharBasicType(node: ACharBasicType, context: IRenderContext) = renderToken(char, context)

    override fun caseAMapPattern(node: AMapPattern, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAUndefinedExp(node: AUndefinedExp, context: IRenderContext) = renderToken(undefined, context)

    override fun caseAUntypedDefinition(node: AUntypedDefinition, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAAndBooleanBinaryExp(node: AAndBooleanBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseAPreExp(node: APreExp, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun defaultSSimpleBlockStm(node: SSimpleBlockStm, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAPreOpExp(node: APreOpExp, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseARepeatTraceDefinition(node: ARepeatTraceDefinition, context: IRenderContext) =
        builder(context).node(node.core).conditional(node.to != 1L, {
            it.conditional(node.from == node.to, {
                it.token(lbrace).token(node.to.toString()).token(rbrace)
            }, {
                it.token(lbrace).token(node.from.toString()).token(comma).space().token(node.to.toString())
                    .token(rbrace)
            })
        }).render

    override fun caseASetSetType(node: ASetSetType, context: IRenderContext) =
        builder(context).conditional(node.empty, {
            it.token(emptySet)
        }, {
            it.token(setLeft).childType(node.setof, node).token(setRight)
        }).render

    override fun caseAVoidReturnType(node: AVoidReturnType, context: IRenderContext) = renderToken(returnToken, context)

    override fun defaultPField(node: PField, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseALessNumericBinaryExp(node: ALessNumericBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseAImplicitFunctionDefinition(node: AImplicitFunctionDefinition, context: IRenderContext) =
        builder(context)
            .conditional(node.access != null, {
                it.node(node.access)
            })
            .token(node.name.name)
            .conditional(node.typeParams.isNotEmpty(), {
                it.token(lsquare).nodeList(node.typeParams).token(rsquare)
            }).lparens().nodeList(node.paramPatterns).rparens().space().node(node.result)
            .conditional(node.body != null, {
                it.space().token(fnEquals).incIndent().nl().node(node.body).decIndent()
            })
            .conditional(node.precondition != null, {
                it.nl().token(pre).space().node(node.precondition)
            })
            .conditional(node.postcondition != null, {
                it.nl().token(post).space().node(node.postcondition)
            }).render

    override fun caseATypeExport(node: ATypeExport, context: IRenderContext) =
        builder(context).conditional(node.struct, {
            it.token(struct).space()
        }).node(node.name).render

    override fun caseAIntegerPattern(node: AIntegerPattern, context: IRenderContext) =
        builder(context).node(node.value).render

    override fun caseATypeBind(node: ATypeBind, context: IRenderContext) =
        builder(context).node(node.pattern).token(colon).space().node(node.type).render

    override fun caseATypeDefinition(node: ATypeDefinition, context: IRenderContext) =
        builder(context)
            .conditional(node.access != null, {
                it.node(node.access)
            })
            .token(node.name.name).space()
            .conditional(node.type is ARecordInvariantType, {
                it.token(record)
            }, {
                it.token(equals)
            })
            .space().setTypeDef().node(node.type).unsetTypeDef()
            .conditional(node.invPattern != null, {
                it.nl().token(inv).space().node(node.invPattern).space().token(fnEquals).space()
                    .node(node.invExpression)
            })
            .conditional(node.eqRelation != null, {
                it.nl().token(eq).space().node(node.eqRelation)
            })
            .conditional(node.ordRelation != null, {
                it.nl().token(ord).space().node(node.ordRelation)
            }).render

    override fun caseAInheritedDefinition(node: AInheritedDefinition, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseADistUnionUnaryExp(node: ADistUnionUnaryExp, context: IRenderContext) =
        renderSUnaryExp(node, dunion, context)

    override fun caseAFromModuleImports(node: AFromModuleImports, context: IRenderContext): String {
        fun processSignatures(section: RenderToken, clazz: Class<*>): (RenderBuilder) -> RenderBuilder {
            val signatures = node.signatures.flatten().filter { clazz.isAssignableFrom(it.javaClass) }
            return if (signatures.isEmpty()) {
                { it }
            } else {
                { builder ->
                    builder.nl().token(section).conditional(clazz != AAllImport::class.java, {
                        it.incIndent().nl()
                            .nodeList(signatures, listOf(newLine)).decIndent()
                    })
                }
            }
        }
        return builder(context).token(from).space().node(node.name).incIndent()
            .apply(processSignatures(all, AAllImport::class.java))
            .apply(processSignatures(importFunctions, AFunctionValueImport::class.java))
            .apply(processSignatures(importOperations, AOperationValueImport::class.java))
            .apply(processSignatures(importValues, AValueValueImport::class.java))
            .apply(processSignatures(importTypes, ATypeImport::class.java))
            .render
    }


    override fun caseARealPattern(node: ARealPattern, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAOperationValueImport(node: AOperationValueImport, context: IRenderContext) =
        builder(context).node(node.name).conditional(node.renamed != null, {
            it.space().token(renamed).space().node(node.renamed)
        }).render

    override fun caseAIotaExp(node: AIotaExp, context: IRenderContext) =
        builder(context).token(iota).space().node(node.bind).space().token(bullet).space().node(node.predicate).render

    override fun caseAFunctionType(node: AFunctionType, context: IRenderContext) =
        builder(context).conditional(node.parameters.isEmpty(), {
            it.token(emptyTuple)
        }, {
            // Note that, is necessary to disambiguate between one parameter that is a product and a list of parameters
            if (node.parameters.size == 1 && (node.parameters.first is AProductType || typeRequiresParentheses(
                    node.parameters.first,
                    node
                ))
            ) {
                it.lparens().node(node.parameters.first).rparens()
            } else {
                it.nodeList(
                    nodeList = node.parameters,
                    separators = listOf(space, multiplication, space),
                    renderNode = { listNode, builder ->
                        if (typeRequiresParentheses(
                                listNode as PType,
                                AProductType::class
                            ) && node.parameters.size > 1
                        ) {
                            builder.lparens().node(listNode).rparens()
                        } else {
                            builder.node(listNode)
                        }
                    })
            }
        }).space().conditional(node.partial, {
            it.token(partialFunction)
        }, {
            it.token(totalFunction)
        }).space().node(node.result).render

    override fun caseAAccessSpecifierAccessSpecifier(node: AAccessSpecifierAccessSpecifier, context: IRenderContext) =
        builder(context)
            .conditional(node.access != null && node.access !is APrivateAccess, {
                it.node(node.access).space()
            })
            // static should be printed for modules, but not classes
            .conditional(node.static != null && context.containerType == ContainerType.vdmClass, {
                it.node(node.static).space()
            })
            .conditional(node.async != null, {
                it.node(node.async).space()
            })
            .conditional(node.pure == true, {
                it.token(pure).space()
            })
            .render

    override fun caseAConcurrentExpressionTraceCoreDefinition(
        node: AConcurrentExpressionTraceCoreDefinition,
        context: IRenderContext
    ) = unhandled(node.javaClass, context)

    override fun caseAImportedDefinition(node: AImportedDefinition, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAObjectPattern(node: AObjectPattern, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun defaultIToken(node: IToken, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun defaultSUnaryExp(node: SUnaryExp, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAProtectedAccess(node: AProtectedAccess, context: IRenderContext) =
        renderToken(protectedToken, context)

    override fun caseILexIdentifierToken(node: ILexIdentifierToken, context: IRenderContext) =
        renderToken(node.name, context)

    override fun caseAIsOfClassExp(node: AIsOfClassExp, context: IRenderContext) =
        builder(context).token(isOfClass).lparens().node(node.className).token(comma).space().node(node.exp)
            .rparens().render

    override fun caseARenamedDefinition(node: ARenamedDefinition, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseILexIntegerToken(node: ILexIntegerToken, context: IRenderContext) =
        renderToken(node.value.toString(), context)

    override fun caseANotEqualBinaryExp(node: ANotEqualBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun defaultPClause(node: PClause, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAMuExp(node: AMuExp, context: IRenderContext) =
        builder(context).token(mu).lparens().node(node.record).token(comma).space().nodeList(node.modifiers)
            .rparens().render

    override fun caseANonDeterministicSimpleBlockStm(node: ANonDeterministicSimpleBlockStm, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAAbsoluteUnaryExp(node: AAbsoluteUnaryExp, context: IRenderContext) =
        renderLeftRightSUnaryExp(node, absLeft, absRight, context)

    override fun caseAAnnotatedUnaryExp(node: AAnnotatedUnaryExp, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAAnnotatedStm(node: AAnnotatedStm, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun defaultSMapType(node: SMapType, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseASameBaseClassExp(node: ASameBaseClassExp, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAProductType(node: AProductType, context: IRenderContext) =
        builder(context).conditional(node.types.size == 1 && typeRequiresParentheses(node.types.first, node), {
            it.lparens().node(node.types.first).rparens()
        }, {
            it.nodeList(node.types, listOf(space, multiplication, space), renderNode = { listNode, builder ->
                if (typeRequiresParentheses(listNode as PType, node)) {
                    builder.lparens().node(listNode).rparens()
                } else {
                    builder.node(listNode)
                }
            })
        }).render

    override fun caseASetBind(node: ASetBind, context: IRenderContext) =
        builder(context).node(node.pattern).space().token(inSet).space().node(node.set).render

    override fun caseAExists1Exp(node: AExists1Exp, context: IRenderContext) =
        builder(context).token(exists1).space().node(node.bind).space().token(bullet).space()
            .node(node.predicate).render

    override fun caseACpuClassDefinition(node: ACpuClassDefinition, context: IRenderContext) =
        renderClass(node, NavigationMarker.cpu, context)

    override fun caseAStateDefinition(node: AStateDefinition, context: IRenderContext) =
        builder(context).token(node.name.name).space().token(of).incIndent().nl()
            .nodeList(node.fields, listOf(newLine)).decIndent()
            .conditional(node.invPattern != null, {
                it.nl().token(inv).space().node(node.invPattern).space().token(fnEquals).space()
                    .node(node.invExpression)
            })
            .conditional(node.initPattern != null, {
                it.nl().token(initToken).space().node(node.initPattern).space().token(fnEquals).space()
                    .node(node.initExpression)
            })
            .nl().token(end).render

    override fun defaultPExports(node: PExports, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseADurationStm(node: ADurationStm, context: IRenderContext) =
        builder(context).token(duration).lparens().node(node.duration).rparens().space().node(node.statement).render

    override fun caseAReverseUnaryExp(node: AReverseUnaryExp, context: IRenderContext) =
        renderSUnaryExp(node, reverse, context)

    override fun defaultPTerm(node: PTerm, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAVariableExp(node: AVariableExp, context: IRenderContext) = renderToken(node.original, context)

    override fun caseATypeMultipleBind(node: ATypeMultipleBind, context: IRenderContext) =
        builder(context).nodeList(node.plist).token(colon).space().node(node.type).render

    override fun caseANilExp(node: ANilExp, context: IRenderContext) = renderToken(nil, context)

    override fun caseAProperSubsetBinaryExp(node: AProperSubsetBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseILexToken(node: ILexToken, context: IRenderContext) = renderToken(node.type.toString(), context)

    override fun caseADefExp(node: ADefExp, context: IRenderContext) =
        builder(context)
            .conditional(context.lineState == LineState.tokened, {
                it.nl().incIndent()
            })
            .token(def).nl().incIndent()
            .nodeList(node.localDefs, listOf(semiColon, newLine)).nl()
            .decIndent().token(inToken).nl().incIndent()
            .node(node.expression).render

    override fun defaultPModules(node: PModules, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseASetCompSetExp(node: ASetCompSetExp, context: IRenderContext) =
        builder(context).token(lbrace).node(node.first).space().token(pipe).space().nodeList(node.bindings)
            .conditional(node.predicate != null, {
                it.space().token(bullet).space().node(node.predicate)
            })
            .token(rbrace).render

    override fun caseANilPattern(node: ANilPattern, context: IRenderContext) = renderToken(nil, context)

    override fun caseAIntLiteralExp(node: AIntLiteralExp, context: IRenderContext) =
        renderToken(node.value.value.toString(), context)

    override fun caseATypeImport(node: ATypeImport, context: IRenderContext) =
        builder(context).node(node.name).conditional(node.renamed != null, {
            it.space().token(renamed).space().node(node.renamed)
        }).render

    override fun caseARealLiteralExp(node: ARealLiteralExp, context: IRenderContext) =
        renderToken(node.value.value.toString(), context)

    override fun caseASetPattern(node: ASetPattern, context: IRenderContext) =
        builder(context).token(lbrace).nodeList(node.plist).token(rbrace).render

    override fun caseATailUnaryExp(node: ATailUnaryExp, context: IRenderContext) = renderSUnaryExp(node, tl, context)

    override fun caseILexQuoteToken(node: ILexQuoteToken, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseACardinalityUnaryExp(node: ACardinalityUnaryExp, context: IRenderContext) =
        renderSUnaryExp(node, card, context)

    override fun caseAValueExport(node: AValueExport, context: IRenderContext) =
        builder(context).nodeList(node.nameList).token(colon).space().node(node.exportType).render

    override fun caseAForIndexStm(node: AForIndexStm, context: IRenderContext) =
        builder(context).token(forToken).space().node(node.`var`).space().token(equals).space().node(node.from).space()
            .token(toToken).space().node(node.to)
            .conditional(node.by != null, {
                it.space().token(by).space()
            }).nl()
            .token(doToken).space().node(node.statement).render

    override fun caseAIfStm(node: AIfStm, context: IRenderContext) =
        builder(context)
            .conditional(context.lineState == LineState.tokened, {
                it.nl().incIndent()
            })
            .token(ifToken).space().node(node.ifExp).nl()
            .token(then).space().node(node.thenStm)
            .conditional(node.elseIf != null && node.elseIf.isNotEmpty(), {
                it.nl().nodeList(node.elseIf, listOf(newLine))
            })
            .conditional(node.elseStm != null, {
                it.nl().token(elseToken).space().node(node.elseStm)
            })
            .render


    override fun caseAIdentifierStateDesignator(node: AIdentifierStateDesignator, context: IRenderContext) =
        builder(context).node(node.name).render

    override fun caseAIfExp(node: AIfExp, context: IRenderContext) =
        builder(context)
            .conditional(context.lineState == LineState.tokened, {
                it.nl().incIndent()
            })
            .token(ifToken).space().node(node.test).nl()
            .token(then).space().node(node.then)
            .conditional(node.elseList != null && node.elseList.isNotEmpty(), {
                it.nl().nodeList(node.elseList, listOf(newLine))
            })
            .conditional(node.`else` != null, {
                it.nl().token(elseToken).space().node(node.`else`)
            })
            .render

    override fun caseAStringPattern(node: AStringPattern, context: IRenderContext) =
        builder(context).token(doubleQuote).token(node.value.value).token(doubleQuote).render

    override fun caseAThreadIdExp(node: AThreadIdExp, context: IRenderContext) = renderToken(threadid, context)

    override fun caseAMultiBindListDefinition(node: AMultiBindListDefinition, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun defaultPImports(node: PImports, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun defaultPStateDesignator(node: PStateDesignator, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseADomainResToBinaryExp(node: ADomainResToBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseAGreaterEqualNumericBinaryExp(node: AGreaterEqualNumericBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseAStateInitExp(node: AStateInitExp, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseASeqConcatBinaryExp(node: ASeqConcatBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseANewExp(node: ANewExp, context: IRenderContext) =
        builder(context).token(new).space().node(node.className).lparens().nodeList(node.args).rparens().render

    override fun caseAAllExport(node: AAllExport, context: IRenderContext) = renderToken(all, context)

    override fun caseATuplePattern(node: ATuplePattern, context: IRenderContext) =
        builder(context).token(mk_).lparens().nodeList(node.plist).rparens().render

    override fun caseAEquivalentBooleanBinaryExp(node: AEquivalentBooleanBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun defaultSSetType(node: SSetType, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseASeqSeqType(node: ASeqSeqType, context: IRenderContext) =
        builder(context).conditional(node.empty, {
            it.token(seqLeft).token(anyType).token(seqRight)
        }, {
            it.token(seqLeft).childType(node.seqof, node).token(seqRight)
        }).render

    override fun caseLong(node: Long, context: IRenderContext) = renderToken(node.toString(), context)

    override fun caseASetIntersectBinaryExp(node: ASetIntersectBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseAQuoteType(node: AQuoteType, context: IRenderContext) =
        builder(context).token(lquote).token(node.value.value).token(rquote).render

    override fun defaultPAccess(node: PAccess, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAWhileStm(node: AWhileStm, context: IRenderContext) =
        builder(context).token(whileToken).space().node(node.exp).nl()
            .token(doToken).space().node(node.statement).render

    override fun defaultPExport(node: PExport, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAMapUnionPattern(node: AMapUnionPattern, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAFunctionValueImport(node: AFunctionValueImport, context: IRenderContext) =
        builder(context).node(node.name).conditional(node.renamed != null, {
            it.space().token(renamed).space().node(node.renamed)
        }).render

    override fun caseString(node: String, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseNameScope(node: NameScope, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAQuoteLiteralExp(node: AQuoteLiteralExp, context: IRenderContext) =
        builder(context).token(lquote).token(node.value.value).token(rquote).render

    override fun caseAConcatenationPattern(node: AConcatenationPattern, context: IRenderContext) =
        builder(context).node(node.left).space().token(concat).space().node(node.right).render

    override fun caseAFieldExp(node: AFieldExp, context: IRenderContext) =
        builder(context)
            .childExpression(node.`object`, node, RenderBuilder.Position.left)
            .token(dot).node(node.field).render

    override fun caseAEqRelation(node: AEqRelation, context: IRenderContext) =
        builder(context).node(node.lhsPattern).space().token(equals).space().node(node.rhsPattern).space()
            .token(fnEquals).space().node(node.relExp).render

    override fun caseAVoidType(node: AVoidType, context: IRenderContext) =
        builder(context).lparens().rparens().render

    override fun caseAExpressionPattern(node: AExpressionPattern, context: IRenderContext) =
        builder(context).node(node.exp).render

    override fun caseILexBooleanToken(node: ILexBooleanToken, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun defaultSSeqExp(node: SSeqExp, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAPeriodicStm(node: APeriodicStm, context: IRenderContext) =
        builder(context).token(periodic).lparens().nodeList(node.args).rparens().lparens().node(node.opname)
            .rparens().render

    override fun caseASporadicStm(node: ASporadicStm, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseASeqPattern(node: ASeqPattern, context: IRenderContext) =
        builder(context).token(lsquare).nodeList(node.plist).token(rsquare).render

    override fun caseAPatternListTypePair(node: APatternListTypePair, context: IRenderContext) =
        builder(context).nodeList(node.patterns).token(colon).space().node(node.type).render

    override fun caseAIndicesUnaryExp(node: AIndicesUnaryExp, context: IRenderContext) =
        renderSUnaryExp(node, inds, context)

    override fun caseAAllImport(node: AAllImport, context: IRenderContext) = builder(context).render

    override fun caseASpecificationStm(node: ASpecificationStm, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseANamedInvariantType(node: ANamedInvariantType, context: IRenderContext) =
        builder(context).ifTypeDef({
            it.unsetTypeDef().node(node.type)
        }, {
            it.name(node.name)
        }).render

    override fun caseAFloorUnaryExp(node: AFloorUnaryExp, context: IRenderContext) =
        renderLeftRightSUnaryExp(node, floorLeft, floorRight, context)

    override fun caseILexCharacterToken(node: ILexCharacterToken, context: IRenderContext) =
        renderToken(node.value.toString(), context)

    override fun caseASet1SetType(node: ASet1SetType, context: IRenderContext) =
        builder(context).token(set1Left).childType(node.setof, node).token(set1Right).render

    override fun defaultSNumericBasicType(node: SNumericBasicType, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAEqualsDefinition(node: AEqualsDefinition, context: IRenderContext) =
        builder(context)
            .conditional(node.pattern == null, {
                it.conditional(node.typebind == null, {
                    it.node(node.setbind)
                }, {
                    it.node(node.typebind)
                })
            }, {
                it.node(node.pattern)
            })
            .space().token(equals).space().node(node.test).render

    override fun defaultPAlternativeStm(node: PAlternativeStm, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun defaultSBasicType(node: SBasicType, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun defaultPDefinition(node: PDefinition, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseILexNameToken(node: ILexNameToken, context: IRenderContext) =
        builder(context).token(node.fullName).conditional(node.typeQualifier != null, {
            it.lparens().nodeList(node.typeQualifier).rparens()
        }).render

    override fun caseALetBeStBindingTraceDefinition(node: ALetBeStBindingTraceDefinition, context: IRenderContext) =
        builder(context).nl().incIndent().token(let).nl().incIndent()
            .node(node.bind).nl()
            .decIndent().token(inToken).nl().incIndent()
            .node(node.body).decIndent().decIndent()
            .render

    override fun caseILexLocation(node: ILexLocation, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseILexComment(node: ILexComment, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseILexCommentList(node: ILexCommentList, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAnnotation(node: Annotation, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun casePTypeSet(node: PTypeSet, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun casePass(node: Pass, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAPatternTypePair(node: APatternTypePair, context: IRenderContext) =
        builder(context).node(node.pattern).token(colon).space().node(node.type).render

    override fun caseACompBinaryExp(node: ACompBinaryExp, context: IRenderContext) = renderSBinaryExp(node, context)

    override fun defaultPPair(node: PPair, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAImplicitOperationDefinition(node: AImplicitOperationDefinition, context: IRenderContext) =
        builder(context)
            .conditional(node.access != null, {
                it.node(node.access)
            })
            .token(node.name.name).lparens().nodeList(node.parameterPatterns).rparens()
            .conditional(node.result != null, {
                it.space().node(node.result)
            })
            .conditional(node.externals.isNotEmpty(), {
                it.nl().token(ext).incIndent().nl().nodeList(node.externals, listOf(newLine)).decIndent()
            })
            .conditional(node.precondition != null, {
                it.nl().token(pre).space().node(node.precondition)
            })
            .conditional(node.postcondition != null, {
                it.nl().token(post).space().node(node.postcondition)
            })
            .conditional(node.errors.isNotEmpty(), {
                it.nl().token(errs).incIndent().nl().nodeList(node.errors, listOf(newLine)).decIndent()
            }).render

    override fun caseADivNumericBinaryExp(node: ADivNumericBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseASeqEnumSeqExp(node: ASeqEnumSeqExp, context: IRenderContext) =
        builder(context).token(lsquare).nodeList(node.members).token(rsquare).render

    override fun caseADistMergeUnaryExp(node: ADistMergeUnaryExp, context: IRenderContext) =
        renderSUnaryExp(node, merge, context)

    override fun defaultSOperationDefinition(node: SOperationDefinition, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseANatOneNumericBasicType(node: ANatOneNumericBasicType, context: IRenderContext) =
        renderToken(nat1, context)

    override fun caseABooleanPattern(node: ABooleanPattern, context: IRenderContext) =
        renderToken(node.value.value.toString(), context)

    override fun caseAModuleExports(node: AModuleExports, context: IRenderContext): String {
        fun processExports(section: RenderToken, clazz: Class<*>): (RenderBuilder) -> RenderBuilder {
            val exports = node.exports.flatten().filter { clazz.isAssignableFrom(it.javaClass) }
            return if (exports.isEmpty()) {
                { it }
            } else {
                { builder ->
                    builder.token(section).conditional(clazz != AAllExport::class.java, {
                        it.incIndent().nl()
                            .nodeList(exports, listOf(newLine)).decIndent().nl()
                    })
                }
            }
        }
        return builder(context).incIndent()
            .apply(processExports(all, AAllExport::class.java))
            .apply(processExports(importFunctions, AFunctionExport::class.java))
            .apply(processExports(importOperations, AOperationExport::class.java))
            .apply(processExports(importValues, AValueExport::class.java))
            .apply(processExports(importTypes, ATypeExport::class.java))
            .decIndent().render
    }


    override fun defaultPMultipleBind(node: PMultipleBind, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseARecordPattern(node: ARecordPattern, context: IRenderContext) =
        builder(context).token(mk_).name(node.typename).lparens().nodeList(node.plist).rparens().render

    override fun caseACyclesStm(node: ACyclesStm, context: IRenderContext) =
        builder(context).token(cycles).space().lparens().node(node.cycles).rparens().space().node(node.statement).render

    override fun caseADistConcatUnaryExp(node: ADistConcatUnaryExp, context: IRenderContext) =
        renderSUnaryExp(node, conc, context)

    override fun caseADivideNumericBinaryExp(node: ADivideNumericBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseAClassInvariantStm(node: AClassInvariantStm, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseASelfObjectDesignator(node: ASelfObjectDesignator, context: IRenderContext) =
        builder(context).node(node.self).render

    override fun caseACasesStm(node: ACasesStm, context: IRenderContext) =
        builder(context).conditional(context.lineState == LineState.tokened, {
            it.nl().incIndent()
        })
            .token(cases).space().node(node.exp).token(colon).nl().incIndent()
            .nodeList(node.cases, listOf(comma, newLine))
            .conditional(node.others != null, {
                it.token(comma).nl().token(others).space().token(partialFunction).space().node(node.others)
            }).nl().decIndent()
            .token(end).render

    override fun caseALenUnaryExp(node: ALenUnaryExp, context: IRenderContext) =
        renderSUnaryExp(node, len, context)

    override fun defaultSBooleanBinaryExp(node: SBooleanBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseANotYetSpecifiedExp(node: ANotYetSpecifiedExp, context: IRenderContext) =
        renderToken(isNotYetSpecified, context)

    override fun caseASystemClassDefinition(node: ASystemClassDefinition, context: IRenderContext) =
        renderClass(node, NavigationMarker.system, context)

    override fun caseAParameterType(node: AParameterType, context: IRenderContext) =
        builder(context).token(paramType).node(node.name).render

    override fun caseATraceDefinitionTerm(node: ATraceDefinitionTerm, context: IRenderContext) =
        builder(context).nodeList(node.list, listOf(space, pipe, space)).render

    override fun defaultPPattern(node: PPattern, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseACaseAlternativeStm(node: ACaseAlternativeStm, context: IRenderContext) =
        builder(context)
            .conditional(node.pattern is AExpressionPattern, {
                it.lparens()
            })
            .node(node.pattern)
            .conditional(node.pattern is AExpressionPattern, {
                it.rparens()
            })
            .space().token(partialFunction).space().node(node.result).render

    override fun caseAPerSyncDefinition(node: APerSyncDefinition, context: IRenderContext) =
        builder(context).token(per).space().node(node.opname).space().token(implies).space().node(node.guard).render

    override fun caseAAlwaysStm(node: AAlwaysStm, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAClassClassDefinition(node: AClassClassDefinition, context: IRenderContext): String {
        return renderClass(node, NavigationMarker.classMarker, context)
    }

    private fun renderClass(
        node: SClassDefinitionBase,
        classNavMarker: NavigationMarker,
        context: IRenderContext
    ): String {
        val sectionTypes = mapOf(
            ATypeDefinition::class.java to NavigationMarker.types,
            AValueDefinition::class.java to NavigationMarker.values,
            SFunctionDefinition::class.java to NavigationMarker.functions,
            SOperationDefinition::class.java to NavigationMarker.operations,
            AInstanceVariableDefinition::class.java to NavigationMarker.instanceVariables,
            AClassInvariantDefinition::class.java to NavigationMarker.instanceVariables,
            AMutexSyncDefinition::class.java to NavigationMarker.sync,
            APerSyncDefinition::class.java to NavigationMarker.sync,
            AThreadDefinition::class.java to NavigationMarker.thread,
            ANamedTraceDefinition::class.java to NavigationMarker.traces
        )

        fun processDefsOfType(section: NavigationMarker, defs: List<PDefinition>): (RenderBuilder) -> RenderBuilder {
            return { builder ->
                builder.token(section.token as RenderToken).addNavigationMarks(section).nl().vspace().incIndent()
                    .nodeList(defs, listOf(semiColon, newLine, newLine), { node ->
                        if (node is PDefinitionBase && node.name != null) {
                            NavigationMarker(node.name.simpleName)
                        } else {
                            null
                        }
                    }).nl().vspace().decIndent()
            }
        }

        fun <T, K> Iterable<T>.groups(keySelector: (T) -> K): List<Pair<K, List<T>>> {
            val groups = mutableListOf<Pair<K, MutableList<T>>>()
            this.forEach { t ->
                val key = keySelector(t)
                if (groups.isEmpty() || groups.last().first != key) {
                    groups.add(Pair(key, mutableListOf<T>(t)))
                } else {
                    groups.last().second.add(t)
                }
            }
            return groups
        }

        fun processDefs() =
            node.definitions.groups { def ->
                val definitionClazz = sectionTypes.keys.find { st -> st.isAssignableFrom(def.javaClass) }
                sectionTypes.get(definitionClazz)
                    ?: throw IllegalArgumentException("Unexpected definition type ${def.javaClass}")
            }.map { def ->
                { b: RenderBuilder -> b.apply(processDefsOfType(def.first, def.second)) }
            }

        return builder(context).setCurrentContainer(node)
            .token(classNavMarker.token as RenderToken).space().token(node.name.name)
            .conditional(node.supertypes.isNotEmpty(), {
                it.space().token(isSubclassOf).space().nodeList(node.supertypes)
            }).addNavigationMarks(classNavMarker).nl().vspace()
            .applyAll(processDefs()).nl()
            .token(end).space().token(node.name.name)
            .render
    }

    override fun caseADomainResByBinaryExp(node: ADomainResByBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseASetEnumSetExp(node: ASetEnumSetExp, context: IRenderContext) =
        builder(context).conditional(node.members.isEmpty(), {
            it.token(emptySet)
        }, {
            it.token(lbrace).nodeList(node.members).token(rbrace)
        }).render

    override fun caseAMapMapType(node: AMapMapType, context: IRenderContext) =
        builder(context).token(map).space().node(node.from).space().token(mapTo).space().childType(node.to, node).render

    override fun caseALessEqualNumericBinaryExp(node: ALessEqualNumericBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseALetDefBindingTraceDefinition(node: ALetDefBindingTraceDefinition, context: IRenderContext) =
        builder(context).nl().incIndent().token(let).nl().incIndent()
            .nodeList(node.localDefs, listOf(comma, newLine)).nl()
            .decIndent().token(inToken).nl().incIndent()
            .node(node.body).decIndent().decIndent()
            .render

    override fun caseAApplyObjectDesignator(node: AApplyObjectDesignator, context: IRenderContext) =
        builder(context).node(node.`object`).lparens().nodeList(node.args).rparens().render

    override fun caseAApplyExp(node: AApplyExp, context: IRenderContext) =
        builder(context)
            .childExpression(node.root, node, RenderBuilder.Position.left)
            .lparens().nodeList(node.args).rparens().render

    override fun caseAMapInverseUnaryExp(node: AMapInverseUnaryExp, context: IRenderContext) =
        renderLeftRightSUnaryExp(node, inverseLeft, inverseRight, context)

    override fun defaultPMaplet(node: PMaplet, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAInMapMapType(node: AInMapMapType, context: IRenderContext) =
        builder(context).token(inmap).space().node(node.from).space().token(inmapTo).space()
            .childType(node.to, node).render

    override fun caseAStringLiteralExp(node: AStringLiteralExp, context: IRenderContext) =
        builder(context).token(doubleQuote).token(node.value.value).token(doubleQuote).render

    override fun caseASubclassResponsibilityStm(node: ASubclassResponsibilityStm, context: IRenderContext) =
        renderToken(isSubclassResponsibility, context)

    override fun caseAIgnorePattern(node: AIgnorePattern, context: IRenderContext) = renderToken(minus, context)

    override fun caseClonableFile(node: ClonableFile, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseTStatic(node: TStatic, context: IRenderContext) = renderToken(static, context)

    override fun caseADistIntersectUnaryExp(node: ADistIntersectUnaryExp, context: IRenderContext) =
        renderSUnaryExp(node, dinter, context)

    override fun caseAExplicitOperationDefinition(node: AExplicitOperationDefinition, context: IRenderContext) =
        builder(context)
            .conditional(node.access != null, {
                it.node(node.access)
            })
            .token(node.name.name).token(colon).space().node(node.type).nl()
            .token(node.name.name).lparens().nodeList(node.parameterPatterns).rparens().space().token(fnEquals)
            .incIndent()
            .nl()
            .node(node.body).decIndent()
            .conditional(node.precondition != null, {
                it.nl().token(pre).space().node(node.precondition)
            })
            .conditional(node.postcondition != null, {
                it.nl().token(post).space().node(node.postcondition)
            }).render

    override fun caseASelfExp(node: ASelfExp, context: IRenderContext) = renderToken(self, context)

    override fun caseAInstanceVariableDefinition(node: AInstanceVariableDefinition, context: IRenderContext) =
        builder(context)
            .conditional(node.access != null, {
                it.node(node.access)
            })
            .node(node.name).token(colon).space().node(node.type)
            .conditional(node.expression !is AUndefinedExp, {
                it.space().token(assign).space().node(node.expression)
            }).render

    override fun defaultPCase(node: PCase, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun defaultPAnnotation(node: PAnnotation, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseAAnnotationAnnotation(node: AAnnotationAnnotation, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAPrivateAccess(node: APrivateAccess, context: IRenderContext) =
        throw IllegalStateException("Private access is not printed as it is default")

    override fun defaultSFunctionDefinition(node: SFunctionDefinition, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun defaultPRelation(node: PRelation, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun caseACasesExp(node: ACasesExp, context: IRenderContext) =
        builder(context)
            .conditional(context.lineState == LineState.tokened, {
                it.nl().incIndent()
            })
            .token(cases).space().node(node.expression).token(colon).nl().incIndent()
            .nodeList(node.cases, listOf(comma, newLine))
            .conditional(node.others != null, {
                it.token(comma).nl().token(others).space().token(partialFunction).space().node(node.others)
            }).nl().decIndent()
            .token(end).render

    override fun caseABracketedExpressionTraceCoreDefinition(
        node: ABracketedExpressionTraceCoreDefinition,
        context: IRenderContext
    ) =
        builder(context).lparens().incIndent().nl()
            .nodeList(node.terms, listOf(semiColon, newLine)).decIndent().nl()
            .rparens().render

    override fun caseANotYetSpecifiedStm(node: ANotYetSpecifiedStm, context: IRenderContext) =
        renderToken(isNotYetSpecified, context)

    override fun caseANotInSetBinaryExp(node: ANotInSetBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseAModNumericBinaryExp(node: AModNumericBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseACallObjectStm(node: ACallObjectStm, context: IRenderContext) =
        builder(context).node(node.designator).token(dot)
            .conditional(node.classname == null, {
                it.node(node.fieldname)
            }, {
                it.node(node.classname)
            })
            .lparens().nodeList(node.args).rparens().render

    override fun caseAPublicAccess(node: APublicAccess, context: IRenderContext) = renderToken(publicToken, context)

    override fun caseARemNumericBinaryExp(node: ARemNumericBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseBoolean(node: Boolean, context: IRenderContext) = renderToken(node.toString(), context)

    override fun caseATimesNumericBinaryExp(node: ATimesNumericBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseASameClassExp(node: ASameClassExp, context: IRenderContext) = unhandled(node.javaClass, context)

    override fun defaultSInvariantType(node: SInvariantType, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAIdentifierPattern(node: AIdentifierPattern, context: IRenderContext) =
        renderToken(node.name.name, context)

    override fun caseAFunctionExport(node: AFunctionExport, context: IRenderContext) =
        builder(context).nodeList(node.nameList).conditional(node.typeParams != null && node.typeParams.isNotEmpty(), {
            it.token(lsquare).token(paramType).nodeList(node.typeParams, listOf(comma, space, paramType)).token(rsquare)
        }).token(colon).space().node(node.exportType).render

    override fun caseALetBeStExp(node: ALetBeStExp, context: IRenderContext) =
        builder(context)
            .conditional(context.lineState == LineState.tokened, {
                it.nl().incIndent()
            })
            .token(let).space().node(node.bind)
            .conditional(node.suchThat != null, {
                it.space().token(be).space().token(st).nl().incIndent().node(node.suchThat).decIndent()
            })
            .nl().token(inToken).space().node(node.value).render

    override fun caseACaseAlternative(node: ACaseAlternative, context: IRenderContext) =
        builder(context).conditional(node.pattern is AExpressionPattern, {
            it.lparens()
        })
            .node(node.pattern)
            .conditional(node.pattern is AExpressionPattern, {
                it.rparens()
            })
            .space().token(partialFunction).space().node(node.result).render

    override fun caseAFieldField(node: AFieldField, context: IRenderContext) =
        builder(context)
            // TODO - is there a better way to determine if this is anonymous?
            .conditional(node.tag.toIntOrNull() == null, {
                it.node(node.tagname).space().conditional(node.equalityAbstraction, {
                    it.token(abstraction)
                }, {
                    it.token(colon)
                }).space()
            }).node(node.type).render

    override fun caseASeqMultipleBind(node: ASeqMultipleBind, context: IRenderContext) =
        builder(context).nodeList(node.plist).space().token(inSeq).space().node(node.seq).render

    override fun caseATixeStmtAlternative(node: ATixeStmtAlternative, context: IRenderContext) =
        unhandled(node.javaClass, context)

    override fun caseAOrdRelation(node: AOrdRelation, context: IRenderContext) =
        builder(context).node(node.lhsPattern).space().token(lessThan).space().node(node.rhsPattern).space()
            .token(fnEquals).space().node(node.relExp).render

    override fun caseAReturnStm(node: AReturnStm, context: IRenderContext) =
        builder(context).token(returnToken).conditional(node.expression != null, {
            it.space().node(node.expression)
        }).render

    override fun caseASeqBind(node: ASeqBind, context: IRenderContext) =
        builder(context).node(node.pattern).space().token(inSeq).space().node(node.seq).render

    override fun caseARangeResToBinaryExp(node: ARangeResToBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseAGreaterNumericBinaryExp(node: AGreaterNumericBinaryExp, context: IRenderContext) =
        renderSBinaryExp(node, context)

    override fun caseInteger(node: Int, context: IRenderContext) = renderToken(node.toString(), context)

    override fun caseAUnionPattern(node: AUnionPattern, context: IRenderContext) =
        builder(context).node(node.left).space().token(union).space().node(node.right).render

    // TODO - Overture creates define statements as let statements.
    // Branch issue683-addExplicitDefineStatements adds an ADefStm to the AST that enables us to disambiguate
    // see https://github.com/overturetool/overture/issues/683

    // override fun caseADefStm(node: ADefStm): String = node.apply(this, DefaultRenderContext())

    // override fun caseADefStm(node: ADefStm, context: IRenderContext) =
    //    builder(context). //
    //            conditional(context.lineState == LineState.tokened, {
    //                it.nl().incIndent()
    //            }). //
    //            token(def).nl().incIndent().//
    //            nodeList(node.localDefs, listOf(semiColon, newLine)).nl(). //
    //            decIndent().token(inToken).nl().incIndent(). //
    //            node(node.statement).build()

}


